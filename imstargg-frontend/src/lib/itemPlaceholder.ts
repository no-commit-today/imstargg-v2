import type { GearRarity } from "@/api/types";

export type ItemKind = "gadget" | "gear" | "starPower" | "hyperCharge" | "buffy";

export function basePlaceholder(kind: ItemKind, gearRarity?: GearRarity): string | null {
  switch (kind) {
    case "gadget":
      return "/assets/gadget_base_empty.webp";
    case "starPower":
      return "/assets/starpower_base01_empty.webp";
    case "hyperCharge":
      return "/assets/hypercharge_base_empty.webp";
    case "gear":
      if (gearRarity === "MYTHIC") return "/assets/gear_mythic_super_base_empty.webp";
      if (gearRarity === "EPIC") return "/assets/gear_epic_super_base_empty.webp";
      return "/assets/gear_base_empty.webp";
    case "buffy":
      return null;
  }
}

export function emptyPlaceholder(kind: ItemKind): string | null {
  switch (kind) {
    case "gadget":
      return "/assets/gadget_empty.webp";
    case "gear":
      return "/assets/gear_empty.webp";
    case "starPower":
      return "/assets/starpower_empty.webp";
    case "hyperCharge":
    case "buffy":
      return null;
  }
}
