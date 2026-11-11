import { z } from "zod";
import type { Prompt } from "./Prompt";

/** The `training_review` prompt: a review of recent training with three concrete improvements. */
const TRAINING_REVIEW: Prompt = {
  name: "training_review",
  title: "Review my training",
  description: "Reviews your recent workouts and runs in Ember and suggests a few concrete improvements.",
  scopes: ["read"],
  args: {
    focus: z.string().max(200).optional().describe("Anything to look at in particular, such as a lift, a race or a goal."),
  },
  text({ focus }) {
    return [
      "Review my training with Ember.",
      ...(focus?.trim() ? [`I especially want to know about: ${focus.trim()}.`] : []),
      "",
      "1. get_overview. If it answers waiting_for_phone, tell me to tap Allow on my phone and wait for me.",
      "2. get_training_volume for the last 8 weeks by body part: weekly hard sets per muscle group against 10 to 20, and",
      "   what is missing or lopsided.",
      "3. For my three to five most done strength exercises, get_exercise_progress: what is going up, what has stalled (no",
      "   gain in estimated 1RM for 3 or more weeks), and what went down.",
      "4. If I run, get_running_overview: weekly kilometres, how steady they are, and pace over time.",
      "5. Consistency: workouts and runs per week, the streaks, and gaps.",
      "6. Sum it up in a short report: what goes well, then at most three changes, each with the numbers behind it and what",
      "   to do in the next 2 to 4 weeks.",
      "7. Offer to turn a change into a template proposal. Propose nothing until I say yes.",
      "",
      "Answer in my language and my units.",
    ].join("\n");
  },
};

/** The `build_template` prompt: designs a template with the owner, then proposes it. */
const BUILD_TEMPLATE: Prompt = {
  name: "build_template",
  title: "Build a template",
  description: "Designs a template with you from your goal and your exercises, and puts it in Ember's inbox.",
  scopes: ["read", "plan"],
  args: {
    goal: z.string().max(200).optional().describe("What the template is for, such as \"upper body, 60 minutes, strength\"."),
  },
  text({ goal }) {
    return [
      "Build a template with me in Ember.",
      goal?.trim() ? `The goal: ${goal.trim()}.` : "First ask me what it is for: goal, days a week, minutes per workout, equipment.",
      "",
      "1. Read get_overview, list_templates, and list_exercises, so the template fits what I already do and the exercises I have.",
      "2. Draft it: exercises in order, and for each the plan (sets, rep range, rest, supersets) and why it is there. Prefer",
      "   exercises from my catalog; suggest a new one only when nothing fits.",
      "3. Show me the draft and change it until I am happy.",
      "4. Then propose it: propose_exercise for any new exercise first, then propose_template with each exercise's plan.",
      "   Tell me it waits in Ember's inbox for me to accept.",
      "",
      "Answer in my language and my units.",
    ].join("\n");
  },
};

/** Every MCP prompt (docs/claude-connector.md#prompts). */
export const CONNECTOR_PROMPTS: readonly Prompt[] = [TRAINING_REVIEW, BUILD_TEMPLATE];
