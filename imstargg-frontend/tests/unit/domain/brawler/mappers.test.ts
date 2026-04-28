import { describe, it, expect } from "vitest";
import { toBrawler, toBrawlerItem } from "@/domain/brawler/mappers";
import type { BrawlerResponse, GadgetResponse } from "@/api/types";

const dtoGadget = (overrides: Partial<GadgetResponse> = {}): GadgetResponse => ({
  id: 23000001,
  name: { ko: "브롤스타즈터틀!", en: "Brawl Box Turtle" },
  imageUrl: "https://cdn/gadget.png",
  ...overrides,
});

const dtoBrawler = (overrides: Partial<BrawlerResponse> = {}): BrawlerResponse => ({
  id: 16000000,
  name: { ko: "셸리", en: "Shelly" },
  rarity: "STARTING_BRAWLER",
  role: "DAMAGE_DEALER",
  gadgets: [dtoGadget()],
  gears: [],
  starPowers: [],
  hyperCharges: [],
  portraitImageUrl: "https://cdn/shelly.png",
  buffies: [],
  ...overrides,
});

describe("toBrawlerItem", () => {
  it("flattens MessagesResponse to ko-first name", () => {
    const item = toBrawlerItem(dtoGadget());
    expect(item.name).toBe("브롤스타즈터틀!");
    expect(item.id).toBe(23000001);
    expect(item.imageUrl).toBe("https://cdn/gadget.png");
  });

  it("preserves null imageUrl", () => {
    expect(toBrawlerItem(dtoGadget({ imageUrl: null })).imageUrl).toBeNull();
  });
});

describe("toBrawler", () => {
  it("flattens name to ko", () => {
    expect(toBrawler(dtoBrawler()).name).toBe("셸리");
  });

  it("falls back to en when ko absent", () => {
    expect(toBrawler(dtoBrawler({ name: { en: "Shelly" } })).name).toBe("Shelly");
  });

  it("maps all item arrays through toBrawlerItem", () => {
    const b = toBrawler(
      dtoBrawler({
        gadgets: [dtoGadget({ id: 1 }), dtoGadget({ id: 2 })],
        starPowers: [dtoGadget({ id: 11 })],
        gears: [dtoGadget({ id: 21 })],
        hyperCharges: [dtoGadget({ id: 31 })],
      }),
    );
    expect(b.gadgets.map((x) => x.id)).toEqual([1, 2]);
    expect(b.starPowers.map((x) => x.id)).toEqual([11]);
    expect(b.gears.map((x) => x.id)).toEqual([21]);
    expect(b.hyperCharges.map((x) => x.id)).toEqual([31]);
  });

  it("preserves portraitImageUrl null", () => {
    expect(toBrawler(dtoBrawler({ portraitImageUrl: null })).portraitImageUrl).toBeNull();
  });
});
