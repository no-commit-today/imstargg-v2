import type { CSSProperties } from "react";
import type { BrawlerRole } from "@/api/types";
import { ROLE_META } from "@/domain/brawler/roleMeta";
import { ROLE_LABEL_KO } from "@/domain/brawler/labels";

interface Props {
  role: BrawlerRole;
  size?: number;
}

export default function ClassIcon({ role, size = 18 }: Props) {
  const meta = ROLE_META[role];
  const style: CSSProperties & Record<string, string | number> = {
    "--cls-color": meta.color,
    width: size,
    height: size,
  };
  return (
    <span className="class-icon" style={style}>
      <img
        src={`/assets/${meta.icon}`}
        alt={ROLE_LABEL_KO[role]}
        style={{ width: size * 0.65, height: size * 0.65 }}
      />
    </span>
  );
}
