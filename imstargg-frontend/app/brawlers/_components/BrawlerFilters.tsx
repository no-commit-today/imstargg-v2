"use client";

import type { BrawlerRarity, BrawlerRole } from "@/api/types";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Search } from "lucide-react";

const ROLES: ReadonlyArray<{ value: BrawlerRole | "ALL"; label: string }> = [
  { value: "ALL", label: "전체" },
  { value: "DAMAGE_DEALER", label: "어태커" },
  { value: "TANK", label: "탱커" },
  { value: "MARKSMAN", label: "마크스맨" },
  { value: "ASSASSIN", label: "어쌔신" },
  { value: "SUPPORT", label: "서포터" },
  { value: "CONTROLLER", label: "컨트롤러" },
  { value: "ARTILLERY", label: "아틸러리" },
];

const RARITIES: ReadonlyArray<{ value: BrawlerRarity | "ALL"; label: string }> = [
  { value: "ALL", label: "전체 희귀도" },
  { value: "COMMON", label: "기본" },
  { value: "RARE", label: "레어" },
  { value: "SUPER_RARE", label: "슈퍼 레어" },
  { value: "EPIC", label: "에픽" },
  { value: "MYTHIC", label: "미식" },
  { value: "LEGENDARY", label: "레전더리" },
  { value: "ULTRA_LEGENDARY", label: "울트라 레전더리" },
];

interface Props {
  role: BrawlerRole | "ALL";
  rarity: BrawlerRarity | "ALL";
  search: string;
  onRoleChange: (v: BrawlerRole | "ALL") => void;
  onRarityChange: (v: BrawlerRarity | "ALL") => void;
  onSearchChange: (v: string) => void;
}

export function BrawlerFilters({
  role,
  rarity,
  search,
  onRoleChange,
  onRarityChange,
  onSearchChange,
}: Props) {
  return (
    <div className="space-y-3">
      <div className="-mx-1 flex gap-1 overflow-x-auto px-1 pb-1">
        {ROLES.map((r) => {
          const active = role === r.value;
          return (
            <button
              key={r.value}
              type="button"
              onClick={() => onRoleChange(r.value)}
              className={
                active
                  ? "shrink-0 rounded border border-foreground bg-foreground px-3 py-1.5 text-xs font-bold text-background"
                  : "shrink-0 rounded border border-border bg-background px-3 py-1.5 text-xs font-bold text-foreground hover:border-foreground"
              }
            >
              {r.label}
            </button>
          );
        })}
      </div>
      <div className="flex flex-wrap items-center gap-2">
        <Select value={rarity} onValueChange={(v) => onRarityChange(v as BrawlerRarity | "ALL")}>
          <SelectTrigger className="w-44" aria-label="희귀도 필터">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {RARITIES.map((r) => (
              <SelectItem key={r.value} value={r.value}>
                {r.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <div className="relative flex-1 min-w-[180px]">
          <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={search}
            onChange={(e) => onSearchChange(e.target.value)}
            placeholder="브롤러 이름 검색"
            className="pl-9"
            aria-label="브롤러 이름 검색"
          />
        </div>
      </div>
    </div>
  );
}
