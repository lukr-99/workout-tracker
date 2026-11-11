export type RandomBytes = (length: number) => Uint8Array;

export const systemRandomBytes: RandomBytes = (length) => crypto.getRandomValues(new Uint8Array(length));
