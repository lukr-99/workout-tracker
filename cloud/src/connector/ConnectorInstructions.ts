/** Claude clients keep only this many characters of a server's instructions and drop the rest. */
export const CONNECTOR_INSTRUCTIONS_LIMIT = 2048;

/**
 * What every Claude client is told when it connects (docs/claude-connector.md, "Rules for Claude").
 * The tool descriptions repeat the rule that matters for each tool. Keep it under
 * CONNECTOR_INSTRUCTIONS_LIMIT.
 */
export const CONNECTOR_INSTRUCTIONS = [
  "Ember is the owner's training app: strength workouts, GPS runs, exercises and templates. The data lives on their phone.",
  "Rules:",
  "1. Read when it helps the conversation, starting with get_overview. The phone shares on request: the first read in a",
  "   while may answer waiting_for_phone; then ask the owner to tap Allow on their phone and call again. After declined,",
  "   do not ask again unless the owner says so.",
  "2. Ground every claim in the data: name the days, weights, sets and paces. Estimated 1RMs are estimates. Data is in kg",
  "   and km; answer in the owner's units from get_overview.",
  "3. You cannot change the owner's data. propose_template and propose_exercise put a proposal in Ember's inbox, where",
  "   the owner accepts or rejects it. Propose only what the owner agreed to in the chat, and say it waits in Ember.",
  "4. Check list_exercises before propose_exercise, and list_proposals before proposing again.",
  "5. Give each template exercise a plan (targetSets, repsMin, repsMax, restSeconds, supersetGroup); use notes only for",
  "   what the plan cannot hold, such as effort or tempo.",
  "6. An exercise tracked per gym differs between locations; compare its sets at the same location.",
  "7. You are not a doctor or physiotherapist. With pain or injury, suggest seeing one, and keep advice general.",
].join("\n");
