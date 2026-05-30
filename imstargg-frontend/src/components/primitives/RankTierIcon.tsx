import Image from "next/image";
import type { SoloRankTier } from "@/api/types";
import { tierColor, tierStickerImage, tierSegmentNumber } from "@/domain/battle/tierLabels";

interface Props {
  tier: SoloRankTier;
  size?: number;
}

export default function RankTierIcon({ tier, size = 18 }: Props) {
  const segment = tierSegmentNumber(tier);
  return (
    <span style={{ position: "relative", display: "inline-block", width: size, height: size }}>
      <Image
        src={tierStickerImage(tier)}
        alt=""
        role="img"
        width={size}
        height={size}
        unoptimized
      />
      {segment && (
        <span
          style={{
            position: "absolute",
            inset: 0,
            display: "grid",
            placeItems: "center",
            color: tierColor(tier),
            fontFamily: "var(--font-num)",
            fontWeight: 800,
            fontSize: size * 0.45,
          }}
        >
          {segment}
        </span>
      )}
    </span>
  );
}
