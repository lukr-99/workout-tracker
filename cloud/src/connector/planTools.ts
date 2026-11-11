import { z } from "zod";
import type { Tool } from "./Tool";
import { ToolSupport } from "./ToolSupport";

const reason = z
  .string()
  .min(1)
  .max(500)
  .describe("Why, in one or two short lines the owner reads in Ember, tied to their data (\"bench has stalled at 80 kg for 5 weeks\").");
const clientRef = z
  .string()
  .min(1)
  .max(100)
  .describe("Your own key for this proposal, unique per proposal; a repeat with the same key answers the first one.");

/**
 * Proposing to the owner (scope plan). Nothing here changes the owner's data: every proposal waits
 * in Ember's inbox until the owner accepts or rejects it (docs/claude-connector.md, "The inbox").
 */
export function planTools(): Tool[] {
  return [
    {
      name: "propose_template",
      title: "Propose a template",
      description:
        "Proposes a template: a name, notes, and exercises in order, each with its plan (target sets, rep range, rest in " +
        "seconds, superset group: exercises with the same number are one superset) and a short note for anything else, such as " +
        "the effort (\"RIR 2\") or a tempo. Use exercise ids from list_exercises, or the exerciseId propose_exercise gave you. " +
        "With replacesTemplateId it proposes a new version of that template. It waits in Ember until the owner accepts it, so " +
        "tell them so. Only after the owner agreed to the plan in the chat. Refused with invalid when an exercise or template is unknown.",
      scope: "plan",
      input: {
        name: z.string().min(1).max(80),
        notes: z.string().max(1000).optional().describe("Notes for the whole template, such as when to do it and how to progress."),
        exercises: z
          .array(
            z.object({
              exerciseId: z.string().min(1).max(64),
              targetSets: z.number().int().min(1).max(20).optional(),
              repsMin: z.number().int().min(1).max(100).optional(),
              repsMax: z.number().int().min(1).max(100).optional(),
              restSeconds: z.number().int().min(0).max(1200).optional(),
              supersetGroup: z.number().int().min(1).max(20).optional(),
              notes: z.string().max(300).optional().describe("Anything the plan has no place for, such as effort or tempo."),
            }),
          )
          .min(1)
          .max(30),
        replacesTemplateId: z.string().min(1).max(64).optional().describe("The template this one replaces, from list_templates."),
        reason,
        clientRef,
      },
      readOnly: false,
      destructive: false,
      idempotent: true,
      async run(context, args) {
        await ToolSupport.shared(context);
        return ToolSupport.proposed(await context.owner.proposeTemplate(args, context.device.id), (title) => `Proposed the template "${title}"; it waits in Ember.`);
      },
    },
    {
      name: "propose_exercise",
      title: "Propose an exercise",
      description:
        "Proposes an exercise the owner's catalog lacks (check list_exercises first). Answers the exerciseId it will have, which " +
        "propose_template can use right away; accepting the template creates the exercise too. Refused with duplicate when the " +
        "catalog or a waiting proposal has the same name, ignoring case and accents.",
      scope: "plan",
      input: {
        name: z.string().min(1).max(80),
        category: z.enum(["strength", "cardio"]),
        primaryBodyPart: z.string().min(1).max(40).describe("Use the catalog's body part names, such as Chest, Back, Quads, Core."),
        secondaryBodyParts: z.array(z.string().min(1).max(40)).max(6).optional(),
        equipment: z.string().max(60).optional(),
        instructions: z.string().max(1500).optional().describe("How to do it, one short step per line."),
        defaultRestSeconds: z.number().int().min(0).max(1200).optional().describe("Its own rest between sets, when it needs one."),
        reason,
        clientRef,
      },
      readOnly: false,
      destructive: false,
      idempotent: true,
      async run(context, args) {
        await ToolSupport.shared(context);
        return ToolSupport.proposed(await context.owner.proposeExercise(args, context.device.id), (title) => `Proposed the exercise "${title}".`);
      },
    },
    {
      name: "withdraw_proposal",
      title: "Withdraw a proposal",
      description: "Takes back one of your proposals that still waits, such as when the owner changed their mind in the chat.",
      scope: "plan",
      input: { id: z.string().min(1).max(64).describe("The proposal's id, from list_proposals.") },
      readOnly: false,
      destructive: false,
      idempotent: true,
      async run(context, { id }: { id: string }) {
        return ToolSupport.proposed(await context.owner.withdrawProposal(id), (title) => `Withdrew "${title}".`);
      },
    },
  ];
}
