import type {
  PlayerResponse,
  PlayerBrawlerResponse,
} from "@/api/types";
import type { Player, PlayerBrawler } from "@/domain/player/types";
import { pickName } from "@/domain/i18n";

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
  const b = dto.brawler;
  return {
    id: b?.id ?? null,
    name: pickName(b?.name),
    rarity: b?.rarity ?? null,
    role: b?.role ?? null,
    power: dto.power,
    rank: dto.rank,
    trophies: dto.trophies,
    highestTrophies: dto.highestTrophies,
    prestigeLevel: dto.prestigeLevel,
    portraitImageUrl: b?.portraitImageUrl ?? null,
    gadgetBuffy: dto.gadgetBuffy,
    starPowerBuffy: dto.starPowerBuffy,
    hyperChargeBuffy: dto.hyperChargeBuffy,
    raw: b ?? null,
  };
}
