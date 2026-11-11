// Worker secrets, set with `wrangler secret put` and never in wrangler.jsonc or the repository.
interface Env {
  /**
   * The owner's setup secret, at least 32 characters. The app sends it once to register the phone
   * (`POST /v1/devices`), also after a reinstall. Without it no device can be set up.
   */
  SETUP_SECRET?: string;
  /** A Firebase service-account key file (JSON). Without it no share requests reach the phone by push. */
  FCM_SERVICE_ACCOUNT?: string;
}
