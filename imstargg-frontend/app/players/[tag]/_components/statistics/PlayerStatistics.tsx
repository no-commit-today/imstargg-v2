"use client";

import { useEffect, useMemo, useState } from "react";
import { useBrawlPassSeasons, useSoloRankSeasons } from "@/hooks/queries/useSeasons";
import {
  usePlayerBrawlerStatistics,
  usePlayerBrawlerRoleStatistics,
  type PlayerStatBattleType,
} from "@/hooks/queries/usePlayerStatistics";
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

  const brawlerItems = brawlerQuery.data ?? [];
  const roleItems = roleQuery.data ?? [];

  const isError = brawlerQuery.isError || roleQuery.isError;

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
      {isError ? (
        <div className="rounded-lg border border-[#D8DCE5] bg-white px-4 py-8 text-center text-sm text-[#5C6271]">
          통계를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.
        </div>
      ) : (
        <>
          <PlayerBrawlerRoleSummary items={roleItems} />
          <PlayerBrawlerStatsList items={brawlerItems} />
        </>
      )}
    </div>
  );
}
