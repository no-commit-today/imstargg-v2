import type { Country } from "@/domain/ranking/country";

export interface RankingPlayer {
  tag: string;
  name: string;
  nameColor: string | null;
  iconId: number;
  clubName: string;
}

export interface PlayerRanking {
  country: Country;
  rank: number;
  player: RankingPlayer;
  trophies: number;
  rankChange: number | null;
}

export interface BrawlerRanking {
  country: Country;
  brawlerId: number;
  rank: number;
  player: RankingPlayer;
  trophies: number;
  rankChange: number | null;
}

export interface ClubRanking {
  country: Country;
  tag: string;
  name: string;
  badgeId: number;
  trophies: number;
  rank: number;
  memberCount: number;
  rankChange: number | null;
}
