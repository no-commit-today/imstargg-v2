import type {
  PlayerResponse,
  PlayerBrawlerResponse,
  MessagesResponse,
} from "@/api/types";
import type { Player, PlayerBrawler } from "@/domain/player/types";

function pickName(messages: MessagesResponse | undefined): string {
  if (!messages) return "(unknown)";
  return messages.ko ?? messages.en ?? Object.values(messages)[0] ?? "(unknown)";
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
    buffies: dto.buffies,
    raw: b ?? null,
  };
}
