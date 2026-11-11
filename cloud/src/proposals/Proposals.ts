import type { Category } from "../workouts/SharedBundle";
import type { WorkoutReader } from "../workouts/WorkoutReader";

export type ProposalKind = "exercise" | "template";
export type ProposalStatus = "pending" | "accepted" | "rejected" | "withdrawn";

/** A new exercise for the catalog, with the id it gets in the app when the owner accepts it. */
export interface ExercisePayload {
  exerciseId: string;
  name: string;
  category: Category;
  primaryBodyPart: string;
  secondaryBodyParts: string[];
  equipment: string;
  /** The owner's note on the exercise. */
  notes: string;
  /** How to do it, one step per line (Ember's "Steps"). */
  instructions: string;
  defaultRestSeconds: number | null;
}

/** The plan a template sets up for one exercise (CONTEXT.md, "Plan"); every part is optional. */
export interface PlanFields {
  targetSets: number | null;
  repsMin: number | null;
  repsMax: number | null;
  restSeconds: number | null;
  supersetGroup: number | null;
}

/** A new template, or a new version of one (replacesTemplateId), with each exercise resolved. */
export interface TemplatePayload {
  templateId: string;
  replacesTemplateId: string | null;
  name: string;
  notes: string;
  exercises: (PlanFields & {
    exerciseId: string;
    name: string;
    category: string;
    bodyPart: string;
    notes: string;
    /** Set when the exercise is itself a proposal; the app creates it with the template. */
    newExercise: ExercisePayload | null;
  })[];
}

export interface ProposalView {
  id: string;
  kind: ProposalKind;
  status: ProposalStatus;
  title: string;
  reason: string;
  payload: ExercisePayload | TemplatePayload;
  createdAt: string;
  decidedAt: string | null;
}

/** The same-named exercise behind a duplicate refusal. */
export interface SameExercise {
  exerciseId: string;
  name: string;
  archived: boolean;
  pending: boolean;
}

export type ProposalResult =
  | { ok: true; proposal: ProposalView; repeated: boolean }
  | { ok: false; code: string; message: string; details?: string[] | SameExercise };

export interface ExerciseInput {
  name: string;
  category: Category;
  primaryBodyPart: string;
  secondaryBodyParts?: string[];
  equipment?: string;
  notes?: string;
  instructions?: string;
  defaultRestSeconds?: number;
  reason: string;
  clientRef: string;
}

export interface TemplateInput {
  name: string;
  notes?: string;
  exercises: ({ exerciseId: string; notes?: string } & Partial<Record<keyof PlanFields, number>>)[];
  replacesTemplateId?: string;
  reason: string;
  clientRef: string;
}

type ProposalRow = {
  id: string;
  kind: ProposalKind;
  status: ProposalStatus;
  title: string;
  reason: string;
  payload: string;
  client_ref: string;
  created_at: string;
  decided_at: string | null;
}

/**
 * Claude's proposals (docs/claude-connector.md, "The inbox"): new exercises and templates that wait in the
 * app until the owner accepts or rejects them. Claude can never change the owner's data directly; it
 * can only withdraw its own pending proposal. Checks run against the shared copy, which the tools
 * make fresh first.
 */
export class Proposals {
  constructor(
    private readonly sql: SqlStorage,
    private readonly reader: WorkoutReader,
    private readonly newId: () => string,
    private readonly now: () => string,
  ) {}

  list(status: ProposalStatus | null, limit: number): ProposalView[] {
    const rows = status
      ? this.sql.exec<ProposalRow>("SELECT * FROM proposals WHERE status = ? ORDER BY created_at DESC LIMIT ?", status, limit)
      : this.sql.exec<ProposalRow>("SELECT * FROM proposals ORDER BY created_at DESC LIMIT ?", limit);
    return rows.toArray().map(view);
  }

  get(id: string): ProposalView | null {
    const row = this.sql.exec<ProposalRow>("SELECT * FROM proposals WHERE id = ?", id).toArray()[0];
    return row ? view(row) : null;
  }

  proposeExercise(input: ExerciseInput, deviceId: string): ProposalResult {
    const repeat = this.repeat(input.clientRef, "exercise");
    if (repeat) return repeat;
    const name = input.name.trim();
    const same = this.sameExercise(name);
    if (same) {
      return {
        ok: false,
        code: "duplicate",
        message: same.archived
          ? `The owner has "${same.name}" archived. Ask them to bring it back instead.`
          : `"${same.name}" is already ${same.pending ? "proposed and waiting for the owner" : "in the catalog"}; use its exerciseId.`,
        details: same,
      };
    }
    const payload: ExercisePayload = {
      exerciseId: this.newId(),
      name,
      category: input.category,
      primaryBodyPart: input.primaryBodyPart.trim(),
      secondaryBodyParts: (input.secondaryBodyParts ?? []).map((part) => part.trim()).filter((part) => part.length > 0),
      equipment: input.equipment?.trim() ?? "",
      notes: input.notes?.trim() ?? "",
      instructions: input.instructions?.trim() ?? "",
      defaultRestSeconds: input.defaultRestSeconds ?? null,
    };
    return { ok: true, proposal: this.insert("exercise", name, input.reason, payload, input.clientRef, deviceId), repeated: false };
  }

  proposeTemplate(input: TemplateInput, deviceId: string): ProposalResult {
    const repeat = this.repeat(input.clientRef, "template");
    if (repeat) return repeat;
    const problems: string[] = [];
    const pending = this.proposedExercises();
    const exercises = input.exercises.map((item, index) => {
      const plan: PlanFields = {
        targetSets: item.targetSets ?? null,
        repsMin: item.repsMin ?? null,
        repsMax: item.repsMax ?? null,
        restSeconds: item.restSeconds ?? null,
        supersetGroup: item.supersetGroup ?? null,
      };
      if (plan.repsMin !== null && plan.repsMax !== null && plan.repsMin > plan.repsMax) {
        problems.push(`exercises[${index}]: repsMin ${plan.repsMin} is more than repsMax ${plan.repsMax}.`);
      }
      const known = this.reader.exercise(item.exerciseId);
      if (known && !known.archived) {
        return { exerciseId: known.id, name: known.name, category: known.category, bodyPart: known.bodyPart, notes: item.notes?.trim() ?? "", ...plan, newExercise: null };
      }
      const proposed = pending.get(item.exerciseId);
      if (proposed) {
        return {
          exerciseId: proposed.exerciseId,
          name: proposed.name,
          category: proposed.category,
          bodyPart: proposed.primaryBodyPart,
          notes: item.notes?.trim() ?? "",
          ...plan,
          newExercise: proposed,
        };
      }
      problems.push(
        known?.archived
          ? `exercises[${index}]: "${known.name}" is archived.`
          : `exercises[${index}]: ${item.exerciseId} is no exercise in the catalog and no exercise you proposed.`,
      );
      return null;
    });
    if (input.replacesTemplateId && !this.reader.templateExists(input.replacesTemplateId)) {
      problems.push(`replacesTemplateId: ${input.replacesTemplateId} is no template of the owner's.`);
    }
    const name = input.name.trim();
    if (!input.replacesTemplateId) {
      const clash = this.reader.templates().find((template) => fold(template.name) === fold(name));
      if (clash) problems.push(`name: the owner already has a template "${clash.name}" (${clash.id}); pick another name or pass replacesTemplateId.`);
    }
    if (problems.length > 0) return { ok: false, code: "invalid", message: `${problems.length} problem(s); fix every one and call again.`, details: problems };
    const payload: TemplatePayload = {
      templateId: input.replacesTemplateId ?? this.newId(),
      replacesTemplateId: input.replacesTemplateId ?? null,
      name,
      notes: input.notes?.trim() ?? "",
      exercises: exercises.filter((item) => item !== null),
    };
    return { ok: true, proposal: this.insert("template", name, input.reason, payload, input.clientRef, deviceId), repeated: false };
  }

  /** Claude takes back its own proposal while it still waits. */
  withdraw(id: string): ProposalResult {
    const proposal = this.get(id);
    if (!proposal) return { ok: false, code: "not_found", message: "No proposal has that id." };
    if (proposal.status === "withdrawn") return { ok: true, proposal, repeated: true };
    if (proposal.status !== "pending") return { ok: false, code: "conflict", message: `The owner already ${proposal.status} it.` };
    this.sql.exec("UPDATE proposals SET status = 'withdrawn', decided_at = ? WHERE id = ?", this.now(), id);
    return { ok: true, proposal: this.get(id)!, repeated: false };
  }

  /**
   * The owner's answer from the app. Accepting a template accepts the exercises it proposed too,
   * because the app creates them with it. Answering twice the same way is fine.
   */
  decide(id: string, decision: "accepted" | "rejected"): ProposalResult {
    const proposal = this.get(id);
    if (!proposal) return { ok: false, code: "not_found", message: "No proposal has that id." };
    if (proposal.status === decision) return { ok: true, proposal, repeated: true };
    if (proposal.status !== "pending") return { ok: false, code: "conflict", message: `The proposal is already ${proposal.status}.` };
    const at = this.now();
    this.sql.exec("UPDATE proposals SET status = ?, decided_at = ? WHERE id = ?", decision, at, id);
    if (decision === "accepted" && proposal.kind === "template") {
      const created = (proposal.payload as TemplatePayload).exercises.flatMap((item) => (item.newExercise ? [item.newExercise.exerciseId] : []));
      for (const row of this.sql.exec<ProposalRow>("SELECT * FROM proposals WHERE kind = 'exercise' AND status = 'pending'").toArray()) {
        if (created.includes((JSON.parse(row.payload) as ExercisePayload).exerciseId)) {
          this.sql.exec("UPDATE proposals SET status = 'accepted', decided_at = ? WHERE id = ?", at, row.id);
        }
      }
    }
    return { ok: true, proposal: this.get(id)!, repeated: false };
  }

  private insert(kind: ProposalKind, title: string, reason: string, payload: ExercisePayload | TemplatePayload, clientRef: string, deviceId: string): ProposalView {
    const id = this.newId();
    this.sql.exec(
      "INSERT INTO proposals (id, kind, status, title, reason, payload, client_ref, device_id, created_at) VALUES (?, ?, 'pending', ?, ?, ?, ?, ?, ?)",
      id,
      kind,
      title,
      reason.trim(),
      JSON.stringify(payload),
      clientRef,
      deviceId,
      this.now(),
    );
    return this.get(id)!;
  }

  private repeat(clientRef: string, kind: ProposalKind): ProposalResult | null {
    const row = this.sql.exec<ProposalRow>("SELECT * FROM proposals WHERE client_ref = ?", clientRef).toArray()[0];
    if (!row) return null;
    if (row.kind !== kind) return { ok: false, code: "conflict", message: `clientRef ${clientRef} was used for a ${row.kind} proposal.` };
    return { ok: true, proposal: view(row), repeated: true };
  }

  /** Exercises Claude proposed that the owner has not rejected, by the id they get in the app. */
  private proposedExercises(): Map<string, ExercisePayload> {
    const rows = this.sql.exec<ProposalRow>("SELECT * FROM proposals WHERE kind = 'exercise' AND status IN ('pending', 'accepted')").toArray();
    return new Map(rows.map((row) => JSON.parse(row.payload) as ExercisePayload).map((payload) => [payload.exerciseId, payload]));
  }

  private sameExercise(name: string): SameExercise | null {
    const target = fold(name);
    const known = this.reader.exercises({ q: null, bodyPart: null, category: null, includeArchived: true }).find((exercise) => fold(exercise.name) === target);
    if (known) return { exerciseId: known.id, name: known.name, archived: known.archived, pending: false };
    const proposed = [...this.proposedExercises().values()].find((exercise) => fold(exercise.name) === target);
    return proposed ? { exerciseId: proposed.exerciseId, name: proposed.name, archived: false, pending: true } : null;
  }
}

/** A name compared without case, accents, or extra spaces. */
export function fold(name: string): string {
  return name.normalize("NFD").replace(/\p{M}/gu, "").toLowerCase().replace(/\s+/g, " ").trim();
}

function view(row: ProposalRow): ProposalView {
  return {
    id: row.id,
    kind: row.kind,
    status: row.status,
    title: row.title,
    reason: row.reason,
    payload: JSON.parse(row.payload) as ExercisePayload | TemplatePayload,
    createdAt: row.created_at,
    decidedAt: row.decided_at,
  };
}
