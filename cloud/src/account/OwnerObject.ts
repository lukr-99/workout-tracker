import { DurableObject } from "cloudflare:workers";
import { newId } from "../crypto/Ids";
import { systemRandomBytes } from "../crypto/RandomBytes";
import { type ExerciseInput, Proposals, type ProposalResult, type ProposalStatus, type TemplateInput } from "../proposals/Proposals";
import { FcmSender, type PushResult } from "../push/FcmSender";
import { FcmServiceAccount } from "../push/FcmServiceAccount";
import { LocalDay } from "../workouts/LocalDay";
import { type RunFilter, RunReader } from "../workouts/RunReader";
import { SharedBundleParser } from "../workouts/SharedBundle";
import { type ShareCounts, SharedStore } from "../workouts/SharedStore";
import { type ExerciseFilter, type VolumeQuery, type WorkoutFilter, WorkoutReader } from "../workouts/WorkoutReader";
import { ACCOUNT_MIGRATIONS } from "./accountMigrations";
import { MigrationRunner } from "./MigrationRunner";

/** How long the phone has to answer a share request before it lapses. */
const REQUEST_LIFETIME_MS = 10 * 60 * 1000;
const REQUEST_HISTORY_MS = 7 * 24 * 60 * 60 * 1000;

export type Platform = "android" | "connector";

export interface DeviceView {
  id: string;
  name: string;
  platform: Platform;
  scopes: string[];
  hasPush: boolean;
  createdAt: string;
  lastSeenAt: string | null;
  revokedAt: string | null;
}

/** What a read tool learns when it asks for the phone's data. */
export type ShareOutcome =
  | { status: "fresh"; sharedAt: string; keepUntil: string; timeZone: string; units: "metric" | "imperial" }
  | { status: "declined" }
  | { status: "waiting"; requestId: string; push: "sent" | "off" | "failed" | "earlier" }
  | { status: "no_phone" };

export type ShareResult = { ok: true; sharedAt: string; keepUntil: string; counts: ShareCounts } | { ok: false; status: number; code: string; message: string; details?: string[] };

type DeviceRow = {
  id: string;
  name: string;
  platform: Platform;
  scopes: string;
  push_token: string | null;
  created_at: string;
  last_seen_at: string | null;
  revoked_at: string | null;
}

type Answer = "shared" | "declined";

const now = () => new Date().toISOString();

/**
 * The owner's one Durable Object (docs/claude-connector.md): the phone and the Claude connector as devices,
 * connector codes, share requests, the short-lived copy of the phone's training data, and Claude's
 * proposals. A read tool that finds no fresh copy opens a share request, wakes the phone by FCM, and
 * waits a little for the answer; the phone's share or decline settles every waiting call.
 */
export class OwnerObject extends DurableObject<Env> {
  private readonly shared = new SharedStore(this.ctx.storage.sql);
  private readonly reader = new WorkoutReader(this.ctx.storage.sql);
  private readonly runReader = new RunReader(this.ctx.storage.sql);
  private readonly proposals = new Proposals(this.ctx.storage.sql, this.reader, () => newId(systemRandomBytes), now);
  private readonly waiters = new Map<string, Set<(answer: Answer) => void>>();
  private sender: FcmSender | null | undefined;

  constructor(ctx: DurableObjectState, env: Env) {
    super(ctx, env);
    ctx.blockConcurrencyWhile(async () => {
      new MigrationRunner(ctx.storage).apply(ACCOUNT_MIGRATIONS, now);
    });
  }

  // Devices

  /** Registers the phone. A phone set up again (a reinstall) replaces the previous one. */
  setupPhone(deviceId: string, name: string, tokenHash: string): DeviceView {
    const at = now();
    this.ctx.storage.transactionSync(() => {
      this.ctx.storage.sql.exec("UPDATE devices SET revoked_at = ? WHERE platform = 'android' AND revoked_at IS NULL", at);
      this.ctx.storage.sql.exec(
        "INSERT INTO devices (id, name, platform, token_hash, created_at) VALUES (?, ?, 'android', ?, ?)",
        deviceId,
        name,
        tokenHash,
        at,
      );
    });
    return this.device(deviceId)!;
  }

  /** The active device holding this token hash, or null. */
  authenticate(tokenHash: string): DeviceView | null {
    const row = this.ctx.storage.sql.exec<DeviceRow>("SELECT * FROM devices WHERE token_hash = ? AND revoked_at IS NULL", tokenHash).toArray()[0];
    if (!row) return null;
    this.ctx.storage.sql.exec("UPDATE devices SET last_seen_at = ? WHERE id = ?", now(), row.id);
    return deviceView(row);
  }

  setPushToken(deviceId: string, pushToken: string | null): void {
    this.ctx.storage.sql.exec("UPDATE devices SET push_token = ? WHERE id = ?", pushToken, deviceId);
  }

  devices(): DeviceView[] {
    return this.ctx.storage.sql.exec<DeviceRow>("SELECT * FROM devices ORDER BY created_at").toArray().map(deviceView);
  }

  revokeDevice(id: string): boolean {
    return this.ctx.storage.sql.exec("UPDATE devices SET revoked_at = ? WHERE id = ? AND revoked_at IS NULL", now(), id).rowsWritten > 0;
  }

  createConnectorCode(codeHash: string, expiresAt: string): void {
    this.ctx.storage.sql.exec("DELETE FROM connector_codes WHERE expires_at <= ?", now());
    this.ctx.storage.sql.exec("INSERT OR REPLACE INTO connector_codes (code_hash, expires_at) VALUES (?, ?)", codeHash, expiresAt);
  }

  /** Uses a connector code up; true when it existed and had not run out. */
  redeemConnectorCode(codeHash: string): boolean {
    const row = this.ctx.storage.sql.exec<{ expires_at: string }>("SELECT expires_at FROM connector_codes WHERE code_hash = ?", codeHash).toArray()[0];
    if (!row) return false;
    this.ctx.storage.sql.exec("DELETE FROM connector_codes WHERE code_hash = ?", codeHash);
    return row.expires_at > now();
  }

  addConnector(deviceId: string, name: string, scopes: string[], tokenHash: string): DeviceView {
    this.ctx.storage.sql.exec(
      "INSERT INTO devices (id, name, platform, token_hash, scopes, created_at) VALUES (?, ?, 'connector', ?, ?, ?)",
      deviceId,
      name,
      tokenHash,
      JSON.stringify(scopes),
      now(),
    );
    return this.device(deviceId)!;
  }

  /** The active connector device, or null once the owner revoked it. */
  connector(deviceId: string): DeviceView | null {
    const device = this.device(deviceId);
    return device && device.platform === "connector" && device.revokedAt === null ? device : null;
  }

  // Sharing

  /**
   * Makes sure Claude can read: a fresh copy answers at once; otherwise the phone is asked (once per
   * open request) and the call waits up to SHARE_WAIT_SECONDS for its answer.
   */
  async requestShare(): Promise<ShareOutcome> {
    const fresh = this.shared.state(now());
    if (fresh) return { status: "fresh", ...fresh };
    const phone = this.phone();
    if (!phone) return { status: "no_phone" };
    let request = this.openRequest();
    let push: "sent" | "off" | "failed" | "earlier" = "earlier";
    if (!request) {
      const at = Date.now();
      request = { id: newId(systemRandomBytes), expiresAt: new Date(at + REQUEST_LIFETIME_MS).toISOString() };
      this.ctx.storage.sql.exec(
        "INSERT INTO share_requests (id, device_id, created_at, expires_at, status) VALUES (?, ?, ?, ?, 'pending')",
        request.id,
        phone.id,
        new Date(at).toISOString(),
        request.expiresAt,
      );
      await this.reschedule();
      const pushed = await this.push(phone.id, { t: "share", r: request.id }, REQUEST_LIFETIME_MS / 1000);
      push = pushed === "sent" ? "sent" : pushed === "off" ? "off" : "failed";
    }
    const answer = await this.wait(request.id, Number(this.env.SHARE_WAIT_SECONDS ?? "25") * 1000);
    if (answer === "declined") return { status: "declined" };
    const shared = this.shared.state(now());
    if (answer === "shared" && shared) return { status: "fresh", ...shared };
    return { status: "waiting", requestId: request.id, push };
  }

  /** Share requests the phone has not answered yet, for an app that missed the push. */
  openRequests(): { id: string; createdAt: string; expiresAt: string }[] {
    return this.ctx.storage.sql
      .exec<{ id: string; created_at: string; expires_at: string }>(
        "SELECT id, created_at, expires_at FROM share_requests WHERE status = 'pending' AND expires_at > ? ORDER BY created_at",
        now(),
      )
      .toArray()
      .map((row) => ({ id: row.id, createdAt: row.created_at, expiresAt: row.expires_at }));
  }

  /**
   * Stores the phone's data, answering [requestId] or, with null, shared by the owner unasked. It
   * stays for SHARE_KEEP_MINUTES and is then deleted.
   */
  async share(requestId: string | null, body: unknown): Promise<ShareResult> {
    if (requestId !== null) {
      const refusal = this.answerable(requestId);
      if (refusal) return refusal;
    }
    const parsed = SharedBundleParser.parse(body);
    if (!parsed.ok) return { ok: false, status: 400, code: "invalid", message: `${parsed.problems.length} problem(s) in the share.`, details: parsed.problems };
    const at = Date.now();
    const sharedAt = new Date(at).toISOString();
    const keepUntil = new Date(at + Number(this.env.SHARE_KEEP_MINUTES ?? "30") * 60 * 1000).toISOString();
    const counts = this.ctx.storage.transactionSync(() => {
      const written = this.shared.replace(parsed.bundle, requestId, sharedAt, keepUntil);
      this.ctx.storage.sql.exec("UPDATE share_requests SET status = 'shared', answered_at = ? WHERE status = 'pending'", sharedAt);
      return written;
    });
    await this.reschedule();
    for (const id of [...this.waiters.keys()]) this.settle(id, "shared");
    return { ok: true, sharedAt, keepUntil, counts };
  }

  async decline(requestId: string): Promise<ShareResult | null> {
    const refusal = this.answerable(requestId);
    if (refusal) return refusal;
    this.ctx.storage.sql.exec("UPDATE share_requests SET status = 'declined', answered_at = ? WHERE id = ?", now(), requestId);
    await this.reschedule();
    this.settle(requestId, "declined");
    return null;
  }

  /** The current copy, for the app's Settings: when it was shared and until when it stays. */
  shareStatus(): { sharedAt: string; keepUntil: string } | null {
    const state = this.shared.state(now());
    return state ? { sharedAt: state.sharedAt, keepUntil: state.keepUntil } : null;
  }

  /** The owner's "stop sharing now": deletes the copy before it runs out. */
  async forgetShare(): Promise<void> {
    this.shared.purge();
    await this.reschedule();
  }

  // Reading the copy (call requestShare first)

  overview() {
    const state = this.shared.state(now());
    if (!state) return null;
    const today = LocalDay.of(Date.now(), state.timeZone);
    return {
      today,
      ownerUnits: state.units === "imperial" ? "pounds and miles" : "kilograms and kilometres",
      sharedAt: state.sharedAt,
      keptUntil: state.keepUntil,
      appVersion: state.appVersion,
      training: this.reader.overview(today),
      running: this.runReader.overview(today, 8),
    };
  }

  today(): string {
    return LocalDay.of(Date.now(), this.shared.state(now())?.timeZone ?? "UTC");
  }

  workouts(filter: WorkoutFilter) {
    return this.reader.workouts(filter);
  }

  workout(id: string) {
    return this.reader.workout(id);
  }

  exercises(filter: ExerciseFilter) {
    return this.reader.exercises(filter);
  }

  exerciseProgress(exerciseId: string, from: string | null, to: string | null, limit: number) {
    return this.reader.exerciseProgress(exerciseId, from, to, limit);
  }

  volume(query: VolumeQuery) {
    return this.reader.volume(query);
  }

  templates() {
    return this.reader.templates();
  }

  runs(filter: RunFilter) {
    return this.runReader.runs(filter);
  }

  run(id: string) {
    return this.runReader.run(id);
  }

  runningOverview(weeks: number) {
    return this.runReader.overview(this.today(), weeks);
  }

  // Proposals

  listProposals(status: ProposalStatus | null, limit: number) {
    return this.proposals.list(status, limit);
  }

  async proposeExercise(input: ExerciseInput, deviceId: string): Promise<ProposalResult> {
    return this.notifyInbox(this.proposals.proposeExercise(input, deviceId));
  }

  async proposeTemplate(input: TemplateInput, deviceId: string): Promise<ProposalResult> {
    return this.notifyInbox(this.proposals.proposeTemplate(input, deviceId));
  }

  withdrawProposal(id: string): ProposalResult {
    return this.proposals.withdraw(id);
  }

  decideProposal(id: string, decision: "accepted" | "rejected"): ProposalResult {
    return this.proposals.decide(id, decision);
  }

  // Housekeeping

  /** Deletes a copy that ran out, lapses unanswered requests, and forgets old ones. */
  override async alarm(): Promise<void> {
    const at = now();
    const keepUntil = this.shared.keepUntil();
    if (keepUntil !== null && keepUntil <= at) this.shared.purge();
    this.ctx.storage.sql.exec("UPDATE share_requests SET status = 'expired' WHERE status = 'pending' AND expires_at <= ?", at);
    this.ctx.storage.sql.exec("DELETE FROM share_requests WHERE created_at < ?", new Date(Date.now() - REQUEST_HISTORY_MS).toISOString());
    this.ctx.storage.sql.exec("DELETE FROM connector_codes WHERE expires_at <= ?", at);
    await this.reschedule();
  }

  private async reschedule(): Promise<void> {
    const times = [
      this.shared.keepUntil(),
      this.ctx.storage.sql.exec<{ at: string | null }>("SELECT MIN(expires_at) AS at FROM share_requests WHERE status = 'pending'").one().at,
    ].filter((time): time is string => time !== null);
    if (times.length === 0) {
      await this.ctx.storage.deleteAlarm();
      return;
    }
    await this.ctx.storage.setAlarm(Date.parse(times.sort()[0]!));
  }

  private answerable(requestId: string): ShareResult | null {
    const row = this.ctx.storage.sql
      .exec<{ status: string; expires_at: string }>("SELECT status, expires_at FROM share_requests WHERE id = ?", requestId)
      .toArray()[0];
    if (!row) return { ok: false, status: 404, code: "not_found", message: "No share request has that id." };
    if (row.status !== "pending" || row.expires_at <= now()) {
      return { ok: false, status: 409, code: "expired", message: "That request was answered or ran out. Claude asks again when it needs the data." };
    }
    return null;
  }

  private openRequest(): { id: string; expiresAt: string } | null {
    const row = this.ctx.storage.sql
      .exec<{ id: string; expires_at: string }>(
        "SELECT id, expires_at FROM share_requests WHERE status = 'pending' AND expires_at > ? ORDER BY created_at DESC LIMIT 1",
        now(),
      )
      .toArray()[0];
    return row ? { id: row.id, expiresAt: row.expires_at } : null;
  }

  private wait(requestId: string, ms: number): Promise<Answer | "timeout"> {
    return new Promise((resolve) => {
      const waiting = this.waiters.get(requestId) ?? new Set<(answer: Answer) => void>();
      this.waiters.set(requestId, waiting);
      const done = (answer: Answer) => {
        clearTimeout(timer);
        resolve(answer);
      };
      const timer = setTimeout(() => {
        waiting.delete(done);
        if (waiting.size === 0) this.waiters.delete(requestId);
        resolve("timeout");
      }, ms);
      waiting.add(done);
    });
  }

  private settle(requestId: string, answer: Answer): void {
    for (const done of this.waiters.get(requestId) ?? []) done(answer);
    this.waiters.delete(requestId);
  }

  private async notifyInbox(result: ProposalResult): Promise<ProposalResult> {
    const phone = this.phone();
    if (result.ok && !result.repeated && phone) await this.push(phone.id, { t: "inbox" }, 24 * 60 * 60);
    return result;
  }

  /** A content-free FCM message to the phone; "off" when no Firebase key or push token is set. */
  private async push(deviceId: string, data: Record<string, string>, ttlSeconds: number): Promise<PushResult | "off"> {
    const token = this.ctx.storage.sql.exec<{ push_token: string | null }>("SELECT push_token FROM devices WHERE id = ?", deviceId).toArray()[0]?.push_token;
    if (this.sender === undefined) {
      const account = FcmServiceAccount.parse(this.env.FCM_SERVICE_ACCOUNT);
      this.sender = account ? new FcmSender(account, fetch.bind(globalThis), Date.now) : null;
    }
    if (!token || !this.sender) return "off";
    try {
      const result = await this.sender.send(token, data, ttlSeconds);
      if (result === "unregistered") this.setPushToken(deviceId, null);
      return result;
    } catch (error) {
      console.error("FCM push failed", error);
      return "failed";
    }
  }

  private phone(): DeviceView | null {
    const row = this.ctx.storage.sql.exec<DeviceRow>("SELECT * FROM devices WHERE platform = 'android' AND revoked_at IS NULL ORDER BY created_at DESC LIMIT 1").toArray()[0];
    return row ? deviceView(row) : null;
  }

  private device(id: string): DeviceView | null {
    const row = this.ctx.storage.sql.exec<DeviceRow>("SELECT * FROM devices WHERE id = ?", id).toArray()[0];
    return row ? deviceView(row) : null;
  }
}

function deviceView(row: DeviceRow): DeviceView {
  return {
    id: row.id,
    name: row.name,
    platform: row.platform,
    scopes: JSON.parse(row.scopes) as string[],
    hasPush: row.push_token !== null,
    createdAt: row.created_at,
    lastSeenAt: row.last_seen_at,
    revokedAt: row.revoked_at,
  };
}
