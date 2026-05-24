"use client";

import Trophy from "@/components/primitives/Trophy";
import { SearchableInput } from "@/components/shared/SearchableInput";
import type {
  SearchableInputResult,
  SearchableInputVariant,
} from "@/components/shared/SearchableInput";
import { playerApi } from "@/api/endpoints/player";

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

function renderPlayerResult(item: SearchableInputResult) {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 13 }}>
      <span style={{ fontWeight: 700, color: "#14161B" }}>{item.name}</span>
      <span style={{ fontFamily: "var(--font-mono)", color: "#5C6271" }}>{item.tag}</span>
      <span style={{ marginLeft: "auto", display: "inline-flex", alignItems: "center", gap: 4 }}>
        <Trophy value={item.trophies} />
      </span>
    </div>
  );
}

export function PlayerSearchInput({
  defaultValue = "",
  variant = "default",
}: {
  defaultValue?: string;
  variant?: SearchableInputVariant;
}) {
  return (
    <SearchableInput
      placeholder="플레이어 이름 또는 #태그"
      inputAriaLabel="플레이어 이름 또는 태그 입력"
      buttonAriaLabel="플레이어 검색"
      defaultValue={defaultValue}
      fetchLiveResults={fetchPlayers}
      renderResult={renderPlayerResult}
      toDetailHref={(item) => `/players/${item.tag.replace(/^#/, "")}`}
      toSearchHref={(q) => `/players/search?q=${encodeURIComponent(q)}`}
      variant={variant}
    />
  );
}
