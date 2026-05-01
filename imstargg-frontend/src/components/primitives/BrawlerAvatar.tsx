import type { CSSProperties } from "react";
import Image from "next/image";
import type { BrawlerRarity } from "@/api/types";

const RARITY_BG: Record<BrawlerRarity, string> = {
  STARTING_BRAWLER: "#A1D5F1",
  RARE: "#6DDB47",
  SUPER_RARE: "#0087FA",
  EPIC: "#AA15E4",
  MYTHIC: "#EA3330",
  LEGENDARY: "#FDF255",
  ULTRA_LEGENDARY: "transparent",
};

const RARITY_BORDER: Record<BrawlerRarity, string> = {
  STARTING_BRAWLER: "#B4B9C7",
  RARE: "#2BD576",
  SUPER_RARE: "#3DE0FF",
  EPIC: "#9146FF",
  MYTHIC: "#FF3A3A",
  LEGENDARY: "#FFC60A",
  ULTRA_LEGENDARY: "#FF8A1F",
};

const ULTRA_LEGENDARY_GRADIENT =
  "linear-gradient(135deg, #ff3a3a 0%, #ff8a1f 16.66%, #ffc60a 33.33%, #2bd576 50%, #3de0ff 66.66%, #9146ff 83.33%, #ff5daa 100%)";

const FALLBACK_GRADIENT =
  "linear-gradient(to bottom right, #FFC60A, #FF8A1F)";

interface Props {
  name: string;
  imageUrl: string | null;
  rarity?: BrawlerRarity | null;
  /** width in px. height = round(size * 0.75) for the 4:3 portrait ratio. */
  size?: number;
  className?: string;
  showFrame?: boolean;
  priority?: boolean;
}

export function BrawlerAvatar({
  name,
  imageUrl,
  rarity,
  size,
  className = "",
  showFrame = false,
  priority = false,
}: Props) {
  const isUltra = rarity === "ULTRA_LEGENDARY";
  const background = rarity
    ? isUltra
      ? ULTRA_LEGENDARY_GRADIENT
      : RARITY_BG[rarity]
    : undefined;
  const frameColor = rarity ? RARITY_BORDER[rarity] : "#B4B9C7";

  const baseStyle: CSSProperties = {
    background,
    boxShadow: showFrame
      ? `inset 0 0 0 2px ${frameColor}, inset 0 0 0 3px rgba(0,0,0,0.2)`
      : undefined,
  };
  if (size) {
    baseStyle.width = size;
    baseStyle.height = Math.round(size * 0.75);
  }

  const declaredWidth = size ?? 80;
  const declaredHeight = size ? Math.round(size * 0.75) : 60;

  if (imageUrl) {
    return (
      <div
        className={`relative overflow-hidden rounded-md ${className}`}
        style={baseStyle}
        title={name}
      >
        <Image
          src={imageUrl}
          alt={name}
          width={declaredWidth}
          height={declaredHeight}
          unoptimized
          priority={priority}
          className="h-full w-full object-cover"
        />
      </div>
    );
  }

  const initial = name.slice(0, 1);
  const fontSize = size ? Math.max(10, size * 0.32) : undefined;
  return (
    <div
      className={`flex items-center justify-center rounded-md font-display font-extrabold text-white ${className}`}
      style={{
        ...baseStyle,
        background: background ?? FALLBACK_GRADIENT,
        fontSize,
      }}
      title={name}
    >
      {initial}
    </div>
  );
}
