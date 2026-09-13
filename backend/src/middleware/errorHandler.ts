import { NextFunction, Request, Response } from "express";

/** 404 for anything that didn't match a route above it. */
export function notFoundHandler(_req: Request, res: Response): void {
  res.status(404).json({ error: "Not found" });
}

/**
 * Last-resort error handler. Never echoes the raw error message/stack to
 * the client (which could leak implementation details); logs it
 * server-side instead. Must keep all four parameters, including the
 * unused `_next`, for Express to recognize this as an error-handling
 * middleware rather than a normal one.
 */
// eslint-disable-next-line @typescript-eslint/no-unused-vars
export function errorHandler(err: unknown, _req: Request, res: Response, _next: NextFunction): void {
  // eslint-disable-next-line no-console
  console.error("Unhandled error:", err);
  res.status(500).json({ error: "Internal server error" });
}
