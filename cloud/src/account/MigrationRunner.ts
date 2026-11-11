import type { Migration } from "./Migration";

/** Applies numbered migrations to one Durable Object's SQLite database, each in its own transaction. */
export class MigrationRunner {
  constructor(private readonly storage: DurableObjectStorage) {}

  /** True once the first migration has run, meaning this object holds an account. */
  isInitialized(): boolean {
    return this.currentVersion() > 0;
  }

  currentVersion(): number {
    const hasTable =
      this.storage.sql.exec("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = 'schema_migrations'").toArray()
        .length > 0;
    if (!hasTable) return 0;
    const row = this.storage.sql.exec<{ version: number | null }>("SELECT MAX(version) AS version FROM schema_migrations").one();
    return row.version ?? 0;
  }

  apply(migrations: readonly Migration[], now: () => string): number {
    migrations.forEach((migration, index) => {
      if (migration.version !== index + 1) {
        throw new Error(`Migration ${migration.name} is out of order; expected version ${index + 1}.`);
      }
    });
    this.storage.sql.exec(
      "CREATE TABLE IF NOT EXISTS schema_migrations (version INTEGER PRIMARY KEY, name TEXT NOT NULL, applied_at TEXT NOT NULL)",
    );
    const current = this.currentVersion();
    for (const migration of migrations.slice(current)) {
      this.storage.transactionSync(() => {
        this.storage.sql.exec(migration.sql);
        this.storage.sql.exec(
          "INSERT INTO schema_migrations (version, name, applied_at) VALUES (?, ?, ?)",
          migration.version,
          migration.name,
          now(),
        );
      });
    }
    return this.currentVersion();
  }
}
