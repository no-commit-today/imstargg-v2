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
  const [recents, setRecents] = useState<SearchEntry[]>([]);
  const [favorites, setFavorites] = useState<SearchEntry[]>([]);
  const [activeIndex, setActiveIndex] = useState<number>(-1);
  const wrapperRef = useRef<HTMLDivElement | null>(null);

  // Hydrate from localStorage after mount to avoid SSR/CSR mismatch.
  // The hydration risk (server renders [] vs client real data) outweighs the
  // cascading-render concern for a dropdown that only opens on user interaction.
  useEffect(() => {
    setRecents(getRecents());
    setFavorites(getFavorites());
  }, []);

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

  // Reset highlighted entry whenever the visible list or open state changes.
  useEffect(() => {
    setActiveIndex(-1);
  }, [open, recents, favorites, value]);

  const flatEntries: SearchEntry[] = [...favorites, ...recents];

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

  function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (!open || flatEntries.length === 0) return;
    if (e.key === "ArrowDown") {
      e.preventDefault();
      setActiveIndex((i) => (i + 1) % flatEntries.length);
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setActiveIndex((i) => (i <= 0 ? flatEntries.length - 1 : i - 1));
    } else if (e.key === "Escape") {
      setOpen(false);
      setActiveIndex(-1);
    } else if (e.key === "Enter" && activeIndex >= 0) {
      e.preventDefault();
      const entry = flatEntries[activeIndex];
      addRecent({ kind: entry.kind, tag: entry.tag });
      setOpen(false);
      router.push(entry.kind === "player" ? routes.player(entry.tag) : routes.club(entry.tag));
    }
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
          onKeyDown={handleKeyDown}
          aria-label="플레이어 태그 입력"
          role="combobox"
          aria-expanded={open}
          aria-haspopup="menu"
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
            activeIndex={activeIndex}
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
