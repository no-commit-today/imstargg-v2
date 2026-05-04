"use client";

import { useEffect, useMemo, useState } from "react";
import { useBrawlPassSeasons, useSoloRankSeasons } from "@/hooks/queries/useSeasons";
import {
  usePlayerBrawlerStatistics,
  usePlayerBrawlerRoleStatistics,
  type PlayerStatBattleType,
} from "@/hooks/queries/usePlayerStatistics";
import { toPlayerBrawlerRoleStat, toPlayerBrawlerStat } from "@/domain/statistics/player";
import { PlayerStatisticsControls } from "./PlayerStatisticsControls";
import { PlayerBrawlerRoleSummary } from "./PlayerBrawlerRoleSummary";
import { PlayerBrawlerStatsList } from "./PlayerBrawlerStatsList";

export function PlayerStatistics({ tag }: { tag: string }) {
  const [battleType, setBattleType] = useState<PlayerStatBattleType>("RANKED");
  const [brawlPassSeasonNumber, setBrawlPassSeasonNumber] = useState<number | null>(null);
  const [soloRankSeasonKey, setSoloRankSeasonKey] = useState<string | null>(null);

  const { data: brawlPassSeasonsData } = useBrawlPassSeasons();
  const { data: soloRankSeasonsData } = useSoloRankSeasons();
  const brawlPassSeasons = useMemo(
    () => brawlPassSeasonsData?.content ?? [],
    [brawlPassSeasonsData],
  );
  const soloRankSeasons = useMemo(
    () => soloRankSeasonsData?.content ?? [],
    [soloRankSeasonsData],
  );

  // 첫 로드 시 default = 가장 최신 시즌 (백엔드는 첫 시즌부터 순서대로 반환)
  useEffect(() => {
    if (brawlPassSeasonNumber === null && brawlPassSeasons.length > 0) {
      setBrawlPassSeasonNumber(brawlPassSeasons[brawlPassSeasons.length - 1].number);
    }
  }, [brawlPassSeasons, brawlPassSeasonNumber]);
  useEffect(() => {
    if (soloRankSeasonKey === null && soloRankSeasons.length > 0) {
      setSoloRankSeasonKey(soloRankSeasons[soloRankSeasons.length - 1].key);
    }
  }, [soloRankSeasons, soloRankSeasonKey]);

  const brawlerQuery = usePlayerBrawlerStatistics({
    tag, battleType, brawlPassSeasonNumber, soloRankSeasonKey,
  });
  const roleQuery = usePlayerBrawlerRoleStatistics({
    tag, battleType, brawlPassSeasonNumber, soloRankSeasonKey,
  });

  const brawlerItems = (brawlerQuery.data?.items ?? []).map(toPlayerBrawlerStat);
  const roleItems = (roleQuery.data?.items ?? []).map(toPlayerBrawlerRoleStat);

  return (
    <div className="space-y-4 py-4">
      <PlayerStatisticsControls
        battleType={battleType}
        onBattleTypeChange={setBattleType}
        brawlPassSeasons={brawlPassSeasons}
        soloRankSeasons={soloRankSeasons}
        brawlPassSeasonNumber={brawlPassSeasonNumber}
        soloRankSeasonKey={soloRankSeasonKey}
        onBrawlPassChange={setBrawlPassSeasonNumber}
        onSoloRankChange={setSoloRankSeasonKey}
      />
      <PlayerBrawlerRoleSummary items={roleItems} />
      <PlayerBrawlerStatsList items={brawlerItems} />
    </div>
  );
}
