import { beforeEach, describe, expect, it } from "vitest";
import { pace } from "../src/workouts/RunReader";
import { estimatedMax, weeklyStreak } from "../src/workouts/WorkoutReader";
import { ClaudeClient } from "./ClaudeClient";
import { IDS, share } from "./Fixtures";
import { PhoneClient } from "./PhoneClient";

let claude: ClaudeClient;

beforeEach(async () => {
  const phone = await PhoneClient.setup();
  claude = await ClaudeClient.connect(phone);
  expect((await phone.share(share())).status).toBe(200);
});

describe("workout tools", () => {
  it("sums up training and running in get_overview", async () => {
    const { structured } = await claude.call("get_overview");
    expect(structured.ownerUnits).toBe("kilograms and kilometres");
    expect(structured.appVersion).toBe("2.11.0");
    expect(structured.training).toMatchObject({
      completedWorkouts: 5,
      workoutsLast7Days: 2,
      workoutsLast30Days: 5,
      totalVolumeKg: 6752.5,
      exercisesInCatalog: 4,
      templates: 1,
      activeWorkout: { name: "Started today" },
    });
    expect(structured.training.locations.map((location: { name: string }) => location.name)).toEqual(["Iron Gym", "Home", "Outdoors"]);
    expect(structured.training.mostDoneExercises[0]).toMatchObject({ exerciseId: IDS.bench, name: "Bench Press", workouts: 3 });
    expect(structured.training.recentWorkouts.map((workout: { name: string }) => workout.name)).toEqual(["Push A", "Legs", "Push A", "Pull A", "Push A"]);
    expect(structured.running).toMatchObject({ totalRuns: 2, totalKm: 15.2, runsLast30Days: 2 });
  });

  it("lists workouts with locations, and leaves warm-ups out of sets and volume", async () => {
    const all = await claude.call("list_workouts", { limit: 2 });
    expect(all.structured.workouts).toHaveLength(2);
    expect(all.structured.more).toBe(true);
    expect(all.structured.workouts[0]).toMatchObject({ name: "Push A", location: "Iron Gym", template: "Push A", workingSets: 3, hardSets: 3, reps: 15, volumeKg: 1275, effort: 7 });
    const legs = await claude.call("list_workouts", { q: "squat" });
    expect(legs.structured.workouts[0]).toMatchObject({ name: "Legs", location: "Home", volumeKg: 1500, notes: "knees felt good" });
    const atHome = await claude.call("list_workouts", { locationId: "home" });
    expect(atHome.structured.workouts.map((workout: { name: string }) => workout.name)).toEqual(["Legs"]);
  });

  it("counts a set as hard within 4 reps of failure or with no effort logged", async () => {
    const { structured } = await claude.call("list_workouts", { q: "Pull" });
    // Rows at RIR 3, 3 and 6, and a pull-up set tagged To failure with no effort.
    expect(structured.workouts[0]).toMatchObject({ workingSets: 4, hardSets: 3, volumeKg: 1540 });
  });

  it("shows one workout in full, with tags, warm-ups and supersets", async () => {
    const pull = await claude.call("list_workouts", { q: "Pull" });
    const { structured } = await claude.call("get_workout", { id: pull.structured.workouts[0].id });
    expect(structured.workout.entries.map((entry: { name: string; supersetGroup: number }) => [entry.name, entry.supersetGroup])).toEqual([
      ["Cable Row", 1],
      ["Pull-up", 1],
    ]);
    expect(structured.workout.entries[1].sets[0]).toMatchObject({ reps: 10, weightKg: 10, warmup: false, tags: ["ToFailure", "Finisher"] });
    const push = await claude.call("list_workouts", { q: "Push", limit: 1 });
    const detail = await claude.call("get_workout", { id: push.structured.workouts[0].id });
    expect(detail.structured.workout.entries[0].sets[0]).toMatchObject({ weightKg: 40, reps: 10, warmup: true, tags: ["Warmup"] });
    expect((await claude.call("get_workout", { id: "nope" })).text).toMatch(/^not_found/);
  });

  it("lists the catalog with how often each exercise was done", async () => {
    const { structured } = await claude.call("list_exercises");
    expect(structured.exercises.map((exercise: { name: string }) => exercise.name)).toEqual(["Back Squat", "Bench Press", "Cable Row", "Pull-up"]);
    expect(structured.exercises.find((exercise: { id: string }) => exercise.id === IDS.bench)).toMatchObject({
      workouts: 3,
      secondaryBodyParts: ["Triceps", "Shoulders"],
      note: "grip 81 cm",
      hasSteps: true,
      defaultRestSeconds: 180,
      trackPerGym: false,
    });
    expect(structured.exercises.find((exercise: { id: string }) => exercise.id === IDS.row).trackPerGym).toBe(true);
    const triceps = await claude.call("list_exercises", { bodyPart: "Triceps" });
    expect(triceps.structured.exercises).toHaveLength(1);
    const archived = await claude.call("list_exercises", { includeArchived: true, q: "curl" });
    expect(archived.structured.exercises[0]).toMatchObject({ name: "Cable Curl", archived: true });
  });

  it("shows an exercise's progress without warm-ups, with records and trend", async () => {
    const { structured } = await claude.call("get_exercise_progress", { exerciseId: IDS.bench });
    expect(structured.sessions.map((session: { topWeightKg: number }) => session.topWeightKg)).toEqual([80, 82.5, 85]);
    expect(structured.sessions[0]).toMatchObject({ location: "Iron Gym", sets: "80x5 RIR 2, 80x5 RIR 2, 80x5 RIR 2", setCount: 3, totalReps: 15, volumeKg: 1200, estimatedOneRepMaxKg: 93.3 });
    expect(structured.records.heaviestWeight.weightKg).toBe(85);
    expect(structured.records.bestEstimatedOneRepMax.kg).toBe(99.2);
    expect(structured.records.mostReps.reps).toBe(5);
    expect(structured.trend).toMatchObject({ firstEstimatedOneRepMaxKg: 93.3, lastEstimatedOneRepMaxKg: 99.2, changePercent: 6.3 });
  });

  it("splits hard sets by body part per week, and by exercise per month", async () => {
    const weeks = await claude.call("get_training_volume", {});
    const groups = weeks.structured.periods.flatMap((period: { groups: { bodyPart: string }[] }) => period.groups);
    const total = (part: string, field: "hardSets" | "workingSets") =>
      groups.filter((group: { bodyPart: string }) => group.bodyPart === part).reduce((sum: number, group: Record<string, number>) => sum + group[field]!, 0);
    expect([total("Chest", "hardSets"), total("Quads", "hardSets"), total("Back", "hardSets"), total("Back", "workingSets")]).toEqual([9, 3, 3, 4]);
    expect(weeks.structured.periods[0].period).toMatch(/^week of \d{4}-\d{2}-\d{2}$/);
    const months = await claude.call("get_training_volume", { period: "month", by: "exercise" });
    expect(months.structured.periods[0].period).toMatch(/^\d{4}-\d{2}$/);
    expect(months.structured.periods[0].groups[0]).toHaveProperty("exercise");
    expect((await claude.call("get_training_volume", { from: "2026-02-30" })).isError).toBe(true);
  });

  it("lists templates with their plans", async () => {
    const { structured } = await claude.call("list_templates");
    expect(structured.templates[0]).toMatchObject({
      id: IDS.pushTemplate,
      name: "Push A",
      exercises: [{ exerciseId: IDS.bench, targetSets: 3, repsMin: 5, repsMax: 5, restSeconds: 180, supersetGroup: null, notes: "RIR 2" }],
    });
  });
});

describe("run tools", () => {
  it("lists runs with pace and route", async () => {
    const { structured } = await claude.call("list_runs", {});
    expect(structured.runs.map((run: { title: string }) => run.title)).toEqual(["Long run", "Run"]);
    expect(structured.runs[0]).toMatchObject({ distanceKm: 10, avgPace: "5:24 /km", movingMinutes: 54, elevationGainM: 40, avgHr: 152, route: "River loop" });
  });

  it("shows one run's splits", async () => {
    const { structured } = await claude.call("get_run", { id: IDS.easyRun });
    expect(structured.run.splits).toHaveLength(6);
    expect(structured.run.splits[0]).toMatchObject({ km: 1, pace: "5:46 /km", full: true });
    expect(structured.run.splits[5]).toMatchObject({ distanceKm: 0.2, full: false });
    expect((await claude.call("get_run", { id: "nope" })).text).toMatch(/^not_found/);
  });

  it("sums up running with weekly kilometres and bests", async () => {
    const { structured } = await claude.call("get_running_overview", { weeks: 4 });
    expect(structured.weekly).toHaveLength(4);
    expect(structured.weekly.reduce((sum: number, week: { km: number }) => sum + week.km, 0)).toBeCloseTo(15.2);
    expect(structured.bests).toMatchObject({
      longestRun: { runId: IDS.longRun, distanceKm: 10 },
      fastestPaceOver5Km: { runId: IDS.longRun, avgPace: "5:24 /km" },
      fastestPaceOver10Km: { runId: IDS.longRun },
      fastestKmSplit: { runId: IDS.longRun, pace: "4:50 /km" },
    });
  });
});

describe("training math", () => {
  it("estimates a one-rep max with Epley, and a single as itself", () => {
    expect(estimatedMax({ reps: 1, weight_kg: 100 })).toBe(100);
    expect(estimatedMax({ reps: 10, weight_kg: 60 })).toBe(80);
    expect(estimatedMax({ reps: 0, weight_kg: 60 })).toBe(0);
  });

  it("counts a weekly streak up to last week while this one is empty", () => {
    // 2026-11-11 is a Wednesday; its week starts on Monday the 9th.
    expect(weeklyStreak(["2026-11-03", "2026-10-27", "2026-10-13"], "2026-11-11")).toBe(2);
    expect(weeklyStreak(["2026-11-10", "2026-11-03"], "2026-11-11")).toBe(2);
    expect(weeklyStreak(["2026-10-20"], "2026-11-11")).toBe(0);
  });

  it("writes a pace as minutes and seconds per kilometre", () => {
    expect(pace(324)).toBe("5:24 /km");
    expect(pace(299.6)).toBe("5:00 /km");
    expect(pace(0)).toBeNull();
  });
});
