import type { BrawlerRarity, BrawlerRole } from "@/api/types";

export const RARITY_LABEL_KO: Record<BrawlerRarity, string> = {
  STARTING_BRAWLER: "기본",
  RARE: "희귀",
  SUPER_RARE: "초희귀",
  EPIC: "영웅",
  MYTHIC: "신화",
  LEGENDARY: "전설",
  ULTRA_LEGENDARY: "울트라 전설",
};

export const ROLE_LABEL_KO: Record<BrawlerRole, string> = {
  DAMAGE_DEALER: "대미지 딜러",
  TANK: "탱커",
  MARKSMAN: "저격수",
  ASSASSIN: "어쌔신",
  SUPPORT: "서포터",
  CONTROLLER: "컨트롤러",
  ARTILLERY: "투척수",
};
