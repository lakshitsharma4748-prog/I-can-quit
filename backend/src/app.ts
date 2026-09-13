import express, { Express } from "express";
import helmet from "helmet";
import { healthRouter } from "./routes/health.routes";
import { blocklistRouter } from "./routes/blocklist.routes";
import { versionRouter } from "./routes/version.routes";
import { requestLogger } from "./middleware/requestLogger";
import { errorHandler, notFoundHandler } from "./middleware/errorHandler";

/**
 * Builds the Express app without starting it listening — kept separate
 * from server.ts so tests can exercise routes directly (via supertest)
 * without binding a real port.
 */
export function createApp(): Express {
  const app = express();

  // Sets a conservative set of security-relevant HTTP headers
  // (X-Content-Type-Options, a restrictive default CSP, etc.) — sensible
  // defaults for a small JSON API with no HTML responses to worry about.
  app.use(helmet());
  app.use(requestLogger);

  app.use(healthRouter);
  app.use(blocklistRouter);
  app.use(versionRouter);

  app.use(notFoundHandler);
  app.use(errorHandler);

  return app;
}
