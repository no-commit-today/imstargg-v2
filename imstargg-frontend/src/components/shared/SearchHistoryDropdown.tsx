"use client";

import Link from "next/link";
import type { SearchEntry, SearchEntryKind } from "@/lib/searchHistory";
import { routes } from "@/config/routes";

interface Props {
  recents: SearchEntry[];
  favorites: SearchEntry[];
  activeIndex: number; // -1 = nothing active. 0..favorites.length-1 = fav, beyond = recents
  onSelect?: () => void;
  onToggleFavorite: (entry: Pick<SearchEntry, "kind" | "tag">) => void;
  onClearRecents: () => void;
  isFavorite: (kind: SearchEntryKind, tag: string) => boolean;
}

function entryHref(e: SearchEntry): string {
  return e.kind === "player" ? routes.player(e.tag) : routes.club(e.tag);
}

export function SearchHistoryDropdown({
  recents, favorites, activeIndex, onSelect, onToggleFavorite, onClearRecents, isFavorite,
}: Props) {
  if (recents.length === 0 && favorites.length === 0) return null;

  const recentsActiveIndex = activeIndex >= favorites.length ? activeIndex - favorites.length : -1;
  const favoritesActiveIndex = activeIndex < favorites.length ? activeIndex : -1;

  return (
    <div
      role="menu"
      style={{
        position: "absolute",
        top: "100%",
        left: 0,
        right: 0,
        marginTop: 4,
        background: "#fff",
        border: "1px solid #D8DCE5",
        borderRadius: 12,
        boxShadow: "0 12px 32px rgba(20,22,27,0.12)",
        padding: 8,
        zIndex: 50,
        maxHeight: 360,
        overflowY: "auto",
      }}
    >
      {favorites.length > 0 && (
        <Section
          title="즐겨찾기"
          entries={favorites}
          activeIndex={favoritesActiveIndex}
          onSelect={onSelect}
          onToggleFavorite={onToggleFavorite}
          isFavorite={isFavorite}
        />
      )}
      {recents.length > 0 && (
        <Section
          title="최근 검색"
          entries={recents}
          activeIndex={recentsActiveIndex}
          onSelect={onSelect}
          onToggleFavorite={onToggleFavorite}
          isFavorite={isFavorite}
          rightAction={
            <button
              type="button"
              onClick={onClearRecents}
              style={{
                fontSize: 11,
                color: "#5C6271",
                background: "none",
                border: "none",
                cursor: "pointer",
              }}
            >
              지우기
            </button>
          }
        />
      )}
    </div>
  );
}

function Section({
  title, entries, activeIndex, onSelect, onToggleFavorite, isFavorite, rightAction,
}: {
  title: string;
  entries: SearchEntry[];
  activeIndex: number;
  onSelect?: () => void;
  onToggleFavorite: (entry: Pick<SearchEntry, "kind" | "tag">) => void;
  isFavorite: (kind: SearchEntryKind, tag: string) => boolean;
  rightAction?: React.ReactNode;
}) {
  return (
    <div style={{ marginBottom: 4 }}>
      <div
        style={{
          display: "flex", justifyContent: "space-between", alignItems: "center",
          padding: "4px 8px", fontSize: 11, fontWeight: 700,
          color: "#8A90A0", textTransform: "uppercase", letterSpacing: "0.04em",
        }}
      >
        <span>{title}</span>
        {rightAction}
      </div>
      <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
        {entries.map((e, idx) => {
          const fav = isFavorite(e.kind, e.tag);
          const isActive = idx === activeIndex;
          return (
            <li key={`${e.kind}:${e.tag}`}>
              <div
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: 4,
                  background: isActive ? "rgba(255,198,10,0.15)" : "transparent",
                  borderRadius: 6,
                }}
              >
                <Link
                  href={entryHref(e)}
                  role="menuitem"
                  onClick={onSelect}
                  style={{
                    flex: 1, display: "flex", alignItems: "center", gap: 8,
                    padding: "6px 8px", borderRadius: 6, color: "#14161B",
                    textDecoration: "none", fontSize: 13,
                  }}
                >
                  <span
                    style={{
                      fontSize: 10, fontWeight: 700, color: "#5C6271",
                      textTransform: "uppercase", minWidth: 36,
                    }}
                  >
                    {e.kind === "player" ? "PLAYER" : "CLUB"}
                  </span>
                  <span style={{ fontFamily: "var(--font-mono)" }}>{e.tag}</span>
                  {e.label && <span style={{ color: "#5C6271" }}>· {e.label}</span>}
                </Link>
                <button
                  type="button"
                  aria-pressed={fav}
                  aria-label={fav ? "즐겨찾기 제거" : "즐겨찾기에 추가"}
                  onClick={(ev) => {
                    ev.preventDefault();
                    ev.stopPropagation();
                    onToggleFavorite({ kind: e.kind, tag: e.tag });
                  }}
                  style={{
                    background: "none", border: "none",
                    color: fav ? "#FFC60A" : "#B4B9C7",
                    cursor: "pointer", fontSize: 16, padding: "4px 8px",
                  }}
                >
                  ★
                </button>
              </div>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
