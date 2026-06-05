"use client";

import type { ReactNode } from "react";
import Image from "next/image";
import Link from "next/link";
import type { Player } from "@/domain/player/types";
import { formatNumber } from "@/lib/format";
import { Card } from "@/components/ui/card";
import { PlayerAvatar } from "@/components/primitives/PlayerAvatar";
import Trophy from "@/components/primitives/Trophy";
import PlayerPrestigeBadge from "@/components/primitives/PlayerPrestigeBadge";
import { RenewalButton } from "@/components/shared/RenewalButton";
import { RenewalStatus } from "@/components/shared/RenewalStatus";
import { routes } from "@/config/routes";

const STAT_NUM_CLASS =
  "font-num font-bold text-[#14161B] tabular-nums [font-variant-numeric:tabular-nums]";

export function PlayerHero({ player }: { player: Player }) {
  const showdownWins = player.victoriesSolo + player.victoriesDuo;
  return (
    <Card className="flex flex-col gap-5 p-6 sm:p-7">
      <header className="flex items-center gap-4 sm:gap-6">
        <PlayerAvatar name={player.name} size={64} className="sm:hidden" />
        <PlayerAvatar name={player.name} size={80} className="hidden sm:flex" />
        <div className="min-w-0 flex-1">
          <h1
            className="flex items-center gap-2 text-2xl font-bold sm:text-3xl"
            style={player.nameColor ? { color: player.nameColor } : undefined}
          >
            <span className="truncate">{player.name}</span>
            <PlayerPrestigeBadge level={player.totalPrestigeLevel} size={24} />
          </h1>
          <p className="font-mono text-sm text-[#5C6271]">{player.tag}</p>
          <p className="mt-1 flex min-w-0 flex-nowrap items-center gap-1.5 text-sm">
            {player.club ? (
              <>
                <Image
                  src="/assets/icon_club_league_masters.png"
                  alt=""
                  width={16}
                  height={16}
                  unoptimized
                />
                <span className="text-[#5C6271]">클럽:</span>
                <Link
                  className="min-w-0 truncate font-medium hover:underline"
                  href={routes.club(player.club.tag)}
                >
                  {player.club.name}
                </Link>
                <span className="text-[#8A90A0]">→</span>
              </>
            ) : (
              <span className="text-[#8A90A0]">소속 클럽 없음</span>
            )}
          </p>
        </div>
        <div className="flex flex-col items-end gap-1 sm:flex-row sm:items-center sm:gap-3">
          <RenewalStatus kind="player" tag={player.tag} />
          <RenewalButton kind="player" tag={player.tag} />
        </div>
      </header>
      <dl className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <StatCell label="트로피">
          <Trophy value={player.trophies} size={20} />
        </StatCell>
        <StatCell label="최고 트로피">
          <Trophy value={player.highestTrophies} size={20} />
        </StatCell>
        <StatCell label="3v3 승">
          <span className={STAT_NUM_CLASS}>{formatNumber(player.victories3vs3)}</span>
        </StatCell>
        <StatCell label="쇼다운 승">
          <span className={STAT_NUM_CLASS}>{formatNumber(showdownWins)}</span>
        </StatCell>
      </dl>
    </Card>
  );
}

function StatCell({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="flex flex-col gap-1 rounded-md bg-[#F6F7FA] px-3 py-2">
      <dt className="text-xs font-medium text-[#5C6271]">{label}</dt>
      <dd>{children}</dd>
    </div>
  );
}
