import type { BrawlerRole } from "@/api/types";

export interface RoleMeta {
  icon: string;
}

export const ROLE_META: Record<BrawlerRole, RoleMeta> = {
  DAMAGE_DEALER: { icon: "icon_class_damage.svg" },
  TANK:          { icon: "icon_class_tank.svg" },
  MARKSMAN:      { icon: "icon_class_marksmen.svg" },
  ASSASSIN:      { icon: "icon_class_assassin.svg" },
  SUPPORT:       { icon: "icon_class_support.svg" },
  CONTROLLER:    { icon: "icon_class_controller.svg" },
  ARTILLERY:     { icon: "icon_class_artillery.svg" },
};
