import { ApiError } from "./ApiError";

const JSON_MAX = 64 * 1024;

export class RequestBody {
  /** [max] is 64 KB for every request except a restore batch (`/v1/import`). */
  static async json(request: Request, max = JSON_MAX): Promise<unknown> {
    const length = Number(request.headers.get("Content-Length") ?? "0");
    if (length > max) throw new ApiError(413, "body_too_large", `JSON bodies are limited to ${max} bytes.`);
    const text = await request.text();
    if (new TextEncoder().encode(text).length > max) {
      throw new ApiError(413, "body_too_large", `JSON bodies are limited to ${max} bytes.`);
    }
    try {
      return JSON.parse(text);
    } catch {
      throw new ApiError(400, "invalid_json", "The body is not valid JSON.");
    }
  }
}
