"use client";

import { SearchableInput } from "@/components/shared/SearchableInput";
import type { SearchableInputResult } from "@/components/shared/SearchableInput";
import { renderSearchResultRow } from "@/components/shared/searchResultRow";
import { clubApi } from "@/api/endpoints/club";

const LIVE_LIMIT = 5;

async function fetchClubs(q: string): Promise<SearchableInputResult[]> {
  const res = await clubApi.search(q, 0, LIVE_LIMIT);
  return res.content.map((c) => ({
    kind: "club",
    tag: c.tag,
    name: c.name,
    trophies: c.trophies,
  }));
}

export function ClubSearchInput({ defaultValue = "" }: { defaultValue?: string }) {
  return (
    <SearchableInput
      placeholder="클럽 이름 또는 #태그"
      inputAriaLabel="클럽 이름 또는 태그 입력"
      buttonAriaLabel="클럽 검색"
      defaultValue={defaultValue}
      liveKind="club"
      fetchLiveResults={fetchClubs}
      renderResult={renderSearchResultRow}
      toDetailHref={(item) => `/clubs/${item.tag.replace(/^#/, "")}`}
      toSearchHref={(q) => `/clubs/search?q=${encodeURIComponent(q)}`}
    />
  );
}
