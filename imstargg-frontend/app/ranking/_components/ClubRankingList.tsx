import Link from "next/link";
import { Card } from "@/components/ui/card";
import { toUrlPath } from "@/lib/tag";
import type { ClubRanking } from "@/domain/ranking/types";
import { RankBadge } from "@app/ranking/_components/RankBadge";

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
            <Card className="flex items-center gap-3 p-3 transition hover:border-foreground">
              <div className="w-20 shrink-0">
                <RankBadge rank={r.rank} rankChange={r.rankChange} />
              </div>
              <div className="min-w-0 flex-1">
                <div className="truncate font-bold">{r.name}</div>
                <div className="text-xs text-muted-foreground">
                  멤버 {r.memberCount}명
                </div>
              </div>
              <div className="shrink-0 text-right tabular-nums">
                <div className="text-sm font-bold">
                  {r.trophies.toLocaleString("ko-KR")}
                </div>
                <div className="text-[10px] text-muted-foreground">트로피</div>
              </div>
            </Card>
          </Link>
        </li>
      ))}
    </ul>
  );
}
