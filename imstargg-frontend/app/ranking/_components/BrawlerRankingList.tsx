import Link from "next/link";
import { Card } from "@/components/ui/card";
import { toUrlPath } from "@/lib/tag";
import type { BrawlerRanking } from "@/domain/ranking/types";
import Trophy from "@/components/primitives/Trophy";
import { RankBadge } from "@app/ranking/_components/RankBadge";

type Props = {
  rankings: BrawlerRanking[];
};

export function BrawlerRankingList({ rankings }: Props) {
  if (rankings.length === 0) {
    return (
      <Card className="p-8 text-center text-muted-foreground">
        랭킹 데이터가 없습니다
      </Card>
    );
  }
  return (
    <ul className="space-y-2">
      {rankings.map((r) => (
        <li key={r.player.tag}>
          <Link href={`/players/${toUrlPath(r.player.tag)}`} className="block">
            <Card className="flex flex-row items-center gap-3 p-3 transition hover:border-foreground">
              <div className="w-20 shrink-0">
                <RankBadge rank={r.rank} rankChange={r.rankChange} />
              </div>
              <div className="min-w-0 flex-1">
                <div className="truncate font-bold">{r.player.name}</div>
                {r.player.clubName && (
                  <div className="truncate text-xs text-muted-foreground">
                    {r.player.clubName}
                  </div>
                )}
              </div>
              <div className="shrink-0">
                <Trophy value={r.trophies} size={16} />
              </div>
            </Card>
          </Link>
        </li>
      ))}
    </ul>
  );
}
