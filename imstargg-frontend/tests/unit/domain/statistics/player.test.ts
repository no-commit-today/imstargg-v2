import { describe, it, expect } from "vitest";
import { toPlayerBrawlerStat, toPlayerBrawlerRoleStat } from "@/domain/statistics/player";
import type {
  PlayerBrawlerStatisticsItemResponse,
  PlayerBrawlerRoleStatisticsItemResponse,
} from "@/api/types";

describe("toPlayerBrawlerStat", () => {
  it("brawler 정보와 결과 카운트를 매핑한다", () => {
    const dto: PlayerBrawlerStatisticsItemResponse = {
      brawler: {
        id: 1,
        name: { ko: "샐리", en: "Shelly" },
        rarity: "STARTING_BRAWLER",
        role: "DAMAGE_DEALER",
        portraitImageUrl: null,
      },
      totalBattleCount: 100,
      victoryCount: 60,
      defeatCount: 35,
      drawCount: 5,
      winRate: 0.6,
    };
    const result = toPlayerBrawlerStat(dto);
    expect(result.brawler?.id).toBe(1);
    expect(result.brawler?.name).toBe("샐리");
    expect(result.totalBattleCount).toBe(100);
    expect(result.winRate).toBe(0.6);
  });

  it("brawler 가 null 이면 brawler 도 null", () => {
    const dto: PlayerBrawlerStatisticsItemResponse = {
      brawler: null,
      totalBattleCount: 10,
      victoryCount: 5,
      defeatCount: 5,
      drawCount: 0,
      winRate: 0.5,
    };
    const result = toPlayerBrawlerStat(dto);
    expect(result.brawler).toBeNull();
  });
});

describe("toPlayerBrawlerRoleStat", () => {
  it("role 과 결과 카운트를 매핑한다", () => {
    const dto: PlayerBrawlerRoleStatisticsItemResponse = {
      role: "TANK",
      totalBattleCount: 50,
      victoryCount: 30,
      defeatCount: 20,
      drawCount: 0,
      winRate: 0.6,
    };
    const result = toPlayerBrawlerRoleStat(dto);
    expect(result.role).toBe("TANK");
    expect(result.winRate).toBe(0.6);
  });
});
