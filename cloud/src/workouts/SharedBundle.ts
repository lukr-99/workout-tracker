import { LocalDay } from "./LocalDay";

/** The most rows one share may hold, so a broken or hostile body cannot fill the object's storage. */
const ROW_LIMIT = 300_000;
const TEXT_MAX = 4000;
/** The share formats this Worker reads (the app's `ClaudeShare.FORMAT`). */
export const SHARE_FORMATS = [1] as const;

// Ember's enums travel as ordinals (domain/OrdinalEnumSerializer.kt); these are their names in order.
const CATEGORIES = ["strength", "cardio"] as const;
const SOURCES = ["seeded", "synced", "custom"] as const;
const STATUSES = ["active", "completed", "discarded"] as const;
const LOCATION_KINDS = ["gym", "home", "outdoors"] as const;
const SET_TAGS = ["Warmup", "Drop", "ToFailure", "Failed", "Negative", "BackOff", "Finisher", "Explosive", "SlowEccentric", "SlowConcentric"] as const;
/** The legacy single set type; 1 is Warmup. */
const SET_TYPE_WARMUP = 1;

export type Category = (typeof CATEGORIES)[number];

export interface ExerciseRow {
  id: string;
  name: string;
  category: Category;
  primaryBodyPart: string;
  secondaryBodyParts: string[];
  equipment: string;
  notes: string;
  instructions: string;
  source: string;
  defaultRestSeconds: number | null;
  trackPerLocation: boolean;
  archived: boolean;
}

export interface LocationRow {
  id: string;
  name: string;
  kind: string;
  archived: boolean;
}

export interface TemplateExerciseRow {
  id: string;
  exerciseId: string;
  exerciseName: string;
  category: Category;
  bodyPart: string;
  sortOrder: number;
  notes: string;
  targetSets: number | null;
  repsMin: number | null;
  repsMax: number | null;
  restSeconds: number | null;
  supersetGroup: number | null;
}

export interface TemplateRow {
  id: string;
  name: string;
  notes: string;
  exercises: TemplateExerciseRow[];
}

export interface SetRow {
  id: string;
  setNumber: number;
  reps: number;
  weightKg: number;
  rir: number | null;
  rpe: number | null;
  durationSeconds: number | null;
  isWarmup: boolean;
  isPr: boolean;
  tags: string[];
  notes: string;
}

export interface EntryRow {
  id: string;
  exerciseId: string;
  exerciseName: string;
  category: Category;
  bodyPart: string;
  sortOrder: number;
  supersetGroup: number | null;
  notes: string;
  sets: SetRow[];
  cardio: { durationSeconds: number; distanceKm: number | null; calories: number | null; notes: string } | null;
}

export interface SessionRow {
  id: string;
  templateId: string | null;
  name: string;
  status: string;
  startedAt: string;
  endedAt: string | null;
  localDay: string;
  durationSeconds: number;
  notes: string;
  locationId: string | null;
  perceivedEffort: number | null;
  bodyweightKg: number | null;
  entries: EntryRow[];
}

export interface RunRow {
  id: string;
  startedAt: string;
  localDay: string;
  durationSeconds: number;
  movingSeconds: number;
  distanceMeters: number;
  avgPaceSecPerKm: number;
  elevationGainM: number;
  calories: number | null;
  avgHr: number | null;
  routeId: string | null;
  notes: string;
  splits: { index: number; distanceMeters: number; durationSeconds: number; paceSecPerKm: number; elevationGainM: number | null; isFull: boolean }[];
}

export interface RouteRow {
  id: string;
  name: string;
  distanceMeters: number;
  elevationGainM: number;
  notes: string;
}

/** One share from the phone, read and checked. */
export interface SharedBundle {
  timeZone: string;
  units: "metric" | "imperial";
  appVersion: string | null;
  formatVersion: string;
  exercises: ExerciseRow[];
  locations: LocationRow[];
  templates: TemplateRow[];
  sessions: SessionRow[];
  runs: RunRow[];
  routes: RouteRow[];
}

export type ParsedShare = { ok: true; bundle: SharedBundle } | { ok: false; problems: string[] };

/**
 * Reads Ember's share, `ClaudeShare` in app/.../data/claude: `{shareFormat, timeZone, units,
 * appVersion, bundle, runs}`. `bundle` is the backup's `ExportBundle` (format 1.13, enums as
 * ordinals, workout times as ISO-8601) without photos, settings or runs; `runs` are the runs without
 * their GPS trace, with the splits the phone worked out, and times as ISO-8601. Unknown fields are
 * ignored, missing text becomes empty, and every problem comes back at once.
 */
export class SharedBundleParser {
  private readonly problems: string[] = [];
  private rows = 0;

  static parse(body: unknown): ParsedShare {
    return new SharedBundleParser().read(body);
  }

  private read(body: unknown): ParsedShare {
    if (!isObject(body) || !isObject(body.bundle)) return { ok: false, problems: ["The body must be a share: {shareFormat, timeZone, units, bundle, runs}."] };
    if (!SHARE_FORMATS.includes(body.shareFormat as never)) {
      return { ok: false, problems: [`shareFormat must be one of ${SHARE_FORMATS.join(", ")}; update the Worker or the app.`] };
    }
    const timeZone = typeof body.timeZone === "string" && LocalDay.isTimeZone(body.timeZone) ? body.timeZone : "UTC";
    const bundle = body.bundle;
    const parsed: SharedBundle = {
      timeZone,
      units: body.units === "imperial" ? "imperial" : "metric",
      appVersion: typeof body.appVersion === "string" ? body.appVersion.slice(0, 40) : null,
      formatVersion: typeof bundle.exportFormatVersion === "string" ? bundle.exportFormatVersion : "1.13",
      exercises: this.list(bundle.exercises, "bundle.exercises").map((value, index) => this.exercise(value, `bundle.exercises[${index}]`)),
      locations: this.list(bundle.locations, "bundle.locations").map((value, index) => this.location(value, `bundle.locations[${index}]`)),
      templates: this.list(bundle.templates, "bundle.templates").map((value, index) => this.template(value, `bundle.templates[${index}]`)),
      sessions: this.list(bundle.sessions, "bundle.sessions").map((value, index) => this.session(value, `bundle.sessions[${index}]`, timeZone)),
      runs: this.list(body.runs, "runs").map((value, index) => this.run(value, `runs[${index}]`, timeZone)),
      routes: this.list(bundle.routes, "bundle.routes").map((value, index) => this.route(value, `bundle.routes[${index}]`)),
    };
    if (this.rows > ROW_LIMIT) this.problems.push(`A share may hold at most ${ROW_LIMIT} rows.`);
    if (this.problems.length > 0) return { ok: false, problems: this.problems.slice(0, 50) };
    return { ok: true, bundle: parsed };
  }

  private exercise(value: unknown, at: string): ExerciseRow {
    const item = this.object(value, at);
    return {
      id: this.id(item.id, `${at}.id`),
      name: this.text(item.name, `${at}.name`),
      category: this.choice(item.category, CATEGORIES, "strength", `${at}.category`),
      primaryBodyPart: this.text(item.primaryBodyPart, `${at}.primaryBodyPart`),
      secondaryBodyParts: this.list(item.secondaryBodyParts, `${at}.secondaryBodyParts`).map((part, index) => this.text(part, `${at}.secondaryBodyParts[${index}]`)),
      equipment: this.text(item.equipment, `${at}.equipment`),
      notes: this.text(item.notes, `${at}.notes`),
      instructions: this.text(item.instructions, `${at}.instructions`),
      source: this.choice(item.source, SOURCES, "custom", `${at}.source`),
      defaultRestSeconds: this.optionalInteger(item.defaultRestSeconds, `${at}.defaultRestSeconds`),
      trackPerLocation: item.trackPerLocation === true,
      archived: item.isArchived === true,
    };
  }

  private location(value: unknown, at: string): LocationRow {
    const item = this.object(value, at);
    return {
      id: this.id(item.id, `${at}.id`),
      name: this.text(item.name, `${at}.name`),
      kind: this.choice(item.kind, LOCATION_KINDS, "gym", `${at}.kind`),
      archived: item.isArchived === true,
    };
  }

  private template(value: unknown, at: string): TemplateRow {
    const item = this.object(value, at);
    return {
      id: this.id(item.id, `${at}.id`),
      name: this.text(item.name, `${at}.name`),
      notes: this.text(item.notes, `${at}.notes`),
      exercises: this.list(item.exercises, `${at}.exercises`).map((child, index) => {
        const where = `${at}.exercises[${index}]`;
        const exercise = this.object(child, where);
        return {
          id: this.id(exercise.id, `${where}.id`),
          exerciseId: this.id(exercise.exerciseId, `${where}.exerciseId`),
          exerciseName: this.text(exercise.exerciseName, `${where}.exerciseName`),
          category: this.choice(exercise.category, CATEGORIES, "strength", `${where}.category`),
          bodyPart: this.text(exercise.bodyPart, `${where}.bodyPart`),
          sortOrder: this.integer(exercise.sortOrder, index, `${where}.sortOrder`),
          notes: this.text(exercise.notes, `${where}.notes`),
          targetSets: this.optionalInteger(exercise.targetSets, `${where}.targetSets`),
          repsMin: this.optionalInteger(exercise.repsMin, `${where}.repsMin`),
          repsMax: this.optionalInteger(exercise.repsMax, `${where}.repsMax`),
          restSeconds: this.optionalInteger(exercise.restSeconds, `${where}.restSeconds`),
          supersetGroup: this.optionalInteger(exercise.supersetGroup, `${where}.supersetGroup`),
        };
      }),
    };
  }

  private session(value: unknown, at: string, timeZone: string): SessionRow {
    const item = this.object(value, at);
    const startedAt = this.instant(item.startedAtUtc, `${at}.startedAtUtc`) ?? "1970-01-01T00:00:00.000Z";
    return {
      id: this.id(item.id, `${at}.id`),
      templateId: this.optionalId(item.templateId),
      name: this.text(item.name, `${at}.name`),
      status: this.choice(item.status, STATUSES, "completed", `${at}.status`),
      startedAt,
      endedAt: item.endedAtUtc == null ? null : this.instant(item.endedAtUtc, `${at}.endedAtUtc`),
      localDay: LocalDay.of(startedAt, timeZone),
      durationSeconds: this.integer(item.durationSeconds, 0, `${at}.durationSeconds`),
      notes: this.text(item.notes, `${at}.notes`),
      locationId: this.optionalId(item.locationId),
      perceivedEffort: this.optionalInteger(item.perceivedEffort, `${at}.perceivedEffort`),
      bodyweightKg: this.number(item.bodyweightKg, `${at}.bodyweightKg`),
      entries: this.list(item.entries, `${at}.entries`).map((entry, index) => this.entry(entry, `${at}.entries[${index}]`)),
    };
  }

  private entry(value: unknown, at: string): EntryRow {
    const item = this.object(value, at);
    const cardio = isObject(item.cardioData) ? item.cardioData : null;
    return {
      id: this.id(item.id, `${at}.id`),
      exerciseId: this.id(item.exerciseId, `${at}.exerciseId`),
      exerciseName: this.text(item.exerciseSnapshotName, `${at}.exerciseSnapshotName`),
      category: this.choice(item.entryType ?? item.exerciseSnapshotCategory, CATEGORIES, "strength", `${at}.entryType`),
      bodyPart: this.text(item.exerciseSnapshotPrimaryBodyPart, `${at}.exerciseSnapshotPrimaryBodyPart`),
      sortOrder: this.integer(item.sortOrder, 0, `${at}.sortOrder`),
      supersetGroup: this.optionalInteger(item.supersetGroup, `${at}.supersetGroup`),
      notes: this.text(item.notes, `${at}.notes`),
      sets: this.list(item.strengthSets, `${at}.strengthSets`).map((child, index) => this.set(child, `${at}.strengthSets[${index}]`, index)),
      cardio: cardio
        ? {
            durationSeconds: this.integer(cardio.durationSeconds, 0, `${at}.cardioData.durationSeconds`),
            distanceKm: this.number(cardio.distanceKm, `${at}.cardioData.distanceKm`),
            calories: this.number(cardio.calories, `${at}.cardioData.calories`),
            notes: this.text(cardio.notes, `${at}.cardioData.notes`),
          }
        : null,
    };
  }

  private set(value: unknown, at: string, index: number): SetRow {
    const set = this.object(value, at);
    const tags = this.list(set.tags, `${at}.tags`).map((tag, tagIndex) => this.choice(tag, SET_TAGS, "Warmup", `${at}.tags[${tagIndex}]`));
    return {
      id: this.id(set.id, `${at}.id`),
      setNumber: this.integer(set.setNumber, index + 1, `${at}.setNumber`),
      reps: this.integer(set.reps, 0, `${at}.reps`),
      weightKg: this.number(set.weightKg, `${at}.weightKg`) ?? 0,
      rir: this.number(set.rir, `${at}.rir`),
      rpe: this.number(set.rpe, `${at}.rpe`),
      durationSeconds: this.optionalInteger(set.durationSeconds, `${at}.durationSeconds`),
      isWarmup: set.isWarmup === true || tags.includes("Warmup") || set.setType === SET_TYPE_WARMUP,
      isPr: set.isPr === true,
      tags,
      notes: this.text(set.notes, `${at}.notes`),
    };
  }

  private run(value: unknown, at: string, timeZone: string): RunRow {
    const item = this.object(value, at);
    const startedAt = this.instant(item.startedAtUtc, `${at}.startedAtUtc`) ?? "1970-01-01T00:00:00.000Z";
    return {
      id: this.id(item.id, `${at}.id`),
      startedAt,
      localDay: LocalDay.of(startedAt, timeZone),
      durationSeconds: this.integer(item.durationSeconds, 0, `${at}.durationSeconds`),
      movingSeconds: this.integer(item.movingSeconds, 0, `${at}.movingSeconds`),
      distanceMeters: this.number(item.distanceMeters, `${at}.distanceMeters`) ?? 0,
      avgPaceSecPerKm: this.number(item.avgPaceSecPerKm, `${at}.avgPaceSecPerKm`) ?? 0,
      elevationGainM: this.number(item.elevationGainM, `${at}.elevationGainM`) ?? 0,
      calories: this.number(item.calories, `${at}.calories`),
      avgHr: this.optionalInteger(item.avgHr, `${at}.avgHr`),
      routeId: this.optionalId(item.routeId),
      notes: this.text(item.notes, `${at}.notes`),
      splits: this.list(item.splits, `${at}.splits`).map((child, index) => {
        const where = `${at}.splits[${index}]`;
        const split = this.object(child, where);
        return {
          index: this.integer(split.index, index + 1, `${where}.index`),
          distanceMeters: this.number(split.distanceMeters, `${where}.distanceMeters`) ?? 0,
          durationSeconds: this.integer(split.durationSeconds, 0, `${where}.durationSeconds`),
          paceSecPerKm: this.number(split.paceSecPerKm, `${where}.paceSecPerKm`) ?? 0,
          elevationGainM: this.number(split.elevationGainM, `${where}.elevationGainM`),
          isFull: split.isFull !== false,
        };
      }),
    };
  }

  private route(value: unknown, at: string): RouteRow {
    const item = this.object(value, at);
    return {
      id: this.id(item.id, `${at}.id`),
      name: this.text(item.name, `${at}.name`),
      distanceMeters: this.number(item.distanceMeters, `${at}.distanceMeters`) ?? 0,
      elevationGainM: this.number(item.elevationGainM, `${at}.elevationGainM`) ?? 0,
      notes: this.text(item.notes, `${at}.notes`),
    };
  }

  private list(value: unknown, at: string): unknown[] {
    if (value == null) return [];
    if (!Array.isArray(value)) {
      this.problems.push(`${at} must be a list.`);
      return [];
    }
    this.rows += value.length;
    return value;
  }

  private object(value: unknown, at: string): Record<string, unknown> {
    if (isObject(value)) return value;
    this.problems.push(`${at} must be an object.`);
    return {};
  }

  private id(value: unknown, at: string): string {
    if (typeof value === "string" && value.length > 0 && value.length <= 64) return value;
    this.problems.push(`${at} must be an id of 1 to 64 characters.`);
    return "";
  }

  private optionalId(value: unknown): string | null {
    return typeof value === "string" && value.length > 0 && value.length <= 64 ? value : null;
  }

  private text(value: unknown, at: string): string {
    if (value == null) return "";
    if (typeof value !== "string") {
      this.problems.push(`${at} must be text.`);
      return "";
    }
    return value.slice(0, TEXT_MAX);
  }

  private integer(value: unknown, fallback: number, at: string): number {
    if (value == null) return fallback;
    if (typeof value === "number" && Number.isFinite(value)) return Math.round(value);
    this.problems.push(`${at} must be a number.`);
    return fallback;
  }

  private optionalInteger(value: unknown, at: string): number | null {
    return value == null ? null : this.integer(value, 0, at);
  }

  private number(value: unknown, at: string): number | null {
    if (value == null) return null;
    if (typeof value === "number" && Number.isFinite(value)) return value;
    this.problems.push(`${at} must be a number.`);
    return null;
  }

  /** An enum as its ordinal, or its name in any case. */
  private choice<T extends string>(value: unknown, names: readonly T[], fallback: T, at: string): T {
    if (value == null) return fallback;
    if (typeof value === "number" && Number.isInteger(value) && value >= 0 && value < names.length) return names[value]!;
    if (typeof value === "string") {
      const name = names.find((candidate) => candidate.toLowerCase() === value.toLowerCase());
      if (name) return name;
    }
    this.problems.push(`${at} must be one of ${names.join(", ")}.`);
    return fallback;
  }

  /** A UTC instant: ISO-8601 with a zone (Ember's InstantMillisSerializer), or epoch milliseconds. */
  private instant(value: unknown, at: string): string | null {
    if (typeof value === "number" && Number.isFinite(value) && value > 0) return new Date(value).toISOString();
    if (typeof value === "string") {
      const zoned = /(Z|[+-]\d{2}:\d{2})$/.test(value) ? value : `${value}Z`;
      const time = Date.parse(zoned);
      if (!Number.isNaN(time)) return new Date(time).toISOString();
    }
    this.problems.push(`${at} must be a date and time.`);
    return null;
  }
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
