/**
 * All environment-derived configuration in one place, read once at
 * startup. Nothing here has a hardcoded secret — see `.env.example` for
 * the variables this expects, and README.md for Railway deployment.
 */

function readEnv(name: string, fallback?: string): string | undefined {
  return process.env[name] ?? fallback;
}

export const config = {
  nodeEnv: readEnv("NODE_ENV", "development") as string,
  port: parseInt(readEnv("PORT", "3000") as string, 10),

  /**
   * Kept in sync with package.json's "version" field by hand rather than
   * imported from it — package.json sits outside tsconfig's `rootDir`
   * (src/), and importing across that boundary complicates the compiled
   * output layout for no real benefit in a service this small.
   */
  serviceVersion: "0.1.0",

  /**
   * Free-form identifier for where the blocklist data currently comes
   * from. The MVP backend always serves its bundled JSON file regardless
   * of this value — it exists now so ops/deployment tooling and future
   * phases have a stable place to point at a different source (e.g. a
   * managed list, or a database-backed one) without changing the API
   * contract.
   */
  blocklistSource: readEnv("BLOCKLIST_SOURCE", "bundled"),

  /**
   * Reserved for a future authenticated endpoint (e.g. an admin API to
   * edit the blocklist remotely). Nothing in this backend currently
   * requires it — every route Phase 10 defines (`/health`,
   * `/api/blocklist`, `/api/version`) is intentionally read-only and
   * unauthenticated, matching the PRD's "Only add additional endpoints
   * when required."
   */
  apiSecret: readEnv("API_SECRET"),

  /**
   * Not used by anything in this codebase yet — no route reads from a
   * database (see src/database/README.md). Declared here so the env var
   * contract in `.env.example` is honest about what Railway deployment
   * will eventually need, without pretending a database is wired in
   * before it actually is.
   */
  databaseUrl: readEnv("DATABASE_URL")
};

export type AppConfig = typeof config;
