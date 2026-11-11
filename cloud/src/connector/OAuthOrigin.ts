const LOOPBACK_HOSTS = new Set(["localhost", "127.0.0.1", "[::1]"]);
const OAUTH_PATHS = ["/mcp", "/authorize", "/oauth/", "/.well-known/"];

/**
 * Whether the connector's OAuth can run on an origin. The OAuth provider accepts https, and plain
 * http only on a loopback host; anything else (an emulator reaching a local Worker at
 * http://10.0.2.2) gets the app API alone, so the API keeps working there (docs/pitfalls.md).
 */
export class OAuthOrigin {
  static allows(url: URL): boolean {
    return url.protocol === "https:" || (url.protocol === "http:" && LOOPBACK_HOSTS.has(url.hostname));
  }

  /** The connector's own routes, which do not exist where OAuth cannot run. */
  static isConnectorPath(pathname: string): boolean {
    return OAUTH_PATHS.some((path) => (path.endsWith("/") ? pathname.startsWith(path) : pathname === path || pathname.startsWith(`${path}/`)));
  }
}
