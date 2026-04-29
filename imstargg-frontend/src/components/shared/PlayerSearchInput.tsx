"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Search } from "lucide-react";
import { isValidTag } from "@/lib/tag";
import { routes } from "@/config/routes";
import { toast } from "sonner";
import {
  addRecent,
  clearRecents,
  getRecents,
  type SearchEntry,
  type SearchEntryKind,
} from "@/lib/searchHistory";
import { getFavorites, isFavorite as readIsFavorite, toggleFavorite } from "@/lib/favorites";
import { SearchHistoryDropdown } from "@/components/shared/SearchHistoryDropdown";

export function PlayerSearchInput({ defaultValue = "" }: { defaultValue?: string }) {
  const router = useRouter();
  const [value, setValue] = useState(defaultValue);
  const [open, setOpen] = useState(false);
  const [recents, setRecents] = useState<SearchEntry[]>(() => getRecents());
  const [favorites, setFavorites] = useState<SearchEntry[]>(() => getFavorites());
  const wrapperRef = useRef<HTMLDivElement | null>(null);

  function refresh() {
    setRecents(getRecents());
    setFavorites(getFavorites());
  }

  useEffect(() => {
    function onMouseDown(e: MouseEvent) {
      if (!wrapperRef.current) return;
      if (!wrapperRef.current.contains(e.target as Node)) setOpen(false);
    }
    document.addEventListener("mousedown", onMouseDown);
    return () => document.removeEventListener("mousedown", onMouseDown);
  }, []);

  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const trimmed = value.trim();
    if (!isValidTag(trimmed)) {
      toast.error("올바른 플레이어 태그를 입력해 주세요. 예: #88J8Q9RPV");
      return;
    }
    addRecent({ kind: "player", tag: trimmed });
    setOpen(false);
    router.push(routes.player(trimmed));
  }

  function handleToggleFavorite(entry: { kind: SearchEntryKind; tag: string }) {
    toggleFavorite(entry);
    refresh();
  }

  function handleClearRecents() {
    clearRecents();
    refresh();
  }

  return (
    <div ref={wrapperRef} style={{ position: "relative", width: "100%", maxWidth: 448 }}>
      <form onSubmit={handleSubmit} className="flex w-full items-center gap-2">
        <Input
          type="text"
          inputMode="text"
          autoCapitalize="characters"
          spellCheck={false}
          placeholder="#PLAYER_TAG"
          value={value}
          onChange={(e) => setValue(e.target.value)}
          onFocus={() => setOpen(true)}
          aria-label="플레이어 태그 입력"
          aria-expanded={open}
          aria-controls="search-history-dropdown"
        />
        <Button type="submit" size="default" aria-label="플레이어 검색">
          <Search className="size-4" />
        </Button>
      </form>
      {open && (
        <div id="search-history-dropdown">
          <SearchHistoryDropdown
            recents={recents}
            favorites={favorites}
            onSelect={() => setOpen(false)}
            onToggleFavorite={handleToggleFavorite}
            onClearRecents={handleClearRecents}
            isFavorite={readIsFavorite}
          />
        </div>
      )}
    </div>
  );
}
