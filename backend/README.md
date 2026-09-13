# SafeShield backend

A small Node.js/TypeScript/Express API that serves the data SafeShield's
Android app needs to check its own version and refresh its blocklist
(PRD Phases 10–12). It holds no user data, no browsing history, and no
per-device identifiers — every route is public and unauthenticated by
design (see Privacy below).

## Endpoints

| Method | Path | Returns |
|---|---|---|
| GET | `/health` | `{ "status": "ok" }` — liveness check |
| GET | `/api/blocklist` | `{ version, updatedAt, domains: [{ domain, category }] }` |
| GET | `/api/version` | `{ backend, blocklistVersion }` |

Anything else returns `404 { "error": "Not found" }`.

## Local development

```bash
npm install
npm run dev          # tsx watch — restarts on file changes
```

```bash
npm run build         # tsc -> dist/
npm start             # runs the compiled dist/server.js
npm test              # vitest + supertest, exercises every route above
npm run typecheck      # tsc --noEmit
```

Copy `.env.example` to `.env` for local overrides (none are required to
run — every variable has a sane default; see `.env.example` for what each
one is for and which aren't wired to anything yet).

## Deployment

**Live at `https://backend-production-eacd8.up.railway.app`** — Railway
project `safeshield-backend`, service `backend`, deployed from this repo's
`claude/new-session-371rys` branch. `app/build.gradle.kts`'s
`BLOCKLIST_API_BASE_URL` already points at it.

```bash
curl https://backend-production-eacd8.up.railway.app/health
curl https://backend-production-eacd8.up.railway.app/api/blocklist
curl https://backend-production-eacd8.up.railway.app/api/version
```

To redeploy elsewhere, or set up your own instance from scratch:

1. In the Railway dashboard, create a new project (or a new service in an
   existing project) from this GitHub repo.
2. **Set the service's Root Directory to `backend/`** — this repo also
   contains the unrelated Android app at its root, and Railway needs to
   know to build from this subdirectory.
3. Railway's Nixpacks builder auto-detects Node.js via `package.json` and
   will run `npm install`, then `npm run build` (from `railway.json`),
   then `npm start`. No Dockerfile is needed.
4. Set environment variables under the service's Variables tab — see
   `.env.example` for the full list. None are required for the API to
   start; `PORT` is set automatically by Railway.
5. `railway.json` configures a health check against `/health` — Railway
   will use this to know the deploy succeeded and to detect future
   crashes.
6. Once deployed, note the public URL Railway assigns (Settings →
   Networking → Generate Domain if one isn't already there) — that's the
   base URL the Android app's blocklist updater (Phase 12) will need.

No database is provisioned or required for this MVP (see
`src/database/README.md`).

## Privacy

This backend never receives which websites a user visits, their IP-derived
location beyond normal HTTP server logging, or any other per-device
identifying information — `/api/blocklist` and `/api/version` take no
parameters and return the same public data to everyone. Request logging
(`src/middleware/requestLogger.ts`) records only method, path, status, and
duration, never a query string or body.
