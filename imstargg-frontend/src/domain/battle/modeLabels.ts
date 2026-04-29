import type { BattleMode } from "@/api/types";

const LABELS: Record<BattleMode, string> = {
  KNOCKOUT: "녹아웃",
  GEM_GRAB: "젬 그랩",
  HEIST: "하이스트",
  HOT_ZONE: "핫 존",
  SIEGE: "공성전",
  BOUNTY: "현상금 사냥",
  BRAWL_BALL: "브롤 볼",
  DUELS: "듀얼",
  SOLO_SHOWDOWN: "솔로 쇼다운",
  DUO_SHOWDOWN: "듀오 쇼다운",
  WIPEOUT: "전멸전",
  VOLLEY_BRAWL: "발리 브롤",
  TROPHY_THIEVES: "트로피 쟁탈전",
  BOSS_FIGHT: "보스전",
  BIG_GAME: "빅 게임",
  BASKET_BRAWL: "바스켓 브롤",
  ROBO_RUMBLE: "로보 럼블",
  PAYLOAD: "페이로드",
  TAKEDOWN: "테이크다운",
  HUNTERS: "헌터스",
  BOT_DROP: "봇 드롭",
  HOLD_THE_TROPHY: "트로피 지키기",
  UNKNOWN: "알 수 없음",
};

export function modeLabel(mode: BattleMode): string {
  return LABELS[mode] ?? mode;
}
