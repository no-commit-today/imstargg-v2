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
    rarity: "STARTING_BRAWLER",
    role: "DAMAGE_DEALER",
    portraitImageUrl: "https://img/shelly.png",
  },
  power: 11,
  rank: 30,
  trophies: 700,
  highestTrophies: 1200,
  prestigeLevel: 3,
  gadgets: [],
  gears: [],
  starPowers: [],
  hyperCharge: null,
  buffies: null,
};

describe("toPlayerBrawler", () => {
  it("maps brawler meta fields", () => {
    const b = toPlayerBrawler(brawlerDto);
    expect(b.brawler?.name).toBe("쉘리");
    expect(b.power).toBe(11);
    expect(b.brawler?.rarity).toBe("STARTING_BRAWLER");
    expect(b.brawler?.portraitImageUrl).toBe("https://img/shelly.png");
  });
  it("falls back to English when ko missing", () => {
    const noKo = {
      ...brawlerDto,
      brawler: { ...brawlerDto.brawler!, name: { en: "SHELLY" } },
    };
    expect(toPlayerBrawler(noKo).brawler?.name).toBe("SHELLY");
  });
  it("handles null brawler", () => {
    const b = toPlayerBrawler({ ...brawlerDto, brawler: null });
    expect(b.brawler).toBeNull();
  });
  it("maps owned item collections", () => {
    const withItems: PlayerBrawlerResponse = {
      ...brawlerDto,
      gadgets: [
        { item: { id: 1, name: { ko: "가젯1" }, imageUrl: null }, owned: true },
        { item: { id: 2, name: { ko: "가젯2" }, imageUrl: null }, owned: false },
      ],
    };
    const b = toPlayerBrawler(withItems);
    expect(b.gadgets).toHaveLength(2);
    expect(b.gadgets[0].owned).toBe(true);
    expect(b.gadgets[1].owned).toBe(false);
    expect(b.gadgets[0].item.id).toBe(1);
  });
  it("gears 의 rarity 를 도메인 모델에 패스스루한다", () => {
    const withGears: PlayerBrawlerResponse = {
      ...brawlerDto,
      gears: [
        { item: { id: 10, name: { ko: "기어1" }, rarity: "EPIC", imageUrl: null }, owned: true },
        { item: { id: 11, name: { ko: "기어2" }, rarity: "MYTHIC", imageUrl: null }, owned: false },
      ],
    };
    const b = toPlayerBrawler(withGears);
    expect(b.gears.map((g) => g.item.rarity)).toEqual(["EPIC", "MYTHIC"]);
  });
});
