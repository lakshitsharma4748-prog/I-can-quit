# database/

Reserved, per the PRD's Phase 10 project structure — not wired to anything
yet. Every route this backend currently serves (`/health`, `/api/blocklist`,
`/api/version`) reads from the bundled JSON file in `../data/`, not a
database (the PRD lists PostgreSQL as needed only "if persistent
server-side storage is required," which isn't the case for a
read-only, static-for-now blocklist).

If a future phase needs real persistent storage — e.g. an admin API to
edit the blocklist without redeploying, or per-device sync state — this is
where a Postgres connection pool and query modules would go, following the
same pattern as the rest of this backend: a thin module here, consumed by
`services/`, never called directly from `controllers/`.
