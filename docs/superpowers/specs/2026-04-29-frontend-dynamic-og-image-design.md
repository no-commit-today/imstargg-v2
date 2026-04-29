# Dynamic OG Image — Frontend Design Spec

**작성일**: 2026-04-29
**상태**: Draft → 승인 대기

## 목적

Player / Club 페이지의 SNS 공유 카드를 정적 logo 에서 동적 1200×630 PNG 로 전환한다. Next.js 16 의 `opengraph-image.tsx` 파일 컨벤션과 `next/og` 의 `ImageResponse` 를 사용하고, DESIGN.md 의 다크 hero + 옐로우 액센트 + KoBrawl Gothic 톤을 그대로 살려 브랜드 일관성을 유지한다.

## 현재 상태

- `app/page.tsx`, `app/brawlers/[id]/page.tsx`, `app/players/[tag]/page.tsx`, `app/clubs/[tag]/page.tsx` 등이 `buildMetadata({ image? })` 를 호출.
- `src/lib/seo.ts` 의 `buildMetadata` 는 `image` 가 미지정일 때 항상 `defaultOgImage = "/assets/logo-full.png"` 를 `openGraph.images` 와 `twitter.images` 둘 다에 박는다.
- Player / Club 페이지는 `image` 미지정 → 모든 공유 카드가 동일 logo.
- Brawler 페이지는 `image: brawler.portraitImageUrl` 을 넘기지만 비율(예: 정사각/세로) 이 1200×630 과 맞지 않아 SNS 가 잘라낸다 — 본 spec 의 out-of-scope.
- `public/fonts/KoBrawl_Gothic40.otf`, `lilitaone-regular-webfont__2_.ttf` 가 이미 self-hosted.

## Goals (성공 기준)

1. `app/players/[tag]/opengraph-image.tsx` 가 `params.tag` 에 대해 `playerApi.get` 결과를 사용한 1200×630 PNG 를 반환한다.
2. `app/players/[tag]/twitter-image.tsx` 가 같은 ImageResponse 빌더(`src/lib/og/playerOgImage.tsx`)를 재사용해 동일 결과를 반환한다.
3. `app/clubs/[tag]/opengraph-image.tsx` / `twitter-image.tsx` 가 동일 패턴으로 클럽 카드를 반환한다.
4. 각 디렉토리에 `opengraph-image.alt.txt` 가 있어 `og:image:alt` 메타와 `twitter:image:alt` 메타가 모두 자동 생성된다.
5. `buildMetadata` 에 `dynamicOg?: boolean` 옵션이 추가되어 `true` 면 `openGraph.images` / `twitter.images` 가 모두 omit 된다 (file convention 자동 적용).
6. Player / Club `generateMetadata` 가 `dynamicOg: true` 를 전달.
7. Player/Club 페이지의 응답 HTML 에 `<meta property="og:image">` 와 `<meta name="twitter:image">` 가 둘 다 생성된다 (file convention endpoint 를 가리킴).
8. 기존 `app/page.tsx`, `app/brawlers/[id]/page.tsx` 의 OG 동작은 변경되지 않는다 (옵션 미사용 → 정적 logo / brawler portrait 그대로).
9. `pnpm typecheck` / `pnpm lint` / `pnpm build` 가 통과한다.

## Non-goals

- Home / 리스트 / Brawler / Event 의 동적 OG (정적 logo 또는 portraitImageUrl 그대로).
- 한국어 추가 디스플레이 폰트 (Pretendard Variable 등) self-host. 이번 spec 은 KoBrawl Gothic + Lilita One + system sans-serif 만 사용.
- 색약/접근성 컨트라스트 추가 검증.
- 외부 SNS validator 자동화 테스트.

## 설계

### 파일 구조

```
imstargg-frontend/
  app/
    players/[tag]/
      opengraph-image.tsx          # 신규: 얇은 wrapper, playerOgImage() 호출
      twitter-image.tsx            # 신규: opengraph-image 와 동일 — re-export
      opengraph-image.alt.txt      # 신규: og + twitter alt 둘 다 사용
    clubs/[tag]/
      opengraph-image.tsx          # 신규
      twitter-image.tsx            # 신규
      opengraph-image.alt.txt      # 신규
  src/
    lib/
      seo.ts                       # buildMetadata 에 dynamicOg 옵션 추가
      og/
        loadOgFonts.ts             # 신규: KoBrawl/Lilita 폰트 readFile 헬퍼
        OgCard.tsx                 # 신규: Player/Club 공통 다크 카드 레이아웃
        playerOgImage.tsx          # 신규: Player ImageResponse 빌더 (opengraph + twitter 공유)
        clubOgImage.tsx            # 신규: Club ImageResponse 빌더 (opengraph + twitter 공유)
```

`OgCard.tsx` 는 ImageResponse 의 JSX 노드를 반환하는 단순 함수 컴포넌트. `playerOgImage.tsx` / `clubOgImage.tsx` 는 fetch + OgCard + ImageResponse 조립까지 책임. file convention 파일(`opengraph-image.tsx`, `twitter-image.tsx`) 은 위 빌더와 메타 export 만 re-export 하는 얇은 wrapper.

이 구조의 핵심: Twitter 카드가 opengraph 카드와 100% 동일한 PNG 를 받으므로 디자인이 분기되지 않고 SNS 별로 메타가 누락되지 않는다.

### `buildMetadata` 옵션 변경

```ts
type Input = {
  title: string;
  description: string;
  path: string;
  image?: string;
  noindex?: boolean;
  dynamicOg?: boolean;   // 신규
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

- `dynamicOg: false` (기본) → 기존 동작과 동일.
- `dynamicOg: true` → `openGraph.images` / `twitter.images` 미지정. Next.js 가 같은 디렉토리의 `opengraph-image.*` file convention 을 자동 picked up.

### 카드 디자인 (1200×630)

브랜드 토큰 (`DESIGN.md`):
- 배경: `#14161B` (`--bg-hero`)
- 액센트: `#FFC60A` (`--brand-yellow`)
- 보조 텍스트: `rgba(255,255,255,0.6)` (white-muted)
- 본문 화이트: `#FFFFFF`
- 폰트: `KoBrawl Gothic` (한글 디스플레이), `Lilita One` (영문 디스플레이)
- 시그니처: 하단 4px 옐로우 stripe (TopNav 의 옐로우 보더 와 동일 모티브)

**Player 레이아웃**:

```
┌────────────────────────────────────────┐ 1200×630
│  IMSTARGG                              │  ← 좌상 32px Lilita One, 옐로우, letter-spacing
│                                        │
│                                        │
│  쉘리                                   │  ← 96px KoBrawl Gothic, 화이트
│  #88J8Q9RPV                            │  ← 32px monospace, 옐로우
│                                        │
│  🏆 12,345    ·    🛡 SJCE              │  ← 36px, white-muted + 화이트
│                                        │
│ ████████████████████████████████ ◀──── │  ← 4px 옐로우 stripe
└────────────────────────────────────────┘
```

- 트로피 아이콘: `/public/assets/trophy.png` 의 자체 호스팅 자산을 base64 / readFile 로 인라인. (단순화: emoji 없이 "트로피" 라벨 텍스트로 시작 — 1차 PR. 이미지 인라인은 후속.)
- 클럽명 없으면 "클럽 없음" 으로 표시.

**Club 레이아웃**: 동일 톤, 텍스트 4 줄 변경 — 클럽명 / 태그 / 트로피 + 멤버수.

### `lib/og/playerOgImage.tsx` (공유 빌더)

```tsx
import { ImageResponse } from "next/og";
import { playerApi } from "@/api/endpoints/player";
import { NotFoundError } from "@/api/errors";
import { loadOgFonts } from "@/lib/og/loadOgFonts";
import { OgCard } from "@/lib/og/OgCard";
import { formatNumber } from "@/lib/format";

export const playerOgAlt = "ImStarGG 플레이어 카드";
export const playerOgSize = { width: 1200, height: 630 } as const;
export const playerOgContentType = "image/png";

export async function playerOgImage(tag: string) {
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
        <OgCard eyebrow="PLAYER" name="플레이어를 찾을 수 없음" tag={tag} stats={[]} />,
        { ...playerOgSize, fonts },
      );
    }
    throw e;
  }
}
```

`clubOgImage.tsx` 도 동일 패턴 — `clubApi.get` + `eyebrow="CLUB"` + stats `[{label:"트로피"...}, {label:"멤버", value:`${members.length}/30`}]`.

### `app/players/[tag]/opengraph-image.tsx` (얇은 wrapper)

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

### `app/players/[tag]/twitter-image.tsx` (opengraph 동일)

```tsx
export {
  default,
  alt,
  size,
  contentType,
} from "./opengraph-image";
```

> Next.js 16 의 file convention 은 `twitter-image` 도 같은 함수 시그니처를 받는다. opengraph-image.tsx 의 default + 메타 export 를 그대로 re-export 하면 두 metadata 가 동일한 PNG 를 가리키며, 별도 fetch 라도 ISR + dedupe 로 비용 최소화.

Club 도 동일 — `clubs/[tag]/opengraph-image.tsx` 와 `twitter-image.tsx` 한 쌍.

### `loadOgFonts.ts`

```ts
import { readFile } from "node:fs/promises";
import { join } from "node:path";

export async function loadOgFonts() {
  const [koBrawl, lilita] = await Promise.all([
    readFile(join(process.cwd(), "public/fonts/KoBrawl_Gothic40.otf")),
    readFile(join(process.cwd(), "public/fonts/lilitaone-regular-webfont__2_.ttf")),
  ]);
  return [
    { name: "KoBrawl Gothic", data: koBrawl, style: "normal" as const, weight: 400 as const },
    { name: "Lilita One", data: lilita, style: "normal" as const, weight: 400 as const },
  ];
}
```

### `OgCard.tsx`

```tsx
type Stat = { label: string; value: string };
interface Props {
  eyebrow: string;       // "PLAYER" | "CLUB"
  name: string;          // 메인 타이틀
  tag: string;           // 보조 타이틀
  stats: Stat[];         // 0-3 개
}

export function OgCard({ eyebrow, name, tag, stats }: Props) {
  return (
    <div style={{
      width: "100%", height: "100%",
      display: "flex", flexDirection: "column",
      background: "#14161B",
      padding: "64px 80px",
      position: "relative",
      fontFamily: "KoBrawl Gothic",
    }}>
      <div style={{
        fontFamily: "Lilita One",
        fontSize: 32,
        color: "#FFC60A",
        letterSpacing: "0.08em",
      }}>
        IMSTARGG · {eyebrow}
      </div>

      <div style={{ flex: 1, display: "flex", flexDirection: "column", justifyContent: "center" }}>
        <div style={{ fontSize: 96, color: "#FFFFFF", lineHeight: 1.1 }}>
          {name}
        </div>
        <div style={{
          marginTop: 12,
          fontFamily: "monospace",
          fontSize: 32,
          color: "#FFC60A",
        }}>
          {tag}
        </div>
        {stats.length > 0 && (
          <div style={{
            marginTop: 36,
            display: "flex",
            gap: 32,
            fontSize: 36,
            color: "rgba(255,255,255,0.85)",
          }}>
            {stats.map((s, i) => (
              <div key={i} style={{ display: "flex", gap: 12 }}>
                <span style={{ color: "rgba(255,255,255,0.5)" }}>{s.label}</span>
                <span>{s.value}</span>
              </div>
            ))}
          </div>
        )}
      </div>

      <div style={{
        position: "absolute",
        left: 0, right: 0, bottom: 0, height: 8,
        background: "#FFC60A",
      }} />
    </div>
  );
}
```

> `next/og` 의 `ImageResponse` 는 satori 기반 — `display: "flex"` 가 필수, 모든 자식 div 도 동일.

### Player / Club generateMetadata 변경

```diff
 return buildMetadata({
   title: `${display} | ImStarGG`,
   description: `${display} — 트로피 ${formatNumber(dto.trophies)}, ${dto.club?.name ?? "클럽 없음"}. 브롤스타즈 플레이어 통계.`,
   path: `/players/${toUrlPath(tag)}`,
+  dynamicOg: true,
 });
```

Club 도 동일 한 줄 추가. NotFound 분기는 dynamicOg 미사용 (정적 logo 유지).

## 테스팅 전략

### 단위 테스트
- `buildMetadata` 의 `dynamicOg` 분기에 대한 Vitest 테스트 추가 (`src/lib/seo.ts` 단위 테스트):
  - `dynamicOg: false` → `openGraph.images` 와 `twitter.images` 가 존재.
  - `dynamicOg: true` → 둘 다 undefined.
- `OgCard` 는 ImageResponse JSX 출력이라 별도 단위 테스트는 만들지 않는다 (시각 회귀는 빌드 + 수동 검증).

### 빌드/타입 검증
- `pnpm typecheck` — 통과.
- `pnpm lint` — 새 파일에 ESLint 경고 0.
- `pnpm build` — file convention 파일이 컴파일되고 메타 endpoint 가 생성되는지 확인.

### 수동 검증 (사용자 또는 후속)
- `pnpm dev` 후 `/players/88J8Q9RPV/opengraph-image` 와 `/players/88J8Q9RPV/twitter-image`, `/clubs/2U8CVLQQ9/opengraph-image` 와 `/clubs/2U8CVLQQ9/twitter-image` 모두 1200×630 PNG 200 응답.
- 동일 페이지 HTML 의 `<meta property="og:image">` 와 `<meta name="twitter:image">` 둘 다 file convention endpoint 를 가리킨다 (curl + grep 으로 확인).
- 한국어 이름 (예: "쉘리") 이 깨지지 않고 KoBrawl Gothic 으로 렌더된다.
- prod 배포 후 `https://www.opengraph.xyz/` 같은 외부 검증 도구로 페이스북/트위터 카드 미리보기 확인 (사용자 영역).

### prod E2E 영향
- 기존 spec (`statistics.spec.ts`, `player-battles.spec.ts`) 는 og 메타를 검증하지 않으므로 회귀 영향 없음.
- 신규 E2E 는 만들지 않는다 (PNG 비교 비용 大).

## 위험과 대응

| 위험 | 대응 |
|---|---|
| `next/og` 의 satori 가 `KoBrawl Gothic` 의 일부 한자/특수 글리프를 못 그릴 수 있다 | KoBrawl Gothic 은 한글 위주 — 일반적인 플레이어/클럽 이름은 정상. 빌드 후 한국어/영문/이모지 케이스를 직접 fetch 해서 확인. 깨지면 fallback 으로 system 폰트 명시. |
| `process.cwd()` 기준 폰트 경로가 prod 빌드에서 상대화돼 깨질 수 있다 | Next.js 공식 예시와 동일 패턴 (`process.cwd() + public/...`). next.js docs 가 이를 권장. |
| satori 가 `display: "flex"` 외 layout 을 거부한다 | OgCard 의 모든 컨테이너에 `display: flex` 를 명시. |
| Twitter card 가 file convention og:image 를 자동 사용하지 않을 수 있다 | `twitter-image.tsx` 를 같은 디렉토리에 두고 `opengraph-image.tsx` 를 re-export 하여 `<meta name="twitter:image">` 도 자동 생성. 빌드 후 HTML 메타로 확인. |
| Player/Club 이름이 매우 길거나 이모지 포함 시 카드 overflow | OgCard 에 `whiteSpace: "nowrap"; overflow: "hidden"; textOverflow: "ellipsis"` 추가 — 단, satori 의 ellipsis 지원은 제한적이므로 후속 보강. 1차에서는 자연 줄바꿈만. |

## 작업 산출물

이 spec 승인 후 별도 plan 문서 (`docs/superpowers/plans/2026-04-29-frontend-dynamic-og-image.md`) 에서 TDD 단위로 분해. 단위 테스트는 `buildMetadata.dynamicOg` 분기 1 종, 시각 검증은 빌드 + 수동.
