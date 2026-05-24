"use client";

import { useState } from "react";
import Link from "next/link";
import { clubApi } from "@/api/endpoints/club";
import type { ClubResponse, SliceResponse } from "@/api/types";
import { Button } from "@/components/ui/button";
import { addRecent } from "@/lib/searchHistory";
import Trophy from "@/components/primitives/Trophy";

const PAGE_SIZE = 20;

interface Props {
  query: string;
  initial: SliceResponse<ClubResponse>;
}

export function ClubSearchResults({ query, initial }: Props) {
  const [items, setItems] = useState<ClubResponse[]>(initial.content);
  const [hasNext, setHasNext] = useState(initial.hasNext);
  const [offset, setOffset] = useState(initial.content.length);
  const [loading, setLoading] = useState(false);

  async function loadMore() {
    setLoading(true);
    try {
      const next = await clubApi.search(query, offset, PAGE_SIZE);
      setItems((prev) => [...prev, ...next.content]);
      setHasNext(next.hasNext);
      setOffset(offset + next.content.length);
    } finally {
      setLoading(false);
    }
  }

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
        {items.map((c) => (
          <li
            key={c.tag}
            style={{
              background: "#FFFFFF",
              border: "1px solid #D8DCE5",
              borderRadius: 12,
            }}
          >
            <Link
              href={`/clubs/${c.tag.replace(/^#/, "")}`}
              onClick={() =>
                addRecent({
                  kind: "club",
                  tag: c.tag,
                  name: c.name,
                  trophies: c.trophies,
                })
              }
              style={{
                display: "flex",
                alignItems: "center",
                gap: 12,
                padding: "12px 16px",
                color: "#14161B",
                textDecoration: "none",
              }}
            >
              <span style={{ fontWeight: 700 }}>{c.name}</span>
              <span style={{ fontFamily: "var(--font-mono)", color: "#5C6271" }}>{c.tag}</span>
              <span style={{ color: "#8A90A0", fontSize: 12 }}>· 멤버 {c.members.length}명</span>
              <span style={{ marginLeft: "auto" }}>
                <Trophy value={c.trophies} />
              </span>
            </Link>
          </li>
        ))}
      </ul>
      {hasNext && (
        <div style={{ display: "flex", justifyContent: "center", marginTop: 16 }}>
          <Button type="button" onClick={loadMore} disabled={loading}>
            {loading ? "불러오는 중…" : "더보기"}
          </Button>
        </div>
      )}
    </div>
  );
}
