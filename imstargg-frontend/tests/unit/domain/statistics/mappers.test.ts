import { describe, expect, it } from "vitest";
import {
  toBrawlerResultStatistics,
  toBrawlerRankStat,
  toPairResultStat,
  toPairRankStat,
  toEnemyResultStat,
  toDailyResultStat,
  toItemOwnership,
} from "@/domain/statistics/mappers";

const fakeBrawler = (id: number) => ({
  id,
  name: { ko: `브롤러${id}` },
  rarity: "RARE" as const,
  role: "DAMAGE_DEALER" as const,
  gadgets: [],
  gears: [],
  starPowers: [],
  hyperCharges: [],
  portraitImageUrl: null,
  buffies: [],
});

describe("toBrawlerResultStatistics", () => {
  it("DTO 를 도메인으로 매핑한다", () => {
    const result = toBrawlerResultStatistics({
      totalBattleCount: 1000,
      items: [
        {
          brawler: fakeBrawler(16000000),
          totalBattleCount: 100,
          pickRate: 0.1,
          winRate: 0.55,
          starPlayerRate: 0.2,
          tierScore: 80,
          tier: "S",
        },
      ],
    });
    expect(result.totalBattleCount).toBe(1000);
    expect(result.items).toHaveLength(1);
    expect(result.items[0].brawler?.id).toBe(16000000);
    expect(result.items[0].tier).toBe("S");
  });

  it("brawler 가 null 이어도 처리한다", () => {
    const result = toBrawlerResultStatistics({
      totalBattleCount: 0,
      items: [
        {
          brawler: null,
          totalBattleCount: 5,
          pickRate: 0.01,
          winRate: 0.5,
          starPlayerRate: 0,
          tierScore: 0,
          tier: "D",
        },
      ],
    });
    expect(result.items[0].brawler).toBeNull();
  });

  it("알 수 없는 tier 는 D 로 fallback", () => {
    const result = toBrawlerResultStatistics({
      totalBattleCount: 0,
      items: [
        {
          brawler: null,
          totalBattleCount: 1,
          pickRate: 0,
          winRate: 0,
          starPlayerRate: 0,
          tierScore: 0,
          tier: "Z",
        },
      ],
    });
    expect(result.items[0].tier).toBe("D");
  });
});

describe("toBrawlerRankStat", () => {
  it("매핑한다", () => {
    const result = toBrawlerRankStat({
      brawler: fakeBrawler(16000001),
      totalBattleCount: 50,
      averageRank: 4.32,
    });
    expect(result.brawler?.id).toBe(16000001);
    expect(result.averageRank).toBe(4.32);
  });
});

describe("toPairResultStat", () => {
  it("매핑한다", () => {
    const result = toPairResultStat({
      brawler: fakeBrawler(1),
      pairBrawler: fakeBrawler(2),
      totalBattleCount: 100,
      pickRate: 0.05,
      winRate: 0.6,
    });
    expect(result.brawler?.id).toBe(1);
    expect(result.pairBrawler?.id).toBe(2);
  });
});

describe("toPairRankStat", () => {
  it("매핑한다", () => {
    const result = toPairRankStat({
      brawler: fakeBrawler(1),
      pairBrawler: fakeBrawler(2),
      totalBattleCount: 50,
      averageRank: 3.0,
    });
    expect(result.averageRank).toBe(3.0);
  });
});

describe("toEnemyResultStat", () => {
  it("매핑한다", () => {
    const result = toEnemyResultStat({
      brawler: fakeBrawler(1),
      enemyBrawler: fakeBrawler(3),
      totalBattleCount: 200,
      pickRate: 0.08,
      winRate: 0.4,
    });
    expect(result.enemyBrawler?.id).toBe(3);
  });
});

describe("toDailyResultStat", () => {
  it("매핑한다", () => {
    const result = toDailyResultStat({
      date: "2026-04-29",
      brawler: null,
      totalBattleCount: 100,
      winRate: 0.55,
      starPlayerRate: 0.2,
    });
    expect(result.date).toBe("2026-04-29");
    expect(result.winRate).toBe(0.55);
  });
});

describe("toItemOwnership", () => {
  it("5개 카테고리를 그대로 매핑", () => {
    const result = toItemOwnership({
      gears: [{ itemId: 1, ownershipRate: 0.5, ownerCount: 100, totalCount: 200 }],
      gadgets: [],
      starPowers: [],
      hyperCharges: [],
      buffies: [{ buffyType: "MYTHIC_GEAR", ownershipRate: 0.1, ownerCount: 10, totalCount: 100 }],
    });
    expect(result.gears).toHaveLength(1);
    expect(result.buffies).toHaveLength(1);
    expect(result.buffies[0].buffyType).toBe("MYTHIC_GEAR");
  });
});
