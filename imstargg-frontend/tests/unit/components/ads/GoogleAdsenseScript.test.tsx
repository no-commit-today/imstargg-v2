import { describe, expect, it, vi } from "vitest";
import {
  GOOGLE_ADSENSE_CLIENT,
  GOOGLE_ADSENSE_SCRIPT_SRC,
  GoogleAdsenseScript,
  shouldLoadGoogleAdsense,
} from "@/components/ads/GoogleAdsenseScript";

describe("shouldLoadGoogleAdsense", () => {
  it("loads only in production", () => {
    expect(shouldLoadGoogleAdsense("production")).toBe(true);
  });

  it("does not load in development", () => {
    expect(shouldLoadGoogleAdsense("development")).toBe(false);
  });

  it("does not load in test", () => {
    expect(shouldLoadGoogleAdsense("test")).toBe(false);
  });
});

describe("GoogleAdsenseScript", () => {
  it("uses the configured publisher id directly", () => {
    expect(GOOGLE_ADSENSE_CLIENT).toBe("ca-pub-5084387143997125");
    expect(GOOGLE_ADSENSE_SCRIPT_SRC).toBe(
      "https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-5084387143997125",
    );
  });

  it("loads after hydration in production", () => {
    vi.stubEnv("NODE_ENV", "production");

    try {
      const script = GoogleAdsenseScript();

      expect(script).not.toBeNull();
      expect(script?.props).toMatchObject({
        id: "google-adsense",
        async: true,
        strategy: "afterInteractive",
        src: GOOGLE_ADSENSE_SCRIPT_SRC,
        crossOrigin: "anonymous",
      });
    } finally {
      vi.unstubAllEnvs();
    }
  });
});
