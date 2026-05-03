import type {
  PlayerResponse,
  PlayerBrawlerResponse,
  PlayerBrawlerItemResponse,
  GadgetResponse,
  GearResponse,
  StarPowerResponse,
  HyperChargeResponse,
} from "@/api/types";
import type {
  Player,
  PlayerBrawler,
  PlayerBrawlerOwnedItem,
} from "@/domain/player/types";
import { toBrawler, toBrawlerItem } from "@/domain/brawler/mappers";

type AnyItemDto =
  | GadgetResponse
  | GearResponse
  | StarPowerResponse
  | HyperChargeResponse;

function toOwnedItem(
  dto: PlayerBrawlerItemResponse<AnyItemDto>,
): PlayerBrawlerOwnedItem {
  return { item: toBrawlerItem(dto.item), owned: dto.owned };
}

export function toPlayer(dto: PlayerResponse): Player {
  return {
    tag: dto.tag,
    name: dto.name,
    nameColor: dto.nameColor,
    iconId: dto.iconId,
    trophies: dto.trophies,
    highestTrophies: dto.highestTrophies,
    expLevel: dto.expLevel,
    expPoints: dto.expPoints,
    victories3vs3: dto.victories3vs3,
    victoriesSolo: dto.victoriesSolo,
    victoriesDuo: dto.victoriesDuo,
    totalPrestigeLevel: dto.totalPrestigeLevel,
    club: dto.club ? { tag: dto.club.tag, name: dto.club.name } : null,
  };
}

export function toPlayerBrawler(dto: PlayerBrawlerResponse): PlayerBrawler {
  return {
    brawler: dto.brawler ? toBrawler(dto.brawler) : null,
    power: dto.power,
    rank: dto.rank,
    trophies: dto.trophies,
    highestTrophies: dto.highestTrophies,
    prestigeLevel: dto.prestigeLevel,
    gadgets: dto.gadgets.map(toOwnedItem),
    gears: dto.gears.map(toOwnedItem),
    starPowers: dto.starPowers.map(toOwnedItem),
    hyperCharge: dto.hyperCharge ? toOwnedItem(dto.hyperCharge) : null,
    buffies: dto.buffies
      ? {
          gadget: { item: dto.buffies.gadget.item, owned: dto.buffies.gadget.owned },
          starPower: { item: dto.buffies.starPower.item, owned: dto.buffies.starPower.owned },
          hyperCharge: { item: dto.buffies.hyperCharge.item, owned: dto.buffies.hyperCharge.owned },
        }
      : null,
  };
}
