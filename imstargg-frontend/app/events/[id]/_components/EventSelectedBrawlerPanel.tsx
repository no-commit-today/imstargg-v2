"use client";

import type { BattleEventModeResultType } from "@/api/types";
import { useEventSelectedStats } from "@/hooks/queries/useEventStats";
import type { StatsFilter } from "@/hooks/queries/useBrawlerStats";
import { PairStatsTable } from "@app/brawlers/[id]/_components/PairStatsTable";
import { EnemyStatsTable } from "@app/brawlers/[id]/_components/EnemyStatsTable";
import { PairRankTable } from "./PairRankTable";

type Props = {
  eventId: number;
  brawlerId: number;
  resultType: BattleEventModeResultType;
  filter: StatsFilter;
  onClose: () => void;
};

function LoadingBox() {
  return (
    <div className="rounded-lg border border-[#D8DCE5] bg-white px-4 py-8 text-center text-sm text-[#5C6271]">
      불러오는 중…
    </div>
  );
}

export function EventSelectedBrawlerPanel({
  eventId,
  brawlerId,
  resultType,
  filter,
  onClose,
}: Props) {
  const { pairResult, enemyResult, pairRank } = useEventSelectedStats(
    eventId,
    brawlerId,
    resultType,
    filter,
  );

  const isRank = resultType === "RANK";
  const loading = isRank
    ? pairRank.isPending
    : pairResult.isPending || enemyResult.isPending;

  return (
    <section className="space-y-3">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-bold">선택한 브롤러 분석</h3>
        <button
          type="button"
          onClick={onClose}
          className="text-xs font-bold text-[#5C6271] hover:text-[#14161B]"
        >
          닫기 ✕
        </button>
      </div>
      {loading ? (
        <LoadingBox />
      ) : (
        <div className="grid grid-cols-1 gap-3 lg:grid-cols-2">
          {isRank ? (
            <>
              <PairRankTable items={pairRank.data ?? []} />
              <div className="rounded-lg border border-[#D8DCE5] bg-white px-4 py-8 text-center text-sm text-[#5C6271]">
                카운터는 결과형 모드 전용입니다.
              </div>
            </>
          ) : (
            <>
              <PairStatsTable items={pairResult.data ?? []} />
              <EnemyStatsTable items={enemyResult.data ?? []} />
            </>
          )}
        </div>
      )}
    </section>
  );
}
