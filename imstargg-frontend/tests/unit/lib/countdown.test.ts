import { describe, it, expect } from "vitest";
import { computeCountdown, formatCountdown } from "@/lib/countdown";

describe("computeCountdown", () => {
  it("computes hours/minutes/seconds for 2h13m45s remaining", () => {
    const now = new Date("2026-04-28T00:00:00Z").getTime();
    const end = "2026-04-28T02:13:45Z";
    const cd = computeCountdown(end, now);
    expect(cd.hours).toBe(2);
    expect(cd.minutes).toBe(13);
    expect(cd.seconds).toBe(45);
    expect(cd.ended).toBe(false);
    expect(cd.totalMs).toBe(2 * 3600_000 + 13 * 60_000 + 45_000);
  });

  it("flags ended when totalMs is zero or negative", () => {
    const now = new Date("2026-04-28T02:00:00Z").getTime();
    const end = "2026-04-28T01:00:00Z";
    const cd = computeCountdown(end, now);
    expect(cd.ended).toBe(true);
    expect(cd.hours).toBe(0);
    expect(cd.minutes).toBe(0);
    expect(cd.seconds).toBe(0);
  });

  it("clamps negative remaining to zero parts", () => {
    const cd = computeCountdown("2026-04-28T01:00:00Z", new Date("2026-04-29T01:00:00Z").getTime());
    expect(cd.hours).toBe(0);
    expect(cd.minutes).toBe(0);
    expect(cd.seconds).toBe(0);
    expect(cd.totalMs).toBeLessThan(0);
    expect(cd.ended).toBe(true);
  });
});

describe("formatCountdown", () => {
  it("pads to HH:MM:SS", () => {
    expect(
      formatCountdown({ hours: 2, minutes: 3, seconds: 5, totalMs: 1, ended: false }),
    ).toBe("02:03:05");
  });

  it("returns 종료됨 when ended", () => {
    expect(
      formatCountdown({ hours: 0, minutes: 0, seconds: 0, totalMs: -1, ended: true }),
    ).toBe("종료됨");
  });
});
