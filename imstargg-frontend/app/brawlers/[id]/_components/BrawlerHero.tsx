"use client";

import type { BrawlerRarity } from "@/api/types";
import type { Brawler } from "@/domain/brawler/types";
import { BrawlerAvatar } from "@/components/primitives/BrawlerAvatar";
import ClassIcon from "@/components/primitives/ClassIcon";
import { RARITY_LABEL_KO, ROLE_LABEL_KO } from "@/domain/brawler/labels";
import { ROLE_META } from "@/domain/brawler/roleMeta";

const RARITY_BG: Record<BrawlerRarity, string> = {
  STARTING_BRAWLER: "#B4B9C7",
  RARE: "#2BD576",
  SUPER_RARE: "#3DE0FF",
  EPIC: "#9146FF",
  MYTHIC: "#FF3A3A",
  LEGENDARY: "#FFC60A",
  ULTRA_LEGENDARY: "#FF8A1F",
};

export function BrawlerHero({ brawler }: { brawler: Brawler }) {
  const meta = ROLE_META[brawler.role];
  const rarityColor = RARITY_BG[brawler.rarity] ?? "#B4B9C7";
  const isLightGradient = brawler.role === "ARTILLERY" || brawler.role === "SUPPORT";
  const nameTextColor = isLightGradient ? "text-[#14161B]" : "text-white";
  const nameFilter = isLightGradient
    ? "drop-shadow(0 2px 0 rgba(255,255,255,0.7))"
    : "drop-shadow(0 4px 8px rgba(0,0,0,0.45)) drop-shadow(0 2px 0 rgba(0,0,0,0.45))";

  return (
    <section
      className="relative overflow-hidden rounded-[18px] border-2 border-[#14161B] bg-white"
      style={{ boxShadow: "var(--shadow-sticker-lg)" }}
    >
      <div
        className="absolute inset-0"
        style={{
          background: `linear-gradient(135deg, ${meta.colorStart} 0%, ${meta.colorEnd} 100%)`,
          clipPath: "polygon(0 0, 62% 0, 52% 100%, 0 100%)",
        }}
        aria-hidden
      />
      <div className="relative flex flex-col gap-4 p-5 sm:flex-row sm:items-center sm:gap-6 sm:p-7">
        <div className="flex justify-center sm:justify-start">
          <BrawlerAvatar
            name={brawler.name}
            imageUrl={brawler.portraitImageUrl}
            rarity={brawler.rarity}
            size={180}
            showFrame
            priority
            className="border-[3px] border-[#14161B] shadow-[0_4px_0_0_rgba(0,0,0,0.4)]"
          />
        </div>
        <div className="flex-1 text-center sm:text-left">
          <h1
            className={`font-display text-4xl leading-tight tracking-[-0.01em] ${nameTextColor} sm:text-6xl`}
            style={{ filter: nameFilter }}
          >
            {brawler.name}
          </h1>
          <div className="mt-3 flex flex-wrap justify-center gap-2 sm:justify-start">
            <span
              className="pill dark inline-flex items-center gap-1.5"
              style={{ boxShadow: "0 3px 0 0 rgba(0,0,0,0.35)" }}
            >
              <ClassIcon role={brawler.role} size={14} />
              {ROLE_LABEL_KO[brawler.role]}
            </span>
            <span
              className="pill inline-flex items-center"
              style={{
                background: rarityColor,
                color: "#14161B",
                boxShadow: "0 3px 0 0 rgba(0,0,0,0.35)",
              }}
            >
              {RARITY_LABEL_KO[brawler.rarity]}
            </span>
          </div>
        </div>
      </div>
    </section>
  );
}
