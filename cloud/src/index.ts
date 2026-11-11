import { type OAuthHelpers, OAuthProvider } from "@cloudflare/workers-oauth-provider";
import { AuthorizePage } from "./connector/AuthorizePage";
import { CONNECTOR_SCOPES } from "./connector/ConnectorScope";
import { mcpHandler } from "./connector/McpHandler";
import { OAuthOrigin } from "./connector/OAuthOrigin";
import { systemRandomBytes } from "./crypto/RandomBytes";
import { ApiError } from "./http/ApiError";
import { ApiRouter } from "./http/ApiRouter";
import { RateLimits } from "./http/RateLimits";

export { OwnerObject } from "./account/OwnerObject";

/** The app API and Claude's sign-in page: everything that is not the OAuth protocol or `/mcp`. */
const appHandler = {
  fetch(request: Request, env: Env & { OAUTH_PROVIDER: OAuthHelpers }): Promise<Response> {
    if (new URL(request.url).pathname === "/authorize") {
      return new AuthorizePage(env, env.OAUTH_PROVIDER, systemRandomBytes, RateLimits.fromEnv(env)).handle(request);
    }
    return new ApiRouter(env, systemRandomBytes, RateLimits.fromEnv(env)).handle(request);
  },
};

/**
 * The OAuth provider wraps the Worker: it serves discovery, client registration, and tokens, checks
 * the token on `/mcp`, and passes everything else to the app handler. Its resource is this Worker's
 * own `/mcp` URL, so one provider is made per origin (the deployed URL, or a local one).
 */
const providers = new Map<string, OAuthProvider<Env>>();

function providerFor(origin: string): OAuthProvider<Env> {
  let provider = providers.get(origin);
  if (!provider) {
    provider = new OAuthProvider<Env>({
      apiRoute: "/mcp",
      // The provider sets ctx.props and ctx.auth before calling it (OAuthResourceContext).
      apiHandler: mcpHandler as never,
      defaultHandler: appHandler as never,
      authorizeEndpoint: "/authorize",
      tokenEndpoint: "/oauth/token",
      clientRegistrationEndpoint: "/oauth/register",
      clientIdMetadataDocumentEnabled: true,
      scopesSupported: [...CONNECTOR_SCOPES],
      resourceMetadata: { resource: `${origin}/mcp`, authorization_servers: [origin], resource_name: "Ember" },
    });
    providers.set(origin, provider);
  }
  return provider;
}

// Composition root: the handlers get the real bindings, randomness, and limits here and nowhere else.
export default {
  fetch(request, env, ctx): Promise<Response> {
    const url = new URL(request.url);
    if (!OAuthOrigin.allows(url)) {
      // No OAuth on this origin (plain http on a non-loopback host): the app API alone.
      if (OAuthOrigin.isConnectorPath(url.pathname)) return Promise.resolve(new ApiError(404, "not_found", "Not found.").toResponse());
      return appHandler.fetch(request, env as Env & { OAUTH_PROVIDER: OAuthHelpers });
    }
    return providerFor(url.origin).fetch(request, env, ctx);
  },
} satisfies ExportedHandler<Env>;
