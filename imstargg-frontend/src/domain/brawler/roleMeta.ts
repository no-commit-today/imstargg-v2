import type { BrawlerRole } from "@/api/types";

export interface RoleMeta {
  icon: string;
  /** Hero 그라디언트용 시작 컬러 — DESIGN.md class color 매핑. */
  colorStart: string;
  /** Hero 그라디언트용 끝 컬러 — colorStart 의 명도 약 -15%. */
  colorEnd: string;
}

export const ROLE_META: Record<BrawlerRole, RoleMeta> = {
  DAMAGE_DEALER: { icon: "icon_class_damage.svg",     colorStart: "#FF3A3A", colorEnd: "#B91515" },
  TANK:          { icon: "icon_class_tank.svg",        colorStart: "#9146FF", colorEnd: "#5C1FB7" },
  MARKSMAN:      { icon: "icon_class_marksmen.svg",    colorStart: "#FF8A1F", colorEnd: "#B85700" },
  ASSASSIN:      { icon: "icon_class_assassin.svg",    colorStart: "#FF5DAA", colorEnd: "#B82B70" },
  SUPPORT:       { icon: "icon_class_support.svg",     colorStart: "#2BD576", colorEnd: "#15914A" },
  CONTROLLER:    { icon: "icon_class_controller.svg",  colorStart: "#3DE0FF", colorEnd: "#0A91B0" },
  ARTILLERY:     { icon: "icon_class_artillery.svg",   colorStart: "#FFC60A", colorEnd: "#B58800" },
};
