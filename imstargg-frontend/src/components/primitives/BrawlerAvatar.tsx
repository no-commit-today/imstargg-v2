import type { CSSProperties } from "react";
import Image from "next/image";
import type { BrawlerRarity } from "@/api/types";

const RARITY_BORDER: Record<BrawlerRarity, string> = {
  STARTING_BRAWLER: "#B4B9C7",
  RARE: "#2BD576",
  SUPER_RARE: "#3DE0FF",
  EPIC: "#9146FF",
  MYTHIC: "#FF3A3A",
  LEGENDARY: "#FFC60A",
  ULTRA_LEGENDARY: "#FF8A1F",
};

interface Props {
  name: string;
  imageUrl: string | null;
  rarity?: BrawlerRarity;
  size?: number;
  className?: string;
  showFrame?: boolean;
}

export function BrawlerAvatar({
  name,
  imageUrl,
  rarity,
  size = 48,
  className = "",
  showFrame = false,
}: Props) {
  const frameColor = rarity ? RARITY_BORDER[rarity] : "#B4B9C7";
  const style: CSSProperties = {
    width: size,
    height: size,
    boxShadow: showFrame
      ? `inset 0 0 0 2px ${frameColor}, inset 0 0 0 3px rgba(0,0,0,0.2)`
      : undefined,
  };

  if (imageUrl) {
    return (
      <div
        className={`relative overflow-hidden rounded-md ${className}`}
        style={style}
        title={name}
      >
        <Image
          src={imageUrl}
          alt={name}
          width={size}
          height={size}
          unoptimized
          className="h-full w-full object-cover"
        />
      </div>
    );
  }

  const initial = name.slice(0, 1);
  const fontSize = Math.max(10, size * 0.4);
  return (
    <div
      className={`flex items-center justify-center rounded-md bg-gradient-to-br from-[#FFC60A] to-[#FF8A1F] font-display font-extrabold text-white ${className}`}
      style={{ ...style, fontSize }}
      title={name}
    >
      {initial}
    </div>
  );
}
