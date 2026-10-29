# imstargg-frontend Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** imstargg-frontend의 아키텍처 기반(API/훅/SEO/테스트 인프라)을 구축하고, 플레이어 도메인을 reference 구현으로 완성한다.

**Architecture:** RSC-first 데이터 패칭 + 클라이언트 TanStack Query 보조. `app/`(라우팅/RSC) → `src/hooks/`(비즈니스 로직) → `src/api/`(HTTP 추상화) → 외부 API 단방향 의존. `src/domain/`은 DTO→화면 모델 매퍼 레이어. shadcn/ui로 일반 UI, 기존 도메인 primitives 유지.

**Tech Stack:** Next.js 16.2.1, React 19.2.4, Tailwind v4, TanStack Query v5, zustand v5, zod, react-hook-form, sonner, shadcn/ui, lucide-react, Vitest + Testing Library + MSW v2, Playwright. 백엔드는 Spring Boot Kotlin (CORS 설정 추가 작업 포함).

**Spec 참조:** `docs/superpowers/specs/2026-04-27-frontend-foundation-design.md`

**테스트 태그:** 플레이어 `#88J8Q9RPV`, 클럽 `#2U8CVLQQ9`.

**작업 디렉토리 컨벤션:** 별도 명시 없으면 `imstargg-frontend/` 기준. 백엔드 작업은 `imstargg-backend/` 기준.

---

## Phase 0: 사전 점검

### Task 0.1: Next.js 16 문서 인덱스 확인

**Files:**
- Read: `imstargg-frontend/node_modules/next/dist/docs/`

- [ ] **Step 1: Next.js 16 docs 디렉토리 존재 확인**

```bash
cd imstargg-frontend
ls node_modules/next/dist/docs/ | head -20
```

Expected: 디렉토리 또는 마크다운 파일 목록. `app/`, `metadata.md`, `caching.md` 등 — 작업 중 의문 시 이 위치 참조.

- [ ] **Step 2: 현재 빌드 통과 여부 베이스라인 확인**

```bash
cd imstargg-frontend && pnpm build
```

Expected: 통과. 통과하지 않으면 이슈 기록 후 진행 (현 시점 mock 코드 기준).

---

## Phase 1: 의존성 설치 & 설정 파일

### Task 1.1: 런타임 의존성 추가

**Files:**
- Modify: `imstargg-frontend/package.json`

- [ ] **Step 1: 런타임 의존성 설치**

```bash
cd imstargg-frontend
pnpm add @tanstack/react-query@^5 @tanstack/react-query-devtools@^5 zustand@^5 \
  zod@^3 react-hook-form@^7 @hookform/resolvers@^3 \
  sonner@^1 \
  clsx tailwind-merge class-variance-authority lucide-react
```

Expected: package.json dependencies에 위 패키지가 추가됨. 설치 성공.

- [ ] **Step 2: dev 의존성 설치**

```bash
pnpm add -D vitest@^2 @vitest/ui @vitejs/plugin-react \
  @testing-library/react@^16 @testing-library/user-event@^14 @testing-library/jest-dom@^6 \
  jsdom msw@^2 \
  @playwright/test@^1 \
  prettier
```

Expected: 위 dev 패키지 모두 설치 성공.

- [ ] **Step 3: Playwright 브라우저 설치**

```bash
pnpm exec playwright install chromium
```

Expected: chromium 다운로드 성공.

- [ ] **Step 4: 커밋**

```bash
git add package.json pnpm-lock.yaml
git commit -m "chore(frontend): add runtime and test dependencies"
```

### Task 1.2: tsconfig paths 설정

**Files:**
- Modify: `imstargg-frontend/tsconfig.json`

- [ ] **Step 1: 현재 tsconfig 확인**

```bash
cat imstargg-frontend/tsconfig.json
```

- [ ] **Step 2: paths 추가**

`compilerOptions.paths`를 다음으로 교체 (기존 `"@/*": ["./*"]`이 있다면 대체):

```json
{
  "compilerOptions": {
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"],
      "@app/*": ["app/*"]
    }
  }
}
```

- [ ] **Step 3: 빌드 통과 확인 (현 시점 코드는 `@/`를 root로 쓰는 곳이 있을 수 있음)**

```bash
cd imstargg-frontend && pnpm tsc --noEmit
```

Expected: `@/`로 import하는 기존 파일이 있다면 에러 발생. 발견 시 해당 import를 임시로 상대 경로 또는 `@app/`로 수정.

- [ ] **Step 4: 커밋**

```bash
git add tsconfig.json
git commit -m "chore(frontend): map @/* to src/* and @app/* to app/*"
```

### Task 1.3: Vitest 설정

**Files:**
- Create: `imstargg-frontend/vitest.config.ts`
- Create: `imstargg-frontend/tests/setup/vitest.setup.ts`
- Create: `imstargg-frontend/tests/setup/msw.ts`
- Modify: `imstargg-frontend/package.json` (scripts)

- [ ] **Step 1: `tests/setup/msw.ts` 작성**

```typescript
import { setupServer } from "msw/node";

export const server = setupServer();
```

- [ ] **Step 2: `tests/setup/vitest.setup.ts` 작성**

```typescript
import "@testing-library/jest-dom/vitest";
import { afterAll, afterEach, beforeAll } from "vitest";
import { server } from "./msw";

beforeAll(() => server.listen({ onUnhandledRequest: "error" }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());
```

- [ ] **Step 3: `vitest.config.ts` 작성**

```typescript
import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import { fileURLToPath } from "node:url";

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
      "@app": fileURLToPath(new URL("./app", import.meta.url)),
    },
  },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./tests/setup/vitest.setup.ts"],
    css: false,
    include: ["tests/**/*.test.{ts,tsx}"],
    exclude: ["tests/e2e/**"],
  },
});
```

- [ ] **Step 4: `package.json` scripts 추가**

`scripts` 필드를 다음과 같이 변경:

```json
"scripts": {
  "dev": "next dev",
  "build": "next build",
  "start": "next start",
  "lint": "eslint",
  "typecheck": "tsc --noEmit",
  "test": "vitest",
  "test:run": "vitest run",
  "test:e2e": "playwright test"
}
```

- [ ] **Step 5: vitest 실행으로 환경 확인**

```bash
pnpm test:run
```

Expected: "No test files found" 또는 0 tests passed. 에러 없이 vitest 자체 기동 확인.

- [ ] **Step 6: 커밋**

```bash
git add vitest.config.ts tests/setup package.json
git commit -m "chore(frontend): configure vitest with msw and jsdom"
```

### Task 1.4: Playwright 설정

**Files:**
- Create: `imstargg-frontend/playwright.config.ts`
- Create: `imstargg-frontend/tests/e2e/fixtures/tags.ts`

- [ ] **Step 1: `tests/e2e/fixtures/tags.ts` 작성**

```typescript
export const TEST_TAGS = {
  player: "#88J8Q9RPV",
  playerEncoded: "%2388J8Q9RPV",
  club: "#2U8CVLQQ9",
  clubEncoded: "%232U8CVLQQ9",
} as const;
```

- [ ] **Step 2: `playwright.config.ts` 작성**

```typescript
import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./tests/e2e",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 1,
  workers: process.env.CI ? 1 : undefined,
  reporter: "list",
  use: {
    baseURL: "http://localhost:3000",
    trace: "retain-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
  webServer: {
    command: "pnpm dev",
    url: "http://localhost:3000",
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
});
```

- [ ] **Step 3: 커밋**

```bash
git add playwright.config.ts tests/e2e/fixtures
git commit -m "chore(frontend): configure playwright with prod api e2e"
```

### Task 1.5: `.env.example` & gitignore 정리

**Files:**
- Create: `imstargg-frontend/.env.example`
- Modify: `imstargg-frontend/.gitignore`

- [ ] **Step 1: `.env.example` 작성**

```text
NEXT_PUBLIC_API_BASE_URL=https://core-api.imstargg.com
NEXT_PUBLIC_SITE_URL=https://imstargg.com
```

- [ ] **Step 2: `.gitignore` 검사**

```bash
grep -E "^\.env(\.local)?$|^\.env\*$" imstargg-frontend/.gitignore
```

Expected: `.env*.local` 또는 동등한 항목 존재 (Next.js 기본 gitignore에 포함). 누락 시 추가:

```
.env*.local
```

- [ ] **Step 3: 로컬 `.env.local` 생성 (커밋 안 함)**

```bash
echo "NEXT_PUBLIC_API_BASE_URL=https://core-api.imstargg.com" > imstargg-frontend/.env.local
echo "NEXT_PUBLIC_SITE_URL=https://imstargg.com" >> imstargg-frontend/.env.local
```

- [ ] **Step 4: 커밋**

```bash
git add .env.example .gitignore
git commit -m "chore(frontend): add env example with prod api base url"
```

---

## Phase 2: 폴더 구조 마이그레이션

### Task 2.1: `src/` 디렉토리 생성 및 기존 코드 이동

**Files:**
- Move: `imstargg-frontend/components/` → `imstargg-frontend/src/components/`
- Move: `imstargg-frontend/lib/` → `imstargg-frontend/src/lib/`

- [ ] **Step 1: 디렉토리 이동**

```bash
cd imstargg-frontend
mkdir -p src
git mv components src/components
git mv lib src/lib
```

- [ ] **Step 2: 기존 `app/_components/` 안의 import 경로 검사**

```bash
grep -r "from \"@/components" app/ 2>/dev/null
grep -r "from \"@/lib" app/ 2>/dev/null
grep -r "from \"\.\./" app/ 2>/dev/null
grep -r "from \"\.\.\.\." app/ src/ 2>/dev/null || true
```

- [ ] **Step 3: import 정리**

`@/` 별칭이 이미 `src/*`로 매핑되었으므로 `@/components/...`, `@/lib/...` import는 **그대로 동작**해야 함. 만약 `app/`에서 컴포넌트를 import하던 상대경로가 깨졌다면 `@/components/...`로 변경.

- [ ] **Step 4: 빌드 확인**

```bash
pnpm tsc --noEmit
pnpm build
```

Expected: 둘 다 통과. 실패 시 import 경로 수정.

- [ ] **Step 5: 커밋**

```bash
git add -A
git commit -m "refactor(frontend): move components and lib into src/"
```

### Task 2.2: 빈 src 하위 디렉토리 구조 생성

**Files:**
- Create: `imstargg-frontend/src/api/`, `endpoints/`, `domain/`, `hooks/queries/`, `stores/`, `config/`, `providers/`, `components/{ui,layout,shared}/`

- [ ] **Step 1: 디렉토리 생성 + .gitkeep**

```bash
cd imstargg-frontend/src
for d in api api/endpoints domain hooks hooks/queries stores config providers \
  components/ui components/layout components/shared; do
  mkdir -p "$d"
done
# 비어있는 디렉토리는 .gitkeep으로 추적
find api domain hooks stores config providers components/ui components/layout components/shared -type d -empty -exec touch {}/.gitkeep \;
```

- [ ] **Step 2: 기존 `src/components/primitives/`, `src/components/{TopNav,MobileTabBar,RenewalButton}.tsx` 분류**

```bash
mkdir -p src/components/layout src/components/shared
git mv src/components/TopNav.tsx src/components/layout/TopNav.tsx
git mv src/components/MobileTabBar.tsx src/components/layout/MobileTabBar.tsx
git mv src/components/RenewalButton.tsx src/components/shared/RenewalButton.tsx
git mv src/components/ToastContainer.tsx src/components/shared/ToastContainer.tsx
```

> 주의: `src/components/primitives/`는 그대로 유지.

- [ ] **Step 3: import 경로 갱신**

```bash
grep -rl "@/components/TopNav" app/ src/ | xargs sed -i '' 's|@/components/TopNav|@/components/layout/TopNav|g'
grep -rl "@/components/MobileTabBar" app/ src/ | xargs sed -i '' 's|@/components/MobileTabBar|@/components/layout/MobileTabBar|g'
grep -rl "@/components/RenewalButton" app/ src/ | xargs sed -i '' 's|@/components/RenewalButton|@/components/shared/RenewalButton|g'
grep -rl "@/components/ToastContainer" app/ src/ | xargs sed -i '' 's|@/components/ToastContainer|@/components/shared/ToastContainer|g'
```

- [ ] **Step 4: 빌드 확인**

```bash
pnpm tsc --noEmit
pnpm build
```

Expected: 통과.

- [ ] **Step 5: 커밋**

```bash
git add -A
git commit -m "refactor(frontend): organize components into layout/shared/primitives/ui"
```

---

## Phase 3: 코어 유틸 (TDD)

### Task 3.1: `lib/cn` 유틸

**Files:**
- Create: `imstargg-frontend/src/lib/cn.ts`
- Create: `imstargg-frontend/tests/unit/lib/cn.test.ts`

- [ ] **Step 1: 실패하는 테스트 작성**

`tests/unit/lib/cn.test.ts`:

```typescript
import { describe, it, expect } from "vitest";
import { cn } from "@/lib/cn";

describe("cn", () => {
  it("merges class names with whitespace", () => {
    expect(cn("a", "b")).toBe("a b");
  });
  it("filters falsy values", () => {
    expect(cn("a", false, undefined, null, "b")).toBe("a b");
  });
  it("dedupes tailwind conflicts (last wins)", () => {
    expect(cn("p-2", "p-4")).toBe("p-4");
  });
});
```

- [ ] **Step 2: 테스트 실행 (실패 확인)**

```bash
pnpm test:run tests/unit/lib/cn.test.ts
```

Expected: FAIL — `Cannot find module '@/lib/cn'`.

- [ ] **Step 3: 구현**

`src/lib/cn.ts`:

```typescript
import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs));
}
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
pnpm test:run tests/unit/lib/cn.test.ts
```

Expected: PASS (3 tests).

- [ ] **Step 5: 커밋**

```bash
git add src/lib/cn.ts tests/unit/lib/cn.test.ts
git commit -m "feat(frontend): add cn util that merges tailwind classes"
```

### Task 3.2: `lib/tag` — 브롤스타즈 태그 정규화 (TDD)

**Files:**
- Create: `imstargg-frontend/src/lib/tag.ts`
- Create: `imstargg-frontend/tests/unit/lib/tag.test.ts`

- [ ] **Step 1: 실패하는 테스트 작성**

`tests/unit/lib/tag.test.ts`:

```typescript
import { describe, it, expect } from "vitest";
import { normalizeTag, stripHash, toUrlPath, isValidTag } from "@/lib/tag";

describe("normalizeTag", () => {
  it.each([
    ["#88J8Q9RPV", "#88J8Q9RPV"],
    ["88J8Q9RPV", "#88J8Q9RPV"],
    ["%2388J8Q9RPV", "#88J8Q9RPV"],
    ["%2388j8q9rpv", "#88J8Q9RPV"],
    [" #88J8Q9RPV ", "#88J8Q9RPV"],
  ])("normalizes %s -> %s", (input, expected) => {
    expect(normalizeTag(input)).toBe(expected);
  });
});

describe("stripHash", () => {
  it("removes leading hash", () => {
    expect(stripHash("#88J8Q9RPV")).toBe("88J8Q9RPV");
    expect(stripHash("88J8Q9RPV")).toBe("88J8Q9RPV");
    expect(stripHash("%2388J8Q9RPV")).toBe("88J8Q9RPV");
  });
});

describe("toUrlPath", () => {
  it("encodes hash as %23", () => {
    expect(toUrlPath("#88J8Q9RPV")).toBe("%2388J8Q9RPV");
    expect(toUrlPath("88J8Q9RPV")).toBe("%2388J8Q9RPV");
  });
});

describe("isValidTag", () => {
  it("accepts valid characters only", () => {
    expect(isValidTag("#88J8Q9RPV")).toBe(true);
    expect(isValidTag("88J8Q9RPV")).toBe(true);
  });
  it("rejects invalid characters", () => {
    expect(isValidTag("##ABC")).toBe(false);
    expect(isValidTag("ABC?")).toBe(false);
    expect(isValidTag("")).toBe(false);
    expect(isValidTag("#")).toBe(false);
  });
});
```

- [ ] **Step 2: 테스트 실행 (실패 확인)**

```bash
pnpm test:run tests/unit/lib/tag.test.ts
```

Expected: FAIL — module not found.

- [ ] **Step 3: 구현**

`src/lib/tag.ts`:

```typescript
const VALID_BODY = /^[0289CGJLPQRUVY]+$/;

function decode(input: string): string {
  let s = input.trim();
  try {
    if (s.includes("%")) s = decodeURIComponent(s);
  } catch {
    // ignore — fall through to validation
  }
  return s.toUpperCase();
}

export function normalizeTag(input: string): string {
  const decoded = decode(input);
  const body = decoded.startsWith("#") ? decoded.slice(1) : decoded;
  return `#${body}`;
}

export function stripHash(input: string): string {
  return normalizeTag(input).slice(1);
}

export function toUrlPath(input: string): string {
  return encodeURIComponent(normalizeTag(input));
}

export function isValidTag(input: string): boolean {
  if (!input) return false;
  const decoded = decode(input);
  if (decoded === "#" || decoded === "") return false;
  const body = decoded.startsWith("#") ? decoded.slice(1) : decoded;
  if (body.length === 0) return false;
  return VALID_BODY.test(body);
}
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
pnpm test:run tests/unit/lib/tag.test.ts
```

Expected: PASS (모든 케이스).

- [ ] **Step 5: 커밋**

```bash
git add src/lib/tag.ts tests/unit/lib/tag.test.ts
git commit -m "feat(frontend): add tag normalization utilities"
```

### Task 3.3: `lib/format` (TDD)

**Files:**
- Create: `imstargg-frontend/src/lib/format.ts`
- Create: `imstargg-frontend/tests/unit/lib/format.test.ts`

- [ ] **Step 1: 실패하는 테스트 작성**

```typescript
import { describe, it, expect } from "vitest";
import { formatNumber, formatRelativeTime } from "@/lib/format";

describe("formatNumber", () => {
  it("uses ko-KR thousands separators", () => {
    expect(formatNumber(1234567)).toBe("1,234,567");
    expect(formatNumber(0)).toBe("0");
  });
});

describe("formatRelativeTime", () => {
  it("returns '방금 전' for very recent times", () => {
    const now = new Date("2026-04-27T10:00:00Z");
    expect(formatRelativeTime("2026-04-27T09:59:55Z", now)).toBe("방금 전");
  });
  it("formats minute differences", () => {
    const now = new Date("2026-04-27T10:00:00Z");
    expect(formatRelativeTime("2026-04-27T09:55:00Z", now)).toBe("5분 전");
  });
  it("formats hour differences", () => {
    const now = new Date("2026-04-27T10:00:00Z");
    expect(formatRelativeTime("2026-04-27T07:00:00Z", now)).toBe("3시간 전");
  });
});
```

- [ ] **Step 2: 실행 (실패)**

```bash
pnpm test:run tests/unit/lib/format.test.ts
```

Expected: FAIL — module not found.

- [ ] **Step 3: 구현**

`src/lib/format.ts`:

```typescript
const NUMBER_FORMATTER = new Intl.NumberFormat("ko-KR");

export function formatNumber(n: number): string {
  return NUMBER_FORMATTER.format(n);
}

export function formatRelativeTime(
  iso: string | Date,
  now: Date = new Date(),
): string {
  const then = typeof iso === "string" ? new Date(iso) : iso;
  const diffSec = Math.floor((now.getTime() - then.getTime()) / 1000);
  if (diffSec < 10) return "방금 전";
  if (diffSec < 60) return `${diffSec}초 전`;
  const diffMin = Math.floor(diffSec / 60);
  if (diffMin < 60) return `${diffMin}분 전`;
  const diffHour = Math.floor(diffMin / 60);
  if (diffHour < 24) return `${diffHour}시간 전`;
  const diffDay = Math.floor(diffHour / 24);
  if (diffDay < 30) return `${diffDay}일 전`;
  return then.toLocaleDateString("ko-KR");
}
```

- [ ] **Step 4: 통과 확인**

```bash
pnpm test:run tests/unit/lib/format.test.ts
```

Expected: PASS.

- [ ] **Step 5: 커밋**

```bash
git add src/lib/format.ts tests/unit/lib/format.test.ts
git commit -m "feat(frontend): add number and relative time formatters"
```

### Task 3.4: `config/env` 환경변수 검증

**Files:**
- Create: `imstargg-frontend/src/config/env.ts`

- [ ] **Step 1: 작성**

```typescript
import { z } from "zod";

const schema = z.object({
  NEXT_PUBLIC_API_BASE_URL: z.string().url(),
  NEXT_PUBLIC_SITE_URL: z.string().url().default("https://imstargg.com"),
});

export const env = schema.parse({
  NEXT_PUBLIC_API_BASE_URL: process.env.NEXT_PUBLIC_API_BASE_URL,
  NEXT_PUBLIC_SITE_URL: process.env.NEXT_PUBLIC_SITE_URL,
});

export type Env = z.infer<typeof schema>;
```

- [ ] **Step 2: 타입체크**

```bash
pnpm typecheck
```

Expected: 통과 (env.local에 값이 있으면 RuntimeError 없이 통과).

- [ ] **Step 3: 커밋**

```bash
git add src/config/env.ts
git commit -m "feat(frontend): add zod-validated env config"
```

### Task 3.5: `config/routes` 경로 상수

**Files:**
- Create: `imstargg-frontend/src/config/routes.ts`

- [ ] **Step 1: 작성**

```typescript
import { toUrlPath } from "@/lib/tag";

export const routes = {
  home: () => "/",
  ranking: () => "/ranking",
  brawlers: () => "/brawlers",
  brawler: (id: string | number) => `/brawlers/${id}`,
  events: () => "/events",
  event: (id: string | number) => `/events/${id}`,
  player: (tag: string) => `/players/${toUrlPath(tag)}`,
  club: (tag: string) => `/clubs/${toUrlPath(tag)}`,
} as const;
```

- [ ] **Step 2: 커밋**

```bash
git add src/config/routes.ts
git commit -m "feat(frontend): add route path helpers"
```

---

## Phase 4: API 레이어

### Task 4.1: `api/errors` 에러 클래스

**Files:**
- Create: `imstargg-frontend/src/api/errors.ts`

- [ ] **Step 1: 작성**

```typescript
export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly path: string,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export class NotFoundError extends ApiError {
  constructor(path: string) {
    super(404, path, `Not found: ${path}`);
    this.name = "NotFoundError";
  }
}

export class ServerError extends ApiError {
  constructor(status: number, path: string) {
    super(status, path, `Server error ${status}: ${path}`);
    this.name = "ServerError";
  }
}

export class NetworkError extends ApiError {
  constructor(path: string, cause?: unknown) {
    super(0, path, `Network error: ${path}`);
    this.name = "NetworkError";
    if (cause) (this as { cause?: unknown }).cause = cause;
  }
}

export async function toApiError(res: Response, path: string): Promise<ApiError> {
  if (res.status === 404) return new NotFoundError(path);
  if (res.status >= 500) return new ServerError(res.status, path);
  let detail = "";
  try {
    detail = await res.text();
  } catch {
    // ignore
  }
  return new ApiError(res.status, path, detail || `HTTP ${res.status}`);
}
```

- [ ] **Step 2: 커밋**

```bash
git add src/api/errors.ts
git commit -m "feat(frontend): add api error hierarchy"
```

### Task 4.2: `api/types` — 백엔드 응답 DTO 타입

**Files:**
- Create: `imstargg-frontend/src/api/types.ts`

- [ ] **Step 1: 작성** (백엔드 `*Response.kt` 1:1 대응)

```typescript
// 모든 타입은 백엔드 com.imstargg.core.api.controller.v1.response.* DTO와 동일.
// 백엔드 변경 시 이 파일과 매퍼만 수정한다.

export interface ListResponse<T> {
  content: T[];
}

// === Player ===
export interface PlayerResponse {
  tag: string;
  name: string;
  nameColor: string | null;
  iconId: number;
  trophies: number;
  highestTrophies: number;
  expLevel: number;
  expPoints: number;
  qualifiedFromChampionshipChallenge: boolean;
  victories3vs3: number;
  victoriesSolo: number;
  victoriesDuo: number;
  bestRoboRumbleTime: number;
  bestTimeAsBigBrawler: number;
  totalPrestigeLevel: number;
  club: PlayerClubResponse | null;
}

export interface PlayerClubResponse {
  tag: string;
  name: string;
}

// === Brawler ===
export type BrawlerRarity =
  | "COMMON" | "RARE" | "SUPER_RARE" | "EPIC"
  | "MYTHIC" | "LEGENDARY" | "ULTRA_LEGENDARY";

export type BrawlerRole =
  | "DAMAGE_DEALER" | "TANK" | "MARKSMAN" | "ASSASSIN"
  | "SUPPORT" | "CONTROLLER" | "ARTILLERY";

export type BuffyType = "MYTHIC_GEAR" | string;

export interface BuffyResponse {
  type: BuffyType;
  imageUrl: string | null;
}

export type Language = "ko" | "en" | string;
export type MessagesResponse = Record<Language, string>;

export interface GadgetResponse { id: number; name: MessagesResponse; imageUrl: string | null; }
export interface GearResponse { id: number; name: MessagesResponse; imageUrl: string | null; }
export interface StarPowerResponse { id: number; name: MessagesResponse; imageUrl: string | null; }
export interface HyperChargeResponse { id: number; name: MessagesResponse; imageUrl: string | null; }

export interface BrawlerResponse {
  id: number;
  name: MessagesResponse;
  rarity: BrawlerRarity;
  role: BrawlerRole;
  gadgets: GadgetResponse[];
  gears: GearResponse[];
  starPowers: StarPowerResponse[];
  hyperCharges: HyperChargeResponse[];
  portraitImageUrl: string | null;
  buffies: BuffyResponse[];
}

export interface PlayerBrawlerResponse {
  brawler: BrawlerResponse | null;
  power: number;
  rank: number;
  trophies: number;
  highestTrophies: number;
  prestigeLevel: number;
  buffies: BuffyResponse[];
}

// === Renewal ===
export type RenewalStatus =
  | "REQUESTED" | "PENDING" | "EXECUTING"
  | "COMPLETED" | "NOT_FOUND" | "FAILED";

export interface RenewalResponse {
  tag: string;
  status: RenewalStatus;
  progressing: boolean;
  canRenew: boolean;
  requestedAt: string;
  executedAt: string | null;
  completedAt: string | null;
  renewedAt: string | null;
}
```

> 주의: `GadgetResponse` 등 보조 DTO의 정확한 필드는 백엔드 파일과 다를 수 있음. 도메인 spec(브롤러)에서 더 정밀화. 현재는 `id/name/imageUrl` 가정으로 충분.

- [ ] **Step 2: 타입체크**

```bash
pnpm typecheck
```

- [ ] **Step 3: 커밋**

```bash
git add src/api/types.ts
git commit -m "feat(frontend): add backend DTO types for player and renewal"
```

### Task 4.3: `api/client` — fetch 래퍼

**Files:**
- Create: `imstargg-frontend/src/api/client.ts`

- [ ] **Step 1: 작성**

```typescript
import { env } from "@/config/env";
import { NetworkError, toApiError } from "@/api/errors";

export type FetchOpts = {
  next?: { revalidate?: number; tags?: string[] };
  signal?: AbortSignal;
  method?: "GET" | "POST";
  body?: unknown;
  cache?: RequestCache;
};

export async function apiFetch<T>(path: string, opts: FetchOpts = {}): Promise<T> {
  const url = `${env.NEXT_PUBLIC_API_BASE_URL}${path}`;
  const init: RequestInit & { next?: FetchOpts["next"] } = {
    method: opts.method ?? "GET",
    headers: {
      Accept: "application/json",
      ...(opts.body ? { "Content-Type": "application/json" } : {}),
    },
    body: opts.body ? JSON.stringify(opts.body) : undefined,
    signal: opts.signal,
    cache: opts.cache,
    next: opts.next,
  };
  let res: Response;
  try {
    res = await fetch(url, init);
  } catch (e) {
    throw new NetworkError(path, e);
  }
  if (!res.ok) throw await toApiError(res, path);
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}
```

- [ ] **Step 2: 커밋**

```bash
git add src/api/client.ts
git commit -m "feat(frontend): add apiFetch wrapper with error normalization"
```

### Task 4.4: `api/endpoints/player` & `api/endpoints/renewal`

**Files:**
- Create: `imstargg-frontend/src/api/endpoints/player.ts`
- Create: `imstargg-frontend/src/api/endpoints/renewal.ts`

- [ ] **Step 1: `endpoints/player.ts` 작성**

```typescript
import { apiFetch, type FetchOpts } from "@/api/client";
import { toUrlPath } from "@/lib/tag";
import type {
  PlayerResponse,
  PlayerBrawlerResponse,
  ListResponse,
} from "@/api/types";

export const playerApi = {
  get: (tag: string, opts?: FetchOpts) =>
    apiFetch<PlayerResponse>(`/v1/players/${toUrlPath(tag)}`, opts),

  getBrawlers: (tag: string, opts?: FetchOpts) =>
    apiFetch<ListResponse<PlayerBrawlerResponse>>(
      `/v1/players/${toUrlPath(tag)}/brawlers`,
      opts,
    ),
};
```

- [ ] **Step 2: `endpoints/renewal.ts` 작성**

```typescript
import { apiFetch, type FetchOpts } from "@/api/client";
import { toUrlPath } from "@/lib/tag";
import type { RenewalResponse } from "@/api/types";

export const renewalApi = {
  getPlayer: (tag: string, opts?: FetchOpts) =>
    apiFetch<RenewalResponse>(`/v1/players/${toUrlPath(tag)}/renewal`, opts),

  renewPlayer: (tag: string) =>
    apiFetch<RenewalResponse>(`/v1/players/${toUrlPath(tag)}/renewal`, {
      method: "POST",
    }),

  getClub: (tag: string, opts?: FetchOpts) =>
    apiFetch<RenewalResponse>(`/v1/clubs/${toUrlPath(tag)}/renewal`, opts),

  renewClub: (tag: string) =>
    apiFetch<RenewalResponse>(`/v1/clubs/${toUrlPath(tag)}/renewal`, {
      method: "POST",
    }),
};
```

- [ ] **Step 3: 커밋**

```bash
git add src/api/endpoints
git commit -m "feat(frontend): add player and renewal endpoint clients"
```

---

## Phase 5: 도메인 모델 + Provider

### Task 5.1: `domain/player` 도메인 타입 + 매퍼 (TDD)

**Files:**
- Create: `imstargg-frontend/src/domain/player/types.ts`
- Create: `imstargg-frontend/src/domain/player/mappers.ts`
- Create: `imstargg-frontend/tests/unit/domain/player/mappers.test.ts`

- [ ] **Step 1: 도메인 타입 작성**

`src/domain/player/types.ts`:

```typescript
import type {
  BrawlerRarity, BrawlerRole, BuffyResponse, BrawlerResponse,
} from "@/api/types";

export interface PlayerClub {
  tag: string;
  name: string;
}

export interface Player {
  tag: string;
  name: string;
  nameColor: string | null;
  iconId: number;
  trophies: number;
  highestTrophies: number;
  expLevel: number;
  expPoints: number;
  victories3vs3: number;
  victoriesSolo: number;
  victoriesDuo: number;
  totalPrestigeLevel: number;
  club: PlayerClub | null;
}

export interface PlayerBrawler {
  id: number | null;
  name: string;
  rarity: BrawlerRarity | null;
  role: BrawlerRole | null;
  power: number;
  rank: number;
  trophies: number;
  highestTrophies: number;
  prestigeLevel: number;
  portraitImageUrl: string | null;
  buffies: BuffyResponse[];
  raw: BrawlerResponse | null;
}
```

- [ ] **Step 2: 실패하는 매퍼 테스트**

`tests/unit/domain/player/mappers.test.ts`:

```typescript
import { describe, it, expect } from "vitest";
import { toPlayer, toPlayerBrawler } from "@/domain/player/mappers";
import type { PlayerResponse, PlayerBrawlerResponse } from "@/api/types";

const dto: PlayerResponse = {
  tag: "#88J8Q9RPV",
  name: "Hero",
  nameColor: "0xff",
  iconId: 28000000,
  trophies: 60000,
  highestTrophies: 65000,
  expLevel: 300,
  expPoints: 999,
  qualifiedFromChampionshipChallenge: true,
  victories3vs3: 1000,
  victoriesSolo: 200,
  victoriesDuo: 100,
  bestRoboRumbleTime: 0,
  bestTimeAsBigBrawler: 0,
  totalPrestigeLevel: 50,
  club: { tag: "#2U8CVLQQ9", name: "MyClub" },
};

describe("toPlayer", () => {
  it("maps base fields", () => {
    const p = toPlayer(dto);
    expect(p.tag).toBe("#88J8Q9RPV");
    expect(p.name).toBe("Hero");
    expect(p.trophies).toBe(60000);
    expect(p.club).toEqual({ tag: "#2U8CVLQQ9", name: "MyClub" });
  });
  it("handles null club", () => {
    expect(toPlayer({ ...dto, club: null }).club).toBeNull();
  });
});

const brawlerDto: PlayerBrawlerResponse = {
  brawler: {
    id: 16000000,
    name: { ko: "쉘리", en: "SHELLY" },
    rarity: "COMMON",
    role: "DAMAGE_DEALER",
    gadgets: [],
    gears: [],
    starPowers: [],
    hyperCharges: [],
    portraitImageUrl: "https://img/shelly.png",
    buffies: [],
  },
  power: 11,
  rank: 30,
  trophies: 700,
  highestTrophies: 1200,
  prestigeLevel: 3,
  buffies: [],
};

describe("toPlayerBrawler", () => {
  it("uses Korean name when present", () => {
    const b = toPlayerBrawler(brawlerDto);
    expect(b.name).toBe("쉘리");
    expect(b.power).toBe(11);
    expect(b.rarity).toBe("COMMON");
    expect(b.portraitImageUrl).toBe("https://img/shelly.png");
  });
  it("falls back to English when ko missing", () => {
    const noKo = {
      ...brawlerDto,
      brawler: { ...brawlerDto.brawler!, name: { en: "SHELLY" } },
    };
    expect(toPlayerBrawler(noKo).name).toBe("SHELLY");
  });
  it("handles null brawler", () => {
    const b = toPlayerBrawler({ ...brawlerDto, brawler: null });
    expect(b.id).toBeNull();
    expect(b.name).toBe("(unknown)");
  });
});
```

- [ ] **Step 3: 실행 (실패)**

```bash
pnpm test:run tests/unit/domain/player/mappers.test.ts
```

Expected: FAIL — mappers module 없음.

- [ ] **Step 4: 매퍼 구현**

`src/domain/player/mappers.ts`:

```typescript
import type {
  PlayerResponse,
  PlayerBrawlerResponse,
  MessagesResponse,
} from "@/api/types";
import type { Player, PlayerBrawler } from "@/domain/player/types";

function pickName(messages: MessagesResponse | undefined): string {
  if (!messages) return "(unknown)";
  return messages.ko ?? messages.en ?? Object.values(messages)[0] ?? "(unknown)";
}

export function toPlayer(dto: PlayerResponse): Player {
  return {
    tag: dto.tag,
    name: dto.name,
    nameColor: dto.nameColor,
    iconId: dto.iconId,
    trophies: dto.trophies,
    highestTrophies: dto.highestTrophies,
    expLevel: dto.expLevel,
    expPoints: dto.expPoints,
    victories3vs3: dto.victories3vs3,
    victoriesSolo: dto.victoriesSolo,
    victoriesDuo: dto.victoriesDuo,
    totalPrestigeLevel: dto.totalPrestigeLevel,
    club: dto.club ? { tag: dto.club.tag, name: dto.club.name } : null,
  };
}

export function toPlayerBrawler(dto: PlayerBrawlerResponse): PlayerBrawler {
  const b = dto.brawler;
  return {
    id: b?.id ?? null,
    name: pickName(b?.name),
    rarity: b?.rarity ?? null,
    role: b?.role ?? null,
    power: dto.power,
    rank: dto.rank,
    trophies: dto.trophies,
    highestTrophies: dto.highestTrophies,
    prestigeLevel: dto.prestigeLevel,
    portraitImageUrl: b?.portraitImageUrl ?? null,
    buffies: dto.buffies,
    raw: b ?? null,
  };
}
```

- [ ] **Step 5: 통과 확인**

```bash
pnpm test:run tests/unit/domain/player/mappers.test.ts
```

Expected: PASS.

- [ ] **Step 6: 커밋**

```bash
git add src/domain/player tests/unit/domain
git commit -m "feat(frontend): add player domain types and mappers"
```

### Task 5.2: `providers/QueryProvider`

**Files:**
- Create: `imstargg-frontend/src/providers/QueryProvider.tsx`

- [ ] **Step 1: 작성**

```tsx
"use client";

import { useState, type ReactNode } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";

export function QueryProvider({ children }: { children: ReactNode }) {
  const [client] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 60_000,
            refetchOnWindowFocus: false,
            retry: 1,
          },
        },
      }),
  );
  return (
    <QueryClientProvider client={client}>
      {children}
      {process.env.NODE_ENV !== "production" ? (
        <ReactQueryDevtools initialIsOpen={false} buttonPosition="bottom-right" />
      ) : null}
    </QueryClientProvider>
  );
}
```

- [ ] **Step 2: 커밋**

```bash
git add src/providers
git commit -m "feat(frontend): add QueryProvider with sensible defaults"
```

### Task 5.3: `hooks/queries/queryKeys`

**Files:**
- Create: `imstargg-frontend/src/hooks/queries/queryKeys.ts`

- [ ] **Step 1: 작성**

```typescript
import { normalizeTag } from "@/lib/tag";

export const queryKeys = {
  player: {
    all: ["player"] as const,
    detail: (tag: string) =>
      [...queryKeys.player.all, "detail", normalizeTag(tag)] as const,
    brawlers: (tag: string) =>
      [...queryKeys.player.all, "brawlers", normalizeTag(tag)] as const,
    renewal: (tag: string) =>
      [...queryKeys.player.all, "renewal", normalizeTag(tag)] as const,
  },
} as const;
```

- [ ] **Step 2: 커밋**

```bash
git add src/hooks/queries/queryKeys.ts
git commit -m "feat(frontend): add query key registry for player domain"
```

### Task 5.4: `hooks/queries/usePlayer` & `usePlayerBrawlers`

**Files:**
- Create: `imstargg-frontend/src/hooks/queries/usePlayer.ts`
- Create: `imstargg-frontend/src/hooks/queries/usePlayerBrawlers.ts`

- [ ] **Step 1: `usePlayer.ts`**

```typescript
"use client";

import { useQuery } from "@tanstack/react-query";
import { playerApi } from "@/api/endpoints/player";
import { toPlayer } from "@/domain/player/mappers";
import type { Player } from "@/domain/player/types";
import { queryKeys } from "@/hooks/queries/queryKeys";

export function usePlayer(tag: string, options?: { initialData?: Player }) {
  return useQuery({
    queryKey: queryKeys.player.detail(tag),
    queryFn: ({ signal }) => playerApi.get(tag, { signal }).then(toPlayer),
    initialData: options?.initialData,
    staleTime: 60_000,
  });
}
```

- [ ] **Step 2: `usePlayerBrawlers.ts`**

```typescript
"use client";

import { useQuery } from "@tanstack/react-query";
import { playerApi } from "@/api/endpoints/player";
import { toPlayerBrawler } from "@/domain/player/mappers";
import type { PlayerBrawler } from "@/domain/player/types";
import { queryKeys } from "@/hooks/queries/queryKeys";

export function usePlayerBrawlers(
  tag: string,
  options?: { initialData?: PlayerBrawler[] },
) {
  return useQuery({
    queryKey: queryKeys.player.brawlers(tag),
    queryFn: ({ signal }) =>
      playerApi.getBrawlers(tag, { signal }).then((res) => res.content.map(toPlayerBrawler)),
    initialData: options?.initialData,
    staleTime: 60_000,
  });
}
```

- [ ] **Step 3: 커밋**

```bash
git add src/hooks/queries/usePlayer.ts src/hooks/queries/usePlayerBrawlers.ts
git commit -m "feat(frontend): add player query hooks"
```

### Task 5.5: `hooks/usePlayerRenewal` (1초 폴링 + invalidate)

**Files:**
- Create: `imstargg-frontend/src/hooks/usePlayerRenewal.ts`

- [ ] **Step 1: 작성**

```typescript
"use client";

import { useEffect, useRef } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { renewalApi } from "@/api/endpoints/renewal";
import { queryKeys } from "@/hooks/queries/queryKeys";

export function usePlayerRenewal(tag: string) {
  const qc = useQueryClient();

  const renewalQuery = useQuery({
    queryKey: queryKeys.player.renewal(tag),
    queryFn: ({ signal }) => renewalApi.getPlayer(tag, { signal }),
    refetchInterval: (q) =>
      q.state.data?.progressing ? 1000 : false,
    refetchIntervalInBackground: false,
    staleTime: 0,
  });

  const prevProgressing = useRef<boolean>(
    renewalQuery.data?.progressing ?? false,
  );
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
    progressing: renewalQuery.data?.progressing ?? false,
    canRenew: renewalQuery.data?.canRenew ?? false,
    trigger: () => trigger.mutate(),
    triggering: trigger.isPending,
  };
}
```

- [ ] **Step 2: 커밋**

```bash
git add src/hooks/usePlayerRenewal.ts
git commit -m "feat(frontend): add 1s polling renewal hook with invalidate"
```

---

## Phase 6: shadcn 설치 & 토스트 교체

### Task 6.1: shadcn 초기화

**Files:**
- Create: `imstargg-frontend/components.json` (shadcn CLI 생성)
- Create: `imstargg-frontend/src/components/ui/*` (shadcn CLI 생성)

- [ ] **Step 1: shadcn init**

```bash
cd imstargg-frontend
pnpm dlx shadcn@latest init
```

프롬프트 답변 가이드:
- Style: `default`
- Base color: `neutral` (또는 사용자 디자인에 맞춰)
- Path for components: `src/components`
- Path aliases: `@/components`, `@/lib/utils` → `@/lib/utils`을 `@/lib/cn` 함수와 동기화 필요. shadcn이 `lib/utils.ts` 생성 시도하면 거부하고, `components.json`에서 `aliases.utils` 를 `@/lib/cn`으로 수정.
- CSS variables: yes
- React Server Components: yes

- [ ] **Step 2: `components.json` 정합 확인**

```bash
cat components.json
```

`aliases`가 다음과 같아야 함 (utils는 cn export하는 우리 모듈을 가리킴):

```json
{
  "aliases": {
    "components": "@/components",
    "utils": "@/lib/cn",
    "ui": "@/components/ui",
    "lib": "@/lib",
    "hooks": "@/hooks"
  }
}
```

만약 shadcn이 자체 `lib/utils.ts`를 만들었다면 삭제하고 `components.json`에서 `utils: "@/lib/cn"`로 바꿈. (shadcn 컴포넌트가 `import { cn } from "@/lib/cn"`로 생성되도록.)

- [ ] **Step 3: 컴포넌트 일괄 설치**

```bash
pnpm dlx shadcn@latest add button input label dialog sheet tabs dropdown-menu \
  tooltip skeleton select sonner card badge separator pagination \
  --yes
```

> `command` 컴포넌트는 도메인 spec(검색 자동완성)에서 추가.

Expected: `src/components/ui/`에 컴포넌트 파일들 생성됨.

- [ ] **Step 4: 빌드 확인**

```bash
pnpm tsc --noEmit
pnpm build
```

Expected: 통과.

- [ ] **Step 5: 커밋**

```bash
git add -A
git commit -m "feat(frontend): initialize shadcn/ui with base components"
```

### Task 6.2: sonner Toaster 통합 + 기존 toast 제거

**Files:**
- Modify: `imstargg-frontend/app/layout.tsx`
- Delete: `imstargg-frontend/src/components/shared/ToastContainer.tsx`
- Delete: `imstargg-frontend/src/lib/toast.ts`
- Modify: 기존 `lib/toast` import 사용하던 모든 파일

- [ ] **Step 1: 기존 사용처 검색**

```bash
cd imstargg-frontend
grep -rn "from \"@/lib/toast\"" app/ src/ 2>/dev/null
grep -rn "from \"@/components/shared/ToastContainer\"" app/ src/ 2>/dev/null
grep -rn "<ToastContainer" app/ src/ 2>/dev/null
```

- [ ] **Step 2: 각 사용처를 sonner로 교체**

`import { showToast } from "@/lib/toast"` → `import { toast } from "sonner"`. 호출:
- `showToast("...")` → `toast("...")`
- `showToast.success("...")` → `toast.success("...")`
- 기타 사용 형태에 맞춰 sonner API로 변환 (`toast.info`, `toast.error`, `toast.warning`).

- [ ] **Step 3: `app/layout.tsx`에 Toaster 추가**

(파일은 다음 Task 7.1에서 layout 전체를 다시 손보므로 여기서는 import + body 안에 `<Toaster richColors position="top-center" />` 한 줄만 추가)

`app/layout.tsx`의 body 끝에:

```tsx
import { Toaster } from "@/components/ui/sonner";
// ...
<body>
  {/* ... 기존 콘텐츠 ... */}
  <Toaster richColors position="top-center" />
</body>
```

또한 기존 `<ToastContainer />` 사용을 제거.

- [ ] **Step 4: 기존 파일 삭제**

```bash
git rm src/components/shared/ToastContainer.tsx
git rm src/lib/toast.ts
```

- [ ] **Step 5: 빌드 확인**

```bash
pnpm tsc --noEmit
pnpm build
```

Expected: 통과.

- [ ] **Step 6: 커밋**

```bash
git add -A
git commit -m "refactor(frontend): replace custom toast with sonner"
```

### Task 6.3: `app/layout.tsx`에 QueryProvider 추가

**Files:**
- Modify: `imstargg-frontend/app/layout.tsx`

- [ ] **Step 1: QueryProvider로 children 래핑**

`app/layout.tsx`의 body 부분 (TopNav, MobileTabBar는 기존 유지):

```tsx
import { QueryProvider } from "@/providers/QueryProvider";
// ...
<body>
  <QueryProvider>
    {/* 기존 헤더/콘텐츠/하단 탭 */}
    {children}
    <Toaster richColors position="top-center" />
  </QueryProvider>
</body>
```

- [ ] **Step 2: 빌드 확인**

```bash
pnpm tsc --noEmit
pnpm build
```

- [ ] **Step 3: 커밋**

```bash
git add app/layout.tsx
git commit -m "feat(frontend): wrap app with QueryProvider"
```

---

## Phase 7: 백엔드 CORS

### Task 7.1: `CorsProperties` & `WebMvcConfig`

**Files:**
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/config/CorsProperties.kt`
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/config/WebMvcConfig.kt`

- [ ] **Step 1: 기존 config 디렉토리 확인**

```bash
find imstargg-backend/core/core-api/src/main/kotlin/com/imstargg -type d -name config
```

없으면 생성: `mkdir -p imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/config`.

- [ ] **Step 2: `CorsProperties.kt` 작성**

```kotlin
package com.imstargg.core.api.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.cors")
data class CorsProperties(
    val allowedOrigins: List<String> = emptyList(),
)
```

- [ ] **Step 3: `WebMvcConfig.kt` 작성**

```kotlin
package com.imstargg.core.api.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@EnableConfigurationProperties(CorsProperties::class)
class WebMvcConfig(
    private val cors: CorsProperties,
) : WebMvcConfigurer {
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

- [ ] **Step 4: 빌드 확인**

```bash
cd imstargg-backend && ./gradlew :core:core-api:compileKotlin
```

Expected: 통과.

- [ ] **Step 5: 커밋 (root에서)**

```bash
cd ..  # imstargg-v2 root
git add imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/config
git commit -m "feat(core-api): add CORS configuration via app.cors.allowed-origins"
```

### Task 7.2: `application-prod.yml`에 origin 등록

**Files:**
- Modify: `imstargg-backend/core/core-api/src/main/resources/application-prod.yml`

- [ ] **Step 1: 현재 파일 확인**

```bash
cat imstargg-backend/core/core-api/src/main/resources/application-prod.yml
```

- [ ] **Step 2: `app.cors.allowed-origins` 섹션 추가**

기존 내용 끝에 추가:

```yaml
app:
  cors:
    allowed-origins:
      - https://imstargg.com
      - http://localhost:3000
```

> 이미 `app:` 키가 있으면 그 아래에 `cors` 추가.

- [ ] **Step 3: 커밋**

```bash
git add imstargg-backend/core/core-api/src/main/resources/application-prod.yml
git commit -m "chore(core-api): register imstargg.com and localhost origins for cors"
```

### Task 7.3: CORS 통합 테스트

**Files:**
- Create: `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/api/config/WebMvcConfigTest.kt`

- [ ] **Step 1: 테스트 작성**

```kotlin
package com.imstargg.core.api.config

import com.imstargg.CoreApiContextTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc

@AutoConfigureMockMvc
@TestPropertySource(properties = [
    "app.cors.allowed-origins=https://imstargg.com,http://localhost:3000",
])
class WebMvcConfigTest(
    @Autowired private val mockMvc: MockMvc,
) : CoreApiContextTest() {
    @Test
    fun `허용된 origin 의 OPTIONS 요청에 CORS 헤더가 응답된다`() {
        mockMvc.perform(
            options("/v1/players/%2388J8Q9RPV")
                .header(HttpHeaders.ORIGIN, "https://imstargg.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"),
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Access-Control-Allow-Origin", "https://imstargg.com"))
    }
}
```

- [ ] **Step 2: 테스트 실행**

```bash
cd imstargg-backend
./gradlew :core:core-api:contextTest --tests "com.imstargg.core.api.config.WebMvcConfigTest"
```

Expected: PASS.

- [ ] **Step 3: 커밋**

```bash
cd ..
git add imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/api/config
git commit -m "test(core-api): verify cors headers for allowed origins"
```

---

## Phase 8: SEO 인프라

### Task 8.1: `lib/seo` & `app/layout.tsx` metadata

**Files:**
- Create: `imstargg-frontend/src/lib/seo.ts`
- Modify: `imstargg-frontend/app/layout.tsx`

- [ ] **Step 1: `src/lib/seo.ts` 작성**

```typescript
import type { Metadata } from "next";
import { env } from "@/config/env";

const SITE = {
  name: "ImStarGG",
  url: env.NEXT_PUBLIC_SITE_URL,
  defaultOgImage: "/assets/logo-full.png",
  locale: "ko_KR",
} as const;

export const SITE_INFO = SITE;

type Input = {
  title: string;
  description: string;
  path: string;
  image?: string;
  noindex?: boolean;
};

export function buildMetadata(input: Input): Metadata {
  const url = `${SITE.url}${input.path}`;
  const image = input.image ?? SITE.defaultOgImage;
  return {
    title: input.title,
    description: input.description,
    alternates: { canonical: url },
    robots: input.noindex ? { index: false, follow: false } : undefined,
    openGraph: {
      title: input.title,
      description: input.description,
      url,
      siteName: SITE.name,
      images: [{ url: image, width: 1200, height: 630 }],
      locale: SITE.locale,
      type: "website",
    },
    twitter: {
      card: "summary_large_image",
      title: input.title,
      description: input.description,
      images: [image],
    },
  };
}
```

- [ ] **Step 2: `app/layout.tsx`에 metadataBase + 기본 metadata**

`app/layout.tsx` 상단에 추가:

```tsx
import type { Metadata } from "next";
import { env } from "@/config/env";
import { buildMetadata } from "@/lib/seo";

export const metadata: Metadata = {
  metadataBase: new URL(env.NEXT_PUBLIC_SITE_URL),
  ...buildMetadata({
    title: "ImStarGG — 브롤스타즈 통계",
    description: "브롤스타즈 플레이어/클럽/브롤러/이벤트 통계와 랭킹.",
    path: "/",
  }),
};
```

- [ ] **Step 3: 빌드 확인**

```bash
pnpm build
```

- [ ] **Step 4: 커밋**

```bash
git add src/lib/seo.ts app/layout.tsx
git commit -m "feat(frontend): add buildMetadata helper and root metadata"
```

### Task 8.2: `app/sitemap.ts` & `app/robots.ts`

**Files:**
- Create: `imstargg-frontend/app/sitemap.ts`
- Create: `imstargg-frontend/app/robots.ts`

- [ ] **Step 1: `app/robots.ts` 작성**

```typescript
import type { MetadataRoute } from "next";
import { env } from "@/config/env";

export default function robots(): MetadataRoute.Robots {
  return {
    rules: { userAgent: "*", allow: "/", disallow: ["/api/"] },
    sitemap: `${env.NEXT_PUBLIC_SITE_URL}/sitemap.xml`,
  };
}
```

- [ ] **Step 2: `app/sitemap.ts` 작성** (Foundation은 정적 페이지만; 브롤러/이벤트 동적 추가는 도메인 spec)

```typescript
import type { MetadataRoute } from "next";
import { env } from "@/config/env";

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const base = env.NEXT_PUBLIC_SITE_URL;
  const now = new Date();
  return [
    { url: `${base}/`, lastModified: now, changeFrequency: "daily", priority: 1.0 },
    { url: `${base}/ranking`, lastModified: now, changeFrequency: "hourly", priority: 0.8 },
    { url: `${base}/brawlers`, lastModified: now, changeFrequency: "weekly", priority: 0.7 },
    { url: `${base}/events`, lastModified: now, changeFrequency: "daily", priority: 0.7 },
  ];
}
```

> 도메인 spec(브롤러/이벤트, 랭킹)에서 동적 URL을 이 함수에 추가한다.

- [ ] **Step 3: 빌드 확인**

```bash
pnpm build
```

- [ ] **Step 4: 커밋**

```bash
git add app/sitemap.ts app/robots.ts
git commit -m "feat(frontend): add static sitemap and robots"
```

---

## Phase 9: 플레이어 도메인 reference 구현

### Task 9.1: `RenewalButton` shadcn 기반 재구현

**Files:**
- Modify: `imstargg-frontend/src/components/shared/RenewalButton.tsx`

- [ ] **Step 1: 현재 파일 확인**

```bash
cat imstargg-frontend/src/components/shared/RenewalButton.tsx
```

- [ ] **Step 2: 다형성 props로 재작성** (player/club 모두 지원)

```tsx
"use client";

import { Button } from "@/components/ui/button";
import { RefreshCw } from "lucide-react";
import { usePlayerRenewal } from "@/hooks/usePlayerRenewal";

type Props =
  | { kind: "player"; tag: string }
  // club 훅은 클럽 도메인 spec에서 구현. 인터페이스만 미리 노출.
  | { kind: "club"; tag: string };

export function RenewalButton(props: Props) {
  if (props.kind === "player") return <PlayerRenewalButton tag={props.tag} />;
  return <ClubRenewalButtonPlaceholder />;
}

function PlayerRenewalButton({ tag }: { tag: string }) {
  const { progressing, canRenew, trigger, triggering } = usePlayerRenewal(tag);
  const disabled = progressing || triggering || !canRenew;
  return (
    <Button
      type="button"
      variant="default"
      size="sm"
      disabled={disabled}
      onClick={trigger}
      aria-label="플레이어 정보 갱신"
    >
      <RefreshCw className={progressing ? "size-4 animate-spin" : "size-4"} />
      <span className="ml-2">{progressing ? "갱신 중…" : "갱신"}</span>
    </Button>
  );
}

function ClubRenewalButtonPlaceholder() {
  return (
    <Button type="button" variant="default" size="sm" disabled>
      <RefreshCw className="size-4" />
      <span className="ml-2">갱신</span>
    </Button>
  );
}
```

- [ ] **Step 3: 기존 사용처 점검**

```bash
grep -rn "RenewalButton" imstargg-frontend/app imstargg-frontend/src
```

`<RenewalButton tag={...} />`처럼 호출하는 곳을 `<RenewalButton kind="player" tag={...} />`로 변경.

- [ ] **Step 4: 빌드 확인**

```bash
pnpm tsc --noEmit
```

- [ ] **Step 5: 커밋**

```bash
git add src/components/shared/RenewalButton.tsx app/ src/
git commit -m "feat(frontend): rebuild RenewalButton with shadcn and player hook"
```

### Task 9.2: `PlayerSearchInput` 컴포넌트

**Files:**
- Create: `imstargg-frontend/src/components/shared/PlayerSearchInput.tsx`

- [ ] **Step 1: 작성**

```tsx
"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Search } from "lucide-react";
import { isValidTag } from "@/lib/tag";
import { routes } from "@/config/routes";
import { toast } from "sonner";

export function PlayerSearchInput({ defaultValue = "" }: { defaultValue?: string }) {
  const router = useRouter();
  const [value, setValue] = useState(defaultValue);

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    const trimmed = value.trim();
    if (!isValidTag(trimmed)) {
      toast.error("올바른 플레이어 태그를 입력해 주세요. 예: #88J8Q9RPV");
      return;
    }
    router.push(routes.player(trimmed));
  }

  return (
    <form onSubmit={handleSubmit} className="flex w-full max-w-md items-center gap-2">
      <Input
        type="text"
        inputMode="text"
        autoCapitalize="characters"
        spellCheck={false}
        placeholder="#PLAYER_TAG"
        value={value}
        onChange={(e) => setValue(e.target.value)}
        aria-label="플레이어 태그 입력"
      />
      <Button type="submit" size="default" aria-label="플레이어 검색">
        <Search className="size-4" />
      </Button>
    </form>
  );
}
```

- [ ] **Step 2: 커밋**

```bash
git add src/components/shared/PlayerSearchInput.tsx
git commit -m "feat(frontend): add player search input with tag validation"
```

### Task 9.3: 플레이어 페이지 RSC 구현

**Files:**
- Modify: `imstargg-frontend/app/players/[tag]/page.tsx`
- Create: `imstargg-frontend/app/players/[tag]/error.tsx`
- Create: `imstargg-frontend/app/players/[tag]/not-found.tsx`

- [ ] **Step 1: 기존 파일 백업 및 점검**

```bash
cat imstargg-frontend/app/players/[tag]/page.tsx
ls imstargg-frontend/app/players/[tag]/_components/
```

- [ ] **Step 2: `app/players/[tag]/page.tsx` 재작성**

```tsx
import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { playerApi } from "@/api/endpoints/player";
import { renewalApi } from "@/api/endpoints/renewal";
import { toPlayer, toPlayerBrawler } from "@/domain/player/mappers";
import { NotFoundError } from "@/api/errors";
import { buildMetadata } from "@/lib/seo";
import { normalizeTag, toUrlPath } from "@/lib/tag";
import { formatNumber } from "@/lib/format";
import { PlayerContent } from "./_components/PlayerContent";

type Params = { params: Promise<{ tag: string }> };

async function loadPlayer(tag: string) {
  return playerApi.get(tag, { next: { revalidate: 60 } });
}

export async function generateMetadata({ params }: Params): Promise<Metadata> {
  const { tag } = await params;
  try {
    const dto = await loadPlayer(tag);
    const display = `${dto.name} (${dto.tag})`;
    return buildMetadata({
      title: `${display} | ImStarGG`,
      description: `${display} — 트로피 ${formatNumber(dto.trophies)}, ${dto.club?.name ?? "클럽 없음"}. 브롤스타즈 플레이어 통계.`,
      path: `/players/${toUrlPath(tag)}`,
    });
  } catch (e) {
    return buildMetadata({
      title: "플레이어를 찾을 수 없음 | ImStarGG",
      description: "요청하신 플레이어를 찾을 수 없습니다.",
      path: `/players/${toUrlPath(tag)}`,
      noindex: true,
    });
  }
}

export default async function PlayerPage({ params }: Params) {
  const { tag } = await params;
  let dto;
  try {
    dto = await loadPlayer(tag);
  } catch (e) {
    if (e instanceof NotFoundError) notFound();
    throw e;
  }
  const brawlers = await playerApi.getBrawlers(tag, { next: { revalidate: 60 } });
  const renewal = await renewalApi.getPlayer(tag, { next: { revalidate: 0 } }).catch(() => null);

  return (
    <PlayerContent
      tag={normalizeTag(tag)}
      initialPlayer={toPlayer(dto)}
      initialBrawlers={brawlers.content.map(toPlayerBrawler)}
      initialRenewal={renewal}
    />
  );
}
```

- [ ] **Step 3: `app/players/[tag]/not-found.tsx`**

```tsx
import Link from "next/link";
import { Button } from "@/components/ui/button";

export default function PlayerNotFound() {
  return (
    <main className="flex flex-col items-center justify-center gap-4 py-24">
      <h1 className="text-2xl font-semibold">플레이어를 찾을 수 없어요</h1>
      <p className="text-muted-foreground">태그가 정확한지 다시 확인해 주세요.</p>
      <Button asChild variant="outline">
        <Link href="/">홈으로</Link>
      </Button>
    </main>
  );
}
```

- [ ] **Step 4: `app/players/[tag]/error.tsx`**

```tsx
"use client";

import { useEffect } from "react";
import { Button } from "@/components/ui/button";

export default function PlayerError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);
  return (
    <main className="flex flex-col items-center justify-center gap-4 py-24">
      <h1 className="text-2xl font-semibold">문제가 발생했어요</h1>
      <p className="text-muted-foreground">잠시 후 다시 시도해 주세요.</p>
      <Button onClick={reset}>다시 시도</Button>
    </main>
  );
}
```

- [ ] **Step 5: 빌드 확인 (PlayerContent 시그니처 변경 필요할 수 있음 — 다음 Task에서 처리)**

```bash
pnpm tsc --noEmit
```

> 컴파일 에러 발생 가능 (PlayerContent props 미스매치). 다음 Task 9.4에서 해결.

- [ ] **Step 6: 커밋 (에러 해결과 함께)** — Task 9.4 완료 후 커밋

### Task 9.4: `PlayerContent` 클라이언트 컴포넌트 — API 데이터로 연결

**Files:**
- Modify: `imstargg-frontend/app/players/[tag]/_components/PlayerContent.tsx`
- Modify: `imstargg-frontend/app/players/[tag]/_components/PlayerStats.tsx`
- Modify: `imstargg-frontend/app/players/[tag]/_components/PlayerSidebar.tsx`
- Modify: `imstargg-frontend/app/players/[tag]/_components/PlayerBrawlers.tsx`
- Modify: `imstargg-frontend/app/players/[tag]/_components/PlayerBattles.tsx`

- [ ] **Step 1: 현재 PlayerContent 시그니처 파악**

```bash
cat imstargg-frontend/app/players/[tag]/_components/PlayerContent.tsx
```

- [ ] **Step 2: `PlayerContent.tsx` 재작성**

```tsx
"use client";

import { usePlayer } from "@/hooks/queries/usePlayer";
import { usePlayerBrawlers } from "@/hooks/queries/usePlayerBrawlers";
import type { Player, PlayerBrawler } from "@/domain/player/types";
import type { RenewalResponse } from "@/api/types";
import { useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { queryKeys } from "@/hooks/queries/queryKeys";
import { PlayerSidebar } from "./PlayerSidebar";
import { PlayerStats } from "./PlayerStats";
import { PlayerBrawlers } from "./PlayerBrawlers";
import { PlayerBattles } from "./PlayerBattles";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";

type Props = {
  tag: string;
  initialPlayer: Player;
  initialBrawlers: PlayerBrawler[];
  initialRenewal: RenewalResponse | null;
};

export function PlayerContent({
  tag,
  initialPlayer,
  initialBrawlers,
  initialRenewal,
}: Props) {
  const qc = useQueryClient();

  // hydrate renewal cache from server snapshot so usePlayerRenewal starts hot
  useEffect(() => {
    if (initialRenewal) {
      qc.setQueryData(queryKeys.player.renewal(tag), initialRenewal);
    }
  }, [tag, initialRenewal, qc]);

  const { data: player } = usePlayer(tag, { initialData: initialPlayer });
  const { data: brawlers } = usePlayerBrawlers(tag, { initialData: initialBrawlers });

  if (!player) return null;
  return (
    <main className="mx-auto w-full max-w-4xl px-4 py-6 space-y-6">
      <PlayerSidebar player={player} />
      <Tabs defaultValue="stats" className="w-full">
        <TabsList>
          <TabsTrigger value="stats">통계</TabsTrigger>
          <TabsTrigger value="brawlers">브롤러</TabsTrigger>
          <TabsTrigger value="battles">배틀</TabsTrigger>
        </TabsList>
        <TabsContent value="stats">
          <PlayerStats player={player} />
        </TabsContent>
        <TabsContent value="brawlers">
          <PlayerBrawlers brawlers={brawlers ?? []} />
        </TabsContent>
        <TabsContent value="battles">
          <PlayerBattles tag={tag} />
        </TabsContent>
      </Tabs>
    </main>
  );
}
```

- [ ] **Step 3: `PlayerSidebar.tsx` 재작성** — `Player` 타입 받기

```tsx
"use client";

import type { Player } from "@/domain/player/types";
import { formatNumber } from "@/lib/format";
import { RenewalButton } from "@/components/shared/RenewalButton";
import { Card } from "@/components/ui/card";
import Link from "next/link";
import { routes } from "@/config/routes";

export function PlayerSidebar({ player }: { player: Player }) {
  return (
    <Card className="flex flex-col gap-3 p-4">
      <header className="flex items-center justify-between gap-2">
        <div>
          <h1 className="text-xl font-semibold">{player.name}</h1>
          <p className="text-sm text-muted-foreground">{player.tag}</p>
        </div>
        <RenewalButton kind="player" tag={player.tag} />
      </header>
      <dl className="grid grid-cols-2 gap-2 text-sm">
        <Stat label="트로피" value={formatNumber(player.trophies)} />
        <Stat label="최고 트로피" value={formatNumber(player.highestTrophies)} />
        <Stat label="레벨" value={formatNumber(player.expLevel)} />
        <Stat label="3v3 승" value={formatNumber(player.victories3vs3)} />
        <Stat label="솔로 승" value={formatNumber(player.victoriesSolo)} />
        <Stat label="듀오 승" value={formatNumber(player.victoriesDuo)} />
      </dl>
      {player.club ? (
        <p className="text-sm">
          클럽:{" "}
          <Link className="underline" href={routes.club(player.club.tag)}>
            {player.club.name}
          </Link>
        </p>
      ) : (
        <p className="text-sm text-muted-foreground">소속 클럽 없음</p>
      )}
    </Card>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between rounded-md bg-muted/40 px-3 py-2">
      <dt className="text-muted-foreground">{label}</dt>
      <dd className="font-medium">{value}</dd>
    </div>
  );
}
```

- [ ] **Step 4: `PlayerStats.tsx` 단순화**

```tsx
"use client";

import type { Player } from "@/domain/player/types";
import { formatNumber } from "@/lib/format";
import { Card } from "@/components/ui/card";

export function PlayerStats({ player }: { player: Player }) {
  return (
    <Card className="grid grid-cols-2 gap-3 p-4 text-sm">
      <Row label="현재 트로피" value={formatNumber(player.trophies)} />
      <Row label="최고 트로피" value={formatNumber(player.highestTrophies)} />
      <Row label="총 프레스티지" value={formatNumber(player.totalPrestigeLevel)} />
      <Row label="EXP 포인트" value={formatNumber(player.expPoints)} />
    </Card>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between border-b py-1 last:border-b-0">
      <span className="text-muted-foreground">{label}</span>
      <span className="font-medium">{value}</span>
    </div>
  );
}
```

- [ ] **Step 5: `PlayerBrawlers.tsx`** — `PlayerBrawler[]` 받기

```tsx
"use client";

import type { PlayerBrawler } from "@/domain/player/types";
import { formatNumber } from "@/lib/format";
import { Card } from "@/components/ui/card";
import Image from "next/image";

export function PlayerBrawlers({ brawlers }: { brawlers: PlayerBrawler[] }) {
  if (brawlers.length === 0) {
    return <p className="text-sm text-muted-foreground">보유한 브롤러가 없습니다.</p>;
  }
  const sorted = [...brawlers].sort((a, b) => b.trophies - a.trophies);
  return (
    <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4">
      {sorted.map((b) => (
        <li key={`${b.id}-${b.name}`}>
          <Card className="flex flex-col items-center gap-1 p-3 text-center text-sm">
            {b.portraitImageUrl ? (
              <Image
                src={b.portraitImageUrl}
                alt={b.name}
                width={64}
                height={64}
                unoptimized
              />
            ) : (
              <div className="h-16 w-16 rounded-full bg-muted" />
            )}
            <p className="font-medium">{b.name}</p>
            <p className="text-muted-foreground">
              파워 {b.power} · 🏆 {formatNumber(b.trophies)}
            </p>
          </Card>
        </li>
      ))}
    </ul>
  );
}
```

- [ ] **Step 6: `PlayerBattles.tsx`** — placeholder (배틀 도메인 spec에서 채움)

```tsx
"use client";

export function PlayerBattles({ tag }: { tag: string }) {
  return (
    <p className="text-sm text-muted-foreground">
      배틀 기록은 곧 제공될 예정입니다.
    </p>
  );
}
```

- [ ] **Step 7: `next.config.ts`에 외부 이미지 도메인 허용** (BrawlStars 이미지 CDN)

```bash
cat imstargg-frontend/next.config.ts
```

`images.remotePatterns`에 BrawlStars 관련 도메인을 추가. 정확한 도메인 미상이면 Image의 `unoptimized` prop을 위 PlayerBrawlers에서 사용 (이미 사용 중) — 이 경우 next.config 변경 불필요.

- [ ] **Step 8: 빌드 + 타입체크**

```bash
cd imstargg-frontend
pnpm tsc --noEmit
pnpm build
```

Expected: 통과.

- [ ] **Step 9: 커밋**

```bash
git add app/players src/
git commit -m "feat(frontend): wire player detail page to live api with rsc + tanstack query"
```

### Task 9.5: 홈 + TopNav 검색 입력 통합

**Files:**
- Modify: `imstargg-frontend/app/page.tsx` 또는 `imstargg-frontend/app/_components/HomeScreen.tsx`
- Modify: `imstargg-frontend/src/components/layout/TopNav.tsx`

- [ ] **Step 1: HomeScreen 검색 영역 교체**

`HomeScreen.tsx` 안의 검색 폼을 `<PlayerSearchInput />`로 교체. 시각적 위치는 기존 디자인 유지.

```tsx
import { PlayerSearchInput } from "@/components/shared/PlayerSearchInput";
// ...
<PlayerSearchInput />
```

- [ ] **Step 2: TopNav 검색 (모바일/데스크톱)** — 이미 검색 UI가 있으면 동일 컴포넌트로 교체. 없으면 데스크톱 헤더 우측에 `<PlayerSearchInput />` 작은 버전 추가 (선택).

- [ ] **Step 3: 빌드**

```bash
pnpm build
```

- [ ] **Step 4: 커밋**

```bash
git add app/ src/components/layout
git commit -m "feat(frontend): wire home and topnav search to PlayerSearchInput"
```

---

## Phase 10: 통합 테스트

### Task 10.1: `playerRenewal.test.tsx` — 폴링 + invalidate 통합 테스트

**Files:**
- Create: `imstargg-frontend/tests/setup/renderWithProviders.tsx`
- Create: `imstargg-frontend/tests/integration/player/playerRenewal.test.tsx`

- [ ] **Step 1: `renderWithProviders.tsx` 작성**

```tsx
import { render } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Toaster } from "@/components/ui/sonner";
import type { ReactElement } from "react";

export function renderWithProviders(ui: ReactElement) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0, staleTime: 0 } },
  });
  const view = render(
    <QueryClientProvider client={queryClient}>
      {ui}
      <Toaster richColors position="top-center" />
    </QueryClientProvider>,
  );
  return { ...view, queryClient };
}
```

- [ ] **Step 2: `tests/integration/player/playerRenewal.test.tsx` 작성** — 실패하는 테스트

```tsx
import { describe, it, expect, beforeEach } from "vitest";
import { http, HttpResponse } from "msw";
import { server } from "../../setup/msw";
import { renderWithProviders } from "../../setup/renderWithProviders";
import { screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { RenewalButton } from "@/components/shared/RenewalButton";

const TAG = "#88J8Q9RPV";
const ENC = "%2388J8Q9RPV";

describe("RenewalButton (player) — 폴링 + 완료", () => {
  beforeEach(() => {
    let progressing = false;
    server.use(
      http.get(`*/v1/players/${ENC}/renewal`, () =>
        HttpResponse.json({
          tag: TAG, status: progressing ? "EXECUTING" : "COMPLETED",
          progressing, canRenew: !progressing,
          requestedAt: "2026-04-27T00:00:00Z", executedAt: null,
          completedAt: progressing ? null : "2026-04-27T00:00:01Z",
          renewedAt: progressing ? null : "2026-04-27T00:00:01Z",
        }),
      ),
      http.post(`*/v1/players/${ENC}/renewal`, () => {
        progressing = true;
        setTimeout(() => { progressing = false; }, 1500);
        return HttpResponse.json({
          tag: TAG, status: "EXECUTING",
          progressing: true, canRenew: false,
          requestedAt: "2026-04-27T00:00:02Z", executedAt: null,
          completedAt: null, renewedAt: null,
        });
      }),
    );
  });

  it("갱신 클릭 → '갱신 요청됨' → 폴링 후 '갱신 완료' 토스트", async () => {
    renderWithProviders(<RenewalButton kind="player" tag={TAG} />);

    const button = await screen.findByRole("button", { name: /갱신/ });
    await userEvent.click(button);

    expect(await screen.findByText(/갱신 요청됨/)).toBeInTheDocument();

    expect(
      await screen.findByText(/갱신 완료/, {}, { timeout: 5000 }),
    ).toBeInTheDocument();
  });
});
```

- [ ] **Step 3: 환경변수 — vitest는 `process.env.NEXT_PUBLIC_API_BASE_URL` 필요**

`vitest.config.ts`의 `test`에 `env` 추가:

```typescript
test: {
  // ...existing...
  env: {
    NEXT_PUBLIC_API_BASE_URL: "https://core-api.test",
    NEXT_PUBLIC_SITE_URL: "https://imstargg.test",
  },
},
```

- [ ] **Step 4: 실행**

```bash
pnpm test:run tests/integration/player/playerRenewal.test.tsx
```

Expected: PASS. 만약 timing issue로 flaky하면 setTimeout 1500ms를 적절히 조정.

- [ ] **Step 5: 커밋**

```bash
git add tests/setup/renderWithProviders.tsx tests/integration vitest.config.ts
git commit -m "test(frontend): integration test for player renewal polling"
```

### Task 10.2: 전체 단위/통합 테스트 통과 확인

- [ ] **Step 1: 모두 실행**

```bash
cd imstargg-frontend
pnpm test:run
```

Expected: 모든 테스트 통과 (cn, tag, format, mappers, playerRenewal).

- [ ] **Step 2: 실패가 있으면 해당 task로 돌아가 수정. 통과하면 다음 Phase로.**

---

## Phase 11: E2E (prod API)

### Task 11.1: `home.spec.ts`

**Files:**
- Create: `imstargg-frontend/tests/e2e/home.spec.ts`

- [ ] **Step 1: 작성**

```typescript
import { test, expect } from "@playwright/test";
import { TEST_TAGS } from "./fixtures/tags";

test("홈 페이지가 로드되고 메타데이터가 노출된다", async ({ page }) => {
  await page.goto("/");
  await expect(page).toHaveTitle(/ImStarGG/);
  const description = await page.locator('meta[name="description"]').getAttribute("content");
  expect(description ?? "").toContain("브롤스타즈");
});

test("플레이어 태그 검색 후 상세 페이지로 이동한다", async ({ page }) => {
  await page.goto("/");
  const input = page.getByLabel("플레이어 태그 입력");
  await input.fill(TEST_TAGS.player);
  await page.getByRole("button", { name: "플레이어 검색" }).click();
  await expect(page).toHaveURL(new RegExp(TEST_TAGS.playerEncoded));
  await expect(page.getByText(TEST_TAGS.player)).toBeVisible({ timeout: 15_000 });
});
```

- [ ] **Step 2: 실행**

```bash
pnpm test:e2e tests/e2e/home.spec.ts
```

Expected: PASS (prod API 가용 시).

- [ ] **Step 3: 커밋**

```bash
git add tests/e2e/home.spec.ts
git commit -m "test(frontend): e2e for home and player search"
```

### Task 11.2: `player.spec.ts` — 갱신 흐름

**Files:**
- Create: `imstargg-frontend/tests/e2e/player.spec.ts`

- [ ] **Step 1: 작성**

```typescript
import { test, expect } from "@playwright/test";
import { TEST_TAGS } from "./fixtures/tags";

test("플레이어 페이지가 정상 데이터로 렌더된다", async ({ page }) => {
  await page.goto(`/players/${TEST_TAGS.playerEncoded}`);
  await expect(page.getByText(TEST_TAGS.player)).toBeVisible({ timeout: 15_000 });
  await expect(page.getByText(/트로피/)).toBeVisible();
});

test("갱신 버튼 클릭 시 토스트 → 폴링 → 완료 토스트", async ({ page }) => {
  await page.goto(`/players/${TEST_TAGS.playerEncoded}`);
  const renewBtn = page.getByRole("button", { name: "플레이어 정보 갱신" });
  await renewBtn.waitFor({ state: "visible", timeout: 15_000 });
  // 갱신 가능 상태가 되도록 (이전 갱신이 진행 중이면 잠시 대기)
  await expect(renewBtn).toBeEnabled({ timeout: 15_000 });
  await renewBtn.click();

  await expect(page.getByText(/갱신 요청됨/)).toBeVisible({ timeout: 5_000 });
  await expect(page.getByText(/갱신 완료/)).toBeVisible({ timeout: 30_000 });
});
```

- [ ] **Step 2: 실행**

```bash
pnpm test:e2e tests/e2e/player.spec.ts
```

Expected: PASS. flaky하면 timeout 조정. 갱신 시간이 길면 30초 → 60초로.

- [ ] **Step 3: 커밋**

```bash
git add tests/e2e/player.spec.ts
git commit -m "test(frontend): e2e for player detail and renewal flow"
```

### Task 11.3: `seo.spec.ts`

**Files:**
- Create: `imstargg-frontend/tests/e2e/seo.spec.ts`

- [ ] **Step 1: 작성**

```typescript
import { test, expect } from "@playwright/test";

test("/robots.txt 가 사이트맵을 가리킨다", async ({ request }) => {
  const res = await request.get("/robots.txt");
  expect(res.status()).toBe(200);
  const body = await res.text();
  expect(body).toMatch(/Sitemap:/);
});

test("/sitemap.xml 이 정적 라우트를 포함한다", async ({ request }) => {
  const res = await request.get("/sitemap.xml");
  expect(res.status()).toBe(200);
  const body = await res.text();
  expect(body).toContain("/ranking");
  expect(body).toContain("/brawlers");
});

test("플레이어 페이지의 canonical 링크가 자기 자신을 가리킨다", async ({ page }) => {
  await page.goto("/players/%2388J8Q9RPV");
  const canonical = await page.locator('link[rel="canonical"]').getAttribute("href");
  expect(canonical ?? "").toContain("/players/%2388J8Q9RPV");
});
```

- [ ] **Step 2: 실행**

```bash
pnpm test:e2e tests/e2e/seo.spec.ts
```

Expected: PASS.

- [ ] **Step 3: 커밋**

```bash
git add tests/e2e/seo.spec.ts
git commit -m "test(frontend): e2e for robots, sitemap, canonical"
```

---

## Phase 12: 미구현 도메인 페이지 빌드 통과 정리

### Task 12.1: 클럽/브롤러/이벤트/랭킹 페이지의 빌드 에러 정리

**Files:**
- Inspect: `imstargg-frontend/app/clubs/`, `app/brawlers/`, `app/events/`, `app/ranking/`

- [ ] **Step 1: 빌드 에러 식별**

```bash
cd imstargg-frontend && pnpm build 2>&1 | tee /tmp/build.log
grep -E "(Type error|error TS|Cannot find)" /tmp/build.log | head -30
```

- [ ] **Step 2: 각 페이지에 TODO 주석 + 임시 placeholder 적용**

기존 mock 사용 페이지에서 컴파일 에러를 발생시키는 import만 제거하거나, 필요 시 다음과 같은 placeholder로 임시 교체:

```tsx
// TODO(domain-spec): 클럽 도메인 spec에서 본격 구현
export default function ClubsPlaceholder() {
  return (
    <main className="py-12 text-center text-muted-foreground">
      클럽 통계는 곧 제공될 예정입니다.
    </main>
  );
}
```

> 가능하면 기존 mock UI를 살리되, 새 컴포넌트(layout/shared) 위치 변경에 따른 import만 정리. 본격 API 연결은 도메인 spec에서.

- [ ] **Step 3: 빌드 통과 확인**

```bash
pnpm build
```

Expected: 통과.

- [ ] **Step 4: 커밋**

```bash
git add app/
git commit -m "chore(frontend): keep non-foundation pages compiling with TODOs"
```

---

## Phase 13: 최종 검증

### Task 13.1: 전체 검증

- [ ] **Step 1: 백엔드 contextTest**

```bash
cd imstargg-backend
./gradlew :core:core-api:contextTest --tests "com.imstargg.core.api.config.WebMvcConfigTest"
```

Expected: PASS.

- [ ] **Step 2: 프론트 typecheck/lint/build**

```bash
cd ../imstargg-frontend
pnpm typecheck && pnpm lint && pnpm build
```

Expected: 모두 통과.

- [ ] **Step 3: 단위/통합 테스트**

```bash
pnpm test:run
```

Expected: 모두 PASS.

- [ ] **Step 4: E2E 테스트 (prod API)**

```bash
pnpm test:e2e
```

Expected: 모두 PASS.

- [ ] **Step 5: 수동 점검 (dev 서버)**

```bash
pnpm dev
```

브라우저에서:
1. `http://localhost:3000` → 검색에 `#88J8Q9RPV` 입력 → 플레이어 페이지로 이동
2. 페이지에 닉네임/트로피/탭(통계/브롤러/배틀)이 보이는지
3. 갱신 버튼 클릭 → "갱신 요청됨" 토스트 → 1초 폴링 동안 버튼 비활성화 → "갱신 완료" 토스트 → 데이터 새로고침
4. `/robots.txt`, `/sitemap.xml` 정상 응답
5. 페이지 소스 보기로 `<title>`, `<link rel="canonical">`, `og:image` 확인

- [ ] **Step 6: 누락사항 또는 문제 발견 시 해당 Task로 돌아가 수정 후 재검증.**

- [ ] **Step 7: 모두 통과 시 최종 머지 커밋 (필요 시):**

```bash
git status
# clean이면 OK. 아니면 잔여 변경사항 정리 후 commit.
```

### Task 13.2: `imstargg-frontend/AGENTS.md` 업데이트

**Files:**
- Modify: `imstargg-frontend/AGENTS.md`

> 원칙: 꼭 필요한 내용만 간결하게. AGENTS.md(Next 16 경고)는 그대로 참조 유지.

- [ ] **Step 1: 다음 내용으로 교체**

```markdown
@AGENTS.md

# imstargg-frontend

브롤스타즈 통계 프론트엔드. Next.js App Router + RSC + TanStack Query.

## 명령어

```bash
pnpm dev              # 개발 서버
pnpm build            # 프로덕션 빌드
pnpm typecheck        # tsc --noEmit
pnpm lint
pnpm test:run         # Vitest
pnpm test:e2e         # Playwright (prod API 호출)
```

## 아키텍처 레이어 (단방향 의존)

```
app/  ──▶  src/hooks/  ──▶  src/api/   ──▶  외부 HTTP
            │
            ▼
        src/domain/  (DTO → 화면 모델 매퍼)
```

- 컴포넌트/RSC는 `src/api/`를 직접 부르지 않고 훅 또는 endpoint 객체 (`playerApi` 등)를 통해서만 호출.
- TanStack Query 키는 `src/hooks/queries/queryKeys.ts`에서만 생성.
- 백엔드 응답 DTO 타입은 `src/api/types.ts`. 변경 시 `src/domain/*/mappers.ts`도 함께 수정.

## 데이터 패칭

- **RSC-first**: 페이지 초기 데이터는 서버 컴포넌트에서 `apiFetch` 호출 + `next.revalidate`.
- **클라이언트 인터랙션**: `useQuery` `initialData`로 RSC 데이터 이어받음. 갱신/폴링 등 동적 영역은 TanStack Query.

## 갱신(Renewal)

`POST /v1/{players|clubs}/{tag}/renewal` 트리거 → `progressing=true` 동안 1초 폴링 → `false`로 전환 시 자동 invalidate. 훅: `usePlayerRenewal`.

## 테스트 원칙

- **네트워크 경계만 모킹**(MSW). 우리 코드(`src/api/*`, `src/hooks/*`, 컴포넌트)는 모킹 금지.
- **사용자 관점**(Testing Library의 role/text 쿼리). 구현 상세 검증 금지.
- 단위 테스트는 순수 함수만(`lib/`, `domain/*/mappers`).
- E2E는 prod API 직접 호출. 테스트 태그: 플레이어 `#88J8Q9RPV`, 클럽 `#2U8CVLQQ9`.

## UI

- 일반 프리미티브: `src/components/ui/` (shadcn/ui).
- 도메인 시각요소: `src/components/primitives/` (Trophy, BrawlerPortrait 등).
- 토스트: `import { toast } from "sonner"`.

## 환경변수

`NEXT_PUBLIC_API_BASE_URL`, `NEXT_PUBLIC_SITE_URL`. 검증은 `src/config/env.ts`.

## 설계/구현 문서

- `docs/superpowers/specs/2026-04-27-frontend-foundation-design.md`
- `docs/superpowers/plans/2026-04-27-frontend-foundation.md`
```

- [ ] **Step 2: 커밋**

```bash
git add imstargg-frontend/AGENTS.md
git commit -m "docs(frontend): update AGENTS.md with foundation conventions"
```

---

## Self-Review (작성자 노트)

**Spec coverage:**
- §3 기술스택 → Phase 1
- §4 폴더 구조 → Phase 2
- §5 API 레이어 → Phase 4
- §6 도메인 모델 + 매퍼 → Phase 5
- §7 훅 레이어 (queryKeys, usePlayer, usePlayerBrawlers, usePlayerRenewal) → Phase 5
- §8 SEO 인프라 → Phase 8
- §9 UI/shadcn + sonner 교체 → Phase 6
- §10 환경변수 + CORS → Task 1.5, Phase 7
- §11 테스트 전략 → Phase 10, 11 (네트워크 경계 모킹, 사용자 관점, prod API E2E)
- §12 플레이어 reference 구현 → Phase 9
- §13 구현 순서 → 본 plan의 Phase 순서와 일치
- §14 검증 기준 → Phase 13

**잠재 위험:**
- shadcn CLI가 자체 `lib/utils.ts`를 만들면 우리 `cn`과 충돌 — Task 6.1 Step 2에서 해결 지침 포함
- vitest에서 `process.env`가 비어 있으면 `env.ts`의 zod parse 실패 — Task 10.1 Step 3에서 vitest config에 env 주입
- E2E의 갱신 흐름은 백엔드 갱신 작업 시간에 의존 → timeout 30s, 필요 시 60s로 조정
- 백엔드 `application-prod.yml`에 `app:` 키가 이미 존재할 수 있음 — Task 7.2에서 기존 키 아래 cors 병합

**미정의 항목:**
- 일부 보조 DTO(GadgetResponse 등) 정확한 필드는 도메인 spec에서 정밀화 — Foundation은 기본 구조만 정의
- 외부 이미지 도메인 next.config 설정 — `unoptimized` 사용으로 우회, 필요 시 도메인 spec에서 추가
