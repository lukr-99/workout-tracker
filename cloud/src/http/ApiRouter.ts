import type { DeviceView, OwnerObject, ShareResult } from "../account/OwnerObject";
import { ConnectorCode } from "../auth/ConnectorCode";
import { DeviceToken } from "../auth/DeviceToken";
import { newId } from "../crypto/Ids";
import type { ProposalResult } from "../proposals/Proposals";
import type { RandomBytes } from "../crypto/RandomBytes";
import { Sha256 } from "../crypto/Sha256";
import { ApiError } from "./ApiError";
import { ClientAddress } from "./ClientAddress";
import type { RateLimits } from "./RateLimits";
import { RequestBody } from "./RequestBody";

/** A whole share may be large: years of sets. */
const SHARE_MAX = 16 * 1024 * 1024;
const CODE_LIFETIME_MS = 10 * 60 * 1000;
const SECRET_MIN = 32;

/** The owner's single Durable Object. */
export function ownerOf(env: Env): DurableObjectStub<OwnerObject> {
  return env.OWNER.get(env.OWNER.idFromName("owner"));
}

/**
 * The phone's API under `/v1` (docs/claude-connector.md, "The app's API"): set up the phone with the setup
 * secret, answer share requests, share or forget data, decide Claude's proposals, and make a
 * connector code for Claude's sign-in. Everything but setup needs the phone's bearer token.
 */
export class ApiRouter {
  constructor(
    private readonly env: Env,
    private readonly random: RandomBytes,
    private readonly limits: RateLimits,
  ) {}

  async handle(request: Request): Promise<Response> {
    try {
      return await this.route(request);
    } catch (error) {
      if (error instanceof ApiError) return error.toResponse();
      throw error;
    }
  }

  private async route(request: Request): Promise<Response> {
    const url = new URL(request.url);
    const path = url.pathname;
    const method = request.method;
    if (method === "GET" && path === "/v1/health") return Response.json({ ok: true });
    if (method === "POST" && path === "/v1/devices") return this.setup(request);
    if (!path.startsWith("/v1/")) throw new ApiError(404, "not_found", "Not found.");

    const owner = ownerOf(this.env);
    const phone = await this.phone(request, owner);
    let match: RegExpExecArray | null;

    if (method === "GET" && path === "/v1/devices") return Response.json({ devices: await owner.devices() });
    if (method === "DELETE" && (match = /^\/v1\/devices\/([0-9a-f]{32})$/.exec(path))) {
      if (!(await owner.revokeDevice(match[1]!))) throw new ApiError(404, "not_found", "No active device has that id.");
      return new Response(null, { status: 204 });
    }
    if (method === "PUT" && path === "/v1/push-token") {
      const body = object(await RequestBody.json(request));
      const token = body.pushToken;
      if (token !== null && (typeof token !== "string" || token.length === 0 || token.length > 4096)) {
        throw new ApiError(400, "invalid", "pushToken must be the FCM registration token or null.");
      }
      await owner.setPushToken(phone.id, token);
      return new Response(null, { status: 204 });
    }
    if (method === "POST" && path === "/v1/connector-codes") {
      await this.limits.check(this.limits.connectorCode, phone.id);
      const code = ConnectorCode.issue(this.random);
      const expiresAt = new Date(Date.now() + CODE_LIFETIME_MS).toISOString();
      await owner.createConnectorCode(await code.hash(), expiresAt);
      return Response.json({ code: code.toString(), mcpUrl: `${url.origin}/mcp`, expiresAt }, { status: 201 });
    }
    if (method === "GET" && path === "/v1/share-requests") return Response.json({ requests: await owner.openRequests() });
    if (method === "POST" && (match = /^\/v1\/share-requests\/([0-9a-f]{32})\/share$/.exec(path))) {
      return shareResponse(await owner.share(match[1]!, await RequestBody.json(request, SHARE_MAX)));
    }
    if (method === "POST" && (match = /^\/v1\/share-requests\/([0-9a-f]{32})\/decline$/.exec(path))) {
      const refused: ShareResult | null = await owner.decline(match[1]!);
      if (refused && !refused.ok) throw new ApiError(refused.status, refused.code, refused.message);
      return new Response(null, { status: 204 });
    }
    if (method === "GET" && path === "/v1/share") return Response.json({ share: await owner.shareStatus() });
    if (method === "POST" && path === "/v1/share") return shareResponse(await owner.share(null, await RequestBody.json(request, SHARE_MAX)));
    if (method === "DELETE" && path === "/v1/share") {
      await owner.forgetShare();
      return new Response(null, { status: 204 });
    }
    if (method === "GET" && path === "/v1/proposals") {
      const status = url.searchParams.get("status");
      if (status !== null && !["pending", "accepted", "rejected", "withdrawn"].includes(status)) {
        throw new ApiError(400, "invalid", "status must be pending, accepted, rejected, or withdrawn.");
      }
      return Response.json({ proposals: await owner.listProposals(status as never, 100) });
    }
    if (method === "POST" && (match = /^\/v1\/proposals\/([0-9a-f]{32})\/decision$/.exec(path))) {
      const decision = object(await RequestBody.json(request)).decision;
      if (decision !== "accepted" && decision !== "rejected") throw new ApiError(400, "invalid", "decision must be accepted or rejected.");
      const result: ProposalResult = await owner.decideProposal(match[1]!, decision);
      if (!result.ok) throw new ApiError(result.code === "not_found" ? 404 : 409, result.code, result.message);
      return Response.json({ proposal: result.proposal });
    }
    throw new ApiError(404, "not_found", "Not found.");
  }

  /** `POST /v1/devices {setupSecret, name}`: registers the phone, replacing an earlier one. */
  private async setup(request: Request): Promise<Response> {
    await this.limits.check(this.limits.setup, ClientAddress.key(request));
    const secret = this.env.SETUP_SECRET;
    if (!secret || secret.length < SECRET_MIN) throw new ApiError(503, "setup_disabled", "The server has no SETUP_SECRET of at least 32 characters.");
    const body = object(await RequestBody.json(request));
    if (typeof body.setupSecret !== "string" || (await Sha256.hex(body.setupSecret)) !== (await Sha256.hex(secret))) {
      throw new ApiError(403, "wrong_secret", "That is not this server's setup secret.");
    }
    const name = typeof body.name === "string" && body.name.trim().length > 0 ? body.name.trim().slice(0, 64) : "Phone";
    const token = DeviceToken.issue(this.random);
    const device = await ownerOf(this.env).setupPhone(newId(this.random), name, await token.secretHash());
    return Response.json({ deviceId: device.id, token: token.toString() }, { status: 201 });
  }

  private async phone(request: Request, owner: DurableObjectStub<OwnerObject>): Promise<DeviceView> {
    const token = DeviceToken.fromAuthorization(request.headers.get("Authorization"));
    const device = token ? await owner.authenticate(await token.secretHash()) : null;
    if (!device || device.platform !== "android") throw new ApiError(401, "unauthorized", "A valid phone token is required.");
    return device;
  }
}

function object(body: unknown): Record<string, unknown> {
  if (typeof body !== "object" || body === null || Array.isArray(body)) throw new ApiError(400, "invalid", "The body must be a JSON object.");
  return body as Record<string, unknown>;
}

function shareResponse(result: ShareResult): Response {
  if (!result.ok) throw new ApiError(result.status, result.code, result.message, {}, result.details);
  return Response.json({ sharedAt: result.sharedAt, keepUntil: result.keepUntil, counts: result.counts });
}
