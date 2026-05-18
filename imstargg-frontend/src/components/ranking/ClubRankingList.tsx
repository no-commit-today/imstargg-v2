import Link from "next/link";
import { Card } from "@/components/ui/card";
import { toUrlPath } from "@/lib/tag";
import type { ClubRanking } from "@/domain/ranking/types";
import Trophy from "@/components/primitives/Trophy";
import { RankBadge } from "@/components/ranking/RankBadge";

type Props = {
  rankings: ClubRanking[];
};

export function ClubRankingList({ rankings }: Props) {
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
        <li key={r.tag}>
          <Link href={`/clubs/${toUrlPath(r.tag)}`} className="block">
            <Card className="flex flex-row items-center gap-3 p-3 transition hover:border-foreground">
              <div className="w-20 shrink-0">
                <RankBadge rank={r.rank} rankChange={r.rankChange} />
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-1.5 truncate font-bold">
                  <img src="/assets/icon_club_league_masters.png" alt="" width={16} height={16} />
                  {r.name}
                </div>
                <div className="text-xs text-muted-foreground">
                  멤버 {r.memberCount}명
                </div>
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
