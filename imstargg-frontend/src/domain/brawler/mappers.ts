import type {
  BrawlerResponse,
  BrawlerItemsResponse,
  BrawlerDetailResponse,
  GadgetResponse,
  GearResponse,
  StarPowerResponse,
  HyperChargeResponse,
} from "@/api/types";
import type {
  Brawler,
  BrawlerItem,
  BrawlerItems,
  BrawlerDetail,
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

export function toBrawlerDetail(dto: BrawlerDetailResponse): BrawlerDetail {
  return {
    brawler: toBrawler(dto.brawler),
    items: toBrawlerItems(dto.items),
  };
}
