import { env, exports } from "cloudflare:workers";
import { describe, expect, it } from "vitest";
import { CONNECTOR_INSTRUCTIONS, CONNECTOR_INSTRUCTIONS_LIMIT } from "../src/connector/ConnectorInstructions";
import { ClaudeClient } from "./ClaudeClient";
import { ORIGIN, PhoneClient } from "./PhoneClient";

describe("connector sign-in", () => {
  it("asks for a token on /mcp and publishes its OAuth metadata", async () => {
    const unauthenticated = await exports.default.fetch(new Request(`${ORIGIN}/mcp`, { method: "POST", body: "{}" }));
    expect(unauthenticated.status).toBe(401);
    expect(unauthenticated.headers.get("WWW-Authenticate")).toContain("resource_metadata");
    const metadata = await (await exports.default.fetch(new Request(`${ORIGIN}/.well-known/oauth-authorization-server`))).json();
    expect(metadata).toMatchObject({ authorization_endpoint: `${ORIGIN}/authorize`, scopes_supported: ["read", "plan"] });
  });

  it("signs Claude in with a connector code and speaks MCP as Ember", async () => {
    const claude = await ClaudeClient.connect(await PhoneClient.setup());
    const initialized = await claude.rpc("initialize", { protocolVersion: "2025-06-18", capabilities: {}, clientInfo: { name: "test", version: "1" } });
    expect(initialized.body.result.serverInfo.name).toBe("Ember");
    expect(initialized.body.result.instructions).toContain("starting with get_overview");
    expect(await claude.toolNames()).toEqual([
      "get_exercise_progress",
      "get_overview",
      "get_run",
      "get_running_overview",
      "get_training_volume",
      "get_workout",
      "list_exercises",
      "list_proposals",
      "list_runs",
      "list_templates",
      "list_workouts",
      "propose_exercise",
      "propose_template",
      "withdraw_proposal",
    ]);
    const prompts = await claude.rpc("prompts/list");
    expect(prompts.body.result.prompts.map((prompt: { name: string }) => prompt.name)).toEqual(["training_review", "build_template"]);
    const review = await claude.rpc("prompts/get", { name: "training_review", arguments: { focus: "my bench" } });
    expect(review.body.result.messages[0].content.text).toContain("I especially want to know about: my bench.");
  });

  it("offers only the read tools to a read-only grant", async () => {
    const claude = await ClaudeClient.connect(await PhoneClient.setup(), ["read"]);
    const names = await claude.toolNames();
    expect(names).toContain("get_overview");
    expect(names.filter((name) => name.startsWith("propose_"))).toEqual([]);
    const prompts = await claude.rpc("prompts/list");
    expect(prompts.body.result.prompts.map((prompt: { name: string }) => prompt.name)).toEqual(["training_review"]);
  });

  it("uses a connector code once, and not after it ran out", async () => {
    const owner = env.OWNER.get(env.OWNER.idFromName("owner"));
    await owner.createConnectorCode("used-once", new Date(Date.now() + 60_000).toISOString());
    expect(await owner.redeemConnectorCode("used-once")).toBe(true);
    expect(await owner.redeemConnectorCode("used-once")).toBe(false);
    await owner.createConnectorCode("ran-out", new Date(Date.now() - 1).toISOString());
    expect(await owner.redeemConnectorCode("ran-out")).toBe(false);
  });

  it("stops answering the moment the owner revokes the connector", async () => {
    const phone = await PhoneClient.setup();
    const claude = await ClaudeClient.connect(phone);
    expect((await claude.rpc("tools/list")).status).toBe(200);
    expect((await phone.request("DELETE", `/v1/devices/${claude.deviceId}`)).status).toBe(204);
    const refused = await claude.rpc("tools/list");
    expect(refused.status).toBe(401);
    expect(refused.body).toContain("removed in Ember");
  });

  it("keeps its instructions short enough that Claude reads the last rule", () => {
    expect(CONNECTOR_INSTRUCTIONS.length).toBeLessThanOrEqual(CONNECTOR_INSTRUCTIONS_LIMIT);
    expect(CONNECTOR_INSTRUCTIONS).toContain("7. You are not a doctor");
  });
});
