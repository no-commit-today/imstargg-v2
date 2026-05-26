import Image from "next/image";
import { Zap } from "lucide-react";
import type { GearRarity } from "@/api/types";
import { basePlaceholder, type ItemKind } from "@/lib/itemPlaceholder";

interface Props {
  kind: ItemKind;
  imageUrl: string | null;
  owned: boolean;
  gearRarity?: GearRarity;
  size?: number;
  name?: string;
  ariaLabel?: string;
}

export default function ItemSlot({
  kind,
  imageUrl,
  owned,
  gearRarity,
  size = 24,
  name,
  ariaLabel,
}: Props) {
  const title = name || undefined;

  if (kind === "buffy" && !imageUrl) {
    return (
      <span
        title={title}
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

  const src = imageUrl ?? basePlaceholder(kind, gearRarity);
  if (!src) return null;

  return (
    <Image
      src={src}
      alt=""
      role="img"
      width={size}
      height={size}
      unoptimized
      title={title}
      aria-label={ariaLabel}
      style={owned ? undefined : { opacity: 0.3, filter: "grayscale(1)" }}
    />
  );
}
