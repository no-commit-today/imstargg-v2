// 모든 타입은 백엔드 com.imstargg.core.api.controller.v1.response.* DTO와 동일.
// 백엔드 변경 시 이 파일과 매퍼만 수정한다.

export interface ListResponse<T> {
  content: T[];
}

// === Player ===
export interface PlayerResponse {
  tag: string;
  name: string;
  nameColor: string | null;
  iconId: number;
  trophies: number;
  highestTrophies: number;
  expLevel: number;
  expPoints: number;
  qualifiedFromChampionshipChallenge: boolean;
  victories3vs3: number;
  victoriesSolo: number;
  victoriesDuo: number;
  bestRoboRumbleTime: number;
  bestTimeAsBigBrawler: number;
  totalPrestigeLevel: number;
  club: PlayerClubResponse | null;
}

export interface PlayerClubResponse {
  tag: string;
  name: string;
}

// === Brawler ===
export type BrawlerRarity =
  | "STARTING_BRAWLER" | "RARE" | "SUPER_RARE" | "EPIC"
  | "MYTHIC" | "LEGENDARY" | "ULTRA_LEGENDARY";

export type GearRarity = "SUPER_RARE" | "EPIC" | "MYTHIC";

export type BrawlerRole =
  | "DAMAGE_DEALER" | "TANK" | "MARKSMAN" | "ASSASSIN"
  | "SUPPORT" | "CONTROLLER" | "ARTILLERY";

export type BuffyType = "MYTHIC_GEAR" | string;

export interface BuffyResponse {
  type: BuffyType;
  imageUrl: string | null;
}

export interface BuffiesResponse {
  gadget: BuffyResponse;
  starPower: BuffyResponse;
  hyperCharge: BuffyResponse;
}

export type Language = "ko" | "en" | string;
export type MessagesResponse = Record<Language, string>;

export interface GadgetResponse { id: number; name: MessagesResponse; imageUrl: string | null; }
export interface GearResponse { id: number; name: MessagesResponse; imageUrl: string | null; }
export interface StarPowerResponse { id: number; name: MessagesResponse; imageUrl: string | null; }
export interface HyperChargeResponse { id: number; name: MessagesResponse; imageUrl: string | null; }

export interface BrawlerResponse {
  id: number;
  name: MessagesResponse;
  rarity: BrawlerRarity;
  role: BrawlerRole;
  portraitImageUrl: string | null;
}

export interface BrawlerItemsResponse {
  gadgets: GadgetResponse[];
  gears: GearResponse[];
  starPowers: StarPowerResponse[];
  hyperCharge: HyperChargeResponse | null;
  buffies: BuffiesResponse | null;
}

export interface BrawlerWithItemsResponse {
  brawler: BrawlerResponse;
  items: BrawlerItemsResponse;
}

export interface PlayerBrawlerItemResponse<T> {
  item: T;
  owned: boolean;
}

export interface PlayerBrawlerBuffiesResponse {
  gadget: PlayerBrawlerItemResponse<BuffyResponse>;
  starPower: PlayerBrawlerItemResponse<BuffyResponse>;
  hyperCharge: PlayerBrawlerItemResponse<BuffyResponse>;
}

export interface PlayerBrawlerResponse {
  brawler: BrawlerResponse | null;
  power: number;
  rank: number;
  trophies: number;
  highestTrophies: number;
  prestigeLevel: number;
  gadgets: PlayerBrawlerItemResponse<GadgetResponse>[];
  gears: PlayerBrawlerItemResponse<GearResponse>[];
  starPowers: PlayerBrawlerItemResponse<StarPowerResponse>[];
  hyperCharge: PlayerBrawlerItemResponse<HyperChargeResponse> | null;
  buffies: PlayerBrawlerBuffiesResponse | null;
}

// === Renewal ===
export type RenewalStatus =
  | "REQUESTED" | "PENDING" | "EXECUTING"
  | "COMPLETED" | "NOT_FOUND" | "FAILED";

export interface RenewalResponse {
  tag: string;
  status: RenewalStatus;
  progressing: boolean;
  canRenew: boolean;
  requestedAt: string;
  executedAt: string | null;
  completedAt: string | null;
  renewedAt: string | null;
}

// === Club ===
export type ClubType = "OPEN" | "INVITE_ONLY" | "CLOSED" | "UNKNOWN";

export type ClubMemberRole =
  | "PRESIDENT"
  | "VICE_PRESIDENT"
  | "SENIOR"
  | "MEMBER"
  | "UNKNOWN";

export interface ClubMemberResponse {
  tag: string;
  name: string;
  nameColor: string | null;
  role: ClubMemberRole | null;
  trophies: number;
  iconId: number;
}

export interface ClubResponse {
  tag: string;
  name: string;
  description: string;
  type: ClubType | null;
  badgeId: number;
  requiredTrophies: number;
  trophies: number;
  members: ClubMemberResponse[];
}

// === BattleEvent ===
export type BattleEventResultType = "RESULT" | "RANK";

export interface BattleEventModeResponse {
  id: number;
  name: MessagesResponse;
  iconImageUrl: string | null;
  resultType: BattleEventResultType;
}

export interface BattleEventResponse {
  id: number;
  mode: BattleEventModeResponse | null;
  mapName: MessagesResponse;
}

export interface RotationBattleEventResponse {
  event: BattleEventResponse;
  startTime: string;  // OffsetDateTime serialized as ISO 8601
  endTime: string;
}

// === Ranking ===
export interface RankingPlayerResponse {
  tag: string;
  name: string;
  nameColor: string | null;
  iconId: number;
  clubName: string;
}

export interface PlayerRankingResponse {
  country: string;
  rank: number;
  player: RankingPlayerResponse;
  trophies: number;
  rankChange: number | null;
}

export interface BrawlerRankingResponse {
  country: string;
  brawlerId: number;
  rank: number;
  player: RankingPlayerResponse;
  trophies: number;
  rankChange: number | null;
}

export interface ClubRankingResponse {
  country: string;
  tag: string;
  name: string;
  badgeId: number;
  trophies: number;
  rank: number;
  memberCount: number;
  rankChange: number | null;
}

// === Statistics ===
export type StatsTierRangeDto =
  | "TROPHY_1000_PLUS" | "TROPHY_2000_PLUS" | "TROPHY_3000_PLUS"
  | "SOLO_RANK_MYTHIC_PLUS" | "SOLO_RANK_LEGENDARY_PLUS" | "SOLO_RANK_MASTER_PLUS";

export type StatsDateRangeDto =
  | "THIS_WEEK" | "SINCE_LAST_WEEK" | "LAST_WEEK"
  | "SINCE_TWO_WEEKS_AGO" | "TWO_WEEKS_AGO"
  | "THIS_MONTH" | "SINCE_LAST_MONTH" | "LAST_MONTH";

export type BrawlerTrophyRangeDto =
  | "TROPHY_500_PLUS" | "TROPHY_1000_PLUS" | "TROPHY_2000_PLUS" | "TROPHY_3000_PLUS";

export type ResultStatsSortByDto = "PICK_RATE" | "WIN_RATE";
export type RankStatsSortByDto = "PICK_RATE" | "AVERAGE_RANK";
export type SortOrderDto = "ASC" | "DESC";

export interface BrawlerResultStatisticsItemResponse {
  brawler: BrawlerResponse | null;
  totalBattleCount: number;
  pickRate: number;
  winRate: number;
  starPlayerRate: number;
  tierScore: number;
  tier: string; // "S" | "A" | "B" | "C" | "D"
}

export interface BrawlerResultStatisticsResponse {
  totalBattleCount: number;
  items: BrawlerResultStatisticsItemResponse[];
}

export interface BrawlerRankStatisticsResponse {
  brawler: BrawlerResponse | null;
  totalBattleCount: number;
  averageRank: number;
}

export interface BrawlerPairResultStatisticsItemResponse {
  brawler: BrawlerResponse | null;
  pairBrawler: BrawlerResponse | null;
  totalBattleCount: number;
  pickRate: number;
  winRate: number;
}

export interface BrawlerPairRankStatisticsResponse {
  brawler: BrawlerResponse | null;
  pairBrawler: BrawlerResponse | null;
  totalBattleCount: number;
  averageRank: number;
}

export interface BrawlerEnemyResultStatisticsItemResponse {
  brawler: BrawlerResponse | null;
  enemyBrawler: BrawlerResponse | null;
  totalBattleCount: number;
  pickRate: number;
  winRate: number;
}

export interface BrawlerResultDailyStatisticsResponse {
  date: string; // ISO yyyy-MM-dd
  brawler: BrawlerResponse | null;
  totalBattleCount: number;
  winRate: number;
  starPlayerRate: number;
}

export interface BrawlerItemOwnershipStatisticsItemResponse {
  itemId: number;
  ownershipRate: number;
  ownerCount: number;
  totalCount: number;
}

export interface BrawlerBuffyOwnershipStatisticsItemResponse {
  buffyType: string;
  ownershipRate: number;
  ownerCount: number;
  totalCount: number;
}

export interface BrawlerItemOwnershipStatisticsResponse {
  gears: BrawlerItemOwnershipStatisticsItemResponse[];
  gadgets: BrawlerItemOwnershipStatisticsItemResponse[];
  starPowers: BrawlerItemOwnershipStatisticsItemResponse[];
  hyperCharges: BrawlerItemOwnershipStatisticsItemResponse[];
  buffies: BrawlerBuffyOwnershipStatisticsItemResponse[];
}

// === Slice (offset/limit pagination) ===
export interface SliceResponse<T> {
  content: T[];
  hasNext: boolean;
}

// === Battle ===
export type BattleMode =
  | "KNOCKOUT" | "GEM_GRAB" | "HEIST" | "HOT_ZONE" | "SIEGE" | "BOUNTY"
  | "BRAWL_BALL" | "DUELS" | "SOLO_SHOWDOWN" | "DUO_SHOWDOWN" | "WIPEOUT"
  | "VOLLEY_BRAWL" | "TROPHY_THIEVES" | "BOSS_FIGHT" | "BIG_GAME" | "BASKET_BRAWL"
  | "ROBO_RUMBLE" | "PAYLOAD" | "TAKEDOWN" | "HUNTERS" | "BOT_DROP"
  | "HOLD_THE_TROPHY" | "UNKNOWN";

export type BattleType =
  | "RANKED" | "SOLO_RANKED" | "FRIENDLY" | "CHALLENGE"
  | "TOURNAMENT" | "CHAMPIONSHIP_CHALLENGE";

export type BattleResult = "VICTORY" | "DEFEAT" | "DRAW";

export type SoloRankTier =
  | "BRONZE_1" | "BRONZE_2" | "BRONZE_3"
  | "SILVER_1" | "SILVER_2" | "SILVER_3"
  | "GOLD_1" | "GOLD_2" | "GOLD_3"
  | "DIAMOND_1" | "DIAMOND_2" | "DIAMOND_3"
  | "MYTHIC_1" | "MYTHIC_2" | "MYTHIC_3"
  | "LEGENDARY_1" | "LEGENDARY_2" | "LEGENDARY_3"
  | "MASTER_1" | "MASTER_2" | "MASTER_3"
  | "PRO";

export interface BattlePlayerBrawlerResponse {
  brawler: BrawlerResponse | null;
  power: number;
  trophies: number | null;
  trophyChange: number | null;
}

export interface BattlePlayerResponse {
  tag: string;
  name: string;
  soloRankTier: SoloRankTier | null;
  brawler: BattlePlayerBrawlerResponse;
}

export interface BattleResponse {
  battleTime: string;
  event: BattleEventResponse | null;
  mode: BattleMode;
  type: BattleType | null;
  result: BattleResult | null;
  duration: number | null;
  starPlayerTag: string | null;
  rank: number | null;
  trophyChange: number | null;
  teams: BattlePlayerResponse[][];
}
