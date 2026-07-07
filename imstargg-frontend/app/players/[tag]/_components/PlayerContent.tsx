"use client";

import { useEffect } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { usePlayer } from "@/hooks/queries/usePlayer";
import { usePlayerBrawlers } from "@/hooks/queries/usePlayerBrawlers";
import { queryKeys } from "@/hooks/queries/queryKeys";
import type { Player, PlayerBrawler } from "@/domain/player/types";
import type { BattleResponse, RenewalResponse, SliceResponse } from "@/api/types";
import { PlayerHero } from "./PlayerHero";
import { PlayerBrawlers } from "./PlayerBrawlers";
import { PlayerBattles } from "./PlayerBattles";
import { PlayerStatistics } from "./statistics/PlayerStatistics";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { AdUnit } from "@/components/ads/AdUnit";
import { AD_SLOTS } from "@/components/ads/slots";

type Props = {
  tag: string;
  initialPlayer: Player;
  initialBrawlers: PlayerBrawler[];
  initialRenewal: RenewalResponse | null;
  initialBattles?: SliceResponse<BattleResponse>;
  battlesFetchedAt?: number;
};

export function PlayerContent({
  tag,
  initialPlayer,
  initialBrawlers,
  initialRenewal,
  initialBattles,
  battlesFetchedAt,
}: Props) {
  const qc = useQueryClient();

  useEffect(() => {
    if (initialRenewal) {
      qc.setQueryData(queryKeys.player.renewal(tag), initialRenewal);
    }
  }, [tag, initialRenewal, qc]);

  const { data: player } = usePlayer(tag, { initialData: initialPlayer });
  const { data: brawlers } = usePlayerBrawlers(tag, { initialData: initialBrawlers });

  if (!player) return null;
  return (
    <main className="mx-auto w-full max-w-7xl space-y-6 px-4 py-6">
      <PlayerHero player={player} />
      <Tabs defaultValue="battles" className="w-full">
        <TabsList variant="line" className="h-11 w-full justify-start gap-4 border-b border-[#D8DCE5] px-0">
          <TabsTrigger
            value="battles"
            className="px-2 text-base font-semibold data-[state=active]:text-[#14161B]"
          >
            배틀
          </TabsTrigger>
          <TabsTrigger
            value="brawlers"
            className="px-2 text-base font-semibold data-[state=active]:text-[#14161B]"
          >
            브롤러
          </TabsTrigger>
          <TabsTrigger
            value="statistics"
            className="px-2 text-base font-semibold data-[state=active]:text-[#14161B]"
          >
            통계
          </TabsTrigger>
        </TabsList>
        <TabsContent value="battles">
          <PlayerBattles
            tag={tag}
            initialBattles={initialBattles}
            battlesFetchedAt={battlesFetchedAt}
          />
        </TabsContent>
        <TabsContent value="brawlers">
          <PlayerBrawlers brawlers={brawlers ?? []} />
        </TabsContent>
        <TabsContent value="statistics">
          <PlayerStatistics tag={tag} />
        </TabsContent>
      </Tabs>
      <AdUnit slot={AD_SLOTS.playerDetail} />
    </main>
  );
}
