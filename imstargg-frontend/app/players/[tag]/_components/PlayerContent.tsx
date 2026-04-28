"use client";

import { useEffect } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { usePlayer } from "@/hooks/queries/usePlayer";
import { usePlayerBrawlers } from "@/hooks/queries/usePlayerBrawlers";
import { queryKeys } from "@/hooks/queries/queryKeys";
import type { Player, PlayerBrawler } from "@/domain/player/types";
import type { RenewalResponse } from "@/api/types";
import { PlayerSidebar } from "./PlayerSidebar";
import { PlayerStats } from "./PlayerStats";
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
    <main className="mx-auto w-full max-w-4xl px-4 py-6 space-y-6">
      <PlayerSidebar player={player} />
      <Tabs defaultValue="stats" className="w-full">
        <TabsList>
          <TabsTrigger value="stats">통계</TabsTrigger>
          <TabsTrigger value="brawlers">브롤러</TabsTrigger>
          <TabsTrigger value="battles">배틀</TabsTrigger>
        </TabsList>
        <TabsContent value="stats">
          <PlayerStats player={player} />
        </TabsContent>
        <TabsContent value="brawlers">
          <PlayerBrawlers brawlers={brawlers ?? []} />
        </TabsContent>
        <TabsContent value="battles">
          <PlayerBattles tag={tag} />
        </TabsContent>
      </Tabs>
    </main>
  );
}
