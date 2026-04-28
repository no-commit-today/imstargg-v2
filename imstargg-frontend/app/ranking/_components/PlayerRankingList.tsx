import Link from "next/link";
import { Card } from "@/components/ui/card";
import { toUrlPath } from "@/lib/tag";
import type { PlayerRanking } from "@/domain/ranking/types";
import { RankBadge } from "@app/ranking/_components/RankBadge";

type Props = {
  rankings: PlayerRanking[];
};

export function PlayerRankingList({ rankings }: Props) {
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
            <Card className="flex items-center gap-3 p-3 transition hover:border-foreground">
              <div className="w-20 shrink-0">
                <RankBadge rank={r.rank} rankChange={r.rankChange} />
              </div>
              <div className="min-w-0 flex-1">
                <div
                  className="truncate font-bold"
                  style={
                    r.player.nameColor
                      ? { color: hexFromColorCode(r.player.nameColor) }
                      : undefined
                  }
                >
                  {r.player.name}
                </div>
                {r.player.clubName && (
                  <div className="truncate text-xs text-muted-foreground">
                    {r.player.clubName}
                  </div>
                )}
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

function hexFromColorCode(code: string): string {
  // 백엔드 nameColor: "0xffXXXXXX" 형식 — 앞 4자(0x + alpha) 제거 후 # prefix
  const m = /^0x[0-9a-fA-F]{2}([0-9a-fA-F]{6})$/.exec(code.trim());
  return m ? `#${m[1]}` : "inherit";
}
