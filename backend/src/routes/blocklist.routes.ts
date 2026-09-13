import { Router } from "express";
import { getBlocklistHandler } from "../controllers/blocklist.controller";

export const blocklistRouter = Router();
blocklistRouter.get("/api/blocklist", getBlocklistHandler);
