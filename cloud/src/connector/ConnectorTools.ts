import { planTools } from "./planTools";
import { readTools } from "./readTools";
import type { Tool } from "./Tool";

/** Every tool registry, one per scope (docs/claude-connector.md). */
export const CONNECTOR_TOOLS: readonly Tool[] = [...readTools(), ...planTools()];
