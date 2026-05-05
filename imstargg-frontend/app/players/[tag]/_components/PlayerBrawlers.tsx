"use client";

import { useState } from "react";
import { ArrowDown, ArrowUp } from "lucide-react";
import type { PlayerBrawler } from "@/domain/player/types";
import {
  sortPlayerBrawlers,
  type PlayerBrawlerSortKey,
  type SortDirection,
} from "@/domain/player/sortPlayerBrawlers";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { PlayerBrawlerCard } from "./PlayerBrawlerCard";

const SORT_OPTIONS: ReadonlyArray<{ value: PlayerBrawlerSortKey; label: string }> = [
  { value: "trophies", label: "트로피" },
  { value: "power", label: "파워레벨" },
  { value: "rarity", label: "희귀도" },
  { value: "name", label: "이름" },
  { value: "id", label: "최신순" },
];

export function PlayerBrawlers({ brawlers }: { brawlers: PlayerBrawler[] }) {
  const [key, setKey] = useState<PlayerBrawlerSortKey>("trophies");
  const [dir, setDir] = useState<SortDirection>("desc");

  if (brawlers.length === 0) {
    return <p className="text-sm text-muted-foreground">보유한 브롤러가 없습니다.</p>;
  }

  const sorted = sortPlayerBrawlers(brawlers, key, dir);

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center gap-2">
        <Select value={key} onValueChange={(v) => setKey(v as PlayerBrawlerSortKey)}>
          <SelectTrigger className="w-32" aria-label="정렬 기준">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {SORT_OPTIONS.map((o) => (
              <SelectItem key={o.value} value={o.value}>
                {o.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Button
          variant="outline"
          size="icon"
          aria-label={dir === "asc" ? "오름차순" : "내림차순"}
          onClick={() => setDir((d) => (d === "asc" ? "desc" : "asc"))}
        >
          {dir === "asc" ? <ArrowUp /> : <ArrowDown />}
        </Button>
      </div>
      <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4">
        {sorted.map((pb) => (
          <li key={`${pb.brawler?.id ?? "x"}-${pb.brawler?.name ?? "x"}`}>
            <PlayerBrawlerCard pb={pb} />
          </li>
        ))}
      </ul>
    </div>
  );
}
