import { Base64Url } from "../crypto/Base64Url";
import type { RandomBytes } from "../crypto/RandomBytes";
import { Sha256 } from "../crypto/Sha256";

const PREFIX = "wt1";
const SECRET_PATTERN = /^[A-Za-z0-9_-]{43}$/;

/** A device's bearer token: `wt1.<secret>`. Only the secret's hash is ever stored. */
export class DeviceToken {
  private constructor(private readonly secret: string) {}

  static parse(value: string): DeviceToken | null {
    const parts = value.split(".");
    if (parts.length !== 2 || parts[0] !== PREFIX || !SECRET_PATTERN.test(parts[1]!)) return null;
    return new DeviceToken(parts[1]!);
  }

  static fromAuthorization(header: string | null): DeviceToken | null {
    if (!header?.startsWith("Bearer ")) return null;
    return DeviceToken.parse(header.slice("Bearer ".length).trim());
  }

  static issue(random: RandomBytes): DeviceToken {
    return new DeviceToken(Base64Url.encode(random(32)));
  }

  secretHash(): Promise<string> {
    return Sha256.hex(this.secret);
  }

  toString(): string {
    return `${PREFIX}.${this.secret}`;
  }
}
