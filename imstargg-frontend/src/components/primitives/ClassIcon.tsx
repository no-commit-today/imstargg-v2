import Image from "next/image";
import type { BrawlerRole } from "@/api/types";
import { ROLE_META } from "@/domain/brawler/roleMeta";
import { ROLE_LABEL_KO } from "@/domain/brawler/labels";

interface Props {
  role: BrawlerRole;
  size?: number;
}

export default function ClassIcon({ role, size = 18 }: Props) {
  const meta = ROLE_META[role];
  const iconSize = Math.round(size * 0.65);
  return (
    <span className="class-icon" style={{ width: size, height: size }}>
      <Image
        src={`/assets/${meta.icon}`}
        alt={ROLE_LABEL_KO[role]}
        width={iconSize}
        height={iconSize}
        unoptimized
        style={{ width: iconSize, height: iconSize }}
      />
    </span>
  );
}
