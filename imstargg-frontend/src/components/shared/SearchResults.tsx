"use client";

import Link from "next/link";
import type { ReactNode } from "react";
import type { ClubResponse, PlayerResponse, SliceResponse } from "@/api/types";
import { Button } from "@/components/ui/button";
import Trophy from "@/components/primitives/Trophy";
import { routes } from "@/config/routes";
import { addRecent } from "@/lib/searchHistory";
import { usePlayerSearch, useClubSearch } from "@/hooks/queries/useSearch";

interface ResultItem {
  tag: string;
  name: string;
  trophies: number;
  href: string;
  extra: ReactNode;
  onSelect: () => void;
}

interface SearchResultsViewProps {
  query: string;
  items: ResultItem[];
  hasNextPage: boolean;
  isFetchingNextPage: boolean;
  fetchNextPage: () => void;
}

function SearchResultsView({
  query,
  items,
  hasNextPage,
  isFetchingNextPage,
  fetchNextPage,
}: SearchResultsViewProps) {
  if (items.length === 0) {
    return (
      <p style={{ color: "#5C6271", padding: "12px 0" }}>
        &quot;{query}&quot; 검색 결과가 없습니다.
      </p>
    );
  }

  return (
    <div>
      <ul style={{ listStyle: "none", padding: 0, margin: 0, display: "grid", gap: 8 }}>
        {items.map((item) => (
          <li
            key={item.tag}
            style={{
              background: "#FFFFFF",
              border: "1px solid #D8DCE5",
              borderRadius: 12,
            }}
          >
            <Link
              href={item.href}
              onClick={item.onSelect}
              style={{
                display: "flex",
                alignItems: "center",
                gap: 12,
                padding: "12px 16px",
                color: "#14161B",
                textDecoration: "none",
              }}
            >
              <span style={{ fontWeight: 700 }}>{item.name}</span>
              <span style={{ fontFamily: "var(--font-mono)", color: "#5C6271" }}>
                {item.tag}
              </span>
              {item.extra}
              <span style={{ marginLeft: "auto" }}>
                <Trophy value={item.trophies} />
              </span>
            </Link>
          </li>
        ))}
      </ul>
      {hasNextPage && (
        <div style={{ display: "flex", justifyContent: "center", marginTop: 16 }}>
          <Button type="button" onClick={() => fetchNextPage()} disabled={isFetchingNextPage}>
            {isFetchingNextPage ? "불러오는 중…" : "더보기"}
          </Button>
        </div>
      )}
    </div>
  );
}

interface PlayerSearchResultsProps {
  query: string;
  initial: SliceResponse<PlayerResponse>;
}

export function PlayerSearchResults({ query, initial }: PlayerSearchResultsProps) {
  const { data, hasNextPage, isFetchingNextPage, fetchNextPage } = usePlayerSearch(query, {
    initialPage: initial,
  });
  const players = data?.pages.flat() ?? [];
  const items: ResultItem[] = players.map((player) => ({
    tag: player.tag,
    name: player.name,
    trophies: player.trophies,
    href: routes.player(player.tag),
    extra: (
      <span style={{ color: "#8A90A0", fontSize: 12 }}>
        · {player.club?.name ?? "클럽 없음"}
      </span>
    ),
    onSelect: () =>
      addRecent({
        kind: "player",
        tag: player.tag,
        name: player.name,
        trophies: player.trophies,
      }),
  }));

  return (
    <SearchResultsView
      query={query}
      items={items}
      hasNextPage={hasNextPage}
      isFetchingNextPage={isFetchingNextPage}
      fetchNextPage={fetchNextPage}
    />
  );
}

interface ClubSearchResultsProps {
  query: string;
  initial: SliceResponse<ClubResponse>;
}

export function ClubSearchResults({ query, initial }: ClubSearchResultsProps) {
  const { data, hasNextPage, isFetchingNextPage, fetchNextPage } = useClubSearch(query, {
    initialPage: initial,
  });
  const clubs = data?.pages.flat() ?? [];
  const items: ResultItem[] = clubs.map((club) => ({
    tag: club.tag,
    name: club.name,
    trophies: club.trophies,
    href: routes.club(club.tag),
    extra: <span style={{ color: "#8A90A0", fontSize: 12 }}>· 멤버 {club.members.length}명</span>,
    onSelect: () =>
      addRecent({
        kind: "club",
        tag: club.tag,
        name: club.name,
        trophies: club.trophies,
      }),
  }));

  return (
    <SearchResultsView
      query={query}
      items={items}
      hasNextPage={hasNextPage}
      isFetchingNextPage={isFetchingNextPage}
      fetchNextPage={fetchNextPage}
    />
  );
}
