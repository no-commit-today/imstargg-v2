import { Zap } from "lucide-react";
import type { GearRarity } from "@/api/types";
import { basePlaceholder, emptyPlaceholder, type ItemKind } from "@/lib/itemPlaceholder";

interface Props {
  kind: ItemKind;
  imageUrl: string | null;
  owned: boolean;
  gearRarity?: GearRarity;
  size?: number;
  ariaLabel?: string;
}

export default function ItemSlot({
  kind,
  imageUrl,
  owned,
  gearRarity,
  size = 24,
  ariaLabel,
}: Props) {
  if (kind === "buffy" && !imageUrl) {
    return (
      <span
        aria-label={ariaLabel}
        style={{
          width: size,
          height: size,
          display: "grid",
          placeItems: "center",
          opacity: owned ? 1 : 0.3,
          color: owned ? "#14161B" : "#8A90A0",
        }}
      >
        <Zap size={size * 0.7} strokeWidth={2.5} />
      </span>
    );
  }

  if (owned) {
    const src = imageUrl ?? basePlaceholder(kind, gearRarity);
    return <img src={src ?? ""} alt="" role="img" width={size} height={size} aria-label={ariaLabel} />;
  }

  const emptySrc = emptyPlaceholder(kind);
  if (emptySrc) {
    return <img src={emptySrc} alt="" role="img" width={size} height={size} aria-label={ariaLabel} />;
  }
  // hyperCharge: empty 자산 부재 → base 를 흐리게
  const baseSrc = imageUrl ?? basePlaceholder(kind, gearRarity);
  return (
    <img
      src={baseSrc ?? ""}
      alt=""
      role="img"
      width={size}
      height={size}
      style={{ opacity: 0.3, filter: "grayscale(1)" }}
      aria-label={ariaLabel}
    />
  );
}
