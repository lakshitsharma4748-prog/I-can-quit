import { Request, Response } from "express";

/** GET /health — liveness check. No auth, no data, just "the process is up." */
export function getHealth(_req: Request, res: Response): void {
  res.status(200).json({ status: "ok" });
}
