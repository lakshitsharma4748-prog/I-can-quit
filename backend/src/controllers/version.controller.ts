import { Request, Response } from "express";
import { config } from "../config";
import { getBlocklist } from "../services/blocklist.service";

/**
 * GET /api/version — lets the app check both the backend's own version
 * and the blocklist's version without a second round trip, so a client
 * can decide whether a blocklist refresh is worth fetching before
 * actually fetching it.
 */
export function getVersionHandler(_req: Request, res: Response): void {
  res.status(200).json({
    backend: config.serviceVersion,
    blocklistVersion: getBlocklist().version
  });
}
