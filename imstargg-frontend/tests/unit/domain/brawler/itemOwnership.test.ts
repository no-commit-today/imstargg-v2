import { describe, expect, it } from "vitest";
import type { BrawlerItems } from "@/domain/brawler/types";
import type { ItemOwnership } from "@/domain/statistics/types";
import { buildBrawlerItemGroups } from "@/domain/brawler/itemOwnership";

const items: BrawlerItems = {
  gadgets: [
    { id: 11, name: "Gadget A", imageUrl: "/gadget-a.webp", rarity: null },
  ],
  gears: [
    { id: 21, name: "Gear A", imageUrl: "/gear-a.webp", rarity: "EPIC" },
  ],
  starPowers: [
    { id: 31, name: "Star Power A", imageUrl: "/star-power-a.webp", rarity: null },
  ],
  hyperCharge: {
    id: 41,
    name: "Hyper Charge A",
    imageUrl: "/hyper-charge-a.webp",
    rarity: null,
  },
  buffies: {
    gadget: { type: "GADGET", imageUrl: "/buffy-gadget.webp" },
    starPower: { type: "STAR_POWER", imageUrl: "/buffy-star-power.webp" },
    hyperCharge: { type: "HYPER_CHARGE", imageUrl: "/buffy-hyper-charge.webp" },
  },
};

const emptyOwnership: ItemOwnership = {
  gadgets: [],
  gears: [],
  starPowers: [],
  hyperCharges: [],
  buffies: [],
};

describe("buildBrawlerItemGroups", () => {
  it("returns item groups in detail row order", () => {
    const groups = buildBrawlerItemGroups(items, emptyOwnership);

    expect(groups.map((group) => group.key)).toEqual([
      "gadgets-starPowers",
      "gears",
      "hyperCharge-buffies",
    ]);
  });

  it("matches normal item ownership by itemId", () => {
    const groups = buildBrawlerItemGroups(items, {
      ...emptyOwnership,
      gadgets: [
        { itemId: 11, ownershipRate: 0.42, ownerCount: 42, totalCount: 100 },
      ],
    });

    const gadget = groups[0].sections[0].items[0];

    expect(gadget).toMatchObject({
      key: "gadget:11",
      kind: "gadget",
      name: "Gadget A",
      imageUrl: "/gadget-a.webp",
      ownership: { rate: 0.42, ownerCount: 42, totalCount: 100 },
    });
  });

  it("leaves ownership undefined when no matching ownership exists", () => {
    const groups = buildBrawlerItemGroups(items, {
      ...emptyOwnership,
      gears: [
        { itemId: 999, ownershipRate: 0.99, ownerCount: 99, totalCount: 100 },
      ],
    });

    const gear = groups[1].sections[0].items[0];

    expect(gear).toMatchObject({
      key: "gear:21",
      kind: "gear",
      name: "Gear A",
      gearRarity: "EPIC",
    });
    expect(gear.ownership).toBeUndefined();
  });

  it("matches buffy ownership by buffyType", () => {
    const groups = buildBrawlerItemGroups(items, {
      ...emptyOwnership,
      buffies: [
        {
          buffyType: "STAR_POWER",
          ownershipRate: 0.12,
          ownerCount: 12,
          totalCount: 100,
        },
      ],
    });

    const buffy = groups[2].sections[1].items[1];

    expect(buffy).toMatchObject({
      key: "buffy:STAR_POWER",
      kind: "buffy",
      name: "스타파워",
      imageUrl: "/buffy-star-power.webp",
      ownership: { rate: 0.12, ownerCount: 12, totalCount: 100 },
    });
  });

  it("handles null hyperCharge and still includes buffies when present", () => {
    const groups = buildBrawlerItemGroups(
      { ...items, hyperCharge: null },
      emptyOwnership,
    );

    const thirdGroup = groups[2];

    expect(thirdGroup.sections[0].items).toEqual([]);
    expect(thirdGroup.sections[1].items.map((item) => item.key)).toEqual([
      "buffy:GADGET",
      "buffy:STAR_POWER",
      "buffy:HYPER_CHARGE",
    ]);
  });

  it("handles null buffies without throwing", () => {
    const groups = buildBrawlerItemGroups(
      { ...items, buffies: null },
      emptyOwnership,
    );

    expect(groups[2].sections[1].items).toEqual([]);
  });

  it("handles partial runtime buffies without throwing", () => {
    const partialItems = {
      ...items,
      buffies: {
        starPower: { type: "STAR_POWER", imageUrl: "/buffy-star-power.webp" },
      },
    } as unknown as BrawlerItems;

    const groups = buildBrawlerItemGroups(partialItems, emptyOwnership);

    expect(groups[2].sections[1].items).toEqual([
      expect.objectContaining({
        key: "buffy:STAR_POWER",
        kind: "buffy",
        name: "스타파워",
      }),
    ]);
  });
});
