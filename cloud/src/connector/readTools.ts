import { z } from "zod";
import { LocalDay } from "../workouts/LocalDay";
import type { Tool } from "./Tool";
import { ToolError } from "./ToolError";
import { ToolSupport } from "./ToolSupport";

const day = z.string().regex(/^\d{4}-\d{2}-\d{2}$/).refine(LocalDay.isDay, "Not a real day.");
const id = (what: string) => z.string().min(1).max(64).describe(what);
const WAITS =
  "Needs the owner's training data: when the phone has not shared in the last half hour, it asks the phone and waits a little; " +
  "a waiting_for_phone answer means ask the owner to tap Allow on their phone, then call again.";

/**
 * Reading the owner's workouts and runs (scope read). Every tool but list_proposals works on the copy the
 * phone shares on request (docs/claude-connector.md, "Sharing on request").
 */
export function readTools(): Tool[] {
  return [
    {
      name: "get_overview",
      title: "Training overview",
      description:
        "Start here. Today's date in the owner's time zone, the units they use, and two parts. training: finished workouts in " +
        "all and in the last 7, 30 and 90 days, the weekly streak, first and last workout day, total volume, the gyms and " +
        "places they train at, the exercises done most, the last five workouts, and any workout in progress. running: runs " +
        `and kilometres, weekly distance for 8 weeks, the streak, bests, and the last five runs. ${WAITS}`,
      scope: "read",
      input: {},
      readOnly: true,
      destructive: false,
      idempotent: true,
      async run(context) {
        await ToolSupport.shared(context);
        return ToolSupport.read((await context.owner.overview()) ?? {});
      },
    },
    {
      name: "list_workouts",
      title: "List workouts",
      description:
        "Finished workouts, newest first, each with its day, location, name, template, duration, exercises, working sets, " +
        "hard sets (within 4 reps of failure, or no effort logged), reps, volume (reps x kg, warm-ups left out), cardio " +
        `minutes and the owner's effort rating. Filter by day range, location id (from get_overview), or text. ${WAITS}`,
      scope: "read",
      input: {
        from: day.optional().describe("First day, YYYY-MM-DD, in the owner's time zone."),
        to: day.optional().describe("Last day, YYYY-MM-DD."),
        q: z.string().min(1).max(100).optional().describe("Text in the workout's name or notes, or an exercise name."),
        locationId: z.string().min(1).max(64).optional().describe("Only workouts at this location, from get_overview."),
        limit: z.number().int().min(1).max(200).optional().describe("How many (default 20, at most 200)."),
      },
      readOnly: true,
      destructive: false,
      idempotent: true,
      async run(context, { from, to, q, locationId, limit }: { from?: string; to?: string; q?: string; locationId?: string; limit?: number }) {
        await ToolSupport.shared(context);
        return ToolSupport.read(await context.owner.workouts({ from: from ?? null, to: to ?? null, q: q ?? null, locationId: locationId ?? null, limit: limit ?? 20 }));
      },
    },
    {
      name: "get_workout",
      title: "Get a workout",
      description:
        "One workout in full: every exercise in order with its superset group and notes, and each set's reps, kg, RIR or RPE, " +
        `tags (Warmup, Drop, ToFailure, Failed, Negative, BackOff, Finisher, Explosive, SlowEccentric, SlowConcentric), and PR mark; or cardio. ${WAITS}`,
      scope: "read",
      input: { id: id("The workout's id, from list_workouts or get_overview.") },
      readOnly: true,
      destructive: false,
      idempotent: true,
      async run(context, { id }: { id: string }) {
        await ToolSupport.shared(context);
        const workout = await context.owner.workout(id);
        if (!workout) throw new ToolError("not_found", "No workout has that id.");
        return ToolSupport.read({ workout });
      },
    },
    {
      name: "list_exercises",
      title: "List exercises",
      description:
        "The owner's exercise catalog, by name: category (strength or cardio), body parts, equipment, the owner's note, whether " +
        "it has steps, its own rest, whether it is tracked per gym, where it came from (seeded, synced, custom), how many " +
        `workouts used it, and when last. Use these ids in propose_template. ${WAITS}`,
      scope: "read",
      input: {
        q: z.string().min(1).max(100).optional().describe("Text in the name or equipment."),
        bodyPart: z.string().min(1).max(60).optional().describe("Only exercises for this body part, such as Chest."),
        category: z.enum(["strength", "cardio"]).optional(),
        includeArchived: z.boolean().optional().describe("Also archived exercises (default false)."),
      },
      readOnly: true,
      destructive: false,
      idempotent: true,
      async run(context, args: { q?: string; bodyPart?: string; category?: string; includeArchived?: boolean }) {
        await ToolSupport.shared(context);
        const exercises = await context.owner.exercises({
          q: args.q ?? null,
          bodyPart: args.bodyPart ?? null,
          category: args.category ?? null,
          includeArchived: args.includeArchived ?? false,
        });
        return ToolSupport.read({ exercises });
      },
    },
    {
      name: "get_exercise_progress",
      title: "Exercise progress",
      description:
        "How one exercise went over time, oldest first: each workout's location, working sets (weight x reps with effort), top " +
        "weight, best set, estimated 1RM (Epley), reps and volume, or cardio minutes and distance; plus records (heaviest, best " +
        "e1RM, most volume, most reps) and the change in e1RM over the range. Warm-ups are left out. For an exercise tracked per " +
        `gym, compare sessions at the same location. Use it to spot progress, stalls and drops. ${WAITS}`,
      scope: "read",
      input: {
        exerciseId: id("The exercise's id, from list_exercises."),
        from: day.optional().describe("First day, YYYY-MM-DD."),
        to: day.optional().describe("Last day, YYYY-MM-DD."),
        limit: z.number().int().min(1).max(200).optional().describe("The latest this many sessions (default 30)."),
      },
      readOnly: true,
      destructive: false,
      idempotent: true,
      async run(context, { exerciseId, from, to, limit }: { exerciseId: string; from?: string; to?: string; limit?: number }) {
        await ToolSupport.shared(context);
        return ToolSupport.read(await context.owner.exerciseProgress(exerciseId, from ?? null, to ?? null, limit ?? 30));
      },
    },
    {
      name: "get_training_volume",
      title: "Training volume",
      description:
        "Hard sets, working sets, reps and volume per week (Monday start) or month, split by body part or by exercise, with " +
        "workouts and cardio minutes per period. The last 8 weeks by default. Ember aims at 10 to 20 hard sets per muscle a " +
        `week. Use it for balance between muscle groups and for load over time. ${WAITS}`,
      scope: "read",
      input: {
        from: day.optional().describe("First day, YYYY-MM-DD (default 8 weeks ago)."),
        to: day.optional().describe("Last day, YYYY-MM-DD (default today)."),
        period: z.enum(["week", "month"]).optional().describe("week (default) or month."),
        by: z.enum(["bodyPart", "exercise"]).optional().describe("bodyPart (default) or exercise."),
      },
      readOnly: true,
      destructive: false,
      idempotent: true,
      async run(context, args: { from?: string; to?: string; period?: "week" | "month"; by?: "bodyPart" | "exercise" }) {
        const { today } = await ToolSupport.shared(context);
        const to = args.to ?? today;
        const from = args.from ?? LocalDay.addDays(LocalDay.weekStart(to), -7 * 7);
        if (from > to) throw new ToolError("invalid", "from is after to.");
        return ToolSupport.read(await context.owner.volume({ from, to, period: args.period ?? "week", by: args.by ?? "bodyPart" }));
      },
    },
    {
      name: "list_templates",
      title: "List templates",
      description:
        "The owner's templates with their exercises in order, each with its plan (target sets, rep range, rest in seconds, " +
        `superset group) and note. ${WAITS}`,
      scope: "read",
      input: {},
      readOnly: true,
      destructive: false,
      idempotent: true,
      async run(context) {
        await ToolSupport.shared(context);
        return ToolSupport.read({ templates: await context.owner.templates() });
      },
    },
    {
      name: "get_running_overview",
      title: "Running overview",
      description:
        "The owner's running over the last weeks: total runs and kilometres, kilometres per week, the weekly streak, the last " +
        `five runs, and bests (longest run, fastest average pace over 5 and 10 km, fastest full kilometre split). ${WAITS}`,
      scope: "read",
      input: { weeks: z.number().int().min(1).max(52).optional().describe("How many weeks of weekly distance (default 12).") },
      readOnly: true,
      destructive: false,
      idempotent: true,
      async run(context, { weeks }: { weeks?: number }) {
        await ToolSupport.shared(context);
        return ToolSupport.read(await context.owner.runningOverview(weeks ?? 12));
      },
    },
    {
      name: "list_runs",
      title: "List runs",
      description:
        "GPS runs, newest first, each with its day, title, distance, time, moving time, average pace, elevation gain, heart " +
        `rate and calories when known, and the route it followed. ${WAITS}`,
      scope: "read",
      input: {
        from: day.optional().describe("First day, YYYY-MM-DD."),
        to: day.optional().describe("Last day, YYYY-MM-DD."),
        limit: z.number().int().min(1).max(200).optional().describe("How many (default 20, at most 200)."),
      },
      readOnly: true,
      destructive: false,
      idempotent: true,
      async run(context, { from, to, limit }: { from?: string; to?: string; limit?: number }) {
        await ToolSupport.shared(context);
        return ToolSupport.read(await context.owner.runs({ from: from ?? null, to: to ?? null, limit: limit ?? 20 }));
      },
    },
    {
      name: "get_run",
      title: "Get a run",
      description: `One run with its kilometre splits (pace, time and elevation gain for each kilometre). ${WAITS}`,
      scope: "read",
      input: { id: id("The run's id, from list_runs or get_running_overview.") },
      readOnly: true,
      destructive: false,
      idempotent: true,
      async run(context, { id }: { id: string }) {
        await ToolSupport.shared(context);
        const run = await context.owner.run(id);
        if (!run) throw new ToolError("not_found", "No run has that id.");
        return ToolSupport.read({ run });
      },
    },
    {
      name: "list_proposals",
      title: "List your proposals",
      description:
        "Exercises and templates you proposed, newest first, and what the owner did with them: pending (waiting in the app), " +
        "accepted, rejected, or withdrawn. Check it before proposing again. Does not need the phone.",
      scope: "read",
      input: {
        status: z.enum(["pending", "accepted", "rejected", "withdrawn"]).optional(),
        limit: z.number().int().min(1).max(100).optional().describe("How many (default 20)."),
      },
      readOnly: true,
      destructive: false,
      idempotent: true,
      async run(context, { status, limit }: { status?: "pending" | "accepted" | "rejected" | "withdrawn"; limit?: number }) {
        return ToolSupport.read({ proposals: await context.owner.listProposals(status ?? null, limit ?? 20) });
      },
    },
  ];
}
