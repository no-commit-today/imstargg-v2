import Link from "next/link";
import type { PlayerRanking } from "@/domain/ranking/types";
import { toUrlPath } from "@/lib/tag";
import Trophy from "@/components/primitives/Trophy";

type Props = { rankings: PlayerRanking[] };

export function HomeRankingSection({ rankings }: Props) {
  const top = rankings.slice(0, 10);
  if (top.length === 0) return null;
  return (
    <section className="space-y-3" aria-labelledby="home-ranking-title">
      <header className="flex items-end justify-between">
        <div>
          <h2 id="home-ranking-title" className="section-title">
            글로벌 트로피 랭킹
          </h2>
          <p className="ml-[14px] text-xs text-[#5C6271]">
            지금 가장 높은 트로피를 보유한 플레이어
          </p>
        </div>
        <Link href="/players" className="text-sm font-semibold text-[#2E7CF6]">
          더 보기
        </Link>
      </header>
      <ol className="data-table divide-y divide-[#ECEEF3]">
        {top.map((r) => (
          <li key={r.player.tag}>
            <Link
              href={`/players/${toUrlPath(r.player.tag)}`}
              className="grid grid-cols-[44px_minmax(0,1fr)_110px] items-center gap-2 px-4 py-2.5"
            >
              <span className="font-num font-bold tabular-nums">{r.rank}</span>
              <span className="truncate font-semibold">{r.player.name}</span>
              <span className="flex justify-end">
                <Trophy value={r.trophies} size={14} />
              </span>
            </Link>
          </li>
        ))}
      </ol>
    </section>
  );
}
