import { describe, it, expect } from "vitest";
import { formatAbsoluteTime, formatRelativeTime } from "@/lib/relativeTime";

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
    // 로컬 자정과 멀리 떨어진 시각을 사용해 타임존 경계 이슈를 회피
    const iso = "2026-04-20T12:00:00Z";
    const d = new Date(iso);
    const expected = `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, "0")}.${String(d.getDate()).padStart(2, "0")}`;
    expect(formatRelativeTime(iso, NOW)).toBe(expected);
  });
});

describe("formatAbsoluteTime", () => {
  it("ISO 문자열을 YYYY.MM.DD HH:mm 으로 포맷한다", () => {
    expect(formatAbsoluteTime("2026-06-03T14:09:00")).toBe("2026.06.03 14:09");
  });

  it("한 자리 월/일/시/분을 0으로 패딩한다", () => {
    expect(formatAbsoluteTime("2026-01-05T03:07:00")).toBe("2026.01.05 03:07");
  });
});
