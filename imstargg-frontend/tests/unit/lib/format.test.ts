import { describe, it, expect } from "vitest";
import { formatNumber } from "@/lib/format";

describe("formatNumber", () => {
  it("uses ko-KR thousands separators", () => {
    expect(formatNumber(1234567)).toBe("1,234,567");
    expect(formatNumber(0)).toBe("0");
  });
});
