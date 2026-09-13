import { NextFunction, Request, Response } from "express";

/**
 * Minimal access log: method, path, status, duration. Deliberately does
 * not log query strings or request bodies — every route this backend
 * serves is a public, unauthenticated GET with no per-user or per-device
 * identifying data in it, so there's nothing sensitive to redact, but the
 * habit of only logging what's needed is the same one the Android app's
 * privacy design follows (see the root README's Privacy section).
 */
export function requestLogger(req: Request, res: Response, next: NextFunction): void {
  const start = Date.now();
  res.on("finish", () => {
    const durationMs = Date.now() - start;
    // eslint-disable-next-line no-console
    console.log(`${req.method} ${req.path} ${res.statusCode} ${durationMs}ms`);
  });
  next();
}
