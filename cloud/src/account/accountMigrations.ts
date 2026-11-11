import initial from "../../migrations/0001_initial.sql";
import type { Migration } from "./Migration";

/** Every OwnerObject migration in order. Applied files are immutable; add the next number. */
export const ACCOUNT_MIGRATIONS: readonly Migration[] = [{ version: 1, name: "0001_initial", sql: initial }];
