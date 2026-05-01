import type { BrawlerRole } from "@/api/types";

export interface RoleMeta {
  icon: string;
}

export const ROLE_META: Record<BrawlerRole, RoleMeta> = {
  DAMAGE_DEALER: { icon: "class_damage.svg" },
  TANK:          { icon: "class_tank.svg" },
  MARKSMAN:      { icon: "class_marksmen.svg" },
  ASSASSIN:      { icon: "class_assassin.svg" },
  SUPPORT:       { icon: "class_support.svg" },
  CONTROLLER:    { icon: "class_controller.svg" },
  ARTILLERY:     { icon: "class_artillery.svg" },
};
