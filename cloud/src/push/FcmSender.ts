import { Base64Url } from "../crypto/Base64Url";
import type { FcmServiceAccount } from "./FcmServiceAccount";

const TOKEN_URL = "https://oauth2.googleapis.com/token";
const SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
const TOKEN_LIFETIME_S = 3600;
const TOKEN_REFRESH_MARGIN_MS = 5 * 60 * 1000;

/** What happened to one push: sent, or the phone's push token is gone and should be forgotten. */
export type PushResult = "sent" | "unregistered" | "failed";

/**
 * Sends a content-free FCM data message, such as `{"t": "share", "r": "<requestId>"}`, through the
 * HTTP v1 API, signing in with the service-account key. No workout data passes through Google.
 * Adapted from Lumen's sender; the access token is cached for its lifetime.
 */
export class FcmSender {
  private accessToken: { value: string; expiresAt: number } | null = null;
  private signingKey: Promise<CryptoKey> | null = null;

  constructor(
    private readonly account: FcmServiceAccount,
    private readonly fetcher: typeof fetch,
    private readonly now: () => number,
  ) {}

  async send(pushToken: string, data: Record<string, string>, ttlSeconds: number): Promise<PushResult> {
    const response = await this.fetcher(`https://fcm.googleapis.com/v1/projects/${this.account.projectId}/messages:send`, {
      method: "POST",
      headers: { Authorization: `Bearer ${await this.token()}`, "Content-Type": "application/json" },
      body: JSON.stringify({ message: { token: pushToken, data, android: { priority: "HIGH", ttl: `${ttlSeconds}s` } } }),
    });
    if (response.ok) return "sent";
    if (response.status === 404 || (response.status === 400 && (await response.text()).includes("registration token"))) {
      return "unregistered";
    }
    if (response.status === 401) this.accessToken = null;
    return "failed";
  }

  private async token(): Promise<string> {
    if (this.accessToken && this.accessToken.expiresAt - TOKEN_REFRESH_MARGIN_MS > this.now()) return this.accessToken.value;
    const issuedAt = Math.floor(this.now() / 1000);
    const assertion = await this.sign({
      iss: this.account.clientEmail,
      scope: SCOPE,
      aud: TOKEN_URL,
      iat: issuedAt,
      exp: issuedAt + TOKEN_LIFETIME_S,
    });
    const response = await this.fetcher(TOKEN_URL, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion }).toString(),
    });
    if (!response.ok) throw new Error(`FCM sign-in failed with ${response.status}.`);
    const body = (await response.json()) as { access_token?: unknown; expires_in?: unknown };
    if (typeof body.access_token !== "string") throw new Error("FCM sign-in returned no access token.");
    const lifetime = typeof body.expires_in === "number" ? body.expires_in : TOKEN_LIFETIME_S;
    this.accessToken = { value: body.access_token, expiresAt: this.now() + lifetime * 1000 };
    return body.access_token;
  }

  private async sign(claims: Record<string, unknown>): Promise<string> {
    const encoder = new TextEncoder();
    const header = Base64Url.encode(encoder.encode(JSON.stringify({ alg: "RS256", typ: "JWT" })));
    const payload = Base64Url.encode(encoder.encode(JSON.stringify(claims)));
    this.signingKey ??= importPrivateKey(this.account.privateKeyPem);
    const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", await this.signingKey, encoder.encode(`${header}.${payload}`));
    return `${header}.${payload}.${Base64Url.encode(new Uint8Array(signature))}`;
  }
}

function importPrivateKey(pem: string): Promise<CryptoKey> {
  const base64 = pem.replace(/-----(BEGIN|END) PRIVATE KEY-----/g, "").replace(/\s+/g, "");
  const der = Uint8Array.from(atob(base64), (char) => char.charCodeAt(0));
  return crypto.subtle.importKey("pkcs8", der, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"]);
}
