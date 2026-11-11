import { cloudflareTest } from "@cloudflare/vitest-plugin";
import { defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [
    cloudflareTest({
      wrangler: { configPath: "./wrangler.jsonc" },
      // A short wait keeps the "phone does not answer" tests fast; the setup secret is the tests' own.
      miniflare: { bindings: { SHARE_WAIT_SECONDS: "2", SETUP_SECRET: "test-setup-secret-that-is-long-enough" } },
    }),
  ],
  // Workers pool start-up, with every file running in parallel, can pass Vitest's default 5 seconds.
  // Every file works on the owner's one Durable Object, so the files run one after another.
  test: { testTimeout: 20_000, fileParallelism: false },
});
