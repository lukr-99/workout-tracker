import { exports } from "cloudflare:workers";

export const ORIGIN = "https://workouts.test";
export const SETUP_SECRET = "test-setup-secret-that-is-long-enough";

/** A fresh documentation-range IPv6 /64, so every client has its own rate limit keys. */
function freshAddress(): string {
  const [a, b] = crypto.getRandomValues(new Uint16Array(2));
  return `2001:db8:${a!.toString(16)}:${b!.toString(16)}::1`;
}

/** Plays the phone app in tests, calling the Worker's fetch handler directly. */
export class PhoneClient {
  constructor(
    readonly token: string | null = null,
    readonly deviceId: string | null = null,
    readonly address: string = freshAddress(),
  ) {}

  /** Sets up a phone (replacing any earlier one) and forgets any shared copy, so each test starts clean. */
  static async setup(name = "Pixel"): Promise<PhoneClient> {
    const response = await new PhoneClient().request("POST", "/v1/devices", { setupSecret: SETUP_SECRET, name });
    const body = (await response.json()) as { token: string; deviceId: string };
    const phone = new PhoneClient(body.token, body.deviceId);
    await phone.request("DELETE", "/v1/share");
    return phone;
  }

  request(method: string, path: string, body?: unknown): Promise<Response> {
    const headers: Record<string, string> = { "CF-Connecting-IP": this.address };
    if (this.token) headers.Authorization = `Bearer ${this.token}`;
    if (body !== undefined) headers["Content-Type"] = "application/json";
    return exports.default.fetch(new Request(ORIGIN + path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) }));
  }

  /** Waits for the share request a tool call opened, as the app would after the push. */
  async nextRequest(): Promise<string> {
    for (let attempt = 0; attempt < 100; attempt++) {
      const { requests } = (await (await this.request("GET", "/v1/share-requests")).json()) as { requests: { id: string }[] };
      if (requests.length > 0) return requests.at(-1)!.id;
      await new Promise((resolve) => setTimeout(resolve, 20));
    }
    throw new Error("No share request came.");
  }

  /** Sends a share as Ember does: the `ClaudeShare` body as it is. */
  share(body: unknown, requestId: string | null = null): Promise<Response> {
    const path = requestId ? `/v1/share-requests/${requestId}/share` : "/v1/share";
    return this.request("POST", path, body);
  }
}
