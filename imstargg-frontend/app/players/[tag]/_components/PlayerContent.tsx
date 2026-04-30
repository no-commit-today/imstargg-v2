"use client";

import { useEffect } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { usePlayer } from "@/hooks/queries/usePlayer";
import { usePlayerBrawlers } from "@/hooks/queries/usePlayerBrawlers";
import { queryKeys } from "@/hooks/queries/queryKeys";
import type { Player, PlayerBrawler } from "@/domain/player/types";
import type { RenewalResponse } from "@/api/types";
import { PlayerHero } from "./PlayerHero";
import { PlayerBrawlers } from "./PlayerBrawlers";
import { PlayerBattles } from "./PlayerBattles";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";

type Props = {
  tag: string;
  initialPlayer: Player;
  initialBrawlers: PlayerBrawler[];
  initialRenewal: RenewalResponse | null;
};

export function PlayerContent({
  tag,
  initialPlayer,
  initialBrawlers,
  initialRenewal,
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
        </TabsList>
        <TabsContent value="battles">
          <PlayerBattles tag={tag} />
        </TabsContent>
        <TabsContent value="brawlers">
          <PlayerBrawlers brawlers={brawlers ?? []} />
        </TabsContent>
      </Tabs>
    </main>
  );
}
