import Trophy from "@/components/primitives/Trophy";
import type { SearchableInputResult } from "@/components/shared/SearchableInput";

/**
 * PlayerSearchInput / ClubSearchInput 라이브 드롭다운 행 — 이름 + 태그 + 트로피.
 * 두 검색 결과의 표시 형태가 동일해 공용으로 둔다.
 */
export function renderSearchResultRow(item: SearchableInputResult) {
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
