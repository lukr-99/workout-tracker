import { exports } from "cloudflare:workers";
import { expect } from "vitest";
import { ORIGIN, type PhoneClient } from "./PhoneClient";

const REDIRECT = "https://claude.ai/api/mcp/auth_callback";

function base64url(bytes: Uint8Array): string {
  return btoa(String.fromCharCode(...bytes)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

/**
 * Plays Claude in tests: registers as an OAuth client, signs in on the Worker's page with a connector
 * code from [phone], exchanges the code for a token, and then calls the MCP server with it.
 */
export class ClaudeClient {
  private rpcId = 0;

  private constructor(
    readonly token: string,
    readonly deviceId: string,
  ) {}

  static async connect(phone: PhoneClient, scopes: string[] = ["read", "plan"]): Promise<ClaudeClient> {
    const created = (await (await phone.request("POST", "/v1/connector-codes")).json()) as { code: string; mcpUrl: string };
    expect(created.mcpUrl).toBe(`${ORIGIN}/mcp`);

    const registration = await exports.default.fetch(
      new Request(`${ORIGIN}/oauth/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ redirect_uris: [REDIRECT], client_name: "Claude", token_endpoint_auth_method: "none" }),
      }),
    );
    expect(registration.status).toBe(201);
    const { client_id: clientId } = (await registration.json()) as { client_id: string };

    const verifier = base64url(crypto.getRandomValues(new Uint8Array(32)));
    const challenge = base64url(new Uint8Array(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(verifier))));
    const authorize = new URL(`${ORIGIN}/authorize`);
    for (const [key, value] of Object.entries({
      response_type: "code",
      client_id: clientId,
      redirect_uri: REDIRECT,
      scope: scopes.join(" "),
      state: "s1",
      code_challenge: challenge,
      code_challenge_method: "S256",
      resource: `${ORIGIN}/mcp`,
    })) {
      authorize.searchParams.set(key, value);
    }
    const page = await exports.default.fetch(new Request(authorize));
    expect(page.status).toBe(200);
    const html = await page.text();
    expect(html).toContain("Connect Claude to Ember");
    const handle = /name=handle value="([^"]+)"/.exec(html)![1]!;
    const cookie = page.headers.getSetCookie().map((value) => value.split(";")[0]).join("; ");

    const post = (code: string) => {
      const form = new URLSearchParams({ handle, code, decision: "connect" });
      for (const scope of scopes) form.append("scope", scope);
      return exports.default.fetch(
        new Request(`${ORIGIN}/authorize`, {
          method: "POST",
          headers: { "Content-Type": "application/x-www-form-urlencoded", Cookie: cookie, "CF-Connecting-IP": phone.address },
          body: form,
          redirect: "manual",
        }),
      );
    };
    const wrong = await post("AAA-AAA");
    expect(wrong.status).toBe(400);
    expect(await wrong.text()).toContain("not valid or has expired");

    const approved = await post(created.code.toLowerCase());
    expect(approved.status).toBe(302);
    const callback = new URL(approved.headers.get("Location")!);
    expect(callback.origin + callback.pathname).toBe(REDIRECT);

    const tokenResponse = await exports.default.fetch(
      new Request(`${ORIGIN}/oauth/token`, {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: new URLSearchParams({
          grant_type: "authorization_code",
          code: callback.searchParams.get("code")!,
          redirect_uri: REDIRECT,
          client_id: clientId,
          code_verifier: verifier,
          resource: `${ORIGIN}/mcp`,
        }),
      }),
    );
    expect(tokenResponse.status).toBe(200);
    const { access_token: token } = (await tokenResponse.json()) as { access_token: string };

    const devices = (await (await phone.request("GET", "/v1/devices")).json()) as {
      devices: { id: string; platform: string; name: string; scopes: string[]; revokedAt: string | null }[];
    };
    const connector = devices.devices.filter((device) => device.platform === "connector" && device.revokedAt === null).at(-1)!;
    expect(connector).toMatchObject({ name: "Claude", scopes });
    return new ClaudeClient(token, connector.id);
  }

  async rpc(method: string, params: Record<string, unknown> = {}): Promise<{ status: number; body: any }> {
    const response = await exports.default.fetch(
      new Request(`${ORIGIN}/mcp`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${this.token}`,
          "Content-Type": "application/json",
          Accept: "application/json, text/event-stream",
          "MCP-Protocol-Version": "2025-06-18",
        },
        body: JSON.stringify({ jsonrpc: "2.0", id: ++this.rpcId, method, params }),
      }),
    );
    return { status: response.status, body: response.status === 200 ? await response.json() : await response.text() };
  }

  async toolNames(): Promise<string[]> {
    const listed = await this.rpc("tools/list");
    return listed.body.result.tools.map((tool: { name: string }) => tool.name).sort();
  }

  /** Calls a tool and returns its text, structured content, and whether it was refused. */
  async call(name: string, args: Record<string, unknown> = {}): Promise<{ isError: boolean; text: string; structured: any }> {
    const { body } = await this.rpc("tools/call", { name, arguments: args });
    if (body.error) throw new Error(`${name}: ${JSON.stringify(body.error)}`);
    const result = body.result;
    return { isError: result.isError === true, text: result.content[0].text, structured: result.structuredContent };
  }
}
