import { describe, it, expect } from "vitest";
import { formatNumber, formatRelativeTime } from "@/lib/format";

describe("formatNumber", () => {
  it("uses ko-KR thousands separators", () => {
    expect(formatNumber(1234567)).toBe("1,234,567");
    expect(formatNumber(0)).toBe("0");
  });
});

describe("formatRelativeTime", () => {
  it("returns '방금 전' for very recent times", () => {
    const now = new Date("2026-04-27T10:00:00Z");
    expect(formatRelativeTime("2026-04-27T09:59:55Z", now)).toBe("방금 전");
  });
  it("formats minute differences", () => {
    const now = new Date("2026-04-27T10:00:00Z");
    expect(formatRelativeTime("2026-04-27T09:55:00Z", now)).toBe("5분 전");
  });
  it("formats hour differences", () => {
    const now = new Date("2026-04-27T10:00:00Z");
    expect(formatRelativeTime("2026-04-27T07:00:00Z", now)).toBe("3시간 전");
  });
});
