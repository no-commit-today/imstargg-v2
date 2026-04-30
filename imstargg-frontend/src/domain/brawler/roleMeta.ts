import type { BrawlerRole } from "@/api/types";

export interface RoleMeta {
  color: string;
  icon: string;
}

export const ROLE_META: Record<BrawlerRole, RoleMeta> = {
  DAMAGE_DEALER: { color: "var(--class-damage)",     icon: "class_damage.svg" },
  TANK:          { color: "var(--class-tank)",       icon: "class_tank.svg" },
  MARKSMAN:      { color: "var(--class-marksman)",   icon: "class_marksmen.svg" },
  ASSASSIN:      { color: "var(--class-assassin)",   icon: "class_assassin.svg" },
  SUPPORT:       { color: "var(--class-support)",    icon: "class_support.svg" },
  CONTROLLER:    { color: "var(--class-controller)", icon: "class_controller.svg" },
  ARTILLERY:     { color: "var(--class-artillery)",  icon: "class_artillery.svg" },
};
