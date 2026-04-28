import type {
  BrawlerRankingResponse,
  ClubRankingResponse,
  PlayerRankingResponse,
  RankingPlayerResponse,
} from "@/api/types";
import { parseCountry } from "@/domain/ranking/country";
import type {
  BrawlerRanking,
  ClubRanking,
  PlayerRanking,
  RankingPlayer,
} from "@/domain/ranking/types";

export function toRankingPlayer(dto: RankingPlayerResponse): RankingPlayer {
  return {
    tag: dto.tag,
    name: dto.name,
    nameColor: dto.nameColor,
    iconId: dto.iconId,
    clubName: dto.clubName,
  };
}

export function toPlayerRanking(dto: PlayerRankingResponse): PlayerRanking {
  return {
    country: parseCountry(dto.country),
    rank: dto.rank,
    player: toRankingPlayer(dto.player),
    trophies: dto.trophies,
    rankChange: dto.rankChange,
  };
}

export function toBrawlerRanking(dto: BrawlerRankingResponse): BrawlerRanking {
  return {
    country: parseCountry(dto.country),
    brawlerId: dto.brawlerId,
    rank: dto.rank,
    player: toRankingPlayer(dto.player),
    trophies: dto.trophies,
    rankChange: dto.rankChange,
  };
}

export function toClubRanking(dto: ClubRankingResponse): ClubRanking {
  return {
    country: parseCountry(dto.country),
    tag: dto.tag,
    name: dto.name,
    badgeId: dto.badgeId,
    trophies: dto.trophies,
    rank: dto.rank,
    memberCount: dto.memberCount,
    rankChange: dto.rankChange,
  };
}
