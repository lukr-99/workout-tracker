import type { OAuthResourceContext } from "@cloudflare/workers-oauth-provider";
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { WebStandardStreamableHTTPServerTransport } from "@modelcontextprotocol/sdk/server/webStandardStreamableHttp.js";
import { CfWorkerJsonSchemaValidator } from "@modelcontextprotocol/sdk/validation/cfworker";
import { ApiError } from "../http/ApiError";
import { ownerOf } from "../http/ApiRouter";
import { RateLimits } from "../http/RateLimits";
import type { ConnectorGrant } from "./ConnectorGrant";
import { CONNECTOR_INSTRUCTIONS } from "./ConnectorInstructions";
import { CONNECTOR_PROMPTS } from "./ConnectorPrompts";
import { connectorScopes } from "./ConnectorScope";
import { CONNECTOR_TOOLS } from "./ConnectorTools";
import type { Prompt } from "./Prompt";
import type { Tool } from "./Tool";
import type { ToolContext } from "./ToolContext";
import { ToolError } from "./ToolError";

/**
 * The remote MCP server at `/mcp` (streamable HTTP, stateless), reached only with an OAuth token the
 * provider verified. Each request checks the connector device is still active, so revoking it in the
 * app stops Claude at once, and offers only the tools both the grant and the device allow.
 */
export const mcpHandler = {
  async fetch(request: Request, env: Env, ctx: OAuthResourceContext<ConnectorGrant>): Promise<Response> {
    const grant = ctx.props;
    const owner = ownerOf(env);
    const device = await owner.connector(grant.deviceId);
    if (device === null) {
      return Response.json(
        { error: "invalid_token", error_description: "This connector was removed in Ember. Connect it again." },
        { status: 401, headers: { "WWW-Authenticate": 'Bearer error="invalid_token"' } },
      );
    }
    const scopes = connectorScopes(device.scopes.filter((scope) => ctx.auth.scope.includes(scope)));
    const server = new McpServer(
      { name: "Ember", version: "1.0.0" },
      {
        capabilities: { tools: {}, prompts: {} },
        // The default validator (Ajv) compiles code at runtime, which Workers do not allow.
        jsonSchemaValidator: new CfWorkerJsonSchemaValidator(),
        instructions: CONNECTOR_INSTRUCTIONS,
      },
    );
    const context: ToolContext = { grant, device, owner };
    const limits = RateLimits.fromEnv(env);
    for (const tool of CONNECTOR_TOOLS) {
      if (scopes.includes(tool.scope)) registerTool(server, tool, context, limits, grant.deviceId);
    }
    for (const prompt of CONNECTOR_PROMPTS) {
      if (prompt.scopes.every((scope) => scopes.includes(scope))) registerPrompt(server, prompt);
    }
    const transport = new WebStandardStreamableHTTPServerTransport({ sessionIdGenerator: undefined, enableJsonResponse: true });
    await server.connect(transport);
    return transport.handleRequest(request);
  },
};

function registerTool(server: McpServer, tool: Tool, context: ToolContext, limits: RateLimits, deviceId: string): void {
  server.registerTool(
    tool.name,
    {
      title: tool.title,
      description: tool.description,
      inputSchema: tool.input,
      annotations: { readOnlyHint: tool.readOnly, destructiveHint: tool.destructive, idempotentHint: tool.idempotent, openWorldHint: false },
    },
    async (args: unknown) => {
      try {
        await limits.check(limits.connectorCall, deviceId);
        const result = await tool.run(context, args);
        return { content: [{ type: "text" as const, text: result.text }], structuredContent: result.structured };
      } catch (error) {
        const refusal =
          error instanceof ToolError
            ? error
            : error instanceof ApiError && error.code === "rate_limited"
              ? new ToolError("rate_limited", "More than 120 calls a minute. Wait a minute, then go on.")
              : null;
        if (!refusal) throw error;
        return { content: [{ type: "text" as const, text: refusal.text() }], isError: true };
      }
    },
  );
}

function registerPrompt(server: McpServer, prompt: Prompt): void {
  server.registerPrompt(prompt.name, { title: prompt.title, description: prompt.description, argsSchema: prompt.args }, async (args: unknown) => ({
    messages: [{ role: "user" as const, content: { type: "text" as const, text: prompt.text(args as Record<string, string | undefined>) } }],
  }));
}
