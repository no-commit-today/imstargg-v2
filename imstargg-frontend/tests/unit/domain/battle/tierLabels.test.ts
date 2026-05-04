import { describe, expect, it } from "vitest";
import { tierColor, tierStickerImage, tierSegmentNumber } from "@/domain/battle/tierLabels";

describe("tierStickerImage", () => {
  it("base tier 별로 단일 자산을 반환한다 (1/2/3 segment 무관)", () => {
    expect(tierStickerImage("BRONZE_1")).toBe("/assets/icon_rank_sticker_bronze.webp");
    expect(tierStickerImage("BRONZE_3")).toBe("/assets/icon_rank_sticker_bronze.webp");
    expect(tierStickerImage("SILVER_2")).toBe("/assets/icon_rank_sticker_silver.webp");
    expect(tierStickerImage("GOLD_2")).toBe("/assets/icon_rank_sticker_gold.webp");
    expect(tierStickerImage("DIAMOND_2")).toBe("/assets/icon_rank_sticker_diamond.webp");
    expect(tierStickerImage("MYTHIC_2")).toBe("/assets/icon_rank_sticker_mythic.webp");
    expect(tierStickerImage("LEGENDARY_2")).toBe("/assets/icon_rank_sticker_legendary.webp");
    expect(tierStickerImage("MASTER_2")).toBe("/assets/icon_rank_sticker_masters.webp");
    expect(tierStickerImage("PRO")).toBe("/assets/icon_rank_sticker_pro.webp");
  });
});

describe("tierSegmentNumber", () => {
  it("BRONZE_2 는 '2' 를 반환한다", () => {
    expect(tierSegmentNumber("BRONZE_2")).toBe("2");
  });

  it("MASTER_3 는 '3' 을 반환한다", () => {
    expect(tierSegmentNumber("MASTER_3")).toBe("3");
  });

  it("PRO 는 단계가 없으므로 null 을 반환한다", () => {
    expect(tierSegmentNumber("PRO")).toBeNull();
  });
});

describe("tierColor", () => {
  it("기존 색 매핑을 유지한다", () => {
    expect(tierColor("BRONZE_1")).toBe("#C77545");
    expect(tierColor("MYTHIC_3")).toBe("#B83BFF");
    expect(tierColor("LEGENDARY_2")).toBe("#FF3A3A");
  });

  it("MASTER 와 PRO 는 같은 옐로우 톤을 사용한다", () => {
    expect(tierColor("MASTER_1")).toBe("#FFD447");
    expect(tierColor("PRO")).toBe("#FFD447");
  });
});
