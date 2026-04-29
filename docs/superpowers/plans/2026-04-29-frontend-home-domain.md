# Home 도메인 프론트엔드 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Home 페이지 SEO 메타데이터 / JSON-LD 보강 + 검색 인풋 최근/즐겨찾기 (localStorage) UX 추가.

**Architecture:** localStorage 헬퍼 (lib) → 표현 컴포넌트 (SearchHistoryDropdown) → 기존 PlayerSearchInput 확장. SEO 는 `app/page.tsx` 에서 `generateMetadata` override + JSON-LD 정적 주입.

**Tech Stack:** Next.js 16 App Router (Metadata API), React 19, Vitest + Testing Library. 외부 라이브러리 추가 없음.

**Spec:** `docs/superpowers/specs/2026-04-29-frontend-home-domain-design.md`

**Convention:** 모든 commit 한국어, trailer `Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>` 포함, HEREDOC 사용.

---

## File Structure

```text
imstargg-frontend/
├── src/lib/
│   ├── searchHistory.ts                  # 신규
│   ├── searchHistory.test.ts             # 신규 (tests/lib/ 아래로)
│   ├── favorites.ts                      # 신규
│   └── favorites.test.ts                 # 신규
├── src/components/shared/
│   ├── PlayerSearchInput.tsx             # 수정 (드롭다운 통합)
│   └── SearchHistoryDropdown.tsx         # 신규
├── tests/components/
│   └── PlayerSearchInput.test.tsx        # 신규
├── tests/lib/
│   ├── searchHistory.test.ts             # 신규
│   └── favorites.test.ts                 # 신규
└── app/page.tsx                          # 수정 (generateMetadata + JSON-LD)
```

테스트는 `tests/` 아래에 둔다 (Vitest 설정이 `tests/**/*.test.{ts,tsx}` 만 매치).

---

## Task 1: SearchEntry 타입 + searchHistory 헬퍼 + 테스트

**Files:**
- Create: `imstargg-frontend/src/lib/searchHistory.ts`
- Create: `imstargg-frontend/tests/lib/searchHistory.test.ts`

- [ ] **Step 1: 실패하는 테스트 작성**

`imstargg-frontend/tests/lib/searchHistory.test.ts`:

```ts
import { describe, it, expect, beforeEach } from "vitest";
import { addRecent, getRecents, clearRecents, RECENTS_LIMIT } from "@/lib/searchHistory";

describe("searchHistory", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("초기 상태는 빈 배열", () => {
    expect(getRecents()).toEqual([]);
  });

  it("addRecent 후 getRecents 로 조회 가능", () => {
    addRecent({ kind: "player", tag: "#ABC" });
    const recents = getRecents();
    expect(recents).toHaveLength(1);
    expect(recents[0].tag).toBe("#ABC");
    expect(recents[0].kind).toBe("player");
    expect(typeof recents[0].addedAt).toBe("number");
  });

  it("최신 항목이 맨 앞", () => {
    addRecent({ kind: "player", tag: "#A" });
    addRecent({ kind: "player", tag: "#B" });
    expect(getRecents().map((r) => r.tag)).toEqual(["#B", "#A"]);
  });

  it("중복 태그(같은 kind+tag)는 가장 최근으로 끌어올림", () => {
    addRecent({ kind: "player", tag: "#A" });
    addRecent({ kind: "player", tag: "#B" });
    addRecent({ kind: "player", tag: "#A" });
    expect(getRecents().map((r) => r.tag)).toEqual(["#A", "#B"]);
  });

  it(`최대 ${RECENTS_LIMIT} 개까지만 유지 (FIFO)`, () => {
    for (let i = 0; i < RECENTS_LIMIT + 5; i++) {
      addRecent({ kind: "player", tag: `#T${i}` });
    }
    expect(getRecents()).toHaveLength(RECENTS_LIMIT);
    expect(getRecents()[0].tag).toBe(`#T${RECENTS_LIMIT + 4}`);
  });

  it("clearRecents 후 빈 배열", () => {
    addRecent({ kind: "player", tag: "#A" });
    clearRecents();
    expect(getRecents()).toEqual([]);
  });

  it("정규화: 입력 태그가 # 없어도 # 붙여서 저장", () => {
    addRecent({ kind: "player", tag: "abc" });
    expect(getRecents()[0].tag).toBe("#ABC");
  });

  it("storage 손상 시 빈 배열 + 키 삭제", () => {
    localStorage.setItem("imstargg.search.recents", "{not json");
    expect(getRecents()).toEqual([]);
    expect(localStorage.getItem("imstargg.search.recents")).toBeNull();
  });
});
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `pnpm --filter imstargg-frontend test:run tests/lib/searchHistory.test.ts`
Expected: FAIL (`Cannot find module '@/lib/searchHistory'`)

- [ ] **Step 3: 구현**

`imstargg-frontend/src/lib/searchHistory.ts`:

```ts
import { normalizeTag } from "@/lib/tag";

export type SearchEntryKind = "player" | "club";

export interface SearchEntry {
  kind: SearchEntryKind;
  tag: string;
  label?: string;
  addedAt: number;
}

const RECENTS_KEY = "imstargg.search.recents";
export const RECENTS_LIMIT = 10;

function isBrowser(): boolean {
  return typeof window !== "undefined" && typeof window.localStorage !== "undefined";
}

function readArray(key: string): SearchEntry[] {
  if (!isBrowser()) return [];
  const raw = window.localStorage.getItem(key);
  if (raw == null) return [];
  try {
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed)) return parsed as SearchEntry[];
    window.localStorage.removeItem(key);
    return [];
  } catch {
    window.localStorage.removeItem(key);
    return [];
  }
}

function writeArray(key: string, entries: SearchEntry[]): void {
  if (!isBrowser()) return;
  window.localStorage.setItem(key, JSON.stringify(entries));
}

export function getRecents(): SearchEntry[] {
  return readArray(RECENTS_KEY);
}

export function addRecent(input: Omit<SearchEntry, "addedAt">): void {
  if (!isBrowser()) return;
  const tag = normalizeTag(input.tag);
  const next: SearchEntry = { ...input, tag, addedAt: Date.now() };
  const filtered = getRecents().filter((e) => !(e.kind === next.kind && e.tag === next.tag));
  const merged = [next, ...filtered].slice(0, RECENTS_LIMIT);
  writeArray(RECENTS_KEY, merged);
}

export function clearRecents(): void {
  if (!isBrowser()) return;
  window.localStorage.removeItem(RECENTS_KEY);
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `pnpm --filter imstargg-frontend test:run tests/lib/searchHistory.test.ts`
Expected: PASS (8/8)

- [ ] **Step 5: Commit**

```bash
git add imstargg-frontend/src/lib/searchHistory.ts imstargg-frontend/tests/lib/searchHistory.test.ts
git commit -m "$(cat <<'EOF'
feat(frontend): 최근 검색 localStorage 헬퍼 추가

플레이어/클럽 태그 최근 검색 10개 유지. 중복은 가장 최근으로 끌어올림,
SSR 안전, 정규화 적용, storage 손상 시 자동 복구.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 2: favorites 헬퍼 + 테스트

**Files:**
- Create: `imstargg-frontend/src/lib/favorites.ts`
- Create: `imstargg-frontend/tests/lib/favorites.test.ts`

- [ ] **Step 1: 실패하는 테스트 작성**

`imstargg-frontend/tests/lib/favorites.test.ts`:

```ts
import { describe, it, expect, beforeEach } from "vitest";
import { getFavorites, isFavorite, toggleFavorite } from "@/lib/favorites";

describe("favorites", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("초기 상태는 빈 배열", () => {
    expect(getFavorites()).toEqual([]);
    expect(isFavorite("player", "#A")).toBe(false);
  });

  it("toggleFavorite 추가 → true 반환 + isFavorite 갱신", () => {
    const result = toggleFavorite({ kind: "player", tag: "#A" });
    expect(result).toBe(true);
    expect(isFavorite("player", "#A")).toBe(true);
    expect(getFavorites()).toHaveLength(1);
  });

  it("toggleFavorite 두 번째 호출 → false 반환 + 제거", () => {
    toggleFavorite({ kind: "player", tag: "#A" });
    const result = toggleFavorite({ kind: "player", tag: "#A" });
    expect(result).toBe(false);
    expect(isFavorite("player", "#A")).toBe(false);
    expect(getFavorites()).toEqual([]);
  });

  it("kind 가 다르면 별도 항목", () => {
    toggleFavorite({ kind: "player", tag: "#A" });
    toggleFavorite({ kind: "club", tag: "#A" });
    expect(getFavorites()).toHaveLength(2);
  });

  it("정규화: # 없이 입력해도 # 붙여 저장 + 비교 가능", () => {
    toggleFavorite({ kind: "player", tag: "abc" });
    expect(isFavorite("player", "#ABC")).toBe(true);
    expect(isFavorite("player", "abc")).toBe(true);
  });

  it("storage 손상 시 빈 배열 + 키 삭제", () => {
    localStorage.setItem("imstargg.search.favorites", "{not json");
    expect(getFavorites()).toEqual([]);
    expect(localStorage.getItem("imstargg.search.favorites")).toBeNull();
  });
});
```

- [ ] **Step 2: 테스트 실패 확인**

Run: `pnpm --filter imstargg-frontend test:run tests/lib/favorites.test.ts`
Expected: FAIL (`Cannot find module '@/lib/favorites'`)

- [ ] **Step 3: 구현**

`imstargg-frontend/src/lib/favorites.ts`:

```ts
import { normalizeTag } from "@/lib/tag";
import type { SearchEntry, SearchEntryKind } from "@/lib/searchHistory";

const FAVORITES_KEY = "imstargg.search.favorites";

function isBrowser(): boolean {
  return typeof window !== "undefined" && typeof window.localStorage !== "undefined";
}

function readArray(): SearchEntry[] {
  if (!isBrowser()) return [];
  const raw = window.localStorage.getItem(FAVORITES_KEY);
  if (raw == null) return [];
  try {
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed)) return parsed as SearchEntry[];
    window.localStorage.removeItem(FAVORITES_KEY);
    return [];
  } catch {
    window.localStorage.removeItem(FAVORITES_KEY);
    return [];
  }
}

function writeArray(entries: SearchEntry[]): void {
  if (!isBrowser()) return;
  window.localStorage.setItem(FAVORITES_KEY, JSON.stringify(entries));
}

export function getFavorites(): SearchEntry[] {
  return readArray();
}

export function isFavorite(kind: SearchEntryKind, tag: string): boolean {
  const norm = normalizeTag(tag);
  return getFavorites().some((e) => e.kind === kind && e.tag === norm);
}

export function toggleFavorite(input: Omit<SearchEntry, "addedAt">): boolean {
  if (!isBrowser()) return false;
  const tag = normalizeTag(input.tag);
  const list = getFavorites();
  const idx = list.findIndex((e) => e.kind === input.kind && e.tag === tag);
  if (idx >= 0) {
    list.splice(idx, 1);
    writeArray(list);
    return false;
  }
  list.unshift({ ...input, tag, addedAt: Date.now() });
  writeArray(list);
  return true;
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run: `pnpm --filter imstargg-frontend test:run tests/lib/favorites.test.ts`
Expected: PASS (6/6)

- [ ] **Step 5: Commit**

```bash
git add imstargg-frontend/src/lib/favorites.ts imstargg-frontend/tests/lib/favorites.test.ts
git commit -m "$(cat <<'EOF'
feat(frontend): 즐겨찾기 localStorage 헬퍼 추가

검색 인풋에서 별표 토글로 즐겨찾기 추가/제거. 정규화 + SSR 안전 +
storage 손상 자동 복구.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 3: SearchHistoryDropdown 컴포넌트

**Files:**
- Create: `imstargg-frontend/src/components/shared/SearchHistoryDropdown.tsx`

- [ ] **Step 1: 컴포넌트 작성**

`imstargg-frontend/src/components/shared/SearchHistoryDropdown.tsx`:

```tsx
"use client";

import Link from "next/link";
import type { SearchEntry, SearchEntryKind } from "@/lib/searchHistory";
import { routes } from "@/config/routes";

interface Props {
  recents: SearchEntry[];
  favorites: SearchEntry[];
  onSelect?: () => void;
  onToggleFavorite: (entry: Pick<SearchEntry, "kind" | "tag">) => void;
  onClearRecents: () => void;
  isFavorite: (kind: SearchEntryKind, tag: string) => boolean;
}

function entryHref(e: SearchEntry): string {
  return e.kind === "player" ? routes.player(e.tag) : routes.club(e.tag);
}

export function SearchHistoryDropdown({
  recents, favorites, onSelect, onToggleFavorite, onClearRecents, isFavorite,
}: Props) {
  if (recents.length === 0 && favorites.length === 0) return null;

  return (
    <div
      role="listbox"
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
          onSelect={onSelect}
          onToggleFavorite={onToggleFavorite}
          isFavorite={isFavorite}
        />
      )}
      {recents.length > 0 && (
        <Section
          title="최근 검색"
          entries={recents}
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
  title, entries, onSelect, onToggleFavorite, isFavorite, rightAction,
}: {
  title: string;
  entries: SearchEntry[];
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
        {entries.map((e) => {
          const fav = isFavorite(e.kind, e.tag);
          return (
            <li key={`${e.kind}:${e.tag}`} role="option" aria-selected="false">
              <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
                <Link
                  href={entryHref(e)}
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
```

- [ ] **Step 2: typecheck**

Run: `pnpm --filter imstargg-frontend typecheck`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add imstargg-frontend/src/components/shared/SearchHistoryDropdown.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): SearchHistoryDropdown 컴포넌트 추가

검색 인풋 아래 즐겨찾기 + 최근 검색 표시. 별표 토글, "지우기" 버튼,
플레이어/클럽 라벨 분리.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 4: PlayerSearchInput 확장 (드롭다운 통합)

**Files:**
- Modify: `imstargg-frontend/src/components/shared/PlayerSearchInput.tsx`

- [ ] **Step 1: 컴포넌트 전체 교체**

`imstargg-frontend/src/components/shared/PlayerSearchInput.tsx` 전체:

```tsx
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
  const wrapperRef = useRef<HTMLDivElement | null>(null);

  function refresh() {
    setRecents(getRecents());
    setFavorites(getFavorites());
  }

  useEffect(() => {
    refresh();
  }, []);

  useEffect(() => {
    function onMouseDown(e: MouseEvent) {
      if (!wrapperRef.current) return;
      if (!wrapperRef.current.contains(e.target as Node)) setOpen(false);
    }
    document.addEventListener("mousedown", onMouseDown);
    return () => document.removeEventListener("mousedown", onMouseDown);
  }, []);

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
          aria-label="플레이어 태그 입력"
          aria-expanded={open}
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
```

- [ ] **Step 2: typecheck + lint**

Run: `pnpm --filter imstargg-frontend typecheck && pnpm --filter imstargg-frontend lint`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add imstargg-frontend/src/components/shared/PlayerSearchInput.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): PlayerSearchInput 에 최근 검색 + 즐겨찾기 드롭다운 통합

포커스 시 드롭다운 노출, 폼 제출 성공 시 최근 검색에 자동 추가,
별표로 즐겨찾기 토글, 외부 클릭 시 닫힘.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 5: PlayerSearchInput 통합 테스트

**Files:**
- Create: `imstargg-frontend/tests/components/PlayerSearchInput.test.tsx`

- [ ] **Step 1: 테스트 작성**

`imstargg-frontend/tests/components/PlayerSearchInput.test.tsx`:

```tsx
import { describe, it, expect, beforeEach, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { PlayerSearchInput } from "@/components/shared/PlayerSearchInput";
import { addRecent, getRecents } from "@/lib/searchHistory";
import { toggleFavorite } from "@/lib/favorites";

const pushMock = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: pushMock }),
}));

vi.mock("sonner", () => ({
  toast: { error: vi.fn(), success: vi.fn(), info: vi.fn() },
}));

describe("PlayerSearchInput", () => {
  beforeEach(() => {
    localStorage.clear();
    pushMock.mockClear();
  });

  it("초기 상태에서 포커스해도 빈 드롭다운은 안 보임", () => {
    render(<PlayerSearchInput />);
    fireEvent.focus(screen.getByLabelText("플레이어 태그 입력"));
    expect(screen.queryByRole("listbox")).toBeNull();
  });

  it("최근 검색이 있으면 포커스 시 드롭다운 노출", () => {
    addRecent({ kind: "player", tag: "#ABC" });
    render(<PlayerSearchInput />);
    fireEvent.focus(screen.getByLabelText("플레이어 태그 입력"));
    expect(screen.getByRole("listbox")).toBeInTheDocument();
    expect(screen.getByText("#ABC")).toBeInTheDocument();
  });

  it("즐겨찾기가 있으면 즐겨찾기 섹션 노출", () => {
    toggleFavorite({ kind: "player", tag: "#FAV" });
    render(<PlayerSearchInput />);
    fireEvent.focus(screen.getByLabelText("플레이어 태그 입력"));
    expect(screen.getByText("즐겨찾기")).toBeInTheDocument();
    expect(screen.getByText("#FAV")).toBeInTheDocument();
  });

  it("폼 제출 성공 시 최근 검색에 추가됨", () => {
    render(<PlayerSearchInput />);
    const input = screen.getByLabelText("플레이어 태그 입력") as HTMLInputElement;
    fireEvent.change(input, { target: { value: "#88J8Q9RPV" } });
    fireEvent.submit(input.closest("form")!);
    expect(pushMock).toHaveBeenCalledWith("/players/%2388J8Q9RPV");
    expect(getRecents()[0].tag).toBe("#88J8Q9RPV");
  });

  it("드롭다운 항목 별표 클릭 시 즐겨찾기 토글", () => {
    addRecent({ kind: "player", tag: "#X" });
    render(<PlayerSearchInput />);
    fireEvent.focus(screen.getByLabelText("플레이어 태그 입력"));
    const starBtn = screen.getByLabelText("즐겨찾기에 추가");
    fireEvent.click(starBtn);
    expect(screen.getByLabelText("즐겨찾기 제거")).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: 테스트 실행**

Run: `pnpm --filter imstargg-frontend test:run tests/components/PlayerSearchInput.test.tsx`
Expected: PASS (5/5)

테스트 환경에 `@testing-library/react` 및 jsdom 가 이미 설정되어 있어야 한다 (vitest config). 만약 미설정이면 다음을 확인:
- `pnpm --filter imstargg-frontend ls @testing-library/react vitest jsdom`
- vitest config 의 `environment: "jsdom"` 여부

미설정이면 testing dependencies 추가 후 진행 (대부분 이미 있음).

- [ ] **Step 3: Commit**

```bash
git add imstargg-frontend/tests/components/PlayerSearchInput.test.tsx
git commit -m "$(cat <<'EOF'
test(frontend): PlayerSearchInput 드롭다운/즐겨찾기 통합 테스트 추가

포커스 시 드롭다운 노출, 폼 제출 후 최근 검색 추가, 별표 토글 동작 검증.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 6: Home 메타데이터 + JSON-LD

**Files:**
- Modify: `imstargg-frontend/app/page.tsx`

- [ ] **Step 1: page.tsx 전체 교체**

`imstargg-frontend/app/page.tsx`:

```tsx
import type { Metadata } from "next";
import { buildMetadata } from "@/lib/seo";
import { env } from "@/config/env";
import HomeScreen from "./_components/HomeScreen";

export const metadata: Metadata = buildMetadata({
  title: "ImStarGG — 브롤스타즈 전적 · 통계 · 메타 검색",
  description:
    "플레이어 태그로 전적을 찾고, 브롤러 메타와 이벤트 정보를 한눈에. 한국어 브롤스타즈 통계 사이트.",
  path: "/",
});

const jsonLd = {
  "@context": "https://schema.org",
  "@type": "WebSite",
  name: "ImStarGG",
  url: env.NEXT_PUBLIC_SITE_URL,
  potentialAction: {
    "@type": "SearchAction",
    target: {
      "@type": "EntryPoint",
      urlTemplate: `${env.NEXT_PUBLIC_SITE_URL}/players/{search_term_string}`,
    },
    "query-input": "required name=search_term_string",
  },
};

export default function Page() {
  return (
    <>
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
      />
      <HomeScreen />
    </>
  );
}
```

- [ ] **Step 2: typecheck + lint**

Run: `pnpm --filter imstargg-frontend typecheck && pnpm --filter imstargg-frontend lint`
Expected: PASS

- [ ] **Step 3: 빌드 확인 (메타 검증)**

Run: `pnpm --filter imstargg-frontend build`
Expected: 빌드 성공. 빌드 산출물에서 home 페이지가 prerender 가능 (server component, env 평가가 빌드 타임에 가능).

빌드 시간이 길다면 typecheck + lint 만 통과 시 다음 step 으로.

- [ ] **Step 4: Commit**

```bash
git add imstargg-frontend/app/page.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): Home 페이지 메타데이터 + JSON-LD 추가

Home 전용 title/description override 와 schema.org WebSite +
SearchAction JSON-LD 주입. 검색엔진 사이트링크 검색박스 자격 충족.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 7: HomeScreen inline style 정리 (선택)

**조건:** Task 1-6 완료 후 `HomeScreen.tsx` 가독성이 떨어진다 판단되는 경우만. 그렇지 않으면 skip.

**Files:**
- Modify: `imstargg-frontend/app/_components/HomeScreen.tsx`
- Modify: `imstargg-frontend/app/globals.css` 또는 신규 `imstargg-frontend/app/styles/home.css` + globals import

- [ ] **Step 1: skip 판단**

`HomeScreen.tsx` 를 다시 본다. inline style 이 100줄 가량인데, 새로운 동작 추가 없이 이를 클래스로 분리하는 것은 surgical change 원칙(GUIDELINE.md "touch only what you must") 에 어긋날 수 있다. 검색 동작 변경(Task 4)이 이 파일에 영향이 없으면 **skip**.

skip 시 Task 7 전체를 무시하고 종료한다.

- [ ] **Step 2: (skip 안 한 경우) 클래스 분리**

`globals.css` 또는 신규 `app/styles/home.css` 에 `.home-hero`, `.home-bg-mask`, `.home-title`, `.home-tagline`, `.home-search-wrap`, `.home-search-hint`, `.home-footer` 정의. `HomeScreen.tsx` 의 인라인 style 제거하고 className 부여.

- [ ] **Step 3: typecheck + lint + 시각 회귀 확인**

Run: `pnpm --filter imstargg-frontend dev` 후 `http://localhost:3000` 에서 시각 동일 여부 확인.

- [ ] **Step 4: Commit (skip 안 한 경우)**

```bash
git add imstargg-frontend/app/_components/HomeScreen.tsx imstargg-frontend/app/globals.css
# 또는 styles/home.css
git commit -m "$(cat <<'EOF'
refactor(frontend): HomeScreen inline style 을 css 클래스로 정리

기능 변화 없음. 가독성 향상.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

## Task 8: 최종 검증

**Files:** 없음 (검증만)

- [ ] **Step 1: 전체 typecheck + lint + test**

Run:
```bash
pnpm --filter imstargg-frontend typecheck \
&& pnpm --filter imstargg-frontend lint \
&& pnpm --filter imstargg-frontend test:run
```

Expected: 모두 PASS. 단위 테스트는 기존 + 신규 (~19개 추가).

- [ ] **Step 2: 시각 확인 (선택)**

`pnpm --filter imstargg-frontend dev` 후 `/` 에서:
- 검색 인풋 포커스 시 드롭다운 (없으면 빈 상태 OK)
- 태그 입력 후 검색 → 다시 포커스 → 최근 검색에 추가됨 확인
- 별표 클릭 → 즐겨찾기 섹션에 노출 확인
- 외부 클릭 시 드롭다운 닫힘 확인
- View source → `<script type="application/ld+json">` 노출 확인
- View source → `<title>` / `<meta name="description">` 가 Home 전용 문구로 적용 확인

- [ ] **Step 3: 푸시**

```bash
git push
```

---

## Self-Review (작성 시점)

**1. Spec coverage:**
- §3.1 Home metadata override → Task 6
- §3.2 JSON-LD WebSite + SearchAction → Task 6
- §4.1 데이터 모델 (SearchEntry) → Task 1
- §4.2 lib 헬퍼 (searchHistory + favorites) → Task 1, Task 2
- §4.3 검색 인풋 + 드롭다운 → Task 4
- §4.4 컴포넌트 구조 → Task 3 (dropdown), Task 4 (input)
- §4.5 UX 디테일 → Task 3, Task 4
- §5 코드 정리 (선택) → Task 7
- §6 테스트 → Task 1, 2, 5

**2. Placeholder scan:** TBD/TODO 패턴 없음. 모든 코드 블록 완전.

**3. Type consistency:** `SearchEntry`, `SearchEntryKind` 는 Task 1 에서 정의되고 Task 2/3/4/5 에서 일관되게 사용됨.

---

## Execution Handoff

Plan complete. Subagent-Driven Development 또는 inline 실행 둘 다 가능. Auto Mode 활성 시 single-implementer + final review 패턴 권장.
