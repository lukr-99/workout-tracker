/** The key a request is limited by when it has no device token: the caller's network address. */
export class ClientAddress {
  /**
   * An IPv4 address as is, an IPv6 address cut to its /64 prefix (one household or server usually
   * holds the whole prefix), and "unknown" when Cloudflare did not say.
   */
  static key(request: Request): string {
    const address = request.headers.get("CF-Connecting-IP")?.trim().toLowerCase() ?? "";
    if (address.length === 0) return "unknown";
    if (!address.includes(":")) return address;
    return `${ClientAddress.expand(address).slice(0, 4).join(":")}::/64`;
  }

  private static expand(address: string): string[] {
    const [head = "", tail] = address.split("::");
    const left = head === "" ? [] : head.split(":");
    const right = tail === undefined || tail === "" ? [] : tail.split(":");
    const missing = tail === undefined ? 0 : 8 - left.length - right.length;
    return [...left, ...Array<string>(Math.max(missing, 0)).fill("0"), ...right].map((group) => group.replace(/^0+(?=.)/, ""));
  }
}
