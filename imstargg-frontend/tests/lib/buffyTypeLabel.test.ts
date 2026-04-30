import { describe, expect, it } from "vitest";
import { buffyTypeLabel } from "@/domain/brawler/buffyTypeLabel";

describe("buffyTypeLabel", () => {
  it("알려진 enum 을 한글로 매핑한다", () => {
    expect(buffyTypeLabel("GADGET")).toBe("가젯");
    expect(buffyTypeLabel("HYPER_CHARGE")).toBe("하이퍼차지");
    expect(buffyTypeLabel("STAR_POWER")).toBe("스타파워");
    expect(buffyTypeLabel("MYTHIC_GEAR")).toBe("신화 기어");
  });

  it("알려지지 않은 값은 원문을 반환한다", () => {
    expect(buffyTypeLabel("UNKNOWN_X")).toBe("UNKNOWN_X");
  });
});
