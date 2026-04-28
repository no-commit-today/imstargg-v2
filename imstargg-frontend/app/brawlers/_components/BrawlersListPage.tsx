"use client";

import { useMemo, useState } from "react";
import type { BrawlerRarity, BrawlerRole } from "@/api/types";
import type { Brawler } from "@/domain/brawler/types";
import { useBrawlers } from "@/hooks/queries/useBrawlers";
import { filterBrawlers, sortByName } from "@/domain/brawler/filters";
import { BrawlerCard } from "./BrawlerCard";
import { BrawlerFilters } from "./BrawlerFilters";

export default function BrawlersListPage({
  initialBrawlers,
}: {
  initialBrawlers: Brawler[];
}) {
  const { data: brawlers = initialBrawlers } = useBrawlers({ initialData: initialBrawlers });
  const [role, setRole] = useState<BrawlerRole | "ALL">("ALL");
  const [rarity, setRarity] = useState<BrawlerRarity | "ALL">("ALL");
  const [search, setSearch] = useState("");

  const filtered = useMemo(() => {
    const opts = {
      role: role === "ALL" ? undefined : role,
      rarity: rarity === "ALL" ? undefined : rarity,
      search: search.trim() || undefined,
    };
    return sortByName(filterBrawlers(brawlers, opts));
  }, [brawlers, role, rarity, search]);

  return (
    <main className="mx-auto w-full max-w-6xl px-4 py-6 space-y-5">
      <header>
        <div className="text-[11px] font-bold uppercase tracking-[0.12em] text-muted-foreground">
          BRAWLERS
        </div>
        <h1 className="font-display text-3xl tracking-[0.02em]">브롤러 도감</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          {brawlers.length}명의 브롤러 · 가젯, 스타파워, 기어, 하이퍼차지 정보
        </p>
      </header>

      <BrawlerFilters
        role={role}
        rarity={rarity}
        search={search}
        onRoleChange={setRole}
        onRarityChange={setRarity}
        onSearchChange={setSearch}
      />

      {filtered.length === 0 ? (
        <p className="py-16 text-center text-sm text-muted-foreground">
          조건에 맞는 브롤러가 없어요.
        </p>
      ) : (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5">
          {filtered.map((b) => (
            <BrawlerCard key={b.id} brawler={b} />
          ))}
        </div>
      )}
    </main>
  );
}
