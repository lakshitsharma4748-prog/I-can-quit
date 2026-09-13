import { Router } from "express";
import { getVersionHandler } from "../controllers/version.controller";

export const versionRouter = Router();
versionRouter.get("/api/version", getVersionHandler);
