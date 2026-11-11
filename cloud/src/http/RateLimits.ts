import { ApiError } from "./ApiError";
import type { RateLimitRule } from "./RateLimitRule";

/**
 * Checks the Workers rate limiting bindings. They count per Cloudflare location and are eventually
 * consistent, so they stop floods and guessing, not a patient attacker spread over many places.
 */
export class RateLimits {
  constructor(
    readonly setup: RateLimitRule,
    readonly connectorCode: RateLimitRule,
    readonly connectorCall: RateLimitRule,
  ) {}

  /** Built from the bindings in wrangler.jsonc; the periods must match the ones configured there. */
  static fromEnv(env: Env): RateLimits {
    return new RateLimits(
      { name: "setup", limiter: env.SETUP_LIMITER, periodSeconds: 60 },
      { name: "connector_code", limiter: env.CONNECTOR_CODE_LIMITER, periodSeconds: 60 },
      { name: "connector_call", limiter: env.CONNECTOR_CALL_LIMITER, periodSeconds: 60 },
    );
  }

  async check(rule: RateLimitRule, key: string): Promise<void> {
    const { success } = await rule.limiter.limit({ key: `${rule.name}:${key}` });
    if (success) return;
    throw new ApiError(429, "rate_limited", "Too many requests. Try again in a minute.", {
      "Retry-After": String(rule.periodSeconds),
    });
  }
}
