import type { ProposalResult } from "../proposals/Proposals";
import type { ToolContext } from "./ToolContext";
import { ToolError } from "./ToolError";
import type { ToolResult } from "./ToolResult";

/** What the tools share: getting the phone's data first, and turning results into answers. */
export class ToolSupport {
  /**
   * Makes sure a fresh share is there, asking the phone when it is not. Returns the owner's today;
   * refuses with the code Claude should act on when the phone has not shared.
   */
  static async shared(context: ToolContext): Promise<{ sharedAt: string; today: string }> {
    const outcome = await context.owner.requestShare();
    switch (outcome.status) {
      case "fresh":
        return { sharedAt: outcome.sharedAt, today: await context.owner.today() };
      case "no_phone":
        throw new ToolError("no_phone", "Ember is not connected to this server yet. Ask the owner to connect it in Ember, Settings, Claude.");
      case "declined":
        throw new ToolError(
          "declined",
          "The owner declined to share their training data from the phone. Do not ask again unless the owner tells you to; then call the tool again.",
        );
      case "waiting":
        throw new ToolError(
          "waiting_for_phone",
          outcome.push === "off" || outcome.push === "failed"
            ? "Asked the owner's phone for their training data, but it could not be woken. Ask the owner to open Ember and allow sharing, then call this tool again."
            : "Asked the owner's phone for their training data. Ask the owner to tap Allow on the Ember notification, then call this tool again.",
        );
    }
  }

  /** A read answer: the structured content as readable JSON. */
  static read(structured: object): ToolResult {
    return { text: JSON.stringify(structured, null, 2), structured: structured as Record<string, unknown> };
  }

  /** A proposal written, or the refusal Claude sees. */
  static proposed(result: ProposalResult, summary: (title: string) => string): ToolResult {
    if (!result.ok) throw new ToolError(result.code, result.message, result.details ?? null);
    const structured = { proposal: result.proposal, repeated: result.repeated };
    const head = result.repeated ? `Already proposed: "${result.proposal.title}".` : summary(result.proposal.title);
    return { text: `${head}\n${JSON.stringify(structured, null, 2)}`, structured };
  }
}
