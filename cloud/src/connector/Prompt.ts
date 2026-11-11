import type { z } from "zod";
import type { ConnectorScope } from "./ConnectorScope";

/** One MCP prompt: a ready routine the owner picks in Claude, offered when the grant has its scopes. */
export interface Prompt {
  name: string;
  title: string;
  description: string;
  scopes: readonly ConnectorScope[];
  args: z.ZodRawShape;
  text(args: Record<string, string | undefined>): string;
}
