import { describe, it, expect } from "vitest";
import { toBrawler, toBrawlerItem, toBrawlerItems, toBrawlerWithItems } from "@/domain/brawler/mappers";
import type { BrawlerResponse, BrawlerItemsResponse, BrawlerWithItemsResponse, GadgetResponse, GearResponse } from "@/api/types";

const dtoGadget = (overrides: Partial<GadgetResponse> = {}): GadgetResponse => ({
  id: 23000001,
  name: { ko: "브롤스타즈터틀!", en: "Brawl Box Turtle" },
  imageUrl: "https://cdn/gadget.png",
  ...overrides,
});

const dtoGear = (overrides: Partial<GearResponse> = {}): GearResponse => ({
  id: 29000001,
  name: { ko: "방어 기어", en: "Shield Gear" },
  rarity: "SUPER_RARE",
  imageUrl: "https://cdn/gear.png",
  ...overrides,
});

const dtoBrawler = (overrides: Partial<BrawlerResponse> = {}): BrawlerResponse => ({
  id: 16000000,
  name: { ko: "셸리", en: "Shelly" },
  rarity: "STARTING_BRAWLER",
  role: "DAMAGE_DEALER",
  portraitImageUrl: "https://cdn/shelly.png",
  ...overrides,
});

const dtoBrawlerItems = (overrides: Partial<BrawlerItemsResponse> = {}): BrawlerItemsResponse => ({
  gadgets: [],
  gears: [],
  starPowers: [],
  hyperCharge: null,
  buffies: null,
  ...overrides,
});

describe("toBrawlerItem", () => {
  it("flattens MessagesResponse to ko-first name", () => {
    const item = toBrawlerItem(dtoGadget());
    expect(item.name).toBe("브롤스타즈터틀!");
    expect(item.id).toBe(23000001);
    expect(item.imageUrl).toBe("https://cdn/gadget.png");
    expect(item.rarity).toBeNull();
  });

  it("preserves null imageUrl", () => {
    expect(toBrawlerItem(dtoGadget({ imageUrl: null })).imageUrl).toBeNull();
  });

  it("toBrawlerItem 은 모든 종류에서 rarity 를 null 로 반환한다", () => {
    expect(toBrawlerItem(dtoGadget()).rarity).toBeNull();
    expect(toBrawlerItem(dtoGear({ rarity: "EPIC" })).rarity).toBeNull();
  });
});

describe("toBrawler", () => {
  it("flattens name to ko", () => {
    expect(toBrawler(dtoBrawler()).name).toBe("셸리");
  });

  it("falls back to en when ko absent", () => {
    expect(toBrawler(dtoBrawler({ name: { en: "Shelly" } })).name).toBe("Shelly");
  });

  it("preserves portraitImageUrl null", () => {
    expect(toBrawler(dtoBrawler({ portraitImageUrl: null })).portraitImageUrl).toBeNull();
  });

  it("maps rarity and role", () => {
    const b = toBrawler(dtoBrawler());
    expect(b.rarity).toBe("STARTING_BRAWLER");
    expect(b.role).toBe("DAMAGE_DEALER");
  });
});

describe("toBrawlerItems", () => {
  it("maps all item arrays through toBrawlerItem", () => {
    const items = toBrawlerItems(
      dtoBrawlerItems({
        gadgets: [dtoGadget({ id: 1 }), dtoGadget({ id: 2 })],
        starPowers: [dtoGadget({ id: 11 })],
        gears: [dtoGear({ id: 21 })],
        hyperCharge: dtoGadget({ id: 31 }),
      }),
    );
    expect(items.gadgets.map((x) => x.id)).toEqual([1, 2]);
    expect(items.starPowers.map((x) => x.id)).toEqual([11]);
    expect(items.gears.map((x) => x.id)).toEqual([21]);
    expect(items.hyperCharge?.id).toBe(31);
  });

  it("returns null hyperCharge when dto is null", () => {
    expect(toBrawlerItems(dtoBrawlerItems()).hyperCharge).toBeNull();
  });

  it("toBrawlerItems 의 gears 는 DTO rarity 를 패스스루한다", () => {
    const items = toBrawlerItems(
      dtoBrawlerItems({
        gears: [dtoGear({ id: 1, rarity: "MYTHIC" }), dtoGear({ id: 2, rarity: "EPIC" })],
      }),
    );
    expect(items.gears.map((g) => g.rarity)).toEqual(["MYTHIC", "EPIC"]);
  });

  it("toBrawlerItems 의 gear 외 항목은 rarity 가 null 이다", () => {
    const items = toBrawlerItems(
      dtoBrawlerItems({
        gadgets: [dtoGadget({ id: 1 })],
        starPowers: [dtoGadget({ id: 2 })],
        hyperCharge: dtoGadget({ id: 3 }),
      }),
    );
    expect(items.gadgets[0].rarity).toBeNull();
    expect(items.starPowers[0].rarity).toBeNull();
    expect(items.hyperCharge?.rarity).toBeNull();
  });
});

describe("toBrawlerWithItems", () => {
  it("composes brawler and items", () => {
    const dto: BrawlerWithItemsResponse = {
      brawler: dtoBrawler(),
      items: dtoBrawlerItems({ gadgets: [dtoGadget()] }),
    };
    const detail = toBrawlerWithItems(dto);
    expect(detail.brawler.name).toBe("셸리");
    expect(detail.items.gadgets).toHaveLength(1);
  });
});
