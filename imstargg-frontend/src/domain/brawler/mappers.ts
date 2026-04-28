import type {
  BrawlerResponse,
  GadgetResponse,
  GearResponse,
  StarPowerResponse,
  HyperChargeResponse,
} from "@/api/types";
import type { Brawler, BrawlerItem } from "@/domain/brawler/types";
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
    gadgets: dto.gadgets.map(toBrawlerItem),
    gears: dto.gears.map(toBrawlerItem),
    starPowers: dto.starPowers.map(toBrawlerItem),
    hyperCharges: dto.hyperCharges.map(toBrawlerItem),
    buffies: dto.buffies,
  };
}
