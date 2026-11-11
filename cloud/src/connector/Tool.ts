import type { z } from "zod";
import type { ConnectorScope } from "./ConnectorScope";
import type { ToolContext } from "./ToolContext";
import type { ToolResult } from "./ToolResult";

/**
 * One connector tool in GoalMaker's descriptor style: what Claude sees (name, title, plain
 * description, typed input, hints), the scope it needs, and what it does. A tool reports a refusal
 * by throwing ToolError, whose text starts with the error code.
 */
export interface Tool {
  name: string;
  title: string;
  description: string;
  scope: ConnectorScope;
  input: z.ZodRawShape;
  readOnly: boolean;
  destructive: boolean;
  idempotent: boolean;
  /** The MCP server checks [args] against [input] before calling this. */
  run(context: ToolContext, args: any): Promise<ToolResult>;
}
