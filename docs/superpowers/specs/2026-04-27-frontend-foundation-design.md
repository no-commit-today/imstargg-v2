# imstargg-frontend Foundation 설계

작성일: 2026-04-27
대상: `imstargg-frontend` (Next.js 16.2.1 + React 19.2.4)
범위: 기술스택 도입, 아키텍처 컨벤션, API 레이어, 훅 레이어, SEO 인프라, 테스트 인프라, **플레이어 도메인 reference 구현**까지.

> 이 문서는 "Foundation" spec입니다. 클럽 / 브롤러 / 이벤트 / 랭킹 / 통계 / 배틀 도메인은 별도의 spec으로 분해되어, 본 spec에서 확정한 패턴을 그대로 복제하여 구현됩니다.

---

## 1. 목표와 비목표

### 목표

- 유지보수·확장에 강한 프론트엔드 아키텍처 확립 (레이어 분리, 의존 방향 단방향)
- `https://core-api.imstargg.com`과의 API 연동 패턴 확정
- SEO 기반 인프라 (metadata / sitemap / robots) 확립
- 테스트 인프라 확립 (Vitest + Testing Library + MSW + Playwright)
- 플레이어 도메인을 reference로 완전히 구현하여, 이후 도메인 spec의 템플릿이 되도록 함
- 백엔드 CORS 설정을 본 spec과 동시에 정합되도록 변경

### 비목표

- 클럽 / 브롤러 / 이벤트 / 랭킹 / 통계 / 배틀 도메인의 완전한 구현 (도메인 spec에서 처리)
- 동적 OG 이미지, JSON-LD 구조화 데이터 (도메인 spec에서)
- i18n (현재 한국어 단일)
- 인증/회원 시스템 (현재 익명 사용 only)
- 디자인 시스템 토큰 재정의 (기존 Tailwind 변수 유지)

---

## 2. 핵심 결정사항

| 결정 | 선택 | 이유 |
|---|---|---|
| 데이터 패칭 패러다임 | **RSC-first + 클라이언트 TanStack Query 보조** | SEO/LCP 유리. 인터랙션은 클라이언트 훅으로 자연스럽게 |
| 백엔드 타입 동기화 | **수동 TS 타입 정의** | 단순, 빠른 시작. 변경 시 mapper 한 곳만 수정 |
| 갱신 UX | **1초 간격 자동 폴링 + 완료 시 자동 invalidate** | TanStack Query `refetchInterval`로 깔끔히 구현 |
| SEO 범위 | **표준 패키지** (페이지별 metadata + sitemap + robots + 정적 OG) | Foundation에서 패턴 확정, 도메인 spec에서 동적 OG/JSON-LD 추가 |
| 플레이어/클럽 인덱싱 | **인덱싱 허용** (canonical 포함, 404는 noindex) | 공유 친화적, SEO 가치 |
| UI 라이브러리 | **shadcn/ui** + 기존 도메인 primitives 유지 | Tailwind v4 기반, 코드 소유, a11y 무료 |
| 토스트 | **sonner** (shadcn 기본) | 기존 커스텀 ToastContainer/lib/toast.ts 교체 |
| 테스트 철학 | **네트워크 경계만 모킹 (MSW), 사용자 관점 검증** | 백엔드 `CoreApiContextTest` 정신 — 리팩터링 내성 |
| E2E API | **prod API 직접 호출** | 최고 충실도, 백엔드 변경 안전망 |
| 테스트 태그 | 플레이어 `#88J8Q9RPV`, 클럽 `#2U8CVLQQ9` | 사용자 지정 |

---

## 3. 기술스택

```text
런타임/프레임워크    Next.js 16.2.1, React 19.2.4         (이미 설치)
스타일             Tailwind v4                          (이미 설치)
서버 데이터         Next.js fetch + revalidate (RSC)
클라이언트 데이터    @tanstack/react-query v5
클라이언트 상태      zustand v5                            (필요 시)
폼/검증            react-hook-form + zod                (검색 입력 검증)
런타임 검증         zod                                  (env, 핵심 경계)
HTTP              fetch (네이티브) + 얇은 래퍼
UI 컴포넌트         shadcn/ui (CLI 설치) + Radix + cva
아이콘             lucide-react
토스트             sonner
유틸리티            clsx, tailwind-merge (cn 헬퍼)
테스트 - 단위/통합   Vitest + @testing-library/react + @testing-library/user-event + jsdom
테스트 - 네트워크    MSW v2
테스트 - E2E       Playwright (prod API)
타입체크/린트       tsc --noEmit, ESLint(이미 설치), Prettier
```

**기존 제거**: `components/ToastContainer.tsx`, `lib/toast.ts` → sonner로 교체.

---

## 4. 폴더 구조

```text
imstargg-frontend/
├── app/                                 # Next.js App Router (라우팅 + RSC)
│   ├── layout.tsx                       # 루트 layout (QueryProvider, Toaster, TopNav 포함)
│   ├── page.tsx                         # 홈
│   ├── players/[tag]/                   # ★ reference 도메인
│   │   ├── page.tsx                     # RSC + generateMetadata
│   │   ├── error.tsx
│   │   ├── not-found.tsx
│   │   └── _components/                 # 라우트 전용 컴포넌트 (앞에 _ → 라우팅 제외)
│   ├── clubs/[tag]/                     # placeholder (도메인 spec에서 완성)
│   ├── brawlers/[id]/                   # placeholder
│   ├── events/[id]/                     # placeholder
│   ├── ranking/                         # placeholder
│   ├── sitemap.ts
│   ├── robots.ts
│   ├── globals.css
│   └── not-found.tsx
│
├── src/
│   ├── api/                             # ★ HTTP 추상화 레이어
│   │   ├── client.ts                    # apiFetch 래퍼
│   │   ├── errors.ts                    # ApiError 계층
│   │   ├── types.ts                     # 백엔드 응답 DTO 타입 (수동)
│   │   └── endpoints/
│   │       ├── player.ts                # ★ Foundation에서 구현
│   │       ├── renewal.ts               # ★ Foundation에서 구현
│   │       ├── club.ts                  # 시그니처만 (도메인 spec에서 채움)
│   │       ├── brawler.ts
│   │       ├── event.ts
│   │       ├── ranking.ts
│   │       ├── battle.ts
│   │       └── statistics.ts
│   │
│   ├── domain/                          # ★ 화면용 도메인 모델 + DTO 매퍼
│   │   └── player/
│   │       ├── types.ts
│   │       └── mappers.ts
│   │
│   ├── hooks/
│   │   ├── queries/
│   │   │   ├── queryKeys.ts             # ★ 단일 출처
│   │   │   ├── usePlayer.ts
│   │   │   └── usePlayerBrawlers.ts
│   │   └── usePlayerRenewal.ts          # 폴링 + invalidate
│   │
│   ├── components/
│   │   ├── ui/                          # shadcn (CLI 설치 결과)
│   │   ├── primitives/                  # 기존 도메인 시각 프리미티브 이전
│   │   ├── layout/                      # TopNav, MobileTabBar
│   │   └── shared/                      # PlayerSearchInput, RenewalButton 등
│   │
│   ├── lib/
│   │   ├── cn.ts
│   │   ├── format.ts
│   │   ├── tag.ts                       # 브롤스타즈 태그 정규화
│   │   └── seo.ts                       # buildMetadata 헬퍼
│   │
│   ├── stores/                          # zustand (현재는 비어 있어도 OK)
│   ├── config/
│   │   ├── env.ts                       # zod로 검증
│   │   └── routes.ts                    # 라우트 경로 상수
│   └── providers/
│       └── QueryProvider.tsx
│
├── tests/
│   ├── setup/
│   │   ├── vitest.setup.ts              # MSW server.listen 등
│   │   ├── msw.ts                       # server, handlers 기본
│   │   └── renderWithProviders.tsx      # 테스트용 Provider 래퍼
│   ├── unit/                            # 순수 함수만
│   │   ├── lib/
│   │   │   ├── tag.test.ts
│   │   │   └── format.test.ts
│   │   └── domain/player/mappers.test.ts
│   ├── integration/                     # 컴포넌트 + 훅 + 실제 QueryClient + MSW
│   │   └── player/
│   │       └── playerRenewal.test.tsx
│   └── e2e/                             # Playwright (prod API)
│       ├── fixtures/tags.ts
│       ├── home.spec.ts
│       ├── player.spec.ts
│       └── seo.spec.ts
│
├── public/                              # 자산 (기존 유지)
├── .env.example
├── playwright.config.ts
├── vitest.config.ts
├── next.config.ts
├── tsconfig.json                        # paths: @/* → src/*, @app/* → app/*
└── package.json
```

### 4.1 `src/` 도입과 마이그레이션

- 현재 루트의 `app/`, `components/`, `lib/`은 정리:
  - `app/` → 그대로 (Next.js 라우팅 위치). 단 `app/_components/*`은 그대로 유지하거나 도메인별 페이지의 `_components/`로 이동.
  - 기존 `components/` → `src/components/` (구조에 맞춰 `layout/`, `shared/`, `primitives/`로 분류)
  - 기존 `lib/` → `src/lib/` (단, `lib/data.ts` 같은 mock은 도메인 spec에서 점진 제거)
- `tsconfig.json`의 `paths`: `"@/*": ["src/*"]`, `"@app/*": ["app/*"]`

### 4.2 레이어 의존 방향 (단방향)

```text
app/  ──▶  hooks/  ──▶  api/     ──▶  외부(HTTP)
   ▲          │           │
   │          ▼           ▼
   └─── components/    domain/      ◀── lib/, config/
```

- 컴포넌트는 `api/`를 직접 호출하지 않는다. RSC도 `api/endpoints/*`만 호출.
- 훅은 컴포넌트를 import하지 않는다.
- `domain/`은 외부 의존이 없는 순수 변환 레이어.

---

## 5. API 레이어

### 5.1 `src/config/env.ts`

```ts
import { z } from "zod";
const schema = z.object({
  NEXT_PUBLIC_API_BASE_URL: z.string().url(),
  NEXT_PUBLIC_SITE_URL: z.string().url().default("https://imstargg.com"),
});
export const env = schema.parse({
  NEXT_PUBLIC_API_BASE_URL: process.env.NEXT_PUBLIC_API_BASE_URL,
  NEXT_PUBLIC_SITE_URL: process.env.NEXT_PUBLIC_SITE_URL,
});
```

`.env.example`:

```text
NEXT_PUBLIC_API_BASE_URL=https://core-api.imstargg.com
NEXT_PUBLIC_SITE_URL=https://imstargg.com
```

### 5.2 `src/api/client.ts`

```ts
type FetchOpts = {
  next?: { revalidate?: number; tags?: string[] };
  signal?: AbortSignal;
  method?: "GET" | "POST";
  body?: unknown;
};

export async function apiFetch<T>(path: string, opts: FetchOpts = {}): Promise<T> {
  const res = await fetch(`${env.NEXT_PUBLIC_API_BASE_URL}${path}`, {
    method: opts.method ?? "GET",
    next: opts.next,
    signal: opts.signal,
    body: opts.body ? JSON.stringify(opts.body) : undefined,
    headers: {
      Accept: "application/json",
      ...(opts.body ? { "Content-Type": "application/json" } : {}),
    },
  });
  if (!res.ok) throw await toApiError(res);
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}
```

### 5.3 `src/api/errors.ts`

```ts
export class ApiError extends Error {
  constructor(public status: number, public code: string | null, public path: string, message: string) {
    super(message);
  }
}
export class NotFoundError extends ApiError {}
export class ServerError extends ApiError {}
export class NetworkError extends ApiError {}
```

라우트의 `not-found.tsx` / `error.tsx`에서 에러 종류로 분기 — Foundation에서는 플레이어 페이지에 적용.

### 5.4 `src/api/endpoints/player.ts`

```ts
import { apiFetch, type FetchOpts } from "@/api/client";
import { normalizeTag } from "@/lib/tag";
import type { PlayerResponse, ListResponse, PlayerBrawlerResponse } from "@/api/types";

export const playerApi = {
  get: (tag: string, opts?: FetchOpts) =>
    apiFetch<PlayerResponse>(`/v1/players/${encodeURIComponent(normalizeTag(tag))}`, opts),
  getBrawlers: (tag: string, opts?: FetchOpts) =>
    apiFetch<ListResponse<PlayerBrawlerResponse>>(
      `/v1/players/${encodeURIComponent(normalizeTag(tag))}/brawlers`,
      opts,
    ),
};
```

### 5.5 `src/api/endpoints/renewal.ts`

```ts
export const renewalApi = {
  getPlayer: (tag: string, opts?: FetchOpts) =>
    apiFetch<RenewalResponse>(`/v1/players/${encodeURIComponent(normalizeTag(tag))}/renewal`, opts),
  renewPlayer: (tag: string) =>
    apiFetch<RenewalResponse>(`/v1/players/${encodeURIComponent(normalizeTag(tag))}/renewal`, { method: "POST" }),
  getClub: (tag: string, opts?: FetchOpts) =>
    apiFetch<RenewalResponse>(`/v1/clubs/${encodeURIComponent(normalizeTag(tag))}/renewal`, opts),
  renewClub: (tag: string) =>
    apiFetch<RenewalResponse>(`/v1/clubs/${encodeURIComponent(normalizeTag(tag))}/renewal`, { method: "POST" }),
};
```

### 5.6 `src/lib/tag.ts`

```ts
// 입력: "#88J8Q9RPV", "88J8Q9RPV", "%2388J8Q9RPV"
// 출력 (normalizeTag): "#88J8Q9RPV"
// 출력 (toUrlPath): URL 경로용 인코딩된 형태 ("%2388J8Q9RPV")
export function normalizeTag(input: string): string { ... }
export function stripHash(input: string): string { ... }   // "88J8Q9RPV"
export function toUrlPath(input: string): string { ... }   // encodeURIComponent
```

### 5.7 캐싱 정책 (RSC fetch)

| 영역 | revalidate | 비고 |
|---|---|---|
| 플레이어/클럽 상세 | 60초 | 갱신 버튼 클릭 시 mutation 후 client invalidate. 서버 캐시는 60초 후 자연 갱신 |
| 브롤러/이벤트 메타 | 3600초 | 자주 안 변함 |
| 랭킹 | 300초 | 5분 |
| 통계 | 600초 | 10분 |
| 배틀 목록 | 60초 | 신규 배틀 자주 추가 |

---

## 6. 도메인 모델 & 매퍼

`src/api/types.ts`는 백엔드 응답 DTO를 그대로 정의 (필드명·옵셔널 일치). `src/domain/player/types.ts`는 화면용 타입.

```ts
// src/api/types.ts
export interface PlayerResponse {
  tag: string;
  name: string;
  trophies: number;
  highestTrophies: number;
  // ... 백엔드 PlayerResponse와 1:1
}
export interface RenewalResponse {
  tag: string;
  status: "PENDING" | "RUNNING" | "COMPLETED" | "FAILED";   // RenewalStatus enum
  progressing: boolean;
  canRenew: boolean;
  requestedAt: string;       // ISO 8601
  executedAt: string | null;
  completedAt: string | null;
  renewedAt: string | null;
}
export interface ListResponse<T> { results: T[]; }   // 백엔드 ListResponse 구조 확인 후 정합
```

```ts
// src/domain/player/types.ts
export interface Player {
  tag: string;             // "#88J8Q9RPV"
  name: string;
  trophies: number;
  highestTrophies: number;
  // 화면 친화 필드 추가 가능
}
// src/domain/player/mappers.ts
export function toPlayer(dto: PlayerResponse): Player { ... }
```

> **백엔드 Response 구조 일치 작업**: 구현 단계에서 각 컨트롤러의 실제 Response 클래스를 읽어 `api/types.ts`를 채움. plan에서 별도 task로 명시.

---

## 7. 훅 레이어

### 7.1 `src/hooks/queries/queryKeys.ts`

```ts
export const queryKeys = {
  player: {
    all: ["player"] as const,
    detail: (tag: string) => [...queryKeys.player.all, "detail", normalizeTag(tag)] as const,
    brawlers: (tag: string) => [...queryKeys.player.all, "brawlers", normalizeTag(tag)] as const,
    renewal: (tag: string) => [...queryKeys.player.all, "renewal", normalizeTag(tag)] as const,
  },
  // club, brawler, event, ranking, battle, statistics — 도메인 spec에서 추가
};
```

### 7.2 `src/hooks/queries/usePlayer.ts`

```ts
export function usePlayer(tag: string, options?: { initialData?: Player }) {
  return useQuery({
    queryKey: queryKeys.player.detail(tag),
    queryFn: ({ signal }) => playerApi.get(tag, { signal }).then(toPlayer),
    initialData: options?.initialData,
    staleTime: 60_000,
  });
}
```

### 7.3 `src/hooks/usePlayerRenewal.ts`

```ts
export function usePlayerRenewal(tag: string) {
  const qc = useQueryClient();
  const renewalQuery = useQuery({
    queryKey: queryKeys.player.renewal(tag),
    queryFn: ({ signal }) => renewalApi.getPlayer(tag, { signal }),
    refetchInterval: (q) => (q.state.data?.progressing ? 1000 : false),
    refetchIntervalInBackground: false,
  });

  const prevProgressing = useRef(renewalQuery.data?.progressing ?? false);
  useEffect(() => {
    const cur = renewalQuery.data?.progressing ?? false;
    if (prevProgressing.current && !cur) {
      qc.invalidateQueries({ queryKey: queryKeys.player.all });
      toast.success("갱신 완료");
    }
    prevProgressing.current = cur;
  }, [renewalQuery.data?.progressing, qc]);

  const trigger = useMutation({
    mutationFn: () => renewalApi.renewPlayer(tag),
    onSuccess: (data) => {
      qc.setQueryData(queryKeys.player.renewal(tag), data);
      toast.info("갱신 요청됨");
    },
    onError: () => toast.error("갱신 요청 실패"),
  });

  return {
    renewal: renewalQuery.data,
    canRenew: renewalQuery.data?.canRenew ?? false,
    progressing: renewalQuery.data?.progressing ?? false,
    trigger: trigger.mutate,
    triggering: trigger.isPending,
  };
}
```

### 7.4 RSC initialData 패턴

```tsx
// app/players/[tag]/page.tsx (RSC)
export default async function PlayerPage({ params }) {
  const dto = await playerApi.get(params.tag, { next: { revalidate: 60 } })
    .catch(notFoundOrThrow);
  const player = toPlayer(dto);
  return <PlayerContent tag={params.tag} initialPlayer={player} />;
}

// _components/PlayerContent.tsx (Client)
"use client";
export function PlayerContent({ tag, initialPlayer }) {
  const { data } = usePlayer(tag, { initialData: initialPlayer });
  // ...
}
```

---

## 8. SEO 인프라

### 8.1 `src/lib/seo.ts`

```ts
const SITE = {
  name: "ImStarGG",
  url: env.NEXT_PUBLIC_SITE_URL,
  ogImage: "/assets/logo-full.png",
};
export function buildMetadata(input: {
  title: string; description: string; path: string;
  image?: string; noindex?: boolean;
}): Metadata {
  const url = `${SITE.url}${input.path}`;
  const image = input.image ?? SITE.ogImage;
  return {
    title: input.title,
    description: input.description,
    alternates: { canonical: url },
    robots: input.noindex ? { index: false, follow: false } : undefined,
    openGraph: {
      title: input.title, description: input.description, url,
      siteName: SITE.name,
      images: [{ url: image, width: 1200, height: 630 }],
      locale: "ko_KR", type: "website",
    },
    twitter: { card: "summary_large_image", title: input.title, description: input.description, images: [image] },
  };
}
```

### 8.2 페이지별 metadata

- `app/layout.tsx`: 사이트 기본 metadata + `metadataBase`.
- 정적 페이지: 모듈 레벨 `export const metadata = buildMetadata({...})`.
- 동적 페이지: `export async function generateMetadata({ params })` 내부에서 RSC fetch와 동일한 `apiFetch`를 호출. Next.js의 fetch dedupe로 중복 호출 방지.
- 404/에러 시: `noindex: true` + 일반 메시지로 대체.

### 8.3 `app/sitemap.ts`

- 정적: `/`, `/ranking`, `/brawlers`, `/events`, `/players`(검색 진입), `/clubs`(검색 진입)
- 동적: 모든 브롤러, 모든 이벤트
- 인기 플레이어/클럽: 인덱싱은 허용하되 sitemap 등록은 도메인 spec에서 결정 (랭킹 N위까지 등)

### 8.4 `app/robots.ts`

```ts
export default function robots(): MetadataRoute.Robots {
  return {
    rules: { userAgent: "*", allow: "/", disallow: ["/api/"] },
    sitemap: `${env.NEXT_PUBLIC_SITE_URL}/sitemap.xml`,
  };
}
```

### 8.5 도메인 spec에서 추가 예정

- 동적 OG 이미지 (`opengraph-image.tsx`) — 플레이어/클럽 카드형
- JSON-LD 구조화 데이터 — 브롤러(`Game`), 이벤트(`Event`) 등
- 랭킹 상위 플레이어/클럽 sitemap 추가

---

## 9. UI 컴포넌트 전략

### 9.1 shadcn 도입 범위 (Foundation)

다음 컴포넌트만 우선 설치 — 플레이어 도메인에 필요한 것 + 공통:

```text
button, input, label, dialog, sheet, tabs, dropdown-menu,
tooltip, skeleton, select, command, sonner, card, badge, separator,
pagination
```

나머지(form 등)는 도메인 spec에서 필요 시 추가.

### 9.2 기존 컴포넌트 정리

- `components/primitives/*` → `src/components/primitives/` 그대로 이동 (도메인 시각요소, 유지)
- `components/TopNav.tsx`, `MobileTabBar.tsx` → `src/components/layout/`
- `components/RenewalButton.tsx` → `src/components/shared/RenewalButton.tsx` (shadcn `Button` 위에 재구현, `usePlayerRenewal` / 향후 `useClubRenewal` 훅 사용)
- `components/ToastContainer.tsx` + `lib/toast.ts` → 제거. 루트 layout에 `<Toaster />`(sonner) 한 줄, 호출은 `import { toast } from "sonner"`.

### 9.3 디자인 토큰

기존 `app/globals.css`, `app/styles/*.css`의 CSS 변수를 그대로 유지. shadcn 설치 시 그 위에 shadcn 토큰 추가 (충돌 시 기존 변수 우선).

---

## 10. 환경변수 & CORS (백엔드 변경 포함)

### 10.1 프론트

- `.env.example` 추가 (위 §5.1)
- `.env.local`은 사용자가 로컬에 작성 (gitignore)

### 10.2 백엔드 CORS (core-api)

**작업 위치**: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/config/`

```kotlin
// CorsProperties.kt
@ConfigurationProperties("app.cors")
data class CorsProperties(
    val allowedOrigins: List<String> = emptyList(),
)

// WebMvcConfig.kt
@Configuration
@EnableConfigurationProperties(CorsProperties::class)
class WebMvcConfig(private val cors: CorsProperties) : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        if (cors.allowedOrigins.isEmpty()) return
        registry.addMapping("/v1/**")
            .allowedOrigins(*cors.allowedOrigins.toTypedArray())
            .allowedMethods("GET", "POST", "OPTIONS")
            .allowCredentials(false)
            .maxAge(3600)
    }
}
```

**프로필별 설정**:

- `application.yml` (기본): `app.cors.allowed-origins:` (비어 있음 → 차단)
- `application-prod.yml`:
  ```yaml
  app:
    cors:
      allowed-origins:

        - https://imstargg.com
        - http://localhost:3000
  ```
  > localhost:3000은 **로컬에서 prod 프로필로 백엔드를 띄워 prod DB에 붙여 테스트할 때**만 의미가 있음. 실제 prod 배포에서도 localhost 허용은 보안적으로 무해 (브라우저는 자기 origin이 localhost일 때만 사용).

**테스트**: 작은 단위 테스트 (CorsProperties 바인딩) + `CoreApiContextTest` 기반 통합 테스트로 OPTIONS 응답 헤더 검증.

---

## 11. 테스트 전략

### 11.1 핵심 원칙 (백엔드 `CoreApiContextTest` 정신)

1. **네트워크 경계에서만 모킹** — MSW로 HTTP 응답만 가짜로. 우리 코드(`api/endpoints/*`, `hooks/*`, 컴포넌트)는 절대 모킹하지 않는다.
2. **사용자 관점에서 테스트** — Testing Library의 `getByRole` / `getByText`로 화면에 보이는 것을 검증. 컴포넌트 내부 state·props·구현 상세는 검증하지 않는다.
3. **통합 우선** — 컴포넌트 + 훅 + 실제 QueryClient + MSW를 묶은 통합 테스트가 기본.
4. **단위 테스트는 순수 함수에만** — `lib/tag.ts`, `domain/*/mappers.ts`, `lib/format.ts`.
5. **E2E는 prod API 직접** — 가장 높은 충실도. 환경 의존이지만 안전망 역할.

### 11.2 금지

- `vi.mock("@/hooks/...")`, `vi.mock("@/api/...")` — 우리 코드 모킹 금지
- `expect(mockFn).toHaveBeenCalledWith(...)` 위주 검증 — 구현 상세 의존
- shallow rendering, `wrapper.find(...).props()`
- 수동 `act()`로 시간 제어 — `findBy*`의 자동 retry로 충분

### 11.3 테스트 베이스

```tsx
// tests/setup/renderWithProviders.tsx
export function renderWithProviders(ui: React.ReactElement) {
  const qc = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 } },
  });
  return {
    ...render(
      <QueryClientProvider client={qc}>
        <Toaster />
        {ui}
      </QueryClientProvider>,
    ),
    queryClient: qc,
  };
}
```

```ts
// tests/setup/msw.ts
export const server = setupServer();
// vitest.setup.ts에서 listen / resetHandlers / close
```

### 11.4 Foundation에 포함되는 테스트

**단위 (Vitest):**

- `tests/unit/lib/tag.test.ts` — `#PREFIX`, URL 인코딩 모든 입력 케이스
- `tests/unit/lib/format.test.ts` — 숫자/날짜 포맷
- `tests/unit/domain/player/mappers.test.ts` — `toPlayer` 변환

**통합 (Vitest + Testing Library + MSW):**

- `tests/integration/player/playerRenewal.test.tsx` — 갱신 클릭 → "갱신 요청됨" 토스트 → progressing이 false로 바뀌면 → "갱신 완료" 토스트 + 플레이어 쿼리 invalidate
- `tests/integration/player/playerSearch.test.tsx` — 검색 입력 → 정상 태그 → 라우터 push 호출 검증 (라우터는 Next 제공 mock 사용)

**E2E (Playwright, prod API):**

1. 홈 진입 → 메타데이터 확인 (`<title>` 포함)
2. 검색 폼에 `#88J8Q9RPV` → `/players/...` 이동 → 닉네임/트로피 표시
3. 플레이어 페이지 갱신 버튼 → "갱신 요청됨" 토스트 → 1초 폴링 동안 버튼 비활성화 → "갱신 완료" 토스트 (실제 백엔드의 `progressing` 동작 의존)
4. `/sitemap.xml` 200 + 적어도 정적 URL 포함 / `/robots.txt` 200

### 11.5 설정

- `vitest.config.ts`: jsdom, `setupFiles: ["tests/setup/vitest.setup.ts"]`, `@/` 별칭, `globals: true`
- `playwright.config.ts`: chromium 단일, `baseURL: http://localhost:3000`, `webServer: { command: "pnpm dev", reuseExistingServer: true }`, `retries: 1`
- `package.json` 스크립트:
  ```
  "test": "vitest",
  "test:run": "vitest run",
  "test:e2e": "playwright test",
  "typecheck": "tsc --noEmit",
  "lint": "eslint",
  "build": "next build"
  ```

---

## 12. Reference 도메인 (플레이어) 구현 범위

Foundation에서 플레이어 도메인을 **end-to-end 완성**하여, 이후 도메인 spec(클럽/브롤러/이벤트/랭킹)이 그대로 복제할 수 있는 템플릿이 되도록 한다.

**포함 범위:**

- API: `api/endpoints/player.ts`, `api/endpoints/renewal.ts`, `api/types.ts` (Player·PlayerBrawler·Renewal·ListResponse)
- 도메인: `domain/player/{types.ts,mappers.ts}`
- 훅: `hooks/queries/{usePlayer,usePlayerBrawlers}.ts`, `hooks/usePlayerRenewal.ts`, `hooks/queries/queryKeys.ts`(player만)
- 페이지: `app/players/[tag]/{page.tsx,error.tsx,not-found.tsx}` + `_components/*` (기존 `PlayerBattles`, `PlayerStats`, `PlayerSidebar`, `PlayerBrawlers`, `PlayerContent`을 API 데이터로 연결, mock 제거)
- 컴포넌트: `components/shared/PlayerSearchInput.tsx` (홈/TopNav에서 사용), `components/shared/RenewalButton.tsx` (플레이어/클럽 모두 지원하도록 prop 다형)
- 메타데이터: 플레이어 페이지 `generateMetadata` 구현 (인덱싱 허용, canonical, OG)
- 테스트: §11.4의 단위·통합·E2E 모두 포함

**Foundation에서는 placeholder로만 남기는 것 (도메인 spec에서 채움):**

- 클럽, 브롤러, 이벤트, 랭킹, 통계, 배틀 페이지: 빌드만 통과하도록 임시 콘텐츠 + 기존 mock 유지 (단, mock을 새 코드 패턴으로 끌어오지는 않음 — 도메인 spec에서 한꺼번에 정리)

---

## 13. 구현 순서 (plan에서 상세화)

```text
1. 의존성 설치 + 설정 파일
   - pnpm add @tanstack/react-query @tanstack/react-query-devtools zustand
             zod react-hook-form @hookform/resolvers sonner
             clsx tailwind-merge class-variance-authority lucide-react
   - pnpm add -D vitest @vitest/ui @testing-library/react @testing-library/user-event
                 @testing-library/jest-dom jsdom msw
                 @playwright/test
   - shadcn init + 컴포넌트 설치 (§9.1)
   - vitest.config.ts, playwright.config.ts, tsconfig paths

2. 폴더 구조 마이그레이션
   - src/ 생성, 기존 lib/ components/ 이동
   - tsconfig paths 업데이트
   - 빌드 통과 확인

3. 코어 인프라 작성
   - config/env, api/client, api/errors, lib/tag, lib/cn, lib/format, lib/seo
   - providers/QueryProvider, app/layout.tsx에 Provider/Toaster 적용

4. 토스트 교체
   - 기존 ToastContainer + lib/toast.ts 제거
   - sonner Toaster 추가, 호출부 sonner로 변경

5. 백엔드 CORS 설정 (별도 작은 변경)
   - CorsProperties, WebMvcConfig
   - application-prod.yml에 allowed-origins
   - 통합 테스트

6. SEO 인프라
   - app/sitemap.ts, app/robots.ts
   - app/layout.tsx metadata + metadataBase
   - lib/seo.buildMetadata

7. 플레이어 도메인 reference 구현
   - api/types.ts (Player/Renewal/ListResponse)
   - api/endpoints/player, renewal
   - domain/player/{types,mappers}
   - hooks/queries/queryKeys, usePlayer, usePlayerBrawlers
   - hooks/usePlayerRenewal
   - components/shared/PlayerSearchInput, RenewalButton
   - app/players/[tag]/page.tsx + generateMetadata + error.tsx + not-found.tsx
   - app/players/[tag]/_components/* 를 API 데이터로 연결

8. 홈 + TopNav 검색 동작 연결
   - PlayerSearchInput 사용, 입력 → 정규화 → /players/[tag]

9. 테스트 작성 및 통과
   - 단위 (§11.4)
   - 통합 (§11.4)
   - E2E (§11.4)

10. 기존 mock 페이지(클럽/브롤러/이벤트/랭킹) 빌드 통과 점검
    - 컴파일 에러만 정리, 동작 mock 유지
    - 도메인 spec에서 본격 구현
```

---

## 14. 검증 기준 (Definition of Done)

- [ ] `pnpm tsc --noEmit` 에러 0
- [ ] `pnpm lint` 통과
- [ ] `pnpm build` 성공
- [ ] `pnpm test:run` (Vitest) 통과
- [ ] `pnpm test:e2e` (Playwright, prod API) 통과
- [ ] 백엔드 `./gradlew :core:core-api:contextTest` 통과 (CORS 추가분)
- [ ] 프론트 dev 서버에서 플레이어 페이지(`/players/%2388J8Q9RPV`) 실데이터 표시
- [ ] 갱신 버튼 클릭 → 1초 폴링 → 갱신 완료 토스트 → 플레이어 데이터 새로고침 (육안 확인)
- [ ] sitemap.xml / robots.txt 정상 응답, 메타데이터 (`view-source`로 OG·canonical 확인)

---

## 15. 도메인 spec 분해 계획 (참고)

본 Foundation spec 이후 작성될 도메인 spec 목록:

1. **클럽 도메인 spec** — 검색, 상세, 멤버 목록, 갱신
2. **브롤러 + 이벤트 spec** — 메타데이터 위주, 통계 일부 연결
3. **랭킹 + 통계 spec** — 페이지네이션, 필터, 통계 시각화
4. **홈 + 배틀 + 검색 향상 spec** — 홈 위젯, 배틀 상세, 검색 자동완성
5. **SEO 강화 spec** — 동적 OG, JSON-LD, sitemap 확장

각 spec은 본 Foundation의 패턴(폴더 구조, API 레이어, 훅 패턴, 테스트 전략)을 그대로 따른다.

---

## 16. 위험 요소 및 대응

| 위험 | 대응 |
|---|---|
| Next.js 16 + React 19가 매우 새로워 라이브러리 호환성 이슈 가능 | 의존성 설치 후 `pnpm build` 즉시 실행. 이슈 발생 시 `node_modules/next/dist/docs/`의 실제 문서 참조 (AGENTS.md 지시) |
| TanStack Query v5 + RSC 통합에서 `initialData` vs `HydrationBoundary` 선택 | Foundation은 `initialData` 단일 패턴으로 통일. 도메인 spec에서 데이터 양이 큰 페이지는 `HydrationBoundary` 도입 검토 |
| prod API 의존 E2E의 불안정성 (네트워크/데이터 변동) | retries 1회, 데이터 변동 가정한 가벼운 어서션 (`>=` 트로피 검증 등) |
| 기존 mock 코드와 새 패턴 공존 시 혼란 | mock 사용 페이지에 주석으로 "TODO(domain-spec)" 명시 |
| 백엔드 응답 필드 변경 시 타입 누락 위험 (수동 동기화 한계) | 매퍼 1곳에 통합, 응답 스키마 변경은 백엔드 PR에서 같이 프론트 PR 생성하는 컨벤션. 향후 OpenAPI 자동화는 별도 spec |
| sonner 토스트 마이그레이션 누락 | grep으로 `from "@/lib/toast"` / `from "../lib/toast"` 모두 제거 확인 |

---

## 부록 A. 참고 백엔드 위치

- 컨트롤러: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/*Controller.kt`
- 응답 DTO: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/response/*Response.kt`
- 도메인: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/`
- 통합 테스트 베이스: `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/CoreApiContextTest.kt`

## 부록 B. 테스트 태그 (사용자 지정)

- 플레이어: `#88J8Q9RPV` (URL 인코딩: `%2388J8Q9RPV`)
- 클럽: `#2U8CVLQQ9` (URL 인코딩: `%232U8CVLQQ9`)
