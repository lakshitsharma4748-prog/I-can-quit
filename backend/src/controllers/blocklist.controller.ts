import { Request, Response } from "express";
import { getBlocklist } from "../services/blocklist.service";

/**
 * GET /api/blocklist — the data `updater/BlocklistUpdater` (Phase 12, on
 * the Android side) fetches and validates before atomically replacing its
 * local Room-backed blocklist. See blocklist.service.ts for what's
 * actually in the response right now.
 */
export function getBlocklistHandler(_req: Request, res: Response): void {
  res.status(200).json(getBlocklist());
}
