import { describe, expect, it } from "vitest";
import request from "supertest";
import { createApp } from "../src/app";

const app = createApp();

describe("GET /health", () => {
  it("returns 200 and status ok", async () => {
    const res = await request(app).get("/health");
    expect(res.status).toBe(200);
    expect(res.body).toEqual({ status: "ok" });
  });
});

describe("GET /api/blocklist", () => {
  it("returns the bundled blocklist with the PRD's test domains", async () => {
    const res = await request(app).get("/api/blocklist");
    expect(res.status).toBe(200);
    expect(res.body).toHaveProperty("version");
    expect(res.body).toHaveProperty("updatedAt");
    expect(Array.isArray(res.body.domains)).toBe(true);

    const domains = res.body.domains.map((d: { domain: string }) => d.domain);
    expect(domains).toContain("blocked-example.test");
    expect(domains).toContain("adult-example.test");
  });

  it("every entry has both a domain and a category", async () => {
    const res = await request(app).get("/api/blocklist");
    for (const entry of res.body.domains) {
      expect(typeof entry.domain).toBe("string");
      expect(entry.domain.length).toBeGreaterThan(0);
      expect(typeof entry.category).toBe("string");
    }
  });
});

describe("GET /api/version", () => {
  it("returns backend and blocklist version strings", async () => {
    const res = await request(app).get("/api/version");
    expect(res.status).toBe(200);
    expect(typeof res.body.backend).toBe("string");
    expect(typeof res.body.blocklistVersion).toBe("string");
  });
});

describe("unknown route", () => {
  it("returns 404 with a JSON body", async () => {
    const res = await request(app).get("/does-not-exist");
    expect(res.status).toBe(404);
    expect(res.body).toEqual({ error: "Not found" });
  });
});

describe("security headers", () => {
  it("sets helmet's baseline headers", async () => {
    const res = await request(app).get("/health");
    expect(res.headers["x-content-type-options"]).toBe("nosniff");
  });
});
