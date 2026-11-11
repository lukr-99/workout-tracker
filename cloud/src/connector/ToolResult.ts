/** What a tool answers: a short text for Claude to read and the same facts as structured content. */
export interface ToolResult {
  text: string;
  structured: Record<string, unknown>;
}
