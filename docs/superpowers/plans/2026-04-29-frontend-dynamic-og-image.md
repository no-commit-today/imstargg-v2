# Dynamic OG Image Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Player/Club 페이지의 SNS 공유 카드를 Next.js 16 file convention(`opengraph-image.tsx`, `twitter-image.tsx`) + `next/og` `ImageResponse` 로 동적 1200×630 PNG 카드로 전환한다.

**Architecture:** `lib/og/` 에 폰트 로더 + 공통 카드 레이아웃 + Player/Club 별 ImageResponse 빌더를 두고, 각 디렉토리의 file convention 파일은 빌더를 호출하는 얇은 wrapper. `buildMetadata` 에 `dynamicOg` 옵션을 추가해 `images` 키 자체를 omit (Next.js 16.2.1 의 `mergeStaticMetadata` 가 `hasOwnProperty('images')` 로 file convention 적용 여부를 판단하기 때문).

**Tech Stack:** Next.js 16 App Router, `next/og` `ImageResponse` (satori 기반), `node:fs/promises` `readFile`, KoBrawl Gothic + Lilita One self-hosted 폰트, Vitest.

**Spec:** `docs/superpowers/specs/2026-04-29-frontend-dynamic-og-image-design.md`

---

## File Structure

| 파일 | 책임 | 작업 |
|---|---|---|
| `imstargg-frontend/src/lib/seo.ts` | 페이지 metadata 빌더 | `dynamicOg?: boolean` 옵션 추가 — true 시 `openGraph.images` / `twitter.images` 키 omit |
| `imstargg-frontend/tests/lib/seo.test.ts` | seo 단위 테스트 | (신규) `dynamicOg` 분기에 대한 hasOwnProperty 검증 |
| `imstargg-frontend/src/lib/og/loadOgFonts.ts` | OG 폰트 readFile 헬퍼 | 신규 |
| `imstargg-frontend/src/lib/og/OgCard.tsx` | 다크 hero + 옐로우 stripe + 텍스트 레이아웃 | 신규 |
| `imstargg-frontend/src/lib/og/playerOgImage.tsx` | Player ImageResponse 빌더 (og + twitter 공유) | 신규 |
| `imstargg-frontend/src/lib/og/clubOgImage.tsx` | Club ImageResponse 빌더 (og + twitter 공유) | 신규 |
| `imstargg-frontend/app/players/[tag]/opengraph-image.tsx` | file convention wrapper | 신규 |
| `imstargg-frontend/app/players/[tag]/twitter-image.tsx` | opengraph-image re-export | 신규 |
| `imstargg-frontend/app/players/[tag]/opengraph-image.alt.txt` | OG/Twitter alt 텍스트 | 신규 |
| `imstargg-frontend/app/clubs/[tag]/opengraph-image.tsx` | file convention wrapper | 신규 |
| `imstargg-frontend/app/clubs/[tag]/twitter-image.tsx` | opengraph-image re-export | 신규 |
| `imstargg-frontend/app/clubs/[tag]/opengraph-image.alt.txt` | OG/Twitter alt 텍스트 | 신규 |
| `imstargg-frontend/app/players/[tag]/page.tsx` | Player 페이지 metadata | `generateMetadata` 의 성공 분기에 `dynamicOg: true` 추가 (NotFound 분기는 그대로) |
| `imstargg-frontend/app/clubs/[tag]/page.tsx` | Club 페이지 metadata | `generateMetadata` 의 성공 분기에 `dynamicOg: true` 추가 (NotFound 분기는 그대로) |

---

### Task 1: `buildMetadata` 에 `dynamicOg` 옵션 + 단위 테스트

**Files:**
- Create: `imstargg-frontend/tests/lib/seo.test.ts`
- Modify: `imstargg-frontend/src/lib/seo.ts`

- [ ] **Step 1: 실패하는 테스트 작성**

`imstargg-frontend/tests/lib/seo.test.ts` 파일을 새로 만든다:

```ts
import { describe, expect, it } from "vitest";
import { buildMetadata } from "@/lib/seo";

describe("buildMetadata", () => {
  const base = {
    title: "T",
    description: "D",
    path: "/p",
  } as const;

  it("dynamicOg 미지정 시 openGraph.images 와 twitter.images 가 hasOwnProperty 로 존재한다", () => {
    const meta = buildMetadata(base);
    expect(meta.openGraph).toBeDefined();
    expect(meta.twitter).toBeDefined();
    // hasOwnProperty 가 true 여야 한다
    expect(Object.prototype.hasOwnProperty.call(meta.openGraph!, "images")).toBe(true);
    expect(Object.prototype.hasOwnProperty.call(meta.twitter!, "images")).toBe(true);
    // 기본 logo 가 들어있다
    expect(meta.openGraph!.images).toEqual([
      { url: "/assets/logo-full.png", width: 1200, height: 630 },
    ]);
    expect(meta.twitter!.images).toEqual(["/assets/logo-full.png"]);
  });

  it("dynamicOg=true 일 때 openGraph 와 twitter 객체에서 images 키가 omit 된다", () => {
    const meta = buildMetadata({ ...base, dynamicOg: true });
    expect(meta.openGraph).toBeDefined();
    expect(meta.twitter).toBeDefined();
    // hasOwnProperty 가 false 여야 file convention 이 picked up 된다
    expect(Object.prototype.hasOwnProperty.call(meta.openGraph!, "images")).toBe(false);
    expect(Object.prototype.hasOwnProperty.call(meta.twitter!, "images")).toBe(false);
  });

  it("dynamicOg=true + image 옵션이 와도 images 는 omit 한다", () => {
    const meta = buildMetadata({ ...base, dynamicOg: true, image: "/x.png" });
    expect(Object.prototype.hasOwnProperty.call(meta.openGraph!, "images")).toBe(false);
    expect(Object.prototype.hasOwnProperty.call(meta.twitter!, "images")).toBe(false);
  });
});
```

- [ ] **Step 2: 테스트 실행 → 실패 확인**

```bash
cd imstargg-frontend
pnpm test:run tests/lib/seo.test.ts
```

Expected: 두 번째/세 번째 테스트 실패 (현재 buildMetadata 가 항상 images 를 채움). 첫 번째 테스트는 통과.

- [ ] **Step 3: 구현 — `seo.ts` 에 `dynamicOg` 분기 추가**

`imstargg-frontend/src/lib/seo.ts` 전체를 다음으로 교체:

```ts
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
  dynamicOg?: boolean;
};

export function buildMetadata(input: Input): Metadata {
  const url = `${SITE.url}${input.path}`;
  const meta: Metadata = {
    title: input.title,
    description: input.description,
    alternates: { canonical: url },
    robots: input.noindex ? { index: false, follow: false } : undefined,
    openGraph: {
      title: input.title,
      description: input.description,
      url,
      siteName: SITE.name,
      locale: SITE.locale,
      type: "website",
    },
    twitter: {
      card: "summary_large_image",
      title: input.title,
      description: input.description,
    },
  };

  if (!input.dynamicOg) {
    const image = input.image ?? SITE.defaultOgImage;
    meta.openGraph!.images = [{ url: image, width: 1200, height: 630 }];
    meta.twitter!.images = [image];
  }
  return meta;
}
```

- [ ] **Step 4: 테스트 실행 → 통과 확인**

```bash
cd imstargg-frontend
pnpm test:run tests/lib/seo.test.ts
```

Expected: 3 개 테스트 모두 통과.

- [ ] **Step 5: typecheck**

```bash
cd imstargg-frontend
pnpm typecheck
```

Expected: 통과.

- [ ] **Step 6: Commit**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/src/lib/seo.ts imstargg-frontend/tests/lib/seo.test.ts
git commit -m "$(cat <<'EOF'
feat(frontend): buildMetadata 에 dynamicOg 옵션 추가

dynamicOg=true 일 때 openGraph 와 twitter 객체에서 images 키 자체를 omit 한다.
Next.js 16.2.1 의 mergeStaticMetadata 는 source 가 hasOwnProperty('images') 를
가지면 file convention 의 og/twitter 이미지를 skip 하므로, 동적 OG 카드를 적용하려는
페이지는 이 옵션을 true 로 설정한다.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: `loadOgFonts` 헬퍼

**Files:**
- Create: `imstargg-frontend/src/lib/og/loadOgFonts.ts`

- [ ] **Step 1: 코드 작성**

`imstargg-frontend/src/lib/og/loadOgFonts.ts`:

```ts
import { readFile } from "node:fs/promises";
import { join } from "node:path";

export interface OgFont {
  name: string;
  data: Buffer;
  style: "normal";
  weight: 400;
}

export async function loadOgFonts(): Promise<OgFont[]> {
  const [koBrawl, lilita] = await Promise.all([
    readFile(join(process.cwd(), "public/fonts/KoBrawl_Gothic40.otf")),
    readFile(join(process.cwd(), "public/fonts/lilitaone-regular-webfont__2_.ttf")),
  ]);
  return [
    { name: "KoBrawl Gothic", data: koBrawl, style: "normal", weight: 400 },
    { name: "Lilita One", data: lilita, style: "normal", weight: 400 },
  ];
}
```

- [ ] **Step 2: typecheck**

```bash
cd imstargg-frontend
pnpm typecheck
```

Expected: 통과.

- [ ] **Step 3: Commit**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/src/lib/og/loadOgFonts.ts
git commit -m "$(cat <<'EOF'
feat(frontend): OG 이미지용 폰트 로더 추가

KoBrawl Gothic + Lilita One self-hosted 폰트를 readFile 로 읽어 ImageResponse 의
fonts 옵션에 넘길 수 있도록 한다. 한글 디스플레이는 KoBrawl Gothic, 영문은 Lilita One.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: `OgCard` 공통 레이아웃

**Files:**
- Create: `imstargg-frontend/src/lib/og/OgCard.tsx`

- [ ] **Step 1: 코드 작성**

`imstargg-frontend/src/lib/og/OgCard.tsx`:

```tsx
export interface OgCardStat {
  label: string;
  value: string;
}

export interface OgCardProps {
  eyebrow: string;
  name: string;
  tag: string;
  stats: OgCardStat[];
}

export function OgCard({ eyebrow, name, tag, stats }: OgCardProps) {
  return (
    <div
      style={{
        width: "100%",
        height: "100%",
        display: "flex",
        flexDirection: "column",
        background: "#14161B",
        padding: "64px 80px",
        position: "relative",
        fontFamily: "KoBrawl Gothic",
      }}
    >
      <div
        style={{
          display: "flex",
          fontFamily: "Lilita One",
          fontSize: 32,
          color: "#FFC60A",
          letterSpacing: "0.08em",
        }}
      >
        IMSTARGG · {eyebrow}
      </div>

      <div
        style={{
          flex: 1,
          display: "flex",
          flexDirection: "column",
          justifyContent: "center",
        }}
      >
        <div
          style={{
            display: "flex",
            fontSize: 96,
            color: "#FFFFFF",
            lineHeight: 1.1,
          }}
        >
          {name}
        </div>
        <div
          style={{
            display: "flex",
            marginTop: 12,
            fontFamily: "monospace",
            fontSize: 32,
            color: "#FFC60A",
          }}
        >
          {tag}
        </div>
        {stats.length > 0 && (
          <div
            style={{
              marginTop: 36,
              display: "flex",
              gap: 32,
              fontSize: 36,
              color: "rgba(255,255,255,0.85)",
            }}
          >
            {stats.map((s, i) => (
              <div key={i} style={{ display: "flex", gap: 12 }}>
                <span style={{ color: "rgba(255,255,255,0.5)" }}>{s.label}</span>
                <span>{s.value}</span>
              </div>
            ))}
          </div>
        )}
      </div>

      <div
        style={{
          display: "flex",
          position: "absolute",
          left: 0,
          right: 0,
          bottom: 0,
          height: 8,
          background: "#FFC60A",
        }}
      />
    </div>
  );
}
```

> 모든 컨테이너에 `display: "flex"` 명시 — satori 가 다른 display 를 거부하므로.

- [ ] **Step 2: typecheck**

```bash
cd imstargg-frontend
pnpm typecheck
```

Expected: 통과.

- [ ] **Step 3: Commit**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/src/lib/og/OgCard.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): OG 이미지 공통 카드 레이아웃 추가

다크 hero (#14161B) + 옐로우 stripe + KoBrawl Gothic 으로 Player/Club 이 공유하는
1200x630 카드 레이아웃. eyebrow / name / tag / stats 를 props 로 받아 ImageResponse
의 JSX 트리로 사용된다.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 4: `playerOgImage` 빌더

**Files:**
- Create: `imstargg-frontend/src/lib/og/playerOgImage.tsx`

- [ ] **Step 1: 코드 작성**

`imstargg-frontend/src/lib/og/playerOgImage.tsx`:

```tsx
import { ImageResponse } from "next/og";
import { playerApi } from "@/api/endpoints/player";
import { NotFoundError } from "@/api/errors";
import { formatNumber } from "@/lib/format";
import { loadOgFonts } from "@/lib/og/loadOgFonts";
import { OgCard } from "@/lib/og/OgCard";

export const playerOgAlt = "ImStarGG 플레이어 카드";
export const playerOgSize = { width: 1200, height: 630 } as const;
export const playerOgContentType = "image/png";

export async function playerOgImage(tag: string): Promise<Response> {
  const fonts = await loadOgFonts();

  try {
    const dto = await playerApi.get(tag, { next: { revalidate: 60 } });
    return new ImageResponse(
      (
        <OgCard
          eyebrow="PLAYER"
          name={dto.name}
          tag={dto.tag}
          stats={[
            { label: "트로피", value: formatNumber(dto.trophies) },
            { label: "클럽", value: dto.club?.name ?? "클럽 없음" },
          ]}
        />
      ),
      { ...playerOgSize, fonts },
    );
  } catch (e) {
    if (e instanceof NotFoundError) {
      return new ImageResponse(
        (
          <OgCard
            eyebrow="PLAYER"
            name="플레이어를 찾을 수 없음"
            tag={tag}
            stats={[]}
          />
        ),
        { ...playerOgSize, fonts },
      );
    }
    throw e;
  }
}
```

- [ ] **Step 2: typecheck**

```bash
cd imstargg-frontend
pnpm typecheck
```

Expected: 통과.

- [ ] **Step 3: Commit**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/src/lib/og/playerOgImage.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): Player OG ImageResponse 빌더 추가

playerApi.get 결과를 받아 OgCard 로 1200x630 PNG 응답을 반환. NotFound 분기는
"플레이어를 찾을 수 없음" 카드. 이 빌더는 opengraph-image.tsx 와 twitter-image.tsx
가 공유하므로 og:image 와 twitter:image 가 동일한 PNG 를 가리킨다.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 5: `clubOgImage` 빌더

**Files:**
- Create: `imstargg-frontend/src/lib/og/clubOgImage.tsx`

- [ ] **Step 1: 코드 작성**

`imstargg-frontend/src/lib/og/clubOgImage.tsx`:

```tsx
import { ImageResponse } from "next/og";
import { clubApi } from "@/api/endpoints/club";
import { NotFoundError } from "@/api/errors";
import { formatNumber } from "@/lib/format";
import { loadOgFonts } from "@/lib/og/loadOgFonts";
import { OgCard } from "@/lib/og/OgCard";

export const clubOgAlt = "ImStarGG 클럽 카드";
export const clubOgSize = { width: 1200, height: 630 } as const;
export const clubOgContentType = "image/png";

export async function clubOgImage(tag: string): Promise<Response> {
  const fonts = await loadOgFonts();

  try {
    const dto = await clubApi.get(tag, { next: { revalidate: 60 } });
    return new ImageResponse(
      (
        <OgCard
          eyebrow="CLUB"
          name={dto.name}
          tag={dto.tag}
          stats={[
            { label: "트로피", value: formatNumber(dto.trophies) },
            { label: "멤버", value: `${dto.members.length}/30` },
          ]}
        />
      ),
      { ...clubOgSize, fonts },
    );
  } catch (e) {
    if (e instanceof NotFoundError) {
      return new ImageResponse(
        (
          <OgCard
            eyebrow="CLUB"
            name="클럽을 찾을 수 없음"
            tag={tag}
            stats={[]}
          />
        ),
        { ...clubOgSize, fonts },
      );
    }
    throw e;
  }
}
```

- [ ] **Step 2: typecheck**

```bash
cd imstargg-frontend
pnpm typecheck
```

Expected: 통과.

- [ ] **Step 3: Commit**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/src/lib/og/clubOgImage.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): Club OG ImageResponse 빌더 추가

clubApi.get 결과를 받아 OgCard 로 1200x630 PNG 응답을 반환. members.length/30 으로
멤버 슬롯 표시. NotFound 분기는 "클럽을 찾을 수 없음".

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 6: Player file convention 파일 (opengraph + twitter + alt)

**Files:**
- Create: `imstargg-frontend/app/players/[tag]/opengraph-image.tsx`
- Create: `imstargg-frontend/app/players/[tag]/twitter-image.tsx`
- Create: `imstargg-frontend/app/players/[tag]/opengraph-image.alt.txt`

- [ ] **Step 1: opengraph-image.tsx 작성**

`imstargg-frontend/app/players/[tag]/opengraph-image.tsx`:

```tsx
import {
  playerOgAlt,
  playerOgContentType,
  playerOgImage,
  playerOgSize,
} from "@/lib/og/playerOgImage";

export const alt = playerOgAlt;
export const size = playerOgSize;
export const contentType = playerOgContentType;

type Props = { params: Promise<{ tag: string }> };

export default async function Image({ params }: Props) {
  const { tag } = await params;
  return playerOgImage(tag);
}
```

- [ ] **Step 2: twitter-image.tsx 작성 (opengraph 와 동일)**

`imstargg-frontend/app/players/[tag]/twitter-image.tsx`:

```tsx
export {
  default,
  alt,
  size,
  contentType,
} from "./opengraph-image";
```

- [ ] **Step 3: alt.txt 작성**

`imstargg-frontend/app/players/[tag]/opengraph-image.alt.txt`:

```
ImStarGG 플레이어 카드
```

- [ ] **Step 4: typecheck**

```bash
cd imstargg-frontend
pnpm typecheck
```

Expected: 통과.

- [ ] **Step 5: Commit**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/app/players/\[tag\]/opengraph-image.tsx \
        imstargg-frontend/app/players/\[tag\]/twitter-image.tsx \
        imstargg-frontend/app/players/\[tag\]/opengraph-image.alt.txt
git commit -m "$(cat <<'EOF'
feat(frontend): Player 디렉토리에 opengraph-image / twitter-image file convention 추가

opengraph-image.tsx 가 playerOgImage 빌더를 호출, twitter-image.tsx 는 동일 export
를 re-export. 같은 디렉토리의 alt.txt 가 og:image:alt 와 twitter:image:alt 메타로
자동 노출. 본 PR 은 file convention 만 추가하므로 다음 PR 의 generateMetadata
변경(dynamicOg: true) 전까지는 buildMetadata 가 박는 정적 logo 가 우선이며 file
convention 은 picked up 되지 않는다 (의도된 단계 분리).

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 7: Club file convention 파일 (opengraph + twitter + alt)

**Files:**
- Create: `imstargg-frontend/app/clubs/[tag]/opengraph-image.tsx`
- Create: `imstargg-frontend/app/clubs/[tag]/twitter-image.tsx`
- Create: `imstargg-frontend/app/clubs/[tag]/opengraph-image.alt.txt`

- [ ] **Step 1: opengraph-image.tsx 작성**

`imstargg-frontend/app/clubs/[tag]/opengraph-image.tsx`:

```tsx
import {
  clubOgAlt,
  clubOgContentType,
  clubOgImage,
  clubOgSize,
} from "@/lib/og/clubOgImage";

export const alt = clubOgAlt;
export const size = clubOgSize;
export const contentType = clubOgContentType;

type Props = { params: Promise<{ tag: string }> };

export default async function Image({ params }: Props) {
  const { tag } = await params;
  return clubOgImage(tag);
}
```

- [ ] **Step 2: twitter-image.tsx 작성**

`imstargg-frontend/app/clubs/[tag]/twitter-image.tsx`:

```tsx
export {
  default,
  alt,
  size,
  contentType,
} from "./opengraph-image";
```

- [ ] **Step 3: alt.txt 작성**

`imstargg-frontend/app/clubs/[tag]/opengraph-image.alt.txt`:

```
ImStarGG 클럽 카드
```

- [ ] **Step 4: typecheck**

```bash
cd imstargg-frontend
pnpm typecheck
```

Expected: 통과.

- [ ] **Step 5: Commit**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/app/clubs/\[tag\]/opengraph-image.tsx \
        imstargg-frontend/app/clubs/\[tag\]/twitter-image.tsx \
        imstargg-frontend/app/clubs/\[tag\]/opengraph-image.alt.txt
git commit -m "$(cat <<'EOF'
feat(frontend): Club 디렉토리에 opengraph-image / twitter-image file convention 추가

clubOgImage 빌더를 호출하는 file convention 한 쌍 + alt.txt. Player 와 동일 패턴.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 8: Player `generateMetadata` 에 `dynamicOg: true`

**Files:**
- Modify: `imstargg-frontend/app/players/[tag]/page.tsx:24`

- [ ] **Step 1: 코드 변경**

`imstargg-frontend/app/players/[tag]/page.tsx` 에서 성공 분기의 `buildMetadata` 호출에 `dynamicOg: true` 한 줄을 추가한다. NotFound 분기는 그대로 둔다.

```tsx
// before (line 24-28)
return buildMetadata({
  title: `${display} | ImStarGG`,
  description: `${display} — 트로피 ${formatNumber(dto.trophies)}, ${dto.club?.name ?? "클럽 없음"}. 브롤스타즈 플레이어 통계.`,
  path: `/players/${toUrlPath(tag)}`,
});

// after
return buildMetadata({
  title: `${display} | ImStarGG`,
  description: `${display} — 트로피 ${formatNumber(dto.trophies)}, ${dto.club?.name ?? "클럽 없음"}. 브롤스타즈 플레이어 통계.`,
  path: `/players/${toUrlPath(tag)}`,
  dynamicOg: true,
});
```

- [ ] **Step 2: typecheck**

```bash
cd imstargg-frontend
pnpm typecheck
```

Expected: 통과.

- [ ] **Step 3: Commit**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/app/players/\[tag\]/page.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): Player 페이지의 generateMetadata 에 dynamicOg: true 추가

성공 분기에 dynamicOg=true 를 전달하여 buildMetadata 가 openGraph.images 와
twitter.images 키를 omit 하도록 한다. Next.js 의 mergeStaticMetadata 가 이제 같은
디렉토리의 opengraph-image.tsx / twitter-image.tsx 를 picked up 하여 동적 PNG 로
og:image, twitter:image 메타를 채운다. NotFound 분기는 그대로 두어 정적 logo 유지.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 9: Club `generateMetadata` 에 `dynamicOg: true`

**Files:**
- Modify: `imstargg-frontend/app/clubs/[tag]/page.tsx:24`

- [ ] **Step 1: 코드 변경**

`imstargg-frontend/app/clubs/[tag]/page.tsx` 에서 성공 분기의 `buildMetadata` 호출에 `dynamicOg: true` 추가:

```tsx
// before (line 24-28)
return buildMetadata({
  title: `${display} | ImStarGG`,
  description: `${display} — 트로피 ${formatNumber(dto.trophies)}, 멤버 ${dto.members.length}/30. 브롤스타즈 클럽 통계.`,
  path: `/clubs/${toUrlPath(tag)}`,
});

// after
return buildMetadata({
  title: `${display} | ImStarGG`,
  description: `${display} — 트로피 ${formatNumber(dto.trophies)}, 멤버 ${dto.members.length}/30. 브롤스타즈 클럽 통계.`,
  path: `/clubs/${toUrlPath(tag)}`,
  dynamicOg: true,
});
```

- [ ] **Step 2: typecheck**

```bash
cd imstargg-frontend
pnpm typecheck
```

Expected: 통과.

- [ ] **Step 3: Commit**

```bash
cd /Users/chodyoungrae/Projects/imstargg-v2
git add imstargg-frontend/app/clubs/\[tag\]/page.tsx
git commit -m "$(cat <<'EOF'
feat(frontend): Club 페이지의 generateMetadata 에 dynamicOg: true 추가

Player 와 동일 — 성공 분기에 dynamicOg=true 를 전달하여 file convention 을
picked up 하도록 한다. NotFound 분기는 정적 logo 유지.

Co-Authored-By: Claude Opus 4.7 (1M context) <noreply@anthropic.com>
EOF
)"
```

---

### Task 10: 통합 검증 (lint, typecheck, unit, build, dev 서버, HTML 메타)

**Files:** (변경 없음, 검증만)

- [ ] **Step 1: lint 통과**

```bash
cd imstargg-frontend
pnpm lint 2>&1 | tail -10
```

Expected: 새 파일에 추가 경고 없음. 기존 잔여 경고 (Trophy/ClubHeader 등 정적 자산 6 곳) 만 유지.

- [ ] **Step 2: typecheck 통과**

```bash
cd imstargg-frontend
pnpm typecheck
```

Expected: 에러 0.

- [ ] **Step 3: 단위 테스트 통과**

```bash
cd imstargg-frontend
pnpm test:run
```

Expected: 새 seo.test.ts 3 개 포함 전체 통과.

- [ ] **Step 4: prod 빌드 — file convention route 가 컴파일됨을 확인**

```bash
cd imstargg-frontend
pnpm build 2>&1 | tail -40
```

Expected:
- 빌드 에러 0.
- 출력에 `/players/[tag]/opengraph-image`, `/players/[tag]/twitter-image`, `/clubs/[tag]/opengraph-image`, `/clubs/[tag]/twitter-image` 라우트가 metadata route 로 리스팅된다.
- `process.cwd() + public/fonts/...` readFile 가 빌드 시 실행돼도 에러가 발생하지 않는다.

- [ ] **Step 5: 개발 서버 + PNG 응답 확인**

```bash
cd imstargg-frontend
pnpm dev &
DEV_PID=$!
# 5 초 대기 후 OG endpoint 4 개를 확인
sleep 8
for path in \
  "/players/88J8Q9RPV/opengraph-image" \
  "/players/88J8Q9RPV/twitter-image" \
  "/clubs/2U8CVLQQ9/opengraph-image" \
  "/clubs/2U8CVLQQ9/twitter-image"; do
  status=$(curl -s -o /tmp/og.bin -w "%{http_code} %{content_type} %{size_download}" "http://localhost:3000$path")
  echo "$path → $status"
done
kill $DEV_PID
```

Expected:
- 4 endpoint 모두 `200 image/png <bytes>` 응답 (10KB 이상).
- 콘솔에 KoBrawl Gothic / Lilita One readFile 에러가 없다.

> 만약 readFile 경로 이슈로 폰트 로드가 실패하면, `loadOgFonts.ts` 의 `process.cwd()` 가 monorepo 루트가 아닌 `imstargg-frontend` 디렉토리를 가리키는지 확인. 가리키지 않으면 절대 경로로 보강.

- [ ] **Step 6: HTML 메타 grep**

dev 서버를 띄운 상태에서 (Step 5 와 동일) Player/Club 페이지의 HTML 메타를 확인:

```bash
cd imstargg-frontend
pnpm dev &
DEV_PID=$!
sleep 8
echo "--- Player 메타 ---"
curl -s http://localhost:3000/players/88J8Q9RPV | grep -E 'og:image|twitter:image' | head -10
echo "--- Club 메타 ---"
curl -s http://localhost:3000/clubs/2U8CVLQQ9 | grep -E 'og:image|twitter:image' | head -10
kill $DEV_PID
```

Expected:
- Player HTML 에 `<meta property="og:image" content="...players/88J8Q9RPV/opengraph-image..."` 와 `<meta name="twitter:image" content="...players/88J8Q9RPV/twitter-image..."` 가 둘 다 노출.
- Club HTML 도 동일.
- `/assets/logo-full.png` 가 `og:image` / `twitter:image` 로 노출되지 않는다.

> 만약 logo-full.png 가 노출되면 `dynamicOg: true` 분기가 제대로 동작하지 않는 것. seo.test.ts 단위 테스트는 통과해도 실제 metadata pipeline 에서 hasOwnProperty 동작 차이가 있을 수 있음 — 디버그 우선순위.

- [ ] **Step 7: NotFound 분기 정적 logo 유지 확인 (선택)**

```bash
curl -s http://localhost:3000/players/INVALID_TAG_FOR_OG | grep -E 'og:image|twitter:image' | head -5
```

Expected: NotFound 분기에서는 정적 `/assets/logo-full.png` 가 `og:image` / `twitter:image` 로 노출. (NotFound 페이지는 `dynamicOg` 미사용.)

- [ ] **Step 8: 추가 commit 없음 — 검증만**

검증이 모두 그린이면 task 종료.

---

## 검증 체크리스트 (요약)

- [ ] `pnpm lint` 신규 경고 없음
- [ ] `pnpm typecheck` 통과
- [ ] `pnpm test:run` 통과 (seo.test.ts 3 개 신규 포함)
- [ ] `pnpm build` 통과, file convention route 4 개 컴파일됨
- [ ] dev 서버에서 4 개 OG endpoint 가 `200 image/png` 응답
- [ ] Player/Club 페이지 HTML 의 `<meta property="og:image">` 와 `<meta name="twitter:image">` 가 file convention endpoint 를 가리킴 (정적 logo 가 아님)
- [ ] git log 에 9 개 commit (task 1~9)

## 위험과 대응

- `process.cwd()` 가 monorepo 루트 (`imstargg-v2/`) 를 반환할 가능성 — Next.js 16 의 default 는 next 앱 디렉토리(`imstargg-frontend/`). 만약 다르면 `process.cwd() + "imstargg-frontend/public/fonts/..."` 또는 `import.meta.url` 기반 절대 경로로 보강. Step 4-5 의 빌드/dev 결과로 확인.
- satori 의 `display: "flex"` 강제 — OgCard 의 모든 컨테이너에 명시했지만, 후속 디자인 추가 시 누락하지 않도록 주의.
- 한국어 글리프 누락 — 일반적인 플레이어/클럽 이름은 KoBrawl Gothic 으로 정상. 한자/이모지는 깨질 수 있음 (Step 5 에서 한국어 케이스 직접 확인). 깨지면 fallback 으로 system 폰트 명시 후속.
- Next.js 메이저 업그레이드 시 `mergeStaticMetadata` 의 hasOwnProperty 분기가 바뀔 수 있음 — Step 6 의 메타 검증을 회귀 가드로 유지.

## Out-of-scope (이번 plan 제외)

- Brawler/Event/Home dynamic OG.
- 한국어 추가 디스플레이 폰트 (Pretendard Variable 등) self-host.
- 외부 SNS validator 자동화 테스트.
- BrawlerPortrait 통합 등 OG 무관 cleanup.
