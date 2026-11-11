import { AuthorizationError, CimdFetchError, type ConsentDescription, type OAuthHelpers } from "@cloudflare/workers-oauth-provider";
import { ConnectorCode } from "../auth/ConnectorCode";
import { DeviceToken } from "../auth/DeviceToken";
import { newId } from "../crypto/Ids";
import type { RandomBytes } from "../crypto/RandomBytes";
import { ApiError } from "../http/ApiError";
import { ownerOf } from "../http/ApiRouter";
import { ClientAddress } from "../http/ClientAddress";
import type { RateLimits } from "../http/RateLimits";
import { connectorScopes } from "./ConnectorScope";

const NAME_MAX = 64;

/**
 * Claude's sign-in page for Ember (`/authorize`), adapted from Aletheia's: it shows which
 * app asks and where the access goes, takes the connector code made in Ember (Settings, Claude,
 * Connect Claude), and lets the owner choose the scopes, read and plan (docs/claude-connector.md). A valid
 * code creates a connector device and completes the OAuth grant.
 */
export class AuthorizePage {
  constructor(
    private readonly env: Env,
    private readonly oauth: OAuthHelpers,
    private readonly random: RandomBytes,
    private readonly limits: RateLimits,
  ) {}

  async handle(request: Request): Promise<Response> {
    try {
      return request.method === "POST" ? await this.submit(request) : await this.show(request);
    } catch (error) {
      if (error instanceof AuthorizationError && error.redirectTo) return Response.redirect(error.redirectTo, 302);
      if (error instanceof AuthorizationError || error instanceof CimdFetchError) {
        const message = error instanceof AuthorizationError ? error.description : "This app could not be verified.";
        return page("Cannot connect", `<p>${escape(message ?? "The sign-in request is not valid.")}</p><p>Start connecting again from Claude.</p>`, 400);
      }
      throw error;
    }
  }

  private async show(request: Request): Promise<Response> {
    const authRequest = await this.oauth.parseAuthRequest(request);
    const details = await this.oauth.describeConsent(authRequest);
    const consent = await this.oauth.beginConsent(authRequest);
    return form(details, consent.handle, null, consent.headers);
  }

  private async submit(request: Request): Promise<Response> {
    const data = await request.formData();
    const handle = String(data.get("handle") ?? "");
    if (data.get("decision") !== "connect") {
      const denied = await this.oauth.denyConsent(request, handle);
      return new Response(null, { status: 302, headers: denied.headers });
    }
    const scopes = connectorScopes(data.getAll("scope").map(String));
    const details: ConsentDescription = JSON.parse(String(data.get("details") ?? "null")) ?? fallbackDetails();
    if (scopes.length === 0) return form(details, handle, "Choose at least one thing Claude may do.");
    try {
      await this.limits.check(this.limits.connectorCode, ClientAddress.key(request));
    } catch (error) {
      if (error instanceof ApiError) return form(details, handle, "Too many tries. Wait a minute and try again.");
      throw error;
    }
    const owner = ownerOf(this.env);
    const code = ConnectorCode.parse(String(data.get("code") ?? ""));
    if (!code || !(await owner.redeemConnectorCode(await code.hash()))) {
      return form(details, handle, "That code is not valid or has expired. In Ember, choose Connect Claude for a new one.");
    }
    const approved = await this.oauth.approveConsent(request, handle, { scope: scopes });
    // The name comes from the registered client, not from the posted form.
    const name = connectorName((await this.oauth.lookupClient(approved.request.clientId))?.clientName ?? "");
    const deviceId = newId(this.random);
    // The connector has no device token; an unusable hash keeps the column's uniqueness rule.
    await owner.addConnector(deviceId, name, scopes, await DeviceToken.issue(this.random).secretHash());
    const { redirectTo } = await this.oauth.completeAuthorization({
      request: approved.request,
      userId: `owner/${deviceId}`,
      metadata: { label: name },
      scope: scopes,
      props: { deviceId },
    });
    approved.headers.set("Location", redirectTo);
    return new Response(null, { status: 302, headers: approved.headers });
  }
}

/** The connector's device name, from the client's self-asserted name: "Claude" for Claude. */
function connectorName(clientName: string): string {
  if (/claude/i.test(clientName)) return "Claude";
  const clean = clientName.replace(/[\u0000-\u001f]/g, "").trim().slice(0, NAME_MAX);
  return clean.length > 0 ? clean : "Connector";
}

function fallbackDetails(): ConsentDescription {
  return { clientId: "", clientName: "The app", redirectUri: "", redirectHost: "the app", redirectIsLoopback: false, scope: [] };
}

function form(details: ConsentDescription, handle: string, problem: string | null, headers = new Headers()): Response {
  const name = escape(details.clientName);
  const origin = details.clientDomain
    ? `Published by <strong>${escape(details.clientDomain)}</strong>.`
    : "This app registered itself; its name is not verified.";
  const loopback = details.redirectIsLoopback
    ? "<p class=warn><strong>This sends access to an app on your computer.</strong> Continue only if you just started connecting from it.</p>"
    : "";
  const body = `
<p>${name} asks to use your Ember. ${origin} Access goes to <strong>${escape(details.redirectHost)}</strong>.</p>
${loopback}
${problem ? `<p class=error role=alert>${escape(problem)}</p>` : ""}
<form method=post>
  <input type=hidden name=handle value="${escape(handle)}">
  <input type=hidden name=details value="${escape(JSON.stringify(details))}">
  <label for=code>Connector code</label>
  <input id=code name=code autocomplete=off autocapitalize=characters spellcheck=false maxlength=7 placeholder="K7Q-4MX" required autofocus>
  <p class=help>In Ember on your phone, open Settings, Claude, and choose Connect Claude. The code works once, for 10 minutes.</p>
  <fieldset>
    <legend>Allow ${name} to</legend>
    <label class=check><input type=checkbox name=scope value=read checked> <span><strong>Read</strong> your workouts, runs, exercises and templates when your phone shares them; Ember asks you first unless you set sharing to automatic</span></label>
    <label class=check><input type=checkbox name=scope value=plan checked> <span><strong>Propose</strong> templates and exercises, which wait in Ember until you accept them (never change or delete your data)</span></label>
  </fieldset>
  <div class=actions>
    <button name=decision value=connect class=primary>Connect</button>
    <button name=decision value=deny formnovalidate>Cancel</button>
  </div>
</form>
<p class=help>You can remove the connection any time in Ember's Settings, Claude.</p>`;
  return page(`Connect ${name} to Ember`, body, problem ? 400 : 200, headers);
}

function page(title: string, body: string, status: number, headers = new Headers()): Response {
  headers.set("Content-Type", "text/html; charset=utf-8");
  headers.set("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; form-action 'self' https:; frame-ancestors 'none'");
  headers.set("X-Frame-Options", "DENY");
  headers.set("Referrer-Policy", "no-referrer");
  headers.set("Cache-Control", "no-store");
  // Ember's colours: a warm orange on a dark or light ground.
  const html = `<!doctype html>
<html lang=en><head><meta charset=utf-8><meta name=viewport content="width=device-width,initial-scale=1">
<title>${title}</title>
<style>
:root{--bg:#f6f5f2;--surface:#ffffff;--text:#1c1b19;--muted:#5f5b54;--border:#cbc6bc;--primary:#c2410c;--on-primary:#ffffff;--danger:#c4312b;--focus:#2f6fe0;color-scheme:light dark}
@media (prefers-color-scheme:dark){:root{--bg:#0e1116;--surface:#161b22;--text:#f2f0ec;--muted:#a8b0ba;--border:#30363d;--primary:#f97316;--on-primary:#1c0b04;--danger:#ef6b64;--focus:#7aa5ff}}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--text);font:16px/1.5 "Segoe UI",Roboto,system-ui,sans-serif;padding:24px 16px}
main{max-width:440px;margin:0 auto;background:var(--surface);border:1px solid var(--border);border-radius:16px;padding:24px}
h1{font-size:22px;margin:0 0 12px}label{display:block;font-weight:600;margin-top:16px}
input[name=code]{width:100%;font:600 22px/1 Consolas,"Roboto Mono",monospace;letter-spacing:.12em;text-transform:uppercase;padding:12px;margin-top:6px;border:1px solid var(--border);border-radius:8px;background:transparent;color:inherit}
input:focus-visible,button:focus-visible{outline:2px solid var(--focus);outline-offset:2px}
fieldset{border:0;padding:0;margin:16px 0 0}legend{font-weight:600}.check{display:flex;gap:10px;font-weight:400;margin-top:10px}
.help{color:var(--muted);font-size:14px}.error{color:var(--danger);font-weight:600}.warn{color:var(--danger)}
.actions{display:flex;gap:12px;margin-top:20px}button{flex:1;min-height:44px;border-radius:999px;border:1px solid var(--border);background:transparent;color:inherit;font:inherit;font-weight:600;cursor:pointer}
.primary{background:var(--primary);border-color:var(--primary);color:var(--on-primary)}
</style></head><body><main><h1>${title}</h1>${body}</main></body></html>`;
  return new Response(html, { status, headers });
}

function escape(value: string): string {
  return value.replace(/[&<>"']/g, (char) => `&#${char.charCodeAt(0)};`);
}
