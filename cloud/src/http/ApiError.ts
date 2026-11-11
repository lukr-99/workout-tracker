/** An error with a stable code that the protocol returns as `{"error": {"code", "message"}}`. */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
    readonly headers: Record<string, string> = {},
    /** Extra detail for the caller, such as every field problem of a refused record. */
    readonly details?: unknown,
  ) {
    super(message);
  }

  toResponse(): Response {
    const error = this.details === undefined ? { code: this.code, message: this.message } : { code: this.code, message: this.message, details: this.details };
    return Response.json({ error }, { status: this.status, headers: this.headers });
  }
}
