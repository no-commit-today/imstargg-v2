import Image from "next/image";
import type { BattleMode } from "@/api/types";
import { modeLabel } from "@/domain/battle/modeLabels";

interface Props {
  iconUrl: string | null;
  mode: BattleMode;
  size?: number;
}

export function BattleModeIcon({ iconUrl, mode, size = 28 }: Props) {
  if (iconUrl) {
    return (
      <Image
        src={iconUrl}
        alt={modeLabel(mode)}
        width={size}
        height={size}
        unoptimized
        style={{ width: size, height: size, objectFit: "contain" }}
      />
    );
  }
  const fontSize = Math.max(10, size * 0.4);
  return (
    <div
      aria-label={modeLabel(mode)}
      style={{
        width: size,
        height: size,
        borderRadius: "50%",
        background: "#3B404D",
        color: "#fff",
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        fontFamily: "var(--font-display)",
        fontSize,
      }}
    >
      {modeLabel(mode).slice(0, 1)}
    </div>
  );
}
