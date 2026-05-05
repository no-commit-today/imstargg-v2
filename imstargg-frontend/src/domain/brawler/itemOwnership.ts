import type { BuffyResponse, GearRarity } from "@/api/types";
import type { BrawlerItem, BrawlerItems } from "@/domain/brawler/types";
import { buffyTypeLabel } from "@/domain/brawler/buffyTypeLabel";
import type {
  BuffyOwnershipItem,
  ItemOwnership,
  ItemOwnershipItem,
} from "@/domain/statistics/types";

export type BrawlerDisplayItemKind =
  | "gadget"
  | "starPower"
  | "gear"
  | "hyperCharge"
  | "buffy";

export type BrawlerDisplayItem = {
  key: string;
  kind: BrawlerDisplayItemKind;
  name: string;
  imageUrl: string | null;
  gearRarity?: GearRarity;
  ownership?: {
    rate: number;
    ownerCount: number;
    totalCount: number;
  };
};

export type BrawlerItemSection = {
  key: string;
  title: string;
  items: BrawlerDisplayItem[];
};

export type BrawlerItemGroup = {
  key: "gadgets-starPowers" | "gears" | "hyperCharge-buffies";
  sections: BrawlerItemSection[];
};

function toOwnership(ownership: ItemOwnershipItem | BuffyOwnershipItem | undefined) {
  if (!ownership) return undefined;
  return {
    rate: ownership.ownershipRate,
    ownerCount: ownership.ownerCount,
    totalCount: ownership.totalCount,
  };
}

function findByItemId(items: ItemOwnershipItem[], itemId: number) {
  return items.find((item) => item.itemId === itemId);
}

function findByBuffyType(items: BuffyOwnershipItem[], buffyType: string) {
  return items.find((item) => item.buffyType === buffyType);
}

function toDisplayItem(
  kind: Exclude<BrawlerDisplayItemKind, "buffy">,
  item: BrawlerItem,
  ownership: ItemOwnershipItem | undefined,
): BrawlerDisplayItem {
  return {
    key: `${kind}:${item.id}`,
    kind,
    name: item.name,
    imageUrl: item.imageUrl,
    gearRarity: item.rarity ?? undefined,
    ownership: toOwnership(ownership),
  };
}

function toBuffyDisplayItem(
  buffy: BuffyResponse,
  ownership: BuffyOwnershipItem | undefined,
): BrawlerDisplayItem {
  return {
    key: `buffy:${buffy.type}`,
    kind: "buffy",
    name: buffyTypeLabel(buffy.type),
    imageUrl: buffy.imageUrl,
    ownership: toOwnership(ownership),
  };
}

function toBuffyItems(items: BrawlerItems, ownership: ItemOwnership) {
  if (!items.buffies) return [];

  return [
    items.buffies.gadget,
    items.buffies.starPower,
    items.buffies.hyperCharge,
  ].map((buffy) =>
    toBuffyDisplayItem(buffy, findByBuffyType(ownership.buffies, buffy.type)),
  );
}

export function buildBrawlerItemGroups(
  items: BrawlerItems,
  ownership: ItemOwnership,
): BrawlerItemGroup[] {
  return [
    {
      key: "gadgets-starPowers",
      sections: [
        {
          key: "gadgets",
          title: "가젯",
          items: items.gadgets.map((item) =>
            toDisplayItem("gadget", item, findByItemId(ownership.gadgets, item.id)),
          ),
        },
        {
          key: "starPowers",
          title: "스타파워",
          items: items.starPowers.map((item) =>
            toDisplayItem(
              "starPower",
              item,
              findByItemId(ownership.starPowers, item.id),
            ),
          ),
        },
      ],
    },
    {
      key: "gears",
      sections: [
        {
          key: "gears",
          title: "기어",
          items: items.gears.map((item) =>
            toDisplayItem("gear", item, findByItemId(ownership.gears, item.id)),
          ),
        },
      ],
    },
    {
      key: "hyperCharge-buffies",
      sections: [
        {
          key: "hyperCharge",
          title: "하이퍼차지",
          items: items.hyperCharge
            ? [
                toDisplayItem(
                  "hyperCharge",
                  items.hyperCharge,
                  findByItemId(ownership.hyperCharges, items.hyperCharge.id),
                ),
              ]
            : [],
        },
        {
          key: "buffies",
          title: "버피",
          items: toBuffyItems(items, ownership),
        },
      ],
    },
  ];
}
