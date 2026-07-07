"use client";

import { SearchableInput } from "@/components/shared/SearchableInput";
import type { SearchableInputResult } from "@/components/shared/SearchableInput";
import { renderSearchResultRow } from "@/components/shared/searchResultRow";
import { playerApi } from "@/api/endpoints/player";
import { routes } from "@/config/routes";

const LIVE_LIMIT = 5;

async function fetchPlayers(q: string): Promise<SearchableInputResult[]> {
  const res = await playerApi.search(q, 0, LIVE_LIMIT);
  return res.content.map((p) => ({
    kind: "player",
    tag: p.tag,
    name: p.name,
    trophies: p.trophies,
  }));
}

export function PlayerSearchInput({ defaultValue = "" }: { defaultValue?: string }) {
  return (
    <SearchableInput
      placeholder="플레이어 이름 또는 #태그"
      inputAriaLabel="플레이어 이름 또는 태그 입력"
      buttonAriaLabel="플레이어 검색"
      defaultValue={defaultValue}
      liveKind="player"
      fetchLiveResults={fetchPlayers}
      renderResult={renderSearchResultRow}
      toDetailHref={(item) => routes.player(item.tag)}
      toSearchHref={(q) => `/players/search?q=${encodeURIComponent(q)}`}
    />
  );
}
