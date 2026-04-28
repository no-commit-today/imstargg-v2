import { describe, expect, it } from "vitest";
import {
  toBrawlerRanking,
  toClubRanking,
  toPlayerRanking,
  toRankingPlayer,
} from "@/domain/ranking/mappers";

const playerDto = {
  tag: "#ABC",
  name: "Player",
  nameColor: "0xffffffff",
  iconId: 28000000,
  clubName: "MyClub",
};

describe("toRankingPlayer", () => {
  it("DTO 필드를 그대로 매핑한다", () => {
    expect(toRankingPlayer(playerDto)).toEqual({
      tag: "#ABC",
      name: "Player",
      nameColor: "0xffffffff",
      iconId: 28000000,
      clubName: "MyClub",
    });
  });

  it("nameColor null 허용", () => {
    expect(toRankingPlayer({ ...playerDto, nameColor: null }).nameColor).toBeNull();
  });
});

describe("toPlayerRanking", () => {
  it("country를 정규화하고 필드를 매핑한다", () => {
    const result = toPlayerRanking({
      country: "kr",
      rank: 1,
      player: playerDto,
      trophies: 50000,
      rankChange: 2,
    });
    expect(result).toEqual({
      country: "kr",
      rank: 1,
      player: {
        tag: "#ABC",
        name: "Player",
        nameColor: "0xffffffff",
        iconId: 28000000,
        clubName: "MyClub",
      },
      trophies: 50000,
      rankChange: 2,
    });
  });

  it("알 수 없는 country는 global로 fallback", () => {
    const result = toPlayerRanking({
      country: "fr",
      rank: 1,
      player: playerDto,
      trophies: 50000,
      rankChange: null,
    });
    expect(result.country).toBe("global");
    expect(result.rankChange).toBeNull();
  });
});

describe("toBrawlerRanking", () => {
  it("brawlerId를 포함해 매핑한다", () => {
    const result = toBrawlerRanking({
      country: "global",
      brawlerId: 16000000,
      rank: 1,
      player: playerDto,
      trophies: 1500,
      rankChange: -1,
    });
    expect(result.brawlerId).toBe(16000000);
    expect(result.rankChange).toBe(-1);
  });
});

describe("toClubRanking", () => {
  it("클럽 정보를 매핑한다", () => {
    const result = toClubRanking({
      country: "kr",
      tag: "#XYZ",
      name: "ClubName",
      badgeId: 8000000,
      trophies: 1000000,
      rank: 1,
      memberCount: 30,
      rankChange: 0,
    });
    expect(result).toEqual({
      country: "kr",
      tag: "#XYZ",
      name: "ClubName",
      badgeId: 8000000,
      trophies: 1000000,
      rank: 1,
      memberCount: 30,
      rankChange: 0,
    });
  });
});
