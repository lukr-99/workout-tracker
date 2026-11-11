import type { SharedBundle } from "./SharedBundle";

const SHARED_TABLES = [
  "shared_exercises",
  "shared_locations",
  "shared_templates",
  "shared_template_exercises",
  "shared_sessions",
  "shared_entries",
  "shared_sets",
  "shared_cardio",
  "shared_runs",
  "shared_splits",
  "shared_routes",
  "share_state",
];

/** When the last share arrived, until when its copy stays, and how the owner reads days and units. */
export interface ShareState {
  requestId: string | null;
  sharedAt: string;
  keepUntil: string;
  timeZone: string;
  units: "metric" | "imperial";
  appVersion: string | null;
}

export interface ShareCounts {
  exercises: number;
  templates: number;
  workouts: number;
  runs: number;
}

type Value = string | number | null;

/**
 * The copy of the phone's training data, kept only while a share is fresh (docs/claude-connector.md).
 * A new share replaces the whole copy; when it runs out, every row goes.
 */
export class SharedStore {
  constructor(private readonly sql: SqlStorage) {}

  /** The current share, or null when there is none or it ran out at [now]. */
  state(now: string): ShareState | null {
    const row = this.sql
      .exec<{ request_id: string | null; shared_at: string; keep_until: string; time_zone: string; units: string; app_version: string | null }>(
        "SELECT request_id, shared_at, keep_until, time_zone, units, app_version FROM share_state WHERE id = 1",
      )
      .toArray()[0];
    if (!row || row.keep_until <= now) return null;
    return {
      requestId: row.request_id,
      sharedAt: row.shared_at,
      keepUntil: row.keep_until,
      timeZone: row.time_zone,
      units: row.units === "imperial" ? "imperial" : "metric",
      appVersion: row.app_version,
    };
  }

  keepUntil(): string | null {
    return this.sql.exec<{ keep_until: string }>("SELECT keep_until FROM share_state WHERE id = 1").toArray()[0]?.keep_until ?? null;
  }

  /** Replaces the copy with [bundle]. Call inside a transaction. */
  replace(bundle: SharedBundle, requestId: string | null, sharedAt: string, keepUntil: string): ShareCounts {
    this.purge();
    const bool = (value: boolean) => (value ? 1 : 0);
    this.insert("share_state", {
      id: 1,
      request_id: requestId,
      shared_at: sharedAt,
      keep_until: keepUntil,
      time_zone: bundle.timeZone,
      units: bundle.units,
      app_version: bundle.appVersion,
      format_version: bundle.formatVersion,
    });
    for (const exercise of bundle.exercises) {
      this.insert("shared_exercises", {
        id: exercise.id,
        name: exercise.name,
        category: exercise.category,
        primary_body_part: exercise.primaryBodyPart,
        secondary_body_parts: JSON.stringify(exercise.secondaryBodyParts),
        equipment: exercise.equipment,
        notes: exercise.notes,
        instructions: exercise.instructions,
        source: exercise.source,
        default_rest_seconds: exercise.defaultRestSeconds,
        track_per_location: bool(exercise.trackPerLocation),
        archived: bool(exercise.archived),
      });
    }
    for (const location of bundle.locations) {
      this.insert("shared_locations", { id: location.id, name: location.name, kind: location.kind, archived: bool(location.archived) });
    }
    for (const template of bundle.templates) {
      this.insert("shared_templates", { id: template.id, name: template.name, notes: template.notes });
      for (const item of template.exercises) {
        this.insert("shared_template_exercises", {
          id: item.id,
          template_id: template.id,
          exercise_id: item.exerciseId,
          exercise_name: item.exerciseName,
          category: item.category,
          body_part: item.bodyPart,
          sort_order: item.sortOrder,
          notes: item.notes,
          target_sets: item.targetSets,
          reps_min: item.repsMin,
          reps_max: item.repsMax,
          rest_seconds: item.restSeconds,
          superset_group: item.supersetGroup,
        });
      }
    }
    for (const session of bundle.sessions) {
      this.insert("shared_sessions", {
        id: session.id,
        template_id: session.templateId,
        name: session.name,
        status: session.status,
        started_at: session.startedAt,
        ended_at: session.endedAt,
        local_day: session.localDay,
        duration_seconds: session.durationSeconds,
        notes: session.notes,
        location_id: session.locationId,
        perceived_effort: session.perceivedEffort,
        bodyweight_kg: session.bodyweightKg,
      });
      for (const entry of session.entries) {
        this.insert("shared_entries", {
          id: entry.id,
          session_id: session.id,
          exercise_id: entry.exerciseId,
          exercise_name: entry.exerciseName,
          category: entry.category,
          body_part: entry.bodyPart,
          sort_order: entry.sortOrder,
          superset_group: entry.supersetGroup,
          notes: entry.notes,
        });
        for (const set of entry.sets) {
          this.insert("shared_sets", {
            id: set.id,
            entry_id: entry.id,
            set_number: set.setNumber,
            reps: set.reps,
            weight_kg: set.weightKg,
            rir: set.rir,
            rpe: set.rpe,
            duration_seconds: set.durationSeconds,
            is_warmup: bool(set.isWarmup),
            is_pr: bool(set.isPr),
            tags: set.tags.join(","),
            notes: set.notes,
          });
        }
        if (entry.cardio) {
          this.insert("shared_cardio", {
            entry_id: entry.id,
            duration_seconds: entry.cardio.durationSeconds,
            distance_km: entry.cardio.distanceKm,
            calories: entry.cardio.calories,
            notes: entry.cardio.notes,
          });
        }
      }
    }
    for (const run of bundle.runs) {
      this.insert("shared_runs", {
        id: run.id,
        started_at: run.startedAt,
        local_day: run.localDay,
        duration_seconds: run.durationSeconds,
        moving_seconds: run.movingSeconds,
        distance_m: run.distanceMeters,
        avg_pace_sec_per_km: run.avgPaceSecPerKm,
        elevation_gain_m: run.elevationGainM,
        calories: run.calories,
        avg_hr: run.avgHr,
        route_id: run.routeId,
        notes: run.notes,
      });
      for (const split of run.splits) {
        this.insert("shared_splits", {
          run_id: run.id,
          idx: split.index,
          distance_m: split.distanceMeters,
          duration_seconds: split.durationSeconds,
          pace_sec_per_km: split.paceSecPerKm,
          elevation_gain_m: split.elevationGainM,
          is_full: bool(split.isFull),
        });
      }
    }
    for (const route of bundle.routes) {
      this.insert("shared_routes", { id: route.id, name: route.name, distance_m: route.distanceMeters, elevation_gain_m: route.elevationGainM, notes: route.notes });
    }
    return {
      exercises: bundle.exercises.length,
      templates: bundle.templates.length,
      workouts: bundle.sessions.filter((session) => session.status === "completed").length,
      runs: bundle.runs.length,
    };
  }

  /** Deletes the whole copy. */
  purge(): void {
    for (const table of SHARED_TABLES) this.sql.exec(`DELETE FROM ${table}`);
  }

  private insert(table: string, row: Record<string, Value>): void {
    const columns = Object.keys(row);
    this.sql.exec(
      `INSERT OR REPLACE INTO ${table} (${columns.join(", ")}) VALUES (${columns.map(() => "?").join(", ")})`,
      ...columns.map((column) => row[column]!),
    );
  }
}
