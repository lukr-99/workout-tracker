import type { RandomBytes } from "../crypto/RandomBytes";
import { Sha256 } from "../crypto/Sha256";

/** Letters and digits that cannot be mistaken for each other (no 0, O, 1, I). */
const ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
const PATTERN = /^[2-9A-HJ-NP-Z]{3}-?[2-9A-HJ-NP-Z]{3}$/;

/** The single-use code the app shows for Claude's sign-in page, such as `K7Q-4MX`. Only its hash is stored. */
export class ConnectorCode {
  private constructor(private readonly value: string) {}

  static issue(random: RandomBytes): ConnectorCode {
    const letters = Array.from(random(6), (byte) => ALPHABET[byte % ALPHABET.length]).join("");
    return new ConnectorCode(`${letters.slice(0, 3)}-${letters.slice(3)}`);
  }

  /** A typed code, forgiving case, spaces, and a missing dash; null when it cannot be one. */
  static parse(text: string): ConnectorCode | null {
    const clean = text.toUpperCase().replace(/\s+/g, "");
    if (!PATTERN.test(clean)) return null;
    const letters = clean.replace("-", "");
    return new ConnectorCode(`${letters.slice(0, 3)}-${letters.slice(3)}`);
  }

  hash(): Promise<string> {
    return Sha256.hex(this.value);
  }

  toString(): string {
    return this.value;
  }
}
