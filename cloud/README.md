# Ember's Claude server

The Cloudflare Worker behind Ember's Claude connector ([docs/claude-connector.md](../docs/claude-connector.md),
[ADR 0002](../docs/adr/0002-claude-connector.md)): Ember's API under `/v1`, Claude's OAuth sign-in,
and the MCP server at `/mcp`. The phone owns the training data; the Worker keeps a copy for 30
minutes after Ember shares it, and Claude's proposals until the owner decides. Deployed at
`https://workout-tracker.lukrejci9.workers.dev`.

## Layout

| Folder | Holds |
|---|---|
| `src/account/` | `OwnerObject`, the owner's one Durable Object with SQLite: devices, connector codes, share requests and the wait for them, the shared copy, proposals, the expiry alarm; and the migration runner |
| `src/auth/` | Device tokens (`wt1.`) and connector codes (`K7Q-4MX`) |
| `src/http/` | Ember's API router, errors, request bodies, rate limits |
| `src/push/` | The FCM sender (from Lumen) and its service-account key |
| `src/workouts/` | Reading Ember's `ClaudeShare` (`SharedBundle`), storing and deleting the copy, and the readers behind the tools: workouts, hard sets, progress and estimated 1RM, volume, runs and splits |
| `src/proposals/` | Claude's proposals and their checks |
| `src/connector/` | The OAuth sign-in page, the MCP server, the read and plan tools, instructions, and prompts |
| `migrations/` | Immutable numbered SQL, applied when the object starts |
| `test/` | Vitest in the Workers runtime (`@cloudflare/vitest-plugin`); `PhoneClient` and `ClaudeClient` play the two sides, and `fixtures/ember-share.json` is the share Ember's `ClaudeShareContractTest` writes |

`src/index.ts` is the composition root. Most infrastructure is adapted from Aletheia and Lumen.

## Commands

With Node.js 22 or later:

```powershell
npm ci
npm run typecheck
npm test
npm run dev        # local Worker on http://127.0.0.1:8799
npm run deploy
```

Secrets (`npx wrangler secret put <name>`): `SETUP_SECRET` (at least 32 characters, required; Ember
asks for it once when it connects) and `FCM_SERVICE_ACCOUNT` (a Firebase service-account key;
without it nothing is pushed and Ember catches up when it is opened). For local runs put them in
`.dev.vars`, which git ignores; the owner's copy of the deployed `SETUP_SECRET` is there too.
