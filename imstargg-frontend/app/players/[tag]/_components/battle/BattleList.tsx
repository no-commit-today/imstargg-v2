"use client";

import { Button } from "@/components/ui/button";
import { BattleRow } from "./BattleRow";
import { BattleSkeleton } from "./BattleSkeleton";
import { usePlayerBattles } from "@/hooks/queries/usePlayerBattles";

interface Props {
  tag: string;
}

export function BattleList({ tag }: Props) {
  const {
    data, isLoading, isError, error,
    fetchNextPage, hasNextPage, isFetchingNextPage, refetch,
  } = usePlayerBattles(tag);

  if (isLoading) return <BattleSkeleton />;

  if (isError) {
    return (
      <div className="card" style={{ padding: 24, textAlign: "center" }}>
        <p style={{ color: "#5C6271", marginBottom: 12 }}>
          배틀 기록을 불러오지 못했어요.
        </p>
        <p style={{ color: "#8A90A0", fontSize: 12, marginBottom: 12 }}>
          {(error as Error)?.message ?? ""}
        </p>
        <Button variant="outline" onClick={() => refetch()}>다시 시도</Button>
      </div>
    );
  }

  const battles = data?.pages.flat() ?? [];
  if (battles.length === 0) {
    return (
      <div className="card" style={{ padding: 32, textAlign: "center", color: "#5C6271" }}>
        최근 배틀 기록이 없어요
      </div>
    );
  }

  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
      {battles.map((b, i) => (
        <BattleRow key={`${b.battleTime}-${i}`} battle={b} ownerTag={tag} />
      ))}
      {hasNextPage ? (
        <div style={{ display: "flex", justifyContent: "center", padding: "16px 0" }}>
          <Button
            variant="outline"
            onClick={() => fetchNextPage()}
            disabled={isFetchingNextPage}
          >
            {isFetchingNextPage ? "불러오는 중..." : "더 보기"}
          </Button>
        </div>
      ) : (
        <div style={{ textAlign: "center", padding: "12px 0", color: "#8A90A0", fontSize: 12 }}>
          더 이상 기록이 없어요
        </div>
      )}
    </div>
  );
}
