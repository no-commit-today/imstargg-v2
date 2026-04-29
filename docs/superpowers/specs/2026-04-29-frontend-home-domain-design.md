# Frontend Home 도메인 설계

> Home 페이지 SEO 보강 + 검색 UX (최근 검색 / 즐겨찾기 localStorage). 보조 카드는 추가하지 않음.

작성일: 2026-04-29

---

## 1. 목표 및 범위

### 목표
랜딩 페이지의 검색 진입점을 강화하고 검색엔진 발견성을 향상시킨다.

### 범위
- **포함**:
  - **SEO**: Home 메타데이터 보강 (title/description/keywords), JSON-LD `WebSite` + `SearchAction` 스키마.
  - **검색 UX**: 최근 검색(플레이어/클럽 태그) + 즐겨찾기 localStorage 저장. 검색 인풋 포커스/입력 시 드롭다운으로 노출.
  - **선택적 코드 정리**: `HomeScreen.tsx` inline style 과도하면 globals.css 클래스로 일부 정리.
- **제외**:
  - 보조 카드 (로테이션/메타/랭커) — 명시적 제외.
  - 자동완성(이름→태그) — 백엔드 검색 API 필요. 별도 도메인.
  - Dynamic OG 이미지 (`opengraph-image.tsx`) — 별도 후속 작업.

### 비-Home 변경 (검색 인풋이 TopNav 등 다른 곳에서도 쓰이면)
- `PlayerSearchInput` 은 Home 외에도 `TopNav` 검색 박스로 재사용될 수 있음. 수정 시 모든 사용처 확인.

---

## 2. 기술 스택

- Next.js 16 App Router (Metadata API), React 19.
- localStorage (브라우저 전용 — SSR 안전 가드 필요).
- 기존 `buildMetadata` 헬퍼 (`src/lib/seo.ts`) 재사용.
- 기존 `PlayerSearchInput` (`src/components/shared/PlayerSearchInput.tsx`) 확장.
- 추가 외부 라이브러리 없음.

---

## 3. SEO 작업

### 3.1 Home 전용 metadata override

`app/page.tsx` 에 `generateMetadata` 추가하여 Home 전용 문구 제공. layout.tsx 의 글로벌 메타와 병합되며 path-specific 정보(canonical, og:url)가 정확히 `/` 로 잡힘.

```ts
// app/page.tsx
import type { Metadata } from "next";
import { buildMetadata } from "@/lib/seo";

export const metadata: Metadata = buildMetadata({
  title: "ImStarGG — 브롤스타즈 전적 · 통계 · 메타 검색",
  description: "플레이어 태그로 전적을 찾고, 브롤러 메타와 이벤트 정보를 한눈에. 한국어 브롤스타즈 통계 사이트.",
  path: "/",
});
```

기존 `RootLayout` 의 globals 는 그대로 유지. `app/page.tsx` 에서 명시적으로 다시 부여하면 페이지 단위에서 우선됨.

### 3.2 JSON-LD WebSite + SearchAction

홈에 검색 가능한 사이트라는 정보를 `application/ld+json` 으로 노출. 구글 검색 결과의 사이트링크 검색박스에 영향.

```tsx
// app/page.tsx (page 컴포넌트 안에 <script type="application/ld+json"> 추가)
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
```

`<script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }} />` 로 페이지에 주입.

> 주의: `urlTemplate` 의 `{search_term_string}` 은 사용자가 직접 입력하는 태그(`#88J8Q9RPV` 같은). `#` 가 URL 에서 fragment 로 처리되지 않도록 검색 폼에서 `encodeURIComponent` 거치는데, 이는 우리 `toUrlPath` 가 이미 처리.

---

## 4. 검색 UX 작업

### 4.1 데이터 모델

localStorage 에 두 키:
- `imstargg.search.recents`: 최근 검색 항목 (FIFO, 최대 10개)
- `imstargg.search.favorites`: 즐겨찾기 항목 (사용자 명시적 토글)

각 항목 형식:
```ts
export interface SearchEntry {
  kind: "player" | "club";
  tag: string;        // normalizeTag 로 정규화된 #ABC 형식
  label?: string;     // (선택) 라벨 — 추후 검색 결과 페이지 진입 후 이름 캐싱 가능
  addedAt: number;    // Date.now()
}
```

라벨은 일단 비워둔다 (자동 캐싱은 후속). 사용자가 즐겨찾기에서 보는 건 태그 자체.

### 4.2 lib 헬퍼

`src/lib/searchHistory.ts`:
```ts
import type { SearchEntry } from "@/lib/searchHistory.types";  // 또는 같은 파일에 export

const RECENTS_KEY = "imstargg.search.recents";
const RECENTS_LIMIT = 10;

export function getRecents(): SearchEntry[];
export function addRecent(entry: Omit<SearchEntry, "addedAt">): void;
export function clearRecents(): void;
```

`src/lib/favorites.ts`:
```ts
const FAVORITES_KEY = "imstargg.search.favorites";

export function getFavorites(): SearchEntry[];
export function isFavorite(kind: SearchEntry["kind"], tag: string): boolean;
export function toggleFavorite(entry: Omit<SearchEntry, "addedAt">): boolean;  // returns new state
```

**SSR 안전**: 모든 함수는 `typeof window === "undefined"` 가드로 시작. SSR 시 빈 배열 / no-op.

**Storage 손상 처리**: JSON.parse 실패 시 빈 배열 반환 + 키 삭제.

### 4.3 검색 인풋 + 드롭다운

`PlayerSearchInput` 의 동작 변경:
- 현재: 폼 제출 시 `/players/{tag}` 이동, 토스트 검증.
- 추가: 폼 제출 성공 시 `addRecent({kind:"player", tag})`. (클럽은 이 컴포넌트가 아니므로 plyr 만)
- 추가: 인풋 포커스 시 / 값이 비어있을 때, 드롭다운 표시:
  - "즐겨찾기" 섹션 (있을 때만)
  - "최근 검색" 섹션
  - 각 항목 옆 ★ 토글 버튼
  - 항목 클릭 시 라우트 이동
  - "지우기" 버튼 (최근 검색만)

### 4.4 컴포넌트 구조

```text
src/components/shared/
├── PlayerSearchInput.tsx              # (수정) 폼 + 드롭다운 wrapper
└── SearchHistoryDropdown.tsx          # (신규) 즐겨찾기/최근 리스트 + 토글
src/lib/
├── searchHistory.ts                   # (신규) 최근 검색 storage
└── favorites.ts                       # (신규) 즐겨찾기 storage
```

`SearchHistoryDropdown` 는 `recents`, `favorites`, `onSelect`, `onToggleFavorite`, `onClearRecents` 만 props 로 받는 stateless 표현 컴포넌트. storage 액세스는 부모 (`PlayerSearchInput`) 에서.

### 4.5 UX 디테일

- **드롭다운 위치**: 인풋 바로 아래, full-width, 흰 카드 + 1px 보더, `--shadow-card-lg` 변종. DESIGN.md TopNav 검색 드롭다운 가이드 참조.
- **포커스 외 클릭 시 닫힘**: `useEffect` 로 `mousedown` 리스너.
- **빈 상태**: 둘 다 비어있으면 드롭다운 표시 안 함.
- **모바일**: 같은 레이아웃 유지 (DESIGN.md 280px 기준이지만 Home 검색은 max-w-md 라 그대로 사용).
- **정규화**: 저장 전 `normalizeTag` 적용. 비교 시 `isSameTag` 사용 (Battle 도메인에서 추출한 lib/tag 의 헬퍼).
- **즐겨찾기 토글**: 별표 클릭 시 추가/제거. 토스트 알림 없이 시각만 즉시 업데이트.

---

## 5. 코드 정리 (선택적)

`HomeScreen.tsx` 의 inline style 이 100줄 가량이라 globals.css 에 클래스 분리 (선택).

**진행 기준:** 검색 드롭다운 추가로 인해 HomeScreen 의 layout 이 컴팩트해지는데, 이때 inline style 과 클래스가 섞이면 가독성이 더 떨어진다. 자연스럽게 `home-hero`, `home-bg-mask`, `home-title` 같은 의미 있는 클래스로 분리.

기준: 새 동작 추가하지 않고, 같은 시각만 유지. 분리 안 해도 되면 skip.

---

## 6. 테스트 전략

### 단위 테스트
- `src/lib/searchHistory.test.ts`: get / add / clear, FIFO 10개 한도, 중복 항목 처리(가장 최근으로 끌어올림), SSR no-op.
- `src/lib/favorites.test.ts`: get / isFavorite / toggleFavorite, 토글 후 상태, SSR no-op.

### 컴포넌트 테스트
- `tests/components/PlayerSearchInput.test.tsx` (신규):
  - 포커스 시 드롭다운 노출 (recents 있을 때)
  - 빈 상태에서 드롭다운 미노출
  - 폼 제출 후 addRecent 호출
  - 최근 항목 클릭 시 라우터 이동
  - 별표 토글 시 favorites 갱신
  
JSDOM localStorage 환경 사용. `mocked` next/navigation router.

### E2E
이미 검색 기본 동작은 테스트 됨 (player.spec.ts 등). 추가 E2E 불필요.

---

## 7. 접근성

- 드롭다운: `role="listbox"` + 항목 `role="option"`, 키보드 ↑↓ 네비, Enter 선택, Esc 닫기.
- 별표 버튼: `aria-pressed={isFavorite}` + `aria-label="즐겨찾기에 추가/제거"`.
- 인풋: `aria-expanded={dropdownOpen}` + `aria-controls={dropdownId}`.

---

## 8. 마이그레이션 / 롤아웃

- localStorage 키 첫 사용 — 데이터 마이그레이션 없음.
- 기존 `PlayerSearchInput` 사용처 (Home, TopNav 가능성) 모두 자동으로 새 동작 적용. 회귀 테스트 시 TopNav 동작도 확인.
- SEO: 메타데이터 / JSON-LD 변경은 이전 동작 유지 + 추가만. 기존 검색 색인에 부정적 영향 없음.

---

## 9. 비기능 요구사항

- localStorage 동작은 모두 동기, 페이지 로드 영향 없음.
- 드롭다운 렌더는 hydration 후 (initial render 는 닫힘 상태) — SSR/CSR 불일치 회피.
- JSON-LD 는 정적 (env 기반), 클라이언트 JS 실행 없음.

---

## 10. Open Questions / 후속

- **검색 가능한 항목 종류 확장**: 클럽 즐겨찾기는 현재 검색바가 플레이어 전용. 클럽 즐겨찾기는 클럽 페이지에서 별도 토글로 추가 (별도 작업).
- **이름 라벨 캐싱**: 사용자가 자기 태그를 입력 후 player 페이지에서 이름이 fetch 되면 그것을 즐겨찾기/최근에 라벨로 캐싱 가능. 별도 작업.
- **Dynamic OG image**: `app/opengraph-image.tsx` 로 동적 이미지 생성. 본 작업 범위 외.
