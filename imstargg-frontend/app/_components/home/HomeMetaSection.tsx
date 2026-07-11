import Link from "next/link";
import type { BrawlerResultStat } from "@/domain/statistics/types";
import { PercentCell } from "@app/_components/statistics/StatNumberCell";

type Props = { stats: BrawlerResultStat[] };

export function HomeMetaSection({ stats }: Props) {
  const top = stats
    .filter((s) => s.brawler !== null)
    .sort((a, b) => b.winRate - a.winRate)
    .slice(0, 10);
  if (top.length === 0) return null;
  return (
    <section className="space-y-3" aria-labelledby="home-meta-title">
      <header className="flex items-end justify-between">
        <div>
          <h2 id="home-meta-title" className="section-title">
            브롤러 메타 TOP 10
          </h2>
          <p className="ml-[14px] text-xs text-[#5C6271]">
            전체 트로피 구간 승률 상위 브롤러
          </p>
        </div>
        <Link href="/brawlers" className="text-sm font-semibold text-[#2E7CF6]">
          더 보기
        </Link>
      </header>
      <ol className="data-table divide-y divide-[#ECEEF3]">
        {top.map((s, i) => (
          <li key={s.brawler!.id}>
            <Link
              href={`/brawlers/${s.brawler!.id}`}
              className="grid grid-cols-[44px_minmax(0,1fr)_88px_88px] items-center gap-2 px-4 py-2.5"
            >
              <span className="font-num font-bold tabular-nums">{i + 1}</span>
              <span className="truncate font-semibold">{s.brawler!.name}</span>
              <span className="text-right">
                <PercentCell value={s.winRate} />
              </span>
              <span className="text-right text-[#5C6271]">
                <PercentCell value={s.pickRate} />
              </span>
            </Link>
          </li>
        ))}
      </ol>
    </section>
  );
}
