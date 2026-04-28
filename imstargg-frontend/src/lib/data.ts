// ImStarGG mock data layer — typed TypeScript port of /tmp/design_pkg/data.js.
// All data is precomputed at module load. No browser APIs touched — safe for RSC.

export type ClassKey =
  | "DAMAGE"
  | "TANK"
  | "MARKSMAN"
  | "ASSASSIN"
  | "SUPPORT"
  | "CONTROLLER"
  | "ARTILLERY";

export type RarityKey =
  | "COMMON"
  | "RARE"
  | "SUPER_RARE"
  | "EPIC"
  | "MYTHIC"
  | "LEGENDARY";

export interface ClassDef {
  name: string;
  color: string;
  icon: string;
}
export interface RarityDef {
  name: string;
  color: string;
}
export interface Brawler {
  id: string;
  name: string;
  cls: ClassKey;
  rarity: RarityKey;
  c1: string;
  c2: string;
}

export const CLS: Record<ClassKey, ClassDef> = {
  DAMAGE: { name: "데미지 딜러", color: "var(--class-damage)", icon: "class_damage.svg" },
  TANK: { name: "탱커", color: "var(--class-tank)", icon: "class_tank.svg" },
  MARKSMAN: { name: "저격수", color: "var(--class-marksman)", icon: "class_marksmen.svg" },
  ASSASSIN: { name: "암살자", color: "var(--class-assassin)", icon: "class_assassin.svg" },
  SUPPORT: { name: "서포터", color: "var(--class-support)", icon: "class_support.svg" },
  CONTROLLER: { name: "컨트롤러", color: "var(--class-controller)", icon: "class_controller.svg" },
  ARTILLERY: { name: "아틸러리", color: "var(--class-artillery)", icon: "class_artillery.svg" },
};

export const RARITY: Record<RarityKey, RarityDef> = {
  COMMON: { name: "일반", color: "var(--rarity-common)" },
  RARE: { name: "레어", color: "var(--rarity-rare)" },
  SUPER_RARE: { name: "슈퍼 레어", color: "var(--rarity-super-rare)" },
  EPIC: { name: "에픽", color: "var(--rarity-epic)" },
  MYTHIC: { name: "미식", color: "var(--rarity-mythic)" },
  LEGENDARY: { name: "레전드", color: "var(--rarity-legendary)" },
};

export const BRAWLERS: Brawler[] = [
  { id: "shelly", name: "쉘리", cls: "DAMAGE", rarity: "COMMON", c1: "#FFB347", c2: "#FF6B6B" },
  { id: "colt", name: "콜트", cls: "MARKSMAN", rarity: "COMMON", c1: "#4A90E2", c2: "#1E5A9F" },
  { id: "bull", name: "불", cls: "TANK", rarity: "COMMON", c1: "#8B6F47", c2: "#5C3D1E" },
  { id: "piper", name: "파이퍼", cls: "MARKSMAN", rarity: "EPIC", c1: "#FF8FA3", c2: "#C2185B" },
  { id: "leon", name: "레온", cls: "ASSASSIN", rarity: "LEGENDARY", c1: "#9DEAB1", c2: "#2BD576" },
  { id: "spike", name: "스파이크", cls: "CONTROLLER", rarity: "LEGENDARY", c1: "#76FFE5", c2: "#00B894" },
  { id: "crow", name: "크로우", cls: "ASSASSIN", rarity: "LEGENDARY", c1: "#7B68EE", c2: "#1A1A2E" },
  { id: "sandy", name: "샌디", cls: "CONTROLLER", rarity: "LEGENDARY", c1: "#FFE082", c2: "#8E7600" },
  { id: "gene", name: "진", cls: "CONTROLLER", rarity: "MYTHIC", c1: "#9146FF", c2: "#4A148C" },
  { id: "tara", name: "타라", cls: "CONTROLLER", rarity: "MYTHIC", c1: "#7B1FA2", c2: "#1A1A2E" },
  { id: "mortis", name: "모티스", cls: "ASSASSIN", rarity: "MYTHIC", c1: "#5C6BC0", c2: "#1A237E" },
  { id: "edgar", name: "에드거", cls: "ASSASSIN", rarity: "EPIC", c1: "#42A5F5", c2: "#0D47A1" },
  { id: "elprimo", name: "엘프리모", cls: "TANK", rarity: "MYTHIC", c1: "#FFCC80", c2: "#E65100" },
  { id: "rosa", name: "로사", cls: "TANK", rarity: "SUPER_RARE", c1: "#A5D6A7", c2: "#2E7D32" },
  { id: "jessie", name: "제시", cls: "CONTROLLER", rarity: "COMMON", c1: "#FFB74D", c2: "#E65100" },
  { id: "penny", name: "페니", cls: "ARTILLERY", rarity: "EPIC", c1: "#F48FB1", c2: "#AD1457" },
  { id: "pam", name: "팸", cls: "SUPPORT", rarity: "EPIC", c1: "#CE93D8", c2: "#6A1B9A" },
  { id: "poco", name: "포코", cls: "SUPPORT", rarity: "SUPER_RARE", c1: "#FFAB91", c2: "#BF360C" },
  { id: "gus", name: "거스", cls: "SUPPORT", rarity: "EPIC", c1: "#80DEEA", c2: "#006064" },
  { id: "byron", name: "바이런", cls: "SUPPORT", rarity: "MYTHIC", c1: "#B39DDB", c2: "#4527A0" },
  { id: "bo", name: "보", cls: "CONTROLLER", rarity: "EPIC", c1: "#A1887F", c2: "#3E2723" },
  { id: "mrp", name: "미스터 P", cls: "ARTILLERY", rarity: "MYTHIC", c1: "#90CAF9", c2: "#0D47A1" },
  { id: "sprout", name: "스프라우트", cls: "ARTILLERY", rarity: "MYTHIC", c1: "#AED581", c2: "#33691E" },
  { id: "meg", name: "메그", cls: "ASSASSIN", rarity: "LEGENDARY", c1: "#FFAB40", c2: "#FF6F00" },
  { id: "ash", name: "애쉬", cls: "TANK", rarity: "EPIC", c1: "#90A4AE", c2: "#263238" },
  { id: "lola", name: "롤라", cls: "CONTROLLER", rarity: "EPIC", c1: "#F8BBD0", c2: "#880E4F" },
  { id: "fang", name: "팽", cls: "ASSASSIN", rarity: "LEGENDARY", c1: "#FFE0B2", c2: "#BF360C" },
  { id: "eve", name: "이브", cls: "ARTILLERY", rarity: "LEGENDARY", c1: "#80CBC4", c2: "#004D40" },
  { id: "janet", name: "자넷", cls: "MARKSMAN", rarity: "LEGENDARY", c1: "#FFCDD2", c2: "#B71C1C" },
  { id: "otis", name: "오티스", cls: "SUPPORT", rarity: "MYTHIC", c1: "#B2DFDB", c2: "#004D40" },
  { id: "sam", name: "샘", cls: "TANK", rarity: "EPIC", c1: "#E6EE9C", c2: "#827717" },
  { id: "griff", name: "그리프", cls: "DAMAGE", rarity: "EPIC", c1: "#A5D6A7", c2: "#1B5E20" },
  { id: "maisie", name: "메이지", cls: "MARKSMAN", rarity: "EPIC", c1: "#F8BBD0", c2: "#AD1457" },
  { id: "chuck", name: "척", cls: "CONTROLLER", rarity: "LEGENDARY", c1: "#FFE082", c2: "#FF6F00" },
  { id: "charlie", name: "찰리", cls: "CONTROLLER", rarity: "LEGENDARY", c1: "#CE93D8", c2: "#4A148C" },
  { id: "mico", name: "미코", cls: "ASSASSIN", rarity: "EPIC", c1: "#BCAAA4", c2: "#3E2723" },
  { id: "kit", name: "킷", cls: "ASSASSIN", rarity: "EPIC", c1: "#80DEEA", c2: "#00838F" },
  { id: "amber", name: "앰버", cls: "DAMAGE", rarity: "LEGENDARY", c1: "#FFE082", c2: "#FF6F00" },
  { id: "lily", name: "릴리", cls: "ASSASSIN", rarity: "LEGENDARY", c1: "#E1BEE7", c2: "#4A148C" },
  { id: "doug", name: "더그", cls: "SUPPORT", rarity: "EPIC", c1: "#FFAB91", c2: "#BF360C" },
  { id: "kenji", name: "켄지", cls: "ASSASSIN", rarity: "LEGENDARY", c1: "#EF9A9A", c2: "#B71C1C" },
  { id: "meeple", name: "미플", cls: "CONTROLLER", rarity: "LEGENDARY", c1: "#9FA8DA", c2: "#1A237E" },
  { id: "juju", name: "주주", cls: "CONTROLLER", rarity: "LEGENDARY", c1: "#B39DDB", c2: "#4A148C" },
];

export function brawler(id: string): Brawler | undefined {
  return BRAWLERS.find((b) => b.id === id);
}

export interface TrophyTier {
  name: string;
  color: string;
  img: string;
}
export function trophyTier(t: number): TrophyTier {
  if (t >= 1500) return { name: "Masters", color: "#FF3A3A", img: "rank_masters.webp" };
  if (t >= 1200) return { name: "Legendary", color: "#B83BFF", img: "rank_legendary.webp" };
  if (t >= 900) return { name: "Diamond", color: "#58D7E8", img: "rank_diamond.webp" };
  if (t >= 600) return { name: "Gold", color: "#F2B93B", img: "rank_gold.webp" };
  if (t >= 300) return { name: "Silver", color: "#9FAFC5", img: "rank_silver.webp" };
  return { name: "Bronze", color: "#C77545", img: "rank_bronze.webp" };
}

export interface PlayerProfile {
  tag: string;
  name: string;
  nameColor: string;
  profileIcon: number;
  trophies: number;
  highestTrophies: number;
  expLevel: number;
  expPoints: number;
  soloVictories: number;
  duoVictories: number;
  trioVictories: number;
  bestTimeAsBigBrawler: number;
  bestRoboRumbleTime: number;
  rankedTier: string;
  rankedTrophies: number;
  club: { tag: string; name: string };
  lastRenewedAt: string;
  canRenew: boolean;
}

export const PLAYER: PlayerProfile = {
  tag: "#9R20YR2YQ",
  name: "StarHunter",
  nameColor: "#FFC60A",
  profileIcon: 28000010,
  trophies: 48820,
  highestTrophies: 51240,
  expLevel: 247,
  expPoints: 78421,
  soloVictories: 1842,
  duoVictories: 982,
  trioVictories: 4129,
  bestTimeAsBigBrawler: 124,
  bestRoboRumbleTime: 412,
  rankedTier: "Masters I",
  rankedTrophies: 1820,
  club: { tag: "#TEAMNINE", name: "팀나인" },
  lastRenewedAt: "2분 전",
  canRenew: true,
};

const OWNED_IDS = [
  "shelly", "colt", "bull", "piper", "leon", "spike", "crow", "gene", "mortis",
  "edgar", "elprimo", "rosa", "jessie", "penny", "poco", "byron", "bo", "mrp",
  "meg", "fang", "janet", "sam", "griff", "maisie", "chuck", "mico", "kit",
  "amber", "sandy", "tara", "pam",
];

function rand(seed: number): number {
  const x = Math.sin(seed) * 10000;
  return x - Math.floor(x);
}

export interface OwnedBrawler extends Brawler {
  power: number;
  rank: number;
  trophies: number;
  highestTrophies: number;
  gears: number;
  starPowers: number;
  gadgets: number;
  hasHypercharge: boolean;
}

export const OWNED_BRAWLERS: OwnedBrawler[] = OWNED_IDS.map((id, i) => {
  const b = brawler(id)!;
  const seed = i * 7 + 13;
  const trophies = Math.floor(200 + rand(seed) * 1300);
  const power = Math.min(11, Math.floor(7 + rand(seed * 2) * 5));
  const rank = Math.min(35, Math.floor(15 + rand(seed * 3) * 21));
  return {
    ...b,
    power,
    rank,
    trophies,
    highestTrophies: Math.floor(trophies * (1.05 + rand(seed * 4) * 0.15)),
    gears: Math.floor(rand(seed * 5) * 3) + 1,
    starPowers: Math.min(2, Math.floor(rand(seed * 6) * 2.5)),
    gadgets: Math.min(2, Math.floor(rand(seed * 7) * 2.5)),
    hasHypercharge: rand(seed * 8) > 0.6,
  };
}).sort((a, b) => b.trophies - a.trophies);

export interface EventInfo {
  mode: string;
  modeKo: string;
  map: string;
  color: string;
}
const EVENTS: EventInfo[] = [
  { mode: "gemGrab", modeKo: "젬 그랩", map: "럭셔리 호텔", color: "#9C27B0" },
  { mode: "brawlBall", modeKo: "브롤 볼", map: "백 포켓", color: "#FFC107" },
  { mode: "heist", modeKo: "하이스트", map: "코스믹 캐번", color: "#FF5722" },
  { mode: "bounty", modeKo: "바운티", map: "스네이크 프레리", color: "#00BCD4" },
  { mode: "soloShowdown", modeKo: "솔로 쇼다운", map: "캐버른 처치", color: "#673AB7" },
  { mode: "knockout", modeKo: "녹아웃", map: "뉴 호라이즌", color: "#FF9800" },
  { mode: "hotZone", modeKo: "핫 존", map: "오픈 비즈니스", color: "#E91E63" },
  { mode: "soloRanked", modeKo: "솔로 랭크", map: "럭셔리 호텔", color: "#FFC60A" },
];

export interface BattlePlayer {
  tag: string;
  name: string;
  brawler: string;
  isMe: boolean;
  trophies: number;
}
export interface Battle {
  id: number;
  battleTime: number;
  event: EventInfo;
  result: "win" | "loss" | "draw" | null;
  rank: number | null;
  trophyChange: number;
  duration: number;
  teams: BattlePlayer[][];
  myBrawler: string;
  myTrophies: number;
  starPlayer: string;
}

function genBattles(): Battle[] {
  const battles: Battle[] = [];
  const meId = "colt";
  for (let i = 0; i < 50; i++) {
    const ev = EVENTS[Math.floor(rand(i * 11 + 3) * EVENTS.length)];
    const isShowdown = ev.mode === "soloShowdown";
    const result: Battle["result"] = isShowdown
      ? null
      : rand(i * 13 + 5) > 0.45
        ? "win"
        : rand(i * 17 + 7) > 0.85
          ? "draw"
          : "loss";
    const rank: number | null = isShowdown ? Math.floor(rand(i * 19) * 10) + 1 : null;
    const trophyChange = isShowdown
      ? rank! <= 4
        ? Math.floor(8 - rank!)
        : -(rank! - 4) * 2
      : result === "win"
        ? 8 + Math.floor(rand(i * 23) * 5)
        : result === "loss"
          ? -(5 + Math.floor(rand(i * 29) * 4))
          : 0;
    const duration = 60 + Math.floor(rand(i * 31) * 180);

    let teams: BattlePlayer[][];
    if (isShowdown) {
      teams = Array.from({ length: 10 }, (_, j) => {
        const bid = j === 0 ? meId : OWNED_IDS[(i + j) % OWNED_IDS.length];
        return [
          {
            tag: j === 0 ? PLAYER.tag : "#P" + (1000 + i * 10 + j),
            name: j === 0 ? PLAYER.name : "Player" + j,
            brawler: bid,
            isMe: j === 0,
            trophies: 700 + Math.floor(rand(i * j + 1) * 900),
          },
        ];
      });
    } else {
      const brawlersInBattle: string[] = [];
      for (let j = 0; j < 6; j++) {
        brawlersInBattle.push(OWNED_IDS[(i * 7 + j * 13) % OWNED_IDS.length]);
      }
      brawlersInBattle[0] = meId;
      teams = [
        [
          { tag: PLAYER.tag, name: PLAYER.name, brawler: brawlersInBattle[0], isMe: true, trophies: 1240 },
          { tag: "#A1" + i, name: "TeamMate1", brawler: brawlersInBattle[1], isMe: false, trophies: 1180 },
          { tag: "#A2" + i, name: "TeamMate2", brawler: brawlersInBattle[2], isMe: false, trophies: 1320 },
        ],
        [
          { tag: "#B1" + i, name: "Enemy1", brawler: brawlersInBattle[3], isMe: false, trophies: 1290 },
          { tag: "#B2" + i, name: "Enemy2", brawler: brawlersInBattle[4], isMe: false, trophies: 1210 },
          { tag: "#B3" + i, name: "Enemy3", brawler: brawlersInBattle[5], isMe: false, trophies: 1350 },
        ],
      ];
    }

    const minutesAgo = Math.floor(rand(i * 37) * 60 * 24 * 7);
    const flat = teams.flat();
    const meEntry = flat.find((p) => p.isMe)!;
    battles.push({
      id: i,
      battleTime: minutesAgo,
      event: ev,
      result,
      rank,
      trophyChange,
      duration,
      teams,
      myBrawler: meEntry.brawler,
      myTrophies: meEntry.trophies,
      starPlayer: flat[0].tag,
    });
  }
  return battles.sort((a, b) => a.battleTime - b.battleTime);
}
export const BATTLES: Battle[] = genBattles();

export interface PlayerStat {
  brawlerId: string;
  battleCount: number;
  winCount: number;
  lossCount: number;
  winRate: number;
  starPlayerCount: number;
  avgTrophyChange: number;
}
function genPlayerStats(): Record<string, PlayerStat> {
  const stats: Record<string, PlayerStat> = {};
  OWNED_BRAWLERS.forEach((b) => {
    const seed = b.id.charCodeAt(0) + b.id.charCodeAt(b.id.length - 1);
    const total = Math.floor(20 + rand(seed) * 180);
    const wins = Math.floor(total * (0.42 + rand(seed * 2) * 0.25));
    const losses = total - wins;
    stats[b.id] = {
      brawlerId: b.id,
      battleCount: total,
      winCount: wins,
      lossCount: losses,
      winRate: wins / total,
      starPlayerCount: Math.floor(wins * (0.1 + rand(seed * 3) * 0.25)),
      avgTrophyChange: (rand(seed * 4) - 0.4) * 6,
    };
  });
  return stats;
}
export const PLAYER_STATS: Record<string, PlayerStat> = genPlayerStats();

export interface ClassRollup {
  battles: number;
  wins: number;
  losses: number;
  brawlers: number;
}
export function rollupByClass(): Record<ClassKey, ClassRollup> {
  const out = {} as Record<ClassKey, ClassRollup>;
  (Object.keys(CLS) as ClassKey[]).forEach((c) => {
    out[c] = { battles: 0, wins: 0, losses: 0, brawlers: 0 };
  });
  OWNED_BRAWLERS.forEach((b) => {
    const s = PLAYER_STATS[b.id];
    out[b.cls].battles += s.battleCount;
    out[b.cls].wins += s.winCount;
    out[b.cls].losses += s.lossCount;
    out[b.cls].brawlers += 1;
  });
  return out;
}

export interface BrawlerMeta extends Brawler {
  winRate: number;
  pickRate: number;
  tier: "S" | "A" | "B" | "C" | "D";
  battleCount: number;
  starRate: number;
}
function genBrawlerMeta(): BrawlerMeta[] {
  return BRAWLERS.map((b, i) => {
    const seed = i * 31 + 7;
    const winRate = 0.42 + rand(seed) * 0.20;
    const pickRate = 0.02 + rand(seed * 2) * 0.18;
    const tier: BrawlerMeta["tier"] =
      winRate > 0.55 ? "S" : winRate > 0.52 ? "A" : winRate > 0.49 ? "B" : winRate > 0.46 ? "C" : "D";
    return {
      ...b,
      winRate,
      pickRate,
      tier,
      battleCount: Math.floor(20000 + rand(seed * 3) * 200000),
      starRate: 0.08 + rand(seed * 4) * 0.08,
    };
  });
}
export const BRAWLER_META: BrawlerMeta[] = genBrawlerMeta();

export interface PairStat {
  brawler: { id: string };
  pairBrawler?: Brawler;
  enemyBrawler?: Brawler;
  totalBattleCount: number;
  pickRate: number;
  winRate: number;
  averageRank: number;
}
export function genPairStats(
  eventId: number,
  brawlerId: string,
  isShowdown: boolean,
  isEnemy: boolean,
): PairStat[] {
  const others = BRAWLERS.filter((x) => x.id !== brawlerId);
  return others
    .map((p, i) => {
      const seed = (eventId + brawlerId.charCodeAt(0) + i * 23) % 1000;
      const winRate = 0.38 + rand(seed) * 0.24;
      const pickRate = 0.01 + rand(seed * 3) * 0.15;
      const battles = 200 + Math.floor(rand(seed * 5) * 8000);
      const averageRank = 2.5 + rand(seed * 7) * 5.5;
      const out: PairStat = {
        brawler: { id: brawlerId },
        totalBattleCount: battles,
        pickRate,
        winRate,
        averageRank,
      };
      if (isEnemy) out.enemyBrawler = p;
      else out.pairBrawler = p;
      return out;
    })
    .sort((a, b) => b.pickRate - a.pickRate);
}

export interface DailyStat {
  date: string;
  totalBattleCount: number;
  winRate: number;
  starPlayerRate: number;
}
export function genBrawlerDailyStats(brawlerId: string, days: number): DailyStat[] {
  const meta = BRAWLER_META.find((x) => x.id === brawlerId);
  const baseWin = meta ? meta.winRate : 0.5;
  const baseStar = meta ? meta.starRate : 0.1;
  const baseBattles = meta ? meta.battleCount / 30 : 5000;
  const out: DailyStat[] = [];
  const today = new Date();
  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(today);
    d.setDate(d.getDate() - i);
    const seed = (brawlerId.charCodeAt(0) + i * 7) % 1000;
    const noise = (rand(seed) - 0.5) * 0.06;
    const battleNoise = 0.7 + rand(seed * 3) * 0.6;
    out.push({
      date: d.toISOString().slice(0, 10),
      totalBattleCount: Math.floor(baseBattles * battleNoise),
      winRate: Math.max(0.30, Math.min(0.70, baseWin + noise)),
      starPlayerRate: Math.max(0.04, Math.min(0.25, baseStar + (rand(seed * 5) - 0.5) * 0.04)),
    });
  }
  return out;
}

export interface OverallPairStat {
  brawler: { id: string };
  pairBrawler: Brawler;
  totalBattleCount: number;
  pickRate: number;
  winRate: number;
}
export function genBrawlerOverallPairStats(brawlerId: string): OverallPairStat[] {
  const others = BRAWLERS.filter((x) => x.id !== brawlerId);
  return others
    .map((p, i) => {
      const seed = (brawlerId.charCodeAt(0) + i * 19) % 1000;
      const winRate = 0.40 + rand(seed) * 0.22;
      const pickRate = 0.005 + rand(seed * 3) * 0.10;
      const battles = 1000 + Math.floor(rand(seed * 5) * 30000);
      return { brawler: { id: brawlerId }, pairBrawler: p, totalBattleCount: battles, pickRate, winRate };
    })
    .sort((a, b) => b.pickRate - a.pickRate);
}

export interface OverallEnemyStat {
  brawler: { id: string };
  enemyBrawler: Brawler;
  totalBattleCount: number;
  pickRate: number;
  winRate: number;
}
export function genBrawlerOverallEnemyStats(brawlerId: string): OverallEnemyStat[] {
  const others = BRAWLERS.filter((x) => x.id !== brawlerId);
  return others
    .map((p, i) => {
      const seed = (brawlerId.charCodeAt(0) * 3 + i * 29) % 1000;
      const winRate = 0.32 + rand(seed) * 0.36;
      const pickRate = 0.005 + rand(seed * 3) * 0.10;
      const battles = 800 + Math.floor(rand(seed * 5) * 28000);
      return { brawler: { id: brawlerId }, enemyBrawler: p, totalBattleCount: battles, pickRate, winRate };
    })
    .sort((a, b) => b.pickRate - a.pickRate);
}

export interface OwnershipItem {
  itemId: string;
  itemName: string;
  ownershipRate: number;
  ownerCount: number;
  totalCount: number;
}
export interface ItemOwnership {
  starPowers: OwnershipItem[];
  gadgets: OwnershipItem[];
  gears: OwnershipItem[];
  hyperCharges: OwnershipItem[];
  buffies: OwnershipItem[];
  totalCount: number;
}
export function genBrawlerItemOwnership(brawlerId: string): ItemOwnership {
  const totalCount = 80000 + Math.floor(rand(brawlerId.charCodeAt(0)) * 200000);
  function mk(prefix: string, count: number, label: string): OwnershipItem[] {
    return Array.from({ length: count }, (_, i) => {
      const seed = (brawlerId.charCodeAt(0) + i * 13 + prefix.charCodeAt(0)) % 1000;
      const ownershipRate = 0.05 + rand(seed) * 0.85;
      return {
        itemId: prefix + (i + 1),
        itemName: `${label} ${i + 1}`,
        ownershipRate,
        ownerCount: Math.floor(totalCount * ownershipRate),
        totalCount,
      };
    }).sort((a, b) => b.ownershipRate - a.ownershipRate);
  }
  return {
    starPowers: mk("SP", 2, "스타파워"),
    gadgets: mk("GD", 2, "가젯"),
    gears: mk("GR", 4, "기어"),
    hyperCharges: mk("HC", 1, "하이퍼차지"),
    buffies: [],
    totalCount,
  };
}

export type RankingType = "PLAYER" | "CLUB" | "BRAWLER";
export interface RankingEntry {
  rank: number;
  delta: number;
  name: string;
  tag: string;
  trophies: number;
  club: string | null;
  brawler: string | null;
}
export function genRanking(type: RankingType, count: number): RankingEntry[] {
  const out: RankingEntry[] = [];
  for (let i = 0; i < count; i++) {
    const seed = i * 13 + (type === "PLAYER" ? 1 : type === "BRAWLER" ? 2 : 3);
    const baseTrophy =
      type === "CLUB"
        ? 1500000 - i * 8000
        : type === "PLAYER"
          ? 90000 - i * 200 - Math.floor(rand(seed) * 120)
          : 2200 - i * 6 - Math.floor(rand(seed) * 8);
    out.push({
      rank: i + 1,
      delta: i < 3 ? 0 : Math.floor((rand(seed) - 0.5) * 10),
      name: type === "CLUB" ? `클럽${i + 1}` : `Player${(i + 1).toString().padStart(2, "0")}`,
      tag: "#" + (1000 + i).toString(36).toUpperCase().padStart(6, "0"),
      trophies: baseTrophy,
      club: type === "PLAYER" ? `Club${(i % 8) + 1}` : null,
      brawler: type === "BRAWLER" ? OWNED_IDS[i % OWNED_IDS.length] : null,
    });
  }
  return out;
}

// ========== Format helpers ==========
export function fmtNum(n: number): string {
  if (n >= 1000000) return (n / 1000000).toFixed(1) + "M";
  if (n >= 10000) return (n / 1000).toFixed(0) + "k";
  if (n >= 1000) return n.toLocaleString();
  return n.toString();
}
export function fmtPct(p: number, digits = 1): string {
  return (p * 100).toFixed(digits) + "%";
}
export function fmtMinAgo(min: number): string {
  if (min < 1) return "방금 전";
  if (min < 60) return min + "분 전";
  const hours = Math.floor(min / 60);
  if (hours < 24) return hours + "시간 전";
  const days = Math.floor(hours / 24);
  return days + "일 전";
}
export function fmtDuration(sec: number): string {
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  return `${m}:${s.toString().padStart(2, "0")}`;
}
