import { describe, it, expect } from "vitest";
import { normalizeTag, stripHash, toUrlPath, isValidTag } from "@/lib/tag";

describe("normalizeTag", () => {
  it.each([
    ["#88J8Q9RPV", "#88J8Q9RPV"],
    ["88J8Q9RPV", "#88J8Q9RPV"],
    ["%2388J8Q9RPV", "#88J8Q9RPV"],
    ["%2388j8q9rpv", "#88J8Q9RPV"],
    [" #88J8Q9RPV ", "#88J8Q9RPV"],
  ])("normalizes %s -> %s", (input, expected) => {
    expect(normalizeTag(input)).toBe(expected);
  });
});

describe("stripHash", () => {
  it("removes leading hash", () => {
    expect(stripHash("#88J8Q9RPV")).toBe("88J8Q9RPV");
    expect(stripHash("88J8Q9RPV")).toBe("88J8Q9RPV");
    expect(stripHash("%2388J8Q9RPV")).toBe("88J8Q9RPV");
  });
});

describe("toUrlPath", () => {
  it("encodes hash as %23", () => {
    expect(toUrlPath("#88J8Q9RPV")).toBe("%2388J8Q9RPV");
    expect(toUrlPath("88J8Q9RPV")).toBe("%2388J8Q9RPV");
  });
});

describe("isValidTag", () => {
  it("accepts valid characters only", () => {
    expect(isValidTag("#88J8Q9RPV")).toBe(true);
    expect(isValidTag("88J8Q9RPV")).toBe(true);
  });
  it("rejects invalid characters", () => {
    expect(isValidTag("##ABC")).toBe(false);
    expect(isValidTag("ABC?")).toBe(false);
    expect(isValidTag("")).toBe(false);
    expect(isValidTag("#")).toBe(false);
  });
});
