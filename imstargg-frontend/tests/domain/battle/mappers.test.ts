import { describe, it, expect } from "vitest";
import { toBattle } from "@/domain/battle/mappers";
import type { BattleResponse } from "@/api/types";

const baseDto: BattleResponse = {
  battleTime: "2026-04-29T11:00:00Z",
  event: {
    id: 1,
    mode: { id: 10, name: { ko: "젬 그랩", en: "Gem Grab" }, iconImageUrl: "x.png", resultType: "RESULT", color: "#8F43EA" },
    mapName: { ko: "다이아몬드 광산", en: "Diamond Mine" },
  },
  mode: "GEM_GRAB",
  type: "RANKED",
  result: "VICTORY",
  duration: 120,
  starPlayerTag: "#A1",
  rank: null,
  trophyChange: 9,
  teams: [
    [
      { tag: "#A1", name: "P1", soloRankTier: null, brawler: { brawler: null, power: 11, trophies: 700, trophyChange: 9 } },
    ],
    [
      { tag: "#B1", name: "P2", soloRankTier: null, brawler: { brawler: null, power: 10, trophies: 500, trophyChange: -6 } },
    ],
  ],
};

describe("toBattle", () => {
  it("rank 가 null 이면 resultType=RESULT", () => {
    const sut = toBattle(baseDto);
    expect(sut.resultType).toBe("RESULT");
    expect(sut.rank).toBeNull();
  });

  it("rank 가 있으면 resultType=RANK", () => {
    const sut = toBattle({ ...baseDto, rank: 3, result: null });
    expect(sut.resultType).toBe("RANK");
    expect(sut.rank).toBe(3);
  });

  it("event.mapName / modeName 은 한국어 우선 추출", () => {
    const sut = toBattle(baseDto);
    expect(sut.event?.mapName).toBe("다이아몬드 광산");
    expect(sut.event?.modeName).toBe("젬 그랩");
    expect(sut.event?.modeIconUrl).toBe("x.png");
    expect(sut.event?.modeResultType).toBe("RESULT");
  });

  it("event 가 null 이면 event=null", () => {
    const sut = toBattle({ ...baseDto, event: null });
    expect(sut.event).toBeNull();
  });

  it("event.mode 가 null 이면 modeName/modeIconUrl/modeResultType 모두 null", () => {
    const sut = toBattle({
      ...baseDto,
      event: { id: 1, mode: null, mapName: { ko: "맵", en: "Map" } },
    });
    expect(sut.event?.modeName).toBeNull();
    expect(sut.event?.modeIconUrl).toBeNull();
    expect(sut.event?.modeResultType).toBeNull();
    expect(sut.event?.mapName).toBe("맵");
  });

  it("빈 teams 처리", () => {
    const sut = toBattle({ ...baseDto, teams: [] });
    expect(sut.teams).toEqual([]);
  });

  it("teams 의 각 플레이어가 BattlePlayer 형태로 매핑됨", () => {
    const sut = toBattle(baseDto);
    expect(sut.teams).toHaveLength(2);
    expect(sut.teams[0][0].tag).toBe("#A1");
    expect(sut.teams[0][0].brawler.power).toBe(11);
    expect(sut.teams[0][0].brawler.trophyChange).toBe(9);
  });
});
