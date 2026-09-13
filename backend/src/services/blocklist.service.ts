import blocklistData from "../data/blocklist.json";

export interface BlocklistDomain {
  domain: string;
  category: string;
}

export interface BlocklistPayload {
  version: string;
  updatedAt: string;
  domains: BlocklistDomain[];
}

/**
 * Serves the bundled test blocklist (`src/data/blocklist.json`) — the same
 * `blocked-example.test` / `adult-example.test` entries the Android app
 * seeds locally in Phase 2/3. This is intentionally still just test data:
 * a real deployment would replace this file (or, per `config.blocklistSource`,
 * point this service at a real curated adult-content list) — sourcing an
 * actual vetted blocklist isn't something this repository can responsibly
 * fabricate, and is left as a real, separate follow-up rather than papered
 * over with placeholder domains presented as if they were real.
 *
 * Kept behind this interface (not read directly by controllers) so a
 * future phase can swap the data source — a database, a remote fetch, a
 * periodic refresh — without changing the HTTP contract in
 * blocklist.controller.ts.
 */
export function getBlocklist(): BlocklistPayload {
  return blocklistData as BlocklistPayload;
}
