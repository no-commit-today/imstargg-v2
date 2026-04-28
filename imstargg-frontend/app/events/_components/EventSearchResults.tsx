"use client";

import type { BattleEvent } from "@/domain/event/types";
import { SoloRankCard } from "./SoloRankCard";

interface Props {
  results: BattleEvent[] | undefined;
  loading: boolean;
}

export function EventSearchResults({ results, loading }: Props) {
  if (loading && !results) {
    return (
      <p className="py-12 text-center text-sm text-muted-foreground">검색 중…</p>
    );
  }
  if (!results || results.length === 0) {
    return (
      <p className="py-12 text-center text-sm text-muted-foreground">
        결과 없음
      </p>
    );
  }
  return (
    <div className="grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-3">
      {results.map((e) => (
        <SoloRankCard key={e.id} event={e} />
      ))}
    </div>
  );
}
