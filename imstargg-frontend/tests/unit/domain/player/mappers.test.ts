import { describe, it, expect } from "vitest";
import { toPlayer, toPlayerBrawler } from "@/domain/player/mappers";
import type { PlayerResponse, PlayerBrawlerResponse } from "@/api/types";

const dto: PlayerResponse = {
  tag: "#88J8Q9RPV",
  name: "Hero",
  nameColor: "0xff",
  iconId: 28000000,
  trophies: 60000,
  highestTrophies: 65000,
  expLevel: 300,
  expPoints: 999,
  qualifiedFromChampionshipChallenge: true,
  victories3vs3: 1000,
  victoriesSolo: 200,
  victoriesDuo: 100,
  bestRoboRumbleTime: 0,
  bestTimeAsBigBrawler: 0,
  totalPrestigeLevel: 50,
  club: { tag: "#2U8CVLQQ9", name: "MyClub" },
};

describe("toPlayer", () => {
  it("maps base fields", () => {
    const p = toPlayer(dto);
    expect(p.tag).toBe("#88J8Q9RPV");
    expect(p.name).toBe("Hero");
    expect(p.trophies).toBe(60000);
    expect(p.club).toEqual({ tag: "#2U8CVLQQ9", name: "MyClub" });
  });
  it("handles null club", () => {
    expect(toPlayer({ ...dto, club: null }).club).toBeNull();
  });
});

const brawlerDto: PlayerBrawlerResponse = {
  brawler: {
    id: 16000000,
    name: { ko: "쉘리", en: "SHELLY" },
    rarity: "COMMON",
    role: "DAMAGE_DEALER",
    gadgets: [],
    gears: [],
    starPowers: [],
    hyperCharges: [],
    portraitImageUrl: "https://img/shelly.png",
    buffies: [],
  },
  power: 11,
  rank: 30,
  trophies: 700,
  highestTrophies: 1200,
  prestigeLevel: 3,
  buffies: [],
};

describe("toPlayerBrawler", () => {
  it("uses Korean name when present", () => {
    const b = toPlayerBrawler(brawlerDto);
    expect(b.name).toBe("쉘리");
    expect(b.power).toBe(11);
    expect(b.rarity).toBe("COMMON");
    expect(b.portraitImageUrl).toBe("https://img/shelly.png");
  });
  it("falls back to English when ko missing", () => {
    const noKo = {
      ...brawlerDto,
      brawler: { ...brawlerDto.brawler!, name: { en: "SHELLY" } },
    };
    expect(toPlayerBrawler(noKo).name).toBe("SHELLY");
  });
  it("handles null brawler", () => {
    const b = toPlayerBrawler({ ...brawlerDto, brawler: null });
    expect(b.id).toBeNull();
    expect(b.name).toBe("(unknown)");
  });
});
