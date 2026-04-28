import { describe, it, expect } from "vitest";
import { pickName } from "@/domain/i18n";

describe("pickName", () => {
  it("prefers ko", () => {
    expect(pickName({ ko: "셸리", en: "Shelly" })).toBe("셸리");
  });
  it("falls back to en when ko absent", () => {
    expect(pickName({ en: "Shelly" })).toBe("Shelly");
  });
  it("falls back to first available value", () => {
    expect(pickName({ ja: "シェリー" })).toBe("シェリー");
  });
  it("returns unknown for null/undefined", () => {
    expect(pickName(undefined)).toBe("(unknown)");
    expect(pickName(null)).toBe("(unknown)");
  });
  it("returns unknown for empty object", () => {
    expect(pickName({})).toBe("(unknown)");
  });
});
