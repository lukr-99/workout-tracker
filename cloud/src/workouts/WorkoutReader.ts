import { LocalDay } from "./LocalDay";

export interface WorkoutSummary {
  id: string;
  name: string;
  day: string;
  location: string;
  template: string | null;
  durationMinutes: number;
  exercises: number;
  workingSets: number;
  hardSets: number;
  reps: number;
  volumeKg: number;
  cardioMinutes: number;
  effort: number | null;
  notes: string;
}

export interface WorkoutFilter {
  from: string | null;
  to: string | null;
  q: string | null;
  locationId: string | null;
  limit: number;
}

export interface ExerciseFilter {
  q: string | null;
  bodyPart: string | null;
  category: string | null;
  includeArchived: boolean;
}

export interface VolumeQuery {
  from: string;
  to: string;
  period: "week" | "month";
  by: "bodyPart" | "exercise";
}

type SummaryRow = {
  id: string;
  name: string;
  local_day: string;
  location: string | null;
  template_name: string | null;
  duration_seconds: number;
  perceived_effort: number | null;
  notes: string;
  exercises: number;
  working_sets: number;
  hard_sets: number;
  reps: number;
  volume: number;
  cardio_seconds: number;
};

type SetRow = {
  entry_id: string;
  set_number: number;
  reps: number;
  weight_kg: number;
  rir: number | null;
  rpe: number | null;
  duration_seconds: number | null;
  is_warmup: number;
  is_pr: number;
  tags: string;
  notes: string;
};

/** A set that counts as work: not a warm-up, with reps. */
export const WORKING = "x.is_warmup = 0 AND x.reps > 0";
/**
 * Ember's hard set (CONTEXT.md): a working set that ended within 4 reps of failure, or had no
 * effort logged. An RPE of 6 or more is within 4 reps.
 */
export const HARD = `${WORKING} AND (x.rir <= 4 OR (x.rir IS NULL AND (x.rpe IS NULL OR x.rpe >= 6)))`;

const SUMMARY_SELECT = `
SELECT s.id, s.name, s.local_day, l.name AS location, t.name AS template_name, s.duration_seconds, s.perceived_effort, s.notes,
  (SELECT COUNT(*) FROM shared_entries e WHERE e.session_id = s.id) AS exercises,
  (SELECT COUNT(*) FROM shared_sets x JOIN shared_entries e ON x.entry_id = e.id WHERE e.session_id = s.id AND ${WORKING}) AS working_sets,
  (SELECT COUNT(*) FROM shared_sets x JOIN shared_entries e ON x.entry_id = e.id WHERE e.session_id = s.id AND ${HARD}) AS hard_sets,
  (SELECT COALESCE(SUM(x.reps), 0) FROM shared_sets x JOIN shared_entries e ON x.entry_id = e.id WHERE e.session_id = s.id AND ${WORKING}) AS reps,
  (SELECT COALESCE(SUM(x.reps * x.weight_kg), 0) FROM shared_sets x JOIN shared_entries e ON x.entry_id = e.id WHERE e.session_id = s.id AND ${WORKING}) AS volume,
  (SELECT COALESCE(SUM(c.duration_seconds), 0) FROM shared_cardio c JOIN shared_entries e ON c.entry_id = e.id WHERE e.session_id = s.id) AS cardio_seconds
FROM shared_sessions s
  LEFT JOIN shared_templates t ON t.id = s.template_id
  LEFT JOIN shared_locations l ON l.id = s.location_id`;

/**
 * Reads the shared copy's workouts for Claude: summaries, one workout, the catalog, an exercise's
 * progress, and training volume per week or month. Only finished workouts count, warm-ups never
 * count toward volume or records, and days are the owner's local days. Weights are kilograms.
 */
export class WorkoutReader {
  constructor(private readonly sql: SqlStorage) {}

  overview(today: string) {
    const all = this.summaries("WHERE s.status = 'completed'", [], 100_000);
    const days = all.map((workout) => workout.day);
    const since = (count: number) => days.filter((day) => day > LocalDay.addDays(today, -count)).length;
    const top = this.sql
      .exec<{ exercise_id: string; exercise_name: string; body_part: string; workouts: number; last_day: string }>(
        `SELECT e.exercise_id, MAX(e.exercise_name) AS exercise_name, MAX(e.body_part) AS body_part,
           COUNT(DISTINCT s.id) AS workouts, MAX(s.local_day) AS last_day
         FROM shared_entries e JOIN shared_sessions s ON s.id = e.session_id
         WHERE s.status = 'completed' GROUP BY e.exercise_id ORDER BY workouts DESC, last_day DESC LIMIT 8`,
      )
      .toArray()
      .map((row) => ({ exerciseId: row.exercise_id, name: row.exercise_name, bodyPart: row.body_part, workouts: row.workouts, lastDay: row.last_day }));
    const active = this.sql
      .exec<{ id: string; name: string; started_at: string }>("SELECT id, name, started_at FROM shared_sessions WHERE status = 'active' ORDER BY started_at DESC LIMIT 1")
      .toArray()[0];
    const count = (table: string, where = "") => this.sql.exec<{ n: number }>(`SELECT COUNT(*) AS n FROM ${table} ${where}`).one().n;
    return {
      completedWorkouts: all.length,
      firstWorkoutDay: days.at(-1) ?? null,
      lastWorkoutDay: days[0] ?? null,
      workoutsLast7Days: since(7),
      workoutsLast30Days: since(30),
      workoutsLast90Days: since(90),
      currentWeeklyStreak: weeklyStreak(days, today),
      totalVolumeKg: round(all.reduce((sum, workout) => sum + workout.volumeKg, 0)),
      exercisesInCatalog: count("shared_exercises", "WHERE archived = 0"),
      templates: count("shared_templates"),
      locations: this.locations(),
      mostDoneExercises: top,
      recentWorkouts: all.slice(0, 5),
      activeWorkout: active ? { id: active.id, name: active.name, startedAt: active.started_at } : null,
    };
  }

  /** The owner's places, archived ones left out. */
  locations(): { id: string; name: string; kind: string }[] {
    return this.sql
      .exec<{ id: string; name: string; kind: string }>("SELECT id, name, kind FROM shared_locations WHERE archived = 0 ORDER BY kind, name")
      .toArray()
      .map((row) => ({ id: row.id, name: row.name, kind: row.kind }));
  }

  workouts(filter: WorkoutFilter): { workouts: WorkoutSummary[]; more: boolean } {
    const where = ["s.status = 'completed'"];
    const values: (string | number)[] = [];
    if (filter.from) {
      where.push("s.local_day >= ?");
      values.push(filter.from);
    }
    if (filter.to) {
      where.push("s.local_day <= ?");
      values.push(filter.to);
    }
    if (filter.locationId) {
      where.push("s.location_id = ?");
      values.push(filter.locationId);
    }
    if (filter.q) {
      where.push("(s.name LIKE ? OR s.notes LIKE ? OR EXISTS (SELECT 1 FROM shared_entries e WHERE e.session_id = s.id AND e.exercise_name LIKE ?))");
      const like = `%${filter.q}%`;
      values.push(like, like, like);
    }
    const rows = this.summaries(`WHERE ${where.join(" AND ")}`, values, filter.limit + 1);
    return { workouts: rows.slice(0, filter.limit), more: rows.length > filter.limit };
  }

  workout(id: string) {
    const summary = this.summaries("WHERE s.id = ?", [id], 1)[0];
    if (!summary) return null;
    const session = this.sql
      .exec<{ status: string; started_at: string; ended_at: string | null; bodyweight_kg: number | null }>(
        "SELECT status, started_at, ended_at, bodyweight_kg FROM shared_sessions WHERE id = ?",
        id,
      )
      .one();
    const entries = this.sql
      .exec<{ id: string; exercise_id: string; exercise_name: string; category: string; body_part: string; superset_group: number | null; notes: string }>(
        "SELECT id, exercise_id, exercise_name, category, body_part, superset_group, notes FROM shared_entries WHERE session_id = ? ORDER BY sort_order",
        id,
      )
      .toArray();
    const sets = this.setsOf(entries.map((entry) => entry.id));
    const cardio = this.cardioOf(entries.map((entry) => entry.id));
    return {
      ...summary,
      status: session.status,
      startedAt: session.started_at,
      endedAt: session.ended_at,
      bodyweightKg: session.bodyweight_kg,
      entries: entries.map((entry) => ({
        exerciseId: entry.exercise_id,
        name: entry.exercise_name,
        category: entry.category,
        bodyPart: entry.body_part,
        supersetGroup: entry.superset_group,
        notes: entry.notes,
        sets: (sets.get(entry.id) ?? []).map(setView),
        cardio: cardio.get(entry.id) ?? null,
      })),
    };
  }

  exercises(filter: ExerciseFilter) {
    const where: string[] = [];
    const values: string[] = [];
    if (!filter.includeArchived) where.push("x.archived = 0");
    if (filter.q) {
      where.push("(x.name LIKE ? OR x.equipment LIKE ?)");
      values.push(`%${filter.q}%`, `%${filter.q}%`);
    }
    if (filter.bodyPart) {
      where.push("(x.primary_body_part LIKE ? OR x.secondary_body_parts LIKE ?)");
      values.push(filter.bodyPart, `%"${filter.bodyPart}"%`);
    }
    if (filter.category) {
      where.push("x.category = ?");
      values.push(filter.category);
    }
    return this.sql
      .exec<{
        id: string;
        name: string;
        category: string;
        primary_body_part: string;
        secondary_body_parts: string;
        equipment: string;
        notes: string;
        instructions: string;
        source: string;
        default_rest_seconds: number | null;
        track_per_location: number;
        archived: number;
        workouts: number;
        last_day: string | null;
      }>(
        `SELECT x.*,
           (SELECT COUNT(DISTINCT e.session_id) FROM shared_entries e JOIN shared_sessions s ON s.id = e.session_id WHERE e.exercise_id = x.id AND s.status = 'completed') AS workouts,
           (SELECT MAX(s.local_day) FROM shared_entries e JOIN shared_sessions s ON s.id = e.session_id WHERE e.exercise_id = x.id AND s.status = 'completed') AS last_day
         FROM shared_exercises x ${where.length > 0 ? `WHERE ${where.join(" AND ")}` : ""} ORDER BY x.name`,
        ...values,
      )
      .toArray()
      .map((row) => ({
        id: row.id,
        name: row.name,
        category: row.category,
        primaryBodyPart: row.primary_body_part,
        secondaryBodyParts: JSON.parse(row.secondary_body_parts) as string[],
        equipment: row.equipment,
        note: row.notes,
        hasSteps: row.instructions.trim().length > 0,
        source: row.source,
        defaultRestSeconds: row.default_rest_seconds,
        trackPerGym: row.track_per_location === 1,
        archived: row.archived === 1,
        workouts: row.workouts,
        lastDay: row.last_day,
      }));
  }

  /** One exercise of the catalog, or null. */
  exercise(id: string): { id: string; name: string; category: string; bodyPart: string; archived: boolean } | null {
    const row = this.sql
      .exec<{ id: string; name: string; category: string; primary_body_part: string; archived: number }>(
        "SELECT id, name, category, primary_body_part, archived FROM shared_exercises WHERE id = ?",
        id,
      )
      .toArray()[0];
    return row ? { id: row.id, name: row.name, category: row.category, bodyPart: row.primary_body_part, archived: row.archived === 1 } : null;
  }

  templateExists(id: string): boolean {
    return this.sql.exec("SELECT 1 FROM shared_templates WHERE id = ?", id).toArray().length > 0;
  }

  /**
   * Every finished workout of one exercise in a range, oldest first: its working sets, best set and
   * estimated 1RM (Epley), at which location; with records and the trend. Warm-ups are left out.
   */
  exerciseProgress(exerciseId: string, from: string | null, to: string | null, limit: number) {
    const sessions = this.sql
      .exec<{ entry_id: string; session_id: string; local_day: string; name: string; location: string | null; exercise_name: string; category: string }>(
        `SELECT e.id AS entry_id, s.id AS session_id, s.local_day, s.name, l.name AS location, e.exercise_name, e.category
         FROM shared_entries e JOIN shared_sessions s ON s.id = e.session_id LEFT JOIN shared_locations l ON l.id = s.location_id
         WHERE e.exercise_id = ? AND s.status = 'completed' AND s.local_day >= ? AND s.local_day <= ?
         ORDER BY s.started_at DESC LIMIT ?`,
        exerciseId,
        from ?? "0000-01-01",
        to ?? "9999-12-31",
        limit,
      )
      .toArray()
      .reverse();
    const sets = this.setsOf(sessions.map((session) => session.entry_id));
    const cardio = this.cardioOf(sessions.map((session) => session.entry_id));
    const points = sessions.map((session) => {
      const done = (sets.get(session.entry_id) ?? []).filter((set) => set.reps > 0 && set.is_warmup === 0);
      const best = done.reduce<SetRow | null>((top, set) => (top === null || estimatedMax(set) > estimatedMax(top) ? set : top), null);
      return {
        day: session.local_day,
        workoutId: session.session_id,
        workout: session.name,
        location: session.location ?? "Unknown location",
        sets: done.map((set) => `${set.weight_kg}x${set.reps}${set.rir !== null ? ` RIR ${set.rir}` : set.rpe !== null ? ` RPE ${set.rpe}` : ""}`).join(", "),
        setCount: done.length,
        totalReps: done.reduce((sum, set) => sum + set.reps, 0),
        mostReps: done.reduce((max, set) => Math.max(max, set.reps), 0),
        volumeKg: round(done.reduce((sum, set) => sum + set.reps * set.weight_kg, 0)),
        topWeightKg: done.reduce((max, set) => Math.max(max, set.weight_kg), 0),
        bestSet: best ? { weightKg: best.weight_kg, reps: best.reps, rir: best.rir, rpe: best.rpe } : null,
        estimatedOneRepMaxKg: best && best.weight_kg > 0 ? round(estimatedMax(best)) : null,
        cardio: cardio.get(session.entry_id) ?? null,
      };
    });
    const loaded = points.filter((point) => point.estimatedOneRepMaxKg !== null);
    const byMax = (pick: (point: (typeof points)[number]) => number) =>
      points.reduce<(typeof points)[number] | null>((top, point) => (top === null || pick(point) > pick(top) ? point : top), null);
    const heaviest = byMax((point) => point.topWeightKg);
    const strongest = byMax((point) => point.estimatedOneRepMaxKg ?? 0);
    const biggest = byMax((point) => point.volumeKg);
    const mostReps = byMax((point) => point.mostReps);
    return {
      exerciseId,
      name: sessions.at(-1)?.exercise_name ?? this.exercise(exerciseId)?.name ?? null,
      category: sessions.at(-1)?.category ?? this.exercise(exerciseId)?.category ?? null,
      sessions: points,
      records:
        points.length === 0
          ? null
          : {
              heaviestWeight: heaviest && heaviest.topWeightKg > 0 ? { day: heaviest.day, weightKg: heaviest.topWeightKg } : null,
              bestEstimatedOneRepMax: strongest && strongest.estimatedOneRepMaxKg ? { day: strongest.day, kg: strongest.estimatedOneRepMaxKg, set: strongest.bestSet } : null,
              mostVolume: biggest && biggest.volumeKg > 0 ? { day: biggest.day, volumeKg: biggest.volumeKg } : null,
              mostReps: mostReps && mostReps.mostReps > 0 ? { day: mostReps.day, reps: mostReps.mostReps } : null,
            },
      trend:
        loaded.length < 2
          ? null
          : {
              firstDay: loaded[0]!.day,
              firstEstimatedOneRepMaxKg: loaded[0]!.estimatedOneRepMaxKg,
              lastDay: loaded.at(-1)!.day,
              lastEstimatedOneRepMaxKg: loaded.at(-1)!.estimatedOneRepMaxKg,
              changePercent: round(((loaded.at(-1)!.estimatedOneRepMaxKg! - loaded[0]!.estimatedOneRepMaxKg!) / loaded[0]!.estimatedOneRepMaxKg!) * 100),
            },
    };
  }

  /** Working sets, hard sets, reps and volume per week or month, by body part or exercise. */
  volume(query: VolumeQuery) {
    const rows = this.sql
      .exec<{ local_day: string; session_id: string; key: string; category: string; working_sets: number; hard_sets: number; reps: number; volume: number; cardio_seconds: number }>(
        `SELECT s.local_day, s.id AS session_id, ${query.by === "bodyPart" ? "e.body_part" : "e.exercise_name"} AS key, e.category,
           (SELECT COUNT(*) FROM shared_sets x WHERE x.entry_id = e.id AND ${WORKING}) AS working_sets,
           (SELECT COUNT(*) FROM shared_sets x WHERE x.entry_id = e.id AND ${HARD}) AS hard_sets,
           (SELECT COALESCE(SUM(x.reps), 0) FROM shared_sets x WHERE x.entry_id = e.id AND ${WORKING}) AS reps,
           (SELECT COALESCE(SUM(x.reps * x.weight_kg), 0) FROM shared_sets x WHERE x.entry_id = e.id AND ${WORKING}) AS volume,
           (SELECT COALESCE(SUM(c.duration_seconds), 0) FROM shared_cardio c WHERE c.entry_id = e.id) AS cardio_seconds
         FROM shared_sessions s JOIN shared_entries e ON e.session_id = s.id
         WHERE s.status = 'completed' AND s.local_day >= ? AND s.local_day <= ?`,
        query.from,
        query.to,
      )
      .toArray();
    type Group = { workingSets: number; hardSets: number; reps: number; volume: number };
    const periods = new Map<string, { workouts: Set<string>; cardioSeconds: number; groups: Map<string, Group> }>();
    for (const row of rows) {
      const period = query.period === "week" ? LocalDay.weekStart(row.local_day) : LocalDay.month(row.local_day);
      const bucket = periods.get(period) ?? { workouts: new Set(), cardioSeconds: 0, groups: new Map() };
      periods.set(period, bucket);
      bucket.workouts.add(row.session_id);
      bucket.cardioSeconds += row.cardio_seconds;
      if (row.category !== "strength") continue;
      const key = row.key.length > 0 ? row.key : "(none)";
      const group = bucket.groups.get(key) ?? { workingSets: 0, hardSets: 0, reps: 0, volume: 0 };
      bucket.groups.set(key, {
        workingSets: group.workingSets + row.working_sets,
        hardSets: group.hardSets + row.hard_sets,
        reps: group.reps + row.reps,
        volume: group.volume + row.volume,
      });
    }
    return {
      from: query.from,
      to: query.to,
      period: query.period,
      by: query.by,
      periods: [...periods.entries()]
        .sort(([a], [b]) => a.localeCompare(b))
        .map(([period, bucket]) => ({
          period: query.period === "week" ? `week of ${period}` : period,
          workouts: bucket.workouts.size,
          cardioMinutes: Math.round(bucket.cardioSeconds / 60),
          groups: [...bucket.groups.entries()]
            .sort(([, a], [, b]) => b.hardSets - a.hardSets)
            .map(([key, group]) => ({ [query.by]: key, hardSets: group.hardSets, workingSets: group.workingSets, reps: group.reps, volumeKg: round(group.volume) })),
        })),
    };
  }

  /** Templates with each exercise's plan: sets, rep range, rest and superset group. */
  templates() {
    const items = this.sql
      .exec<{
        template_id: string;
        exercise_id: string;
        exercise_name: string;
        category: string;
        body_part: string;
        notes: string;
        target_sets: number | null;
        reps_min: number | null;
        reps_max: number | null;
        rest_seconds: number | null;
        superset_group: number | null;
      }>("SELECT * FROM shared_template_exercises ORDER BY template_id, sort_order")
      .toArray();
    return this.sql
      .exec<{ id: string; name: string; notes: string }>("SELECT id, name, notes FROM shared_templates ORDER BY name")
      .toArray()
      .map((template) => ({
        id: template.id,
        name: template.name,
        notes: template.notes,
        exercises: items
          .filter((item) => item.template_id === template.id)
          .map((item) => ({
            exerciseId: item.exercise_id,
            name: item.exercise_name,
            category: item.category,
            bodyPart: item.body_part,
            targetSets: item.target_sets,
            repsMin: item.reps_min,
            repsMax: item.reps_max,
            restSeconds: item.rest_seconds,
            supersetGroup: item.superset_group,
            notes: item.notes,
          })),
      }));
  }

  private summaries(where: string, values: (string | number)[], limit: number): WorkoutSummary[] {
    return this.sql
      .exec<SummaryRow>(`${SUMMARY_SELECT} ${where} ORDER BY s.started_at DESC LIMIT ?`, ...values, limit)
      .toArray()
      .map((row) => ({
        id: row.id,
        name: row.name,
        day: row.local_day,
        location: row.location ?? "Unknown location",
        template: row.template_name,
        durationMinutes: Math.round(row.duration_seconds / 60),
        exercises: row.exercises,
        workingSets: row.working_sets,
        hardSets: row.hard_sets,
        reps: row.reps,
        volumeKg: round(row.volume),
        cardioMinutes: Math.round(row.cardio_seconds / 60),
        effort: row.perceived_effort,
        notes: row.notes,
      }));
  }

  private setsOf(entryIds: string[]): Map<string, SetRow[]> {
    const byEntry = new Map<string, SetRow[]>();
    for (const chunk of chunks(entryIds, 90)) {
      const rows = this.sql
        .exec<SetRow>(
          `SELECT entry_id, set_number, reps, weight_kg, rir, rpe, duration_seconds, is_warmup, is_pr, tags, notes FROM shared_sets WHERE entry_id IN (${chunk.map(() => "?").join(", ")}) ORDER BY set_number`,
          ...chunk,
        )
        .toArray();
      for (const row of rows) byEntry.set(row.entry_id, [...(byEntry.get(row.entry_id) ?? []), row]);
    }
    return byEntry;
  }

  private cardioOf(entryIds: string[]): Map<string, { minutes: number; distanceKm: number | null; calories: number | null; notes: string }> {
    const byEntry = new Map<string, { minutes: number; distanceKm: number | null; calories: number | null; notes: string }>();
    for (const chunk of chunks(entryIds, 90)) {
      const rows = this.sql
        .exec<{ entry_id: string; duration_seconds: number; distance_km: number | null; calories: number | null; notes: string }>(
          `SELECT entry_id, duration_seconds, distance_km, calories, notes FROM shared_cardio WHERE entry_id IN (${chunk.map(() => "?").join(", ")})`,
          ...chunk,
        )
        .toArray();
      for (const row of rows) {
        byEntry.set(row.entry_id, { minutes: round(row.duration_seconds / 60), distanceKm: row.distance_km, calories: row.calories, notes: row.notes });
      }
    }
    return byEntry;
  }
}

/** Epley's estimate of a one-rep max (CONTEXT.md, "Estimated 1RM"); a single is its own weight. */
export function estimatedMax(set: { reps: number; weight_kg: number }): number {
  if (set.reps <= 0) return 0;
  return set.reps === 1 ? set.weight_kg : set.weight_kg * (1 + set.reps / 30);
}

/** Weeks in a row with at least one of [days], up to this week, or up to last week while this one is empty. */
export function weeklyStreak(days: readonly string[], today: string): number {
  const weeks = new Set(days.map((day) => LocalDay.weekStart(day)));
  let week = LocalDay.weekStart(today);
  if (!weeks.has(week)) week = LocalDay.addDays(week, -7);
  let streak = 0;
  while (weeks.has(week)) {
    streak++;
    week = LocalDay.addDays(week, -7);
  }
  return streak;
}

function setView(set: SetRow) {
  return {
    setNumber: set.set_number,
    reps: set.reps,
    weightKg: set.weight_kg,
    rir: set.rir,
    rpe: set.rpe,
    durationSeconds: set.duration_seconds,
    warmup: set.is_warmup === 1,
    personalRecord: set.is_pr === 1,
    tags: set.tags.length > 0 ? set.tags.split(",") : [],
    notes: set.notes,
  };
}

export function round(value: number): number {
  return Math.round(value * 10) / 10;
}

function chunks<T>(values: readonly T[], size: number): T[][] {
  const result: T[][] = [];
  for (let index = 0; index < values.length; index += size) result.push(values.slice(index, index + size));
  return result;
}
