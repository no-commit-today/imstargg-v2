import Link from "next/link";
import { routes } from "@/config/routes";
import type { BrawlerRanking } from "@/domain/ranking/types";
import BrawlerTrophy from "@/components/primitives/BrawlerTrophy";
import { PlayerAvatar } from "@/components/primitives/PlayerAvatar";
import { RankNumber } from "@/components/ranking/RankNumber";
import { RankChange } from "@/components/ranking/RankChange";

type Props = {
  rankings: BrawlerRanking[];
};

function railFor(rank: number): string {
  if (rank === 1) return "gold";
  if (rank === 2) return "silver";
  if (rank === 3) return "bronze";
  return "neutral";
}

const GRID =
  "grid grid-cols-[44px_minmax(0,1fr)_56px_88px] items-center gap-2 sm:grid-cols-[64px_minmax(0,1fr)_80px_120px] sm:gap-3";

export function BrawlerRankingList({ rankings }: Props) {
  if (rankings.length === 0) {
    return (
      <div className="data-table">
        <div className="px-4 py-12 text-center text-sm text-[#5C6271]">
          랭킹 데이터가 없습니다
        </div>
      </div>
    );
  }
  return (
    <div className="data-table">
      <div className={`data-table-head ${GRID}`}>
        <span>순위</span>
        <span>플레이어</span>
        <span className="text-right">변동</span>
        <span className="text-right">트로피</span>
      </div>
      {rankings.map((r) => (
        <Link
          key={r.player.tag}
          href={routes.player(r.player.tag)}
          data-rail={railFor(r.rank)}
          className={`data-row text-[13px] sm:text-sm ${GRID}`}
        >
          <span className="flex justify-center">
            <RankNumber rank={r.rank} />
          </span>
          <span className="flex min-w-0 items-center gap-2">
            <PlayerAvatar name={r.player.name} size={32} />
            <span className="flex min-w-0 flex-col">
              <span
                className="truncate font-bold"
                style={r.player.nameColor ? { color: r.player.nameColor } : undefined}
              >
                {r.player.name}
              </span>
              {r.player.clubName && (
                <span className="truncate text-[11px] text-[#5C6271]">
                  {r.player.clubName}
                </span>
              )}
            </span>
          </span>
          <span className="flex justify-end">
            <RankChange change={r.rankChange} />
          </span>
          <span className="flex justify-end">
            <BrawlerTrophy value={r.trophies} size={14} />
          </span>
        </Link>
      ))}
    </div>
  );
}
