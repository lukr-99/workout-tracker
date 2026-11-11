/**
 * A tool's refusal (docs/claude-connector.md, "Errors"). Claude sees `code: message`, so it can act on the
 * code; `details` (such as an answer's problems) follows as JSON.
 */
export class ToolError extends Error {
  constructor(
    readonly code: string,
    message: string,
    readonly details: unknown = null,
  ) {
    super(message);
  }

  text(): string {
    const head = `${this.code}: ${this.message}`;
    return this.details === null ? head : `${head}\n${JSON.stringify(this.details, null, 2)}`;
  }
}
