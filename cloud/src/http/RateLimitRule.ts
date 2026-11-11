/** A limited action, how often one key may do it, and the period the binding counts over. */
export type RateLimitRule = {
  readonly name: "setup" | "connector_code" | "connector_call";
  readonly limiter: RateLimit;
  readonly periodSeconds: 10 | 60;
};
