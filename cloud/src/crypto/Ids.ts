import type { RandomBytes } from "./RandomBytes";

/** A new id in the app's own format: 32 lowercase hex characters, like C#'s Guid.ToString("N"). */
export function newId(random: RandomBytes): string {
  return Array.from(random(16), (byte) => byte.toString(16).padStart(2, "0")).join("");
}

export function isId(value: string): boolean {
  return /^[0-9a-f]{32}$/.test(value);
}
