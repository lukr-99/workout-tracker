/** Ids as Ember makes them (UUID strings), readable in tests. */
export const IDS = {
  bench: "00000000-0000-4000-8000-000000000001",
  squat: "00000000-0000-4000-8000-000000000002",
  row: "00000000-0000-4000-8000-000000000003",
  pullUp: "00000000-0000-4000-8000-000000000006",
  oldCurl: "00000000-0000-4000-8000-000000000005",
  pushTemplate: "00000000-0000-4000-8000-0000000000a1",
  gym: "00000000-0000-4000-8000-0000000000b1",
  longRun: "00000000-0000-4000-8000-0000000000c1",
  easyRun: "00000000-0000-4000-8000-0000000000c2",
  riverRoute: "00000000-0000-4000-8000-0000000000d1",
};

let counter = 0;
const nextId = () => `10000000-0000-4000-8000-${(++counter).toString(16).padStart(12, "0")}`;

/** Noon UTC [days] days ago, as Ember's InstantMillisSerializer writes it. */
function daysAgo(days: number): string {
  return new Date(Date.now() - days * 86_400_000).toISOString().slice(0, 10) + "T12:00:00Z";
}

/** A working set [weight, reps, rir]; tags are SetTag ordinals (0 Warmup, 2 ToFailure, 6 Finisher). */
type SetSpec = [number, number, number | null] | { warmup: [number, number] } | { tagged: [number, number, number[]] };

function strength(exerciseId: string, name: string, bodyPart: string, sets: SetSpec[], supersetGroup: number | null = null) {
  const id = nextId();
  return {
    id,
    workoutSessionId: "",
    exerciseId,
    exerciseSnapshotName: name,
    exerciseSnapshotCategory: 0,
    exerciseSnapshotPrimaryBodyPart: bodyPart,
    sortOrder: 0,
    entryType: 0,
    notes: "",
    supersetGroup,
    weightUnitOverride: null,
    startedAtUtc: null,
    completedAtUtc: null,
    strengthSets: sets.map((spec, index) => {
      const base = { id: nextId(), workoutEntryId: id, setNumber: index + 1, rpe: null, performedAtUtc: null, notes: "", isPr: false, durationSeconds: null, setType: 0 };
      if (Array.isArray(spec)) return { ...base, weightKg: spec[0], reps: spec[1], rir: spec[2], isWarmup: false, tags: [] };
      if ("warmup" in spec) return { ...base, weightKg: spec.warmup[0], reps: spec.warmup[1], rir: null, isWarmup: true, tags: [0] };
      return { ...base, weightKg: spec.tagged[0], reps: spec.tagged[1], rir: null, isWarmup: false, tags: spec.tagged[2] };
    }),
    cardioData: null,
  };
}

function session(name: string, days: number, entries: unknown[], status = 1, locationId: string | null = IDS.gym) {
  return {
    id: nextId(),
    templateId: name === "Push A" ? IDS.pushTemplate : null,
    name,
    startedAtUtc: daysAgo(days),
    endedAtUtc: null,
    completedDateUtc: daysAgo(days),
    durationSeconds: 3600,
    notes: name === "Legs" ? "knees felt good" : "",
    status,
    perceivedEffort: 7,
    bodyweightKg: null,
    source: 0,
    externalKey: null,
    locationId,
    entries,
  };
}

const times = (weight: number, reps: number, rir: number | null, count = 3): SetSpec[] => Array.from({ length: count }, () => [weight, reps, rir] as SetSpec);

function split(index: number, paceSecPerKm: number, distanceMeters = 1000, isFull = true) {
  return { index, distanceMeters, durationSeconds: Math.round((paceSecPerKm * distanceMeters) / 1000), paceSecPerKm, elevationGainM: 4, isFull };
}

/**
 * Ember's share (`ClaudeShare`): bench 80, 82.5, 85 kg for 3x5 at RIR 2 over two weeks with a warm-up
 * each time, a pull day with an easy back-off set, a leg day at Home, a workout in progress and a
 * discarded one; and two runs with splits. Completed working volume: 1200 + 1440 + 1237.5 + 1500 +
 * 1275 + 100 (pull-ups 10x10 bodyweight logged as 10 kg) = 6752.5 kg.
 */
export function share() {
  return {
    shareFormat: 1,
    timeZone: "Europe/Prague",
    units: "metric",
    appVersion: "2.11.0",
    bundle: {
      exportedAtUtc: new Date().toISOString(),
      exportFormatVersion: "1.13",
      exercises: [
        { id: IDS.bench, name: "Bench Press", category: 0, primaryBodyPart: "Chest", secondaryBodyParts: ["Triceps", "Shoulders"], equipment: "Barbell", notes: "grip 81 cm", source: 0, externalSourceId: null, isArchived: false, defaultRestSeconds: 180, instructions: "Lie down\nPress", videoUrl: null, trackPerLocation: false, splitPromptMuted: false },
        { id: IDS.squat, name: "Back Squat", category: 0, primaryBodyPart: "Quads", secondaryBodyParts: ["Glutes"], equipment: "Barbell", notes: "", source: 0, externalSourceId: null, isArchived: false, instructions: "" },
        { id: IDS.row, name: "Cable Row", category: 0, primaryBodyPart: "Back", secondaryBodyParts: [], equipment: "Cable", notes: "", source: 2, externalSourceId: null, isArchived: false, trackPerLocation: true },
        { id: IDS.pullUp, name: "Pull-up", category: 0, primaryBodyPart: "Back", secondaryBodyParts: ["Biceps"], equipment: "Bodyweight", notes: "", source: 0, isArchived: false },
        { id: IDS.oldCurl, name: "Cable Curl", category: 0, primaryBodyPart: "Biceps", secondaryBodyParts: [], equipment: "Cable", notes: "", source: 2, isArchived: true },
      ],
      templates: [
        {
          id: IDS.pushTemplate,
          name: "Push A",
          notes: "",
          exercises: [{ id: nextId(), exerciseId: IDS.bench, exerciseName: "Bench Press", category: 0, bodyPart: "Chest", sortOrder: 0, notes: "RIR 2", targetSets: 3, repsMin: 5, repsMax: 5, restSeconds: 180, supersetGroup: null }],
        },
      ],
      sessions: [
        session("Push A", 1, [strength(IDS.bench, "Bench Press", "Chest", [{ warmup: [40, 10] }, ...times(85, 5, 2)])]),
        session("Legs", 3, [strength(IDS.squat, "Back Squat", "Quads", times(100, 5, 1))], 1, "home"),
        session("Push A", 8, [strength(IDS.bench, "Bench Press", "Chest", [{ warmup: [40, 10] }, ...times(82.5, 5, 2)])]),
        session("Pull A", 10, [
          strength(IDS.row, "Cable Row", "Back", [...times(60, 8, 3, 2), [60, 8, 6]], 1),
          strength(IDS.pullUp, "Pull-up", "Back", [{ tagged: [10, 10, [2, 6]] }], 1),
        ]),
        session("Push A", 15, [strength(IDS.bench, "Bench Press", "Chest", [{ warmup: [40, 10] }, ...times(80, 5, 2)])]),
        session("Started today", 0, [strength(IDS.bench, "Bench Press", "Chest", times(90, 1, 0, 1))], 0),
        session("Thrown away", 2, [strength(IDS.squat, "Back Squat", "Quads", times(200, 1, 0, 1))], 2),
      ],
      runs: [],
      routes: [{ id: IDS.riverRoute, name: "River loop", distanceMeters: 10000, elevationGainM: 40, encodedPolyline: "", createdAtUtc: 0, notes: "", points: [] }],
      appVersion: "2.11.0",
      settings: null,
      photos: [],
      locations: [
        { id: "home", name: "Home", kind: 1, latitude: null, longitude: null, isArchived: false },
        { id: "outdoors", name: "Outdoors", kind: 2, latitude: null, longitude: null, isArchived: false },
        { id: IDS.gym, name: "Iron Gym", kind: 0, latitude: 50.08, longitude: 14.42, isArchived: false },
      ],
      locationNotes: [],
    },
    runs: [
      {
        id: IDS.longRun,
        sessionId: null,
        startedAtUtc: daysAgo(2),
        durationSeconds: 3300,
        movingSeconds: 3240,
        distanceMeters: 10000,
        avgPaceSecPerKm: 324,
        elevationGainM: 40,
        calories: 700,
        avgHr: 152,
        source: 0,
        externalKey: null,
        routeId: IDS.riverRoute,
        notes: "Long run\nfelt easy",
        splits: [...Array.from({ length: 9 }, (_, index) => split(index + 1, 330 - index * 2)), split(10, 290)],
      },
      {
        id: IDS.easyRun,
        sessionId: null,
        startedAtUtc: daysAgo(9),
        durationSeconds: 1800,
        movingSeconds: 1800,
        distanceMeters: 5200,
        avgPaceSecPerKm: 346,
        elevationGainM: 12,
        calories: null,
        avgHr: null,
        source: 0,
        externalKey: null,
        routeId: null,
        notes: "",
        splits: [...Array.from({ length: 5 }, (_, index) => split(index + 1, 346)), split(6, 350, 200, false)],
      },
    ],
  };
}
