"use client";

import Link from "next/link";
import type { Player } from "@/domain/player/types";
import { formatNumber } from "@/lib/format";
import { RenewalButton } from "@/components/shared/RenewalButton";
import { Card } from "@/components/ui/card";
import { routes } from "@/config/routes";

export function PlayerSidebar({ player }: { player: Player }) {
  return (
    <Card className="flex flex-col gap-3 p-4">
      <header className="flex items-center justify-between gap-2">
        <div>
          <h1 className="text-xl font-semibold">{player.name}</h1>
          <p className="text-sm text-muted-foreground">{player.tag}</p>
        </div>
        <RenewalButton kind="player" tag={player.tag} />
      </header>
      <dl className="grid grid-cols-2 gap-2 text-sm">
        <Stat label="트로피" value={formatNumber(player.trophies)} />
        <Stat label="최고 트로피" value={formatNumber(player.highestTrophies)} />
        <Stat label="레벨" value={formatNumber(player.expLevel)} />
        <Stat label="3v3 승" value={formatNumber(player.victories3vs3)} />
        <Stat label="솔로 승" value={formatNumber(player.victoriesSolo)} />
        <Stat label="듀오 승" value={formatNumber(player.victoriesDuo)} />
      </dl>
      {player.club ? (
        <p className="text-sm">
          클럽:{" "}
          <Link className="underline" href={routes.club(player.club.tag)}>
            {player.club.name}
          </Link>
        </p>
      ) : (
        <p className="text-sm text-muted-foreground">소속 클럽 없음</p>
      )}
    </Card>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between rounded-md bg-muted/40 px-3 py-2">
      <dt className="text-muted-foreground">{label}</dt>
      <dd className="font-medium">{value}</dd>
    </div>
  );
}
