import { describe, expect, it } from "vitest";
import { SharedBundleParser } from "../src/workouts/SharedBundle";
import { ClaudeClient } from "./ClaudeClient";
import share from "./fixtures/ember-share.json";
import { PhoneClient } from "./PhoneClient";

/**
 * The share exactly as Ember writes it: app/src/test/.../ClaudeShareContractTest.kt writes this
 * fixture from Kotlin, so a change on either side of the contract fails one of the two tests.
 */
describe("Ember's share", () => {
  it("reads every part of the share Ember writes", () => {
    const parsed = SharedBundleParser.parse(share);
    if (!parsed.ok) throw new Error(parsed.problems.join("\n"));
    const { bundle } = parsed;
    expect(bundle).toMatchObject({ timeZone: "Europe/Prague", units: "metric", appVersion: "2.11.0", formatVersion: "1.13" });
    expect(bundle.exercises.map((exercise) => [exercise.name, exercise.category])).toEqual([
      ["Bench Press", "strength"],
      ["Treadmill", "cardio"],
    ]);
    expect(bundle.exercises[0]).toMatchObject({ instructions: "Lie down\nPress", defaultRestSeconds: 180, secondaryBodyParts: ["Triceps"] });
    expect(bundle.locations.map((location) => [location.id, location.kind])).toEqual([
      ["home", "home"],
      ["outdoors", "outdoors"],
      ["loc-gym", "gym"],
    ]);
    expect(bundle.templates[0]!.exercises[0]).toMatchObject({ targetSets: 3, repsMin: 5, repsMax: 8, restSeconds: 180, notes: "RIR 2" });
    const session = bundle.sessions[0]!;
    expect(session).toMatchObject({ status: "completed", locationId: "loc-gym", perceivedEffort: 8, startedAt: "2025-11-10T13:00:00.000Z", localDay: "2025-11-10" });
    expect(session.entries[0]!.supersetGroup).toBe(1);
    expect(session.entries[0]!.sets.map((set) => [set.isWarmup, set.isPr, set.tags])).toEqual([
      [true, false, ["Warmup"]],
      [false, false, []],
      [false, true, ["ToFailure", "Finisher"]],
    ]);
    expect(session.entries[0]!.sets[2]).toMatchObject({ reps: 4, weightKg: 85, rpe: 9.5, rir: null });
    const run = bundle.runs[0]!;
    expect(run).toMatchObject({ id: "r-1", distanceMeters: 2301, avgHr: 150, notes: "Easy\nlegs fine", localDay: "2025-11-11" });
    expect(run.splits.filter((split) => split.isFull)).toHaveLength(2);
  });

  it("answers Claude from the share Ember writes", async () => {
    const phone = await PhoneClient.setup();
    const claude = await ClaudeClient.connect(phone);
    expect((await phone.share(share)).status).toBe(200);
    const workouts = await claude.call("list_workouts", { from: "2025-01-01" });
    // The warm-up is left out; the RIR 2 set and the RPE 9.5 set are both hard.
    expect(workouts.structured.workouts[0]).toMatchObject({ location: "Iron Gym", workingSets: 2, hardSets: 2, volumeKg: 765, effort: 8 });
    const runs = await claude.call("list_runs", { from: "2025-01-01" });
    expect(runs.structured.runs[0]).toMatchObject({ title: "Easy", distanceKm: 2.3 });
  });
});
