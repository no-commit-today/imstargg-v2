import type { CSSProperties } from "react";
import { brawler, RARITY } from "@/lib/data";

interface Props {
  id: string;
  size?: number;
  className?: string;
  showFrame?: boolean;
}

export default function BrawlerPortrait({ id, size = 36, className = "", showFrame = false }: Props) {
  const b = brawler(id);
  if (!b) {
    return <div style={{ width: size, height: size, background: "#ddd", borderRadius: 8 }} />;
  }
  const fontSize = Math.max(10, size * 0.36);
  const style: CSSProperties & Record<string, string | number> = {
    width: size,
    height: size,
    "--bp-c1": b.c1,
    "--bp-c2": b.c2,
    fontSize,
    boxShadow: showFrame
      ? `inset 0 0 0 2px ${RARITY[b.rarity].color}, inset 0 0 0 3px rgba(0,0,0,0.2)`
      : "none",
  };
  return (
    <div className={`brawler-portrait ${className}`} style={style} title={b.name}>
      <span>{b.name.slice(0, 1)}</span>
    </div>
  );
}
