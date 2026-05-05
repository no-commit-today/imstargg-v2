import { describe, expect, it } from "vitest";
import { shouldLoadGoogleAdsense } from "@/components/ads/GoogleAdsenseScript";

describe("shouldLoadGoogleAdsense", () => {
  it("loads only in production with a client id", () => {
    expect(
      shouldLoadGoogleAdsense("production", "ca-pub-5084387143997125"),
    ).toBe(true);
  });

  it("does not load in development", () => {
    expect(
      shouldLoadGoogleAdsense("development", "ca-pub-5084387143997125"),
    ).toBe(false);
  });

  it("does not load in test", () => {
    expect(shouldLoadGoogleAdsense("test", "ca-pub-5084387143997125")).toBe(
      false,
    );
  });

  it("does not load without a client id", () => {
    expect(shouldLoadGoogleAdsense("production", "")).toBe(false);
  });
});
