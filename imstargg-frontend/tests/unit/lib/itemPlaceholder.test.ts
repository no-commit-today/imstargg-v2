import { describe, expect, it } from "vitest";
import { basePlaceholder, emptyPlaceholder } from "@/lib/itemPlaceholder";

describe("basePlaceholder", () => {
  it("gadget 의 base placeholder 를 반환한다", () => {
    expect(basePlaceholder("gadget")).toBe("/assets/gadget_base_empty.webp");
  });

  it("starPower 의 base placeholder 를 반환한다", () => {
    expect(basePlaceholder("starPower")).toBe("/assets/starpower_base01_empty.webp");
  });

  it("hyperCharge 의 base placeholder 를 반환한다", () => {
    expect(basePlaceholder("hyperCharge")).toBe("/assets/hypercharge_base_empty.webp");
  });

  it("gear 는 rarity 에 따라 다른 자산을 반환한다", () => {
    expect(basePlaceholder("gear", "SUPER_RARE")).toBe("/assets/gear_base_empty.webp");
    expect(basePlaceholder("gear", "EPIC")).toBe("/assets/gear_epic_super_base_empty.webp");
    expect(basePlaceholder("gear", "MYTHIC")).toBe("/assets/gear_mythic_super_base_empty.webp");
  });

  it("gear rarity 가 없으면 일반 base 를 반환한다", () => {
    expect(basePlaceholder("gear")).toBe("/assets/gear_base_empty.webp");
  });

  it("buffy 는 자산이 없으므로 null 을 반환한다", () => {
    expect(basePlaceholder("buffy")).toBeNull();
  });
});

describe("emptyPlaceholder", () => {
  it("gadget/gear/starPower 의 empty placeholder 를 반환한다", () => {
    expect(emptyPlaceholder("gadget")).toBe("/assets/gadget_empty.webp");
    expect(emptyPlaceholder("gear")).toBe("/assets/gear_empty.webp");
    expect(emptyPlaceholder("starPower")).toBe("/assets/starpower_empty.webp");
  });

  it("hyperCharge 와 buffy 는 empty 자산이 없으므로 null 을 반환한다", () => {
    expect(emptyPlaceholder("hyperCharge")).toBeNull();
    expect(emptyPlaceholder("buffy")).toBeNull();
  });
});
