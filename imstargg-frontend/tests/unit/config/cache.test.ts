import { describe, expect, it } from "vitest";
import { CACHE_SECONDS, QUERY_STALE_TIME } from "@/config/cache";

describe("frontend cache policy", () => {
  it("keeps high-freshness cache windows short", () => {
    expect(CACHE_SECONDS.SHORT).toBe(60);
    expect(QUERY_STALE_TIME.SHORT).toBe(60_000);
  });

  it("keeps brawler reference data under one hour on primary pages", () => {
    expect(CACHE_SECONDS.BRAWLER_REFERENCE).toBe(3600);
    expect(QUERY_STALE_TIME.BRAWLER_REFERENCE).toBe(60 * 60_000);
  });

  it("allows long cache only for SEO helper paths", () => {
    expect(CACHE_SECONDS.SEO_REFERENCE).toBe(86400);
  });
});
