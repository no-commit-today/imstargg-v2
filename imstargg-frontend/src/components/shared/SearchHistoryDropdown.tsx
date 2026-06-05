"use client";

import Image from "next/image";
import Link from "next/link";
import type { SearchEntry, SearchEntryKind } from "@/lib/searchHistory";
import { routes } from "@/config/routes";

interface Props {
  recents: SearchEntry[];
  favorites: SearchEntry[];
  activeIndex: number; // -1 = nothing active. 0..favorites.length-1 = fav, beyond = recents
  onSelect?: () => void;
  onToggleFavorite: (entry: Omit<SearchEntry, "addedAt">) => void;
  onClearRecents: () => void;
  isFavorite: (kind: SearchEntryKind, tag: string) => boolean;
  variant?: "default" | "topnav";
}

function entryHref(e: SearchEntry): string {
  return e.kind === "player" ? routes.player(e.tag) : routes.club(e.tag);
}

export function SearchHistoryDropdown({
  recents, favorites, activeIndex, onSelect, onToggleFavorite, onClearRecents, isFavorite,
  variant = "default",
}: Props) {
  if (recents.length === 0 && favorites.length === 0) return null;

  const recentsActiveIndex = activeIndex >= favorites.length ? activeIndex - favorites.length : -1;
  const favoritesActiveIndex = activeIndex < favorites.length ? activeIndex : -1;

  const positionStyle: React.CSSProperties =
    variant === "topnav"
      ? {
          position: "absolute",
          top: "100%",
          right: 0,
          marginTop: 6,
          width: 320,
          maxWidth: "calc(100vw - 24px)",
          zIndex: 100,
        }
      : {
          position: "absolute",
          top: "100%",
          left: 0,
          right: 0,
          marginTop: 4,
          zIndex: 50,
        };

  return (
    <div
      role="menu"
      style={{
        ...positionStyle,
        background: "#fff",
        border: "1px solid #D8DCE5",
        borderRadius: 12,
        boxShadow: "0 12px 32px rgba(20,22,27,0.12)",
        padding: 8,
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
  onToggleFavorite: (entry: Omit<SearchEntry, "addedAt">) => void;
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
                    flex: 1,
                    display: "flex",
                    alignItems: "center",
                    gap: 8,
                    padding: "6px 8px",
                    borderRadius: 6,
                    color: "#14161B",
                    textDecoration: "none",
                    fontSize: 13,
                  }}
                >
                  <span
                    style={{
                      fontSize: 10,
                      fontWeight: 700,
                      color: "#5C6271",
                      textTransform: "uppercase",
                      minWidth: 36,
                    }}
                  >
                    {e.kind === "player" ? "PLAYER" : "CLUB"}
                  </span>
                  <span style={{ fontWeight: 700 }}>{e.name}</span>
                  <span style={{ fontFamily: "var(--font-mono)", color: "#5C6271" }}>
                    {e.tag}
                  </span>
                  <span
                    style={{
                      marginLeft: "auto",
                      display: "inline-flex",
                      alignItems: "center",
                      gap: 4,
                      fontFamily: "var(--font-num)",
                      fontWeight: 700,
                      color: "#14161B",
                      fontVariantNumeric: "tabular-nums",
                    }}
                  >
                    {e.trophies.toLocaleString()}
                    <Image
                      src="/assets/icon_trophy.png"
                      alt=""
                      width={14}
                      height={14}
                      unoptimized
                    />
                  </span>
                </Link>
                <button
                  type="button"
                  aria-pressed={fav}
                  aria-label={fav ? "즐겨찾기 제거" : "즐겨찾기에 추가"}
                  onClick={(ev) => {
                    ev.preventDefault();
                    ev.stopPropagation();
                    onToggleFavorite({ kind: e.kind, tag: e.tag, name: e.name, trophies: e.trophies });
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
