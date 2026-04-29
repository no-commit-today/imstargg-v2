import { describe, it, expect } from "vitest";
import { formatRelativeTime } from "@/lib/relativeTime";

const NOW = new Date("2026-04-29T12:00:00Z").getTime();

describe("formatRelativeTime", () => {
  it("30초 이내는 '방금 전'", () => {
    expect(formatRelativeTime("2026-04-29T11:59:50Z", NOW)).toBe("방금 전");
  });
  it("분 단위", () => {
    expect(formatRelativeTime("2026-04-29T11:55:00Z", NOW)).toBe("5분 전");
  });
  it("시간 단위", () => {
    expect(formatRelativeTime("2026-04-29T09:00:00Z", NOW)).toBe("3시간 전");
  });
  it("일 단위 (7일 이내)", () => {
    expect(formatRelativeTime("2026-04-26T12:00:00Z", NOW)).toBe("3일 전");
  });
  it("7일 초과는 절대 날짜 yyyy.MM.dd", () => {
    expect(formatRelativeTime("2026-04-20T12:00:00Z", NOW)).toBe("2026.04.20");
  });
});
