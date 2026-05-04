import type {
  BrawlerResponse,
  BrawlerItemsResponse,
  BrawlerWithItemsResponse,
  GadgetResponse,
  GearResponse,
  StarPowerResponse,
  HyperChargeResponse,
} from "@/api/types";
import type {
  Brawler,
  BrawlerItem,
  BrawlerItems,
  BrawlerWithItems,
} from "@/domain/brawler/types";
import { pickName } from "@/domain/i18n";

type ItemDto =
  | GadgetResponse
  | GearResponse
  | StarPowerResponse
  | HyperChargeResponse;

export function toBrawlerItem(dto: ItemDto): BrawlerItem {
  return {
    id: dto.id,
    name: pickName(dto.name),
    imageUrl: dto.imageUrl,
    rarity: "rarity" in dto ? dto.rarity : null,
  };
}

export function toBrawler(dto: BrawlerResponse): Brawler {
  return {
    id: dto.id,
    name: pickName(dto.name),
    rarity: dto.rarity,
    role: dto.role,
    portraitImageUrl: dto.portraitImageUrl,
  };
}

export function toBrawlerItems(dto: BrawlerItemsResponse): BrawlerItems {
  return {
    gadgets: dto.gadgets.map(toBrawlerItem),
    gears: dto.gears.map(toBrawlerItem),
    starPowers: dto.starPowers.map(toBrawlerItem),
    hyperCharge: dto.hyperCharge ? toBrawlerItem(dto.hyperCharge) : null,
    buffies: dto.buffies,
  };
}

export function toBrawlerWithItems(dto: BrawlerWithItemsResponse): BrawlerWithItems {
  return {
    brawler: toBrawler(dto.brawler),
    items: toBrawlerItems(dto.items),
  };
}
