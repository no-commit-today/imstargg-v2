import { describe, expect, it } from "vitest";
import {
  GOOGLE_ADSENSE_CLIENT,
  GOOGLE_ADSENSE_SCRIPT_SRC,
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

describe("GOOGLE_ADSENSE constants", () => {
  it("uses the configured publisher id directly", () => {
    expect(GOOGLE_ADSENSE_CLIENT).toBe("ca-pub-5084387143997125");
    expect(GOOGLE_ADSENSE_SCRIPT_SRC).toBe(
      "https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-5084387143997125",
    );
  });
});
