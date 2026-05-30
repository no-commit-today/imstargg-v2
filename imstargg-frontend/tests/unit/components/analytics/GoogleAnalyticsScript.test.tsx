import { describe, expect, it, vi } from "vitest";
import {
  GOOGLE_ANALYTICS_ID,
  GoogleAnalyticsScript,
  shouldLoadGoogleAnalytics,
} from "@/components/analytics/GoogleAnalyticsScript";

describe("shouldLoadGoogleAnalytics", () => {
  it("loads only in production", () => {
    expect(shouldLoadGoogleAnalytics("production")).toBe(true);
  });

  it("does not load in development", () => {
    expect(shouldLoadGoogleAnalytics("development")).toBe(false);
  });

  it("does not load in test", () => {
    expect(shouldLoadGoogleAnalytics("test")).toBe(false);
  });
});

describe("GoogleAnalyticsScript", () => {
  it("uses the configured measurement id directly", () => {
    expect(GOOGLE_ANALYTICS_ID).toBe("G-SWX7PCDRXW");
  });

  it("renders the GA component in production", () => {
    vi.stubEnv("NODE_ENV", "production");

    try {
      const element = GoogleAnalyticsScript();

      expect(element).not.toBeNull();
      expect(element?.props).toMatchObject({ gaId: GOOGLE_ANALYTICS_ID });
    } finally {
      vi.unstubAllEnvs();
    }
  });

  it("renders nothing outside production", () => {
    vi.stubEnv("NODE_ENV", "development");

    try {
      expect(GoogleAnalyticsScript()).toBeNull();
    } finally {
      vi.unstubAllEnvs();
    }
  });
});
