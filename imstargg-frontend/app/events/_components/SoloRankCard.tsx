"use client";

import Image from "next/image";
import Link from "next/link";
import type { BattleEvent } from "@/domain/event/types";
import { getModeStyle } from "./modeStyles";

export function SoloRankCard({ event }: { event: BattleEvent }) {
  const modeName = event.mode?.name ?? "이벤트";
  const initial = modeName.slice(0, 1);
  const modeStyle = getModeStyle(modeName);

  return (
    <Link href={`/events/${event.id}`} className="solorank-card group">
      <div
        className="solorank-card-icon-wrap"
        style={{ background: modeStyle.bg, color: modeStyle.fg }}
      >
        {event.mode?.iconImageUrl ? (
          <Image
            src={event.mode.iconImageUrl}
            alt={modeName}
            width={36}
            height={36}
            className="solorank-card-icon"
            unoptimized
          />
        ) : (
          <span className="solorank-card-icon-fallback">{initial}</span>
        )}
      </div>
      <div className="solorank-card-meta">
        <span className="solorank-card-mode">{modeName}</span>
        <span className="solorank-card-map">{event.mapName}</span>
      </div>
    </Link>
  );
}
