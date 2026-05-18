"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Search } from "lucide-react";
import {
  addRecent,
  clearRecents,
  getRecents,
  type SearchEntry,
  type SearchEntryKind,
} from "@/lib/searchHistory";
import { getFavorites, isFavorite as readIsFavorite, toggleFavorite } from "@/lib/favorites";
import { SearchHistoryDropdown } from "@/components/shared/SearchHistoryDropdown";

const DEBOUNCE_MS = 250;
const MIN_QUERY_LEN = 2;

export interface SearchableInputResult {
  kind: SearchEntryKind;
  tag: string;
  name: string;
  trophies: number;
}

export type SearchableInputVariant = "default" | "topnav";

interface Props {
  placeholder: string;
  inputAriaLabel: string;
  buttonAriaLabel: string;
  defaultValue?: string;
  fetchLiveResults: (q: string) => Promise<SearchableInputResult[]>;
  renderResult: (item: SearchableInputResult, active: boolean) => React.ReactNode;
  toDetailHref: (item: SearchableInputResult) => string;
  toSearchHref: (q: string) => string;
  variant?: SearchableInputVariant;
}

export function SearchableInput({
  placeholder,
  inputAriaLabel,
  buttonAriaLabel,
  defaultValue = "",
  fetchLiveResults,
  renderResult,
  toDetailHref,
  toSearchHref,
  variant = "default",
}: Props) {
  const router = useRouter();
  const [value, setValue] = useState(defaultValue);
  const [open, setOpen] = useState(false);
  const [recents, setRecents] = useState<SearchEntry[]>([]);
  const [favorites, setFavorites] = useState<SearchEntry[]>([]);
  const [liveResults, setLiveResults] = useState<SearchableInputResult[]>([]);
  const [activeIndex, setActiveIndex] = useState(-1);
  const wrapperRef = useRef<HTMLDivElement | null>(null);
  const abortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    setRecents(getRecents());
    setFavorites(getFavorites());
  }, []);

  useEffect(() => {
    function onMouseDown(e: MouseEvent) {
      if (!wrapperRef.current) return;
      if (!wrapperRef.current.contains(e.target as Node)) setOpen(false);
    }
    document.addEventListener("mousedown", onMouseDown);
    return () => document.removeEventListener("mousedown", onMouseDown);
  }, []);

  // 디바운스된 라이브 검색
  useEffect(() => {
    const trimmed = value.trim();
    if (trimmed.length < MIN_QUERY_LEN) {
      setLiveResults([]);
      return;
    }
    const handle = setTimeout(async () => {
      abortRef.current?.abort();
      const ctrl = new AbortController();
      abortRef.current = ctrl;
      try {
        const results = await fetchLiveResults(trimmed);
        if (!ctrl.signal.aborted) setLiveResults(results);
      } catch (err) {
        if (!(err instanceof DOMException && err.name === "AbortError")) {
          // 라이브 검색 실패는 조용히 무시 — 사용자는 Enter 로 결과 페이지 진입 가능
        }
      }
    }, DEBOUNCE_MS);
    return () => clearTimeout(handle);
  }, [value, fetchLiveResults]);

  useEffect(() => {
    setActiveIndex(-1);
  }, [open, liveResults, recents, favorites]);

  function refresh() {
    setRecents(getRecents());
    setFavorites(getFavorites());
  }

  const showingLive = value.trim().length >= MIN_QUERY_LEN;
  const navItems: Array<{ entry: SearchableInputResult; href: string }> = showingLive
    ? liveResults.map((r) => ({ entry: r, href: toDetailHref(r) }))
    : [];

  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const trimmed = value.trim();
    if (!trimmed) return;
    setOpen(false);
    router.push(toSearchHref(trimmed));
  }

  function handleSelectLive(item: SearchableInputResult) {
    addRecent({ kind: item.kind, tag: item.tag, name: item.name, trophies: item.trophies });
    setOpen(false);
    router.push(toDetailHref(item));
  }

  function handleToggleFavorite(entry: Omit<SearchEntry, "addedAt">) {
    toggleFavorite(entry);
    refresh();
  }

  function handleClearRecents() {
    clearRecents();
    refresh();
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (!open) return;
    if (showingLive) {
      if (navItems.length === 0) return;
      if (e.key === "ArrowDown") {
        e.preventDefault();
        setActiveIndex((i) => (i + 1) % navItems.length);
      } else if (e.key === "ArrowUp") {
        e.preventDefault();
        setActiveIndex((i) => (i <= 0 ? navItems.length - 1 : i - 1));
      } else if (e.key === "Enter" && activeIndex >= 0) {
        e.preventDefault();
        handleSelectLive(navItems[activeIndex].entry);
      }
      return;
    }
    // history 모드
    const flat = [...favorites, ...recents];
    if (flat.length === 0) return;
    if (e.key === "ArrowDown") {
      e.preventDefault();
      setActiveIndex((i) => (i + 1) % flat.length);
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setActiveIndex((i) => (i <= 0 ? flat.length - 1 : i - 1));
    } else if (e.key === "Enter" && activeIndex >= 0) {
      e.preventDefault();
      const entry = flat[activeIndex];
      const href =
        entry.kind === "player"
          ? `/players/${entry.tag.replace(/^#/, "")}`
          : `/clubs/${entry.tag.replace(/^#/, "")}`;
      setOpen(false);
      router.push(href);
    }
  }

  const wrapperStyle: React.CSSProperties =
    variant === "topnav"
      ? { position: "relative" }
      : { position: "relative", width: "100%", maxWidth: 448 };
  const wrapperClassName = variant === "topnav" ? "topnav-search-wrap" : undefined;

  const commonInputProps = {
    value,
    onChange: (e: React.ChangeEvent<HTMLInputElement>) => setValue(e.target.value),
    onFocus: () => setOpen(true),
    onKeyDown: handleKeyDown,
    placeholder,
    spellCheck: false,
    "aria-label": inputAriaLabel,
    role: "combobox" as const,
    "aria-expanded": open,
    "aria-haspopup": "menu" as const,
    "aria-controls": "search-dropdown",
  };

  return (
    <div ref={wrapperRef} className={wrapperClassName} style={wrapperStyle}>
      {variant === "topnav" ? (
        <form onSubmit={handleSubmit} className="topnav-search">
          <Search aria-hidden="true" />
          <input type="text" inputMode="text" {...commonInputProps} />
          <button
            type="submit"
            className="topnav-search-submit"
            aria-label={buttonAriaLabel}
          >
            <Search aria-hidden="true" />
          </button>
        </form>
      ) : (
        <form onSubmit={handleSubmit} className="flex w-full items-center gap-2">
          <Input type="text" inputMode="text" {...commonInputProps} />
          <Button type="submit" size="default" aria-label={buttonAriaLabel}>
            <Search className="size-4" />
          </Button>
        </form>
      )}
      {open && (
        <div id="search-dropdown">
          {showingLive ? (
            <LiveResultsDropdown
              items={navItems}
              activeIndex={activeIndex}
              renderResult={renderResult}
              onSelect={handleSelectLive}
              variant={variant}
            />
          ) : (
            <SearchHistoryDropdown
              recents={recents}
              favorites={favorites}
              activeIndex={activeIndex}
              onSelect={() => setOpen(false)}
              onToggleFavorite={handleToggleFavorite}
              onClearRecents={handleClearRecents}
              isFavorite={readIsFavorite}
              variant={variant}
            />
          )}
        </div>
      )}
    </div>
  );
}

function dropdownPositionStyle(variant: SearchableInputVariant): React.CSSProperties {
  if (variant === "topnav") {
    return {
      position: "absolute",
      top: "100%",
      right: 0,
      marginTop: 6,
      width: 320,
      maxWidth: "calc(100vw - 24px)",
      background: "#fff",
      border: "1px solid #D8DCE5",
      borderRadius: 12,
      boxShadow: "0 12px 32px rgba(20,22,27,0.12)",
      zIndex: 100,
    };
  }
  return {
    position: "absolute",
    top: "100%",
    left: 0,
    right: 0,
    marginTop: 4,
    background: "#fff",
    border: "1px solid #D8DCE5",
    borderRadius: 12,
    boxShadow: "0 12px 32px rgba(20,22,27,0.12)",
    zIndex: 50,
  };
}

function LiveResultsDropdown({
  items,
  activeIndex,
  renderResult,
  onSelect,
  variant,
}: {
  items: Array<{ entry: SearchableInputResult; href: string }>;
  activeIndex: number;
  renderResult: (item: SearchableInputResult, active: boolean) => React.ReactNode;
  onSelect: (item: SearchableInputResult) => void;
  variant: SearchableInputVariant;
}) {
  const baseStyle = dropdownPositionStyle(variant);
  if (items.length === 0) {
    return (
      <div
        role="menu"
        style={{
          ...baseStyle,
          padding: 12,
          color: "#8A90A0",
          fontSize: 13,
        }}
      >
        검색 결과가 없습니다
      </div>
    );
  }
  return (
    <ul
      role="menu"
      style={{
        ...baseStyle,
        padding: 8,
        maxHeight: 360,
        overflowY: "auto",
        listStyle: "none",
        margin: 0,
      }}
    >
      {items.map((it, idx) => {
        const active = idx === activeIndex;
        return (
          <li key={`${it.entry.kind}:${it.entry.tag}`}>
            <button
              type="button"
              role="menuitem"
              onClick={() => onSelect(it.entry)}
              style={{
                display: "block",
                width: "100%",
                textAlign: "left",
                background: active ? "rgba(255,198,10,0.15)" : "transparent",
                border: "none",
                borderRadius: 6,
                padding: "6px 8px",
                cursor: "pointer",
              }}
            >
              {renderResult(it.entry, active)}
            </button>
          </li>
        );
      })}
    </ul>
  );
}
