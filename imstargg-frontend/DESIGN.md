# Design System — ImStarGG

## 1. Visual Theme & Atmosphere

ImStarGG는 브롤스타즈 전적 통계 서비스로, 스티커처럼 두툼하고 게임 느낌이 살아 있는 비주얼 위에 op.gg 같은 **데이터 밀도 높은 정보 레이아웃**을 얹은 하이브리드 시스템이다. 표면은 따뜻한 오프-화이트(`#F6F7FA`)로 바탕을 깔고, 카드는 순백(`#FFFFFF`)에 얇은 회색 보더(`#D8DCE5`)를 둘러 정보가 겹겹이 쌓이도록 설계한다. 그 위에서 브랜드 옐로우(`#FFC60A`)와 잉크 블랙(`#14161B`)이 만나 브롤스타즈 특유의 "스티커 / 캐릭터 굿즈" 정서를 만들어낸다.

타이포그래피는 4-패밀리 시스템으로 역할이 명확하게 갈린다. **KoBrawl Gothic**은 한국어 디스플레이 — 두툼하고 둥근 게임 폰트로 로고·페이지 타이틀에 쓰이며, 영문 디스플레이는 같은 결의 **Lilita One**으로 페어링한다. 본문은 한·영 모두 깔끔하게 받쳐주는 **Pretendard Variable**, 숫자는 데이터 가독성을 위해 **Bricolage Grotesque**의 tabular-nums를 쓴다. 즉 "캐릭터스러운 헤더 / 깔끔한 본문 / 스코어보드 같은 숫자"가 한 화면에서 공존한다.

ImStarGG의 시그니처 모션은 "스티커 그림자(`0 3px 0 0 rgba(0,0,0,0.35)`)" — 흩날리는 블러 그림자 대신 단단한 오프셋 그림자로 카드·뱃지를 종이 스티커처럼 띄워 올린다. 동시에 인터랙션 전반의 곡선은 `cubic-bezier(0.34, 1.56, 0.64, 1)` 오버슈트 이징(`--ease-pop`)을 기본으로 둬서 게임 UI 같은 "통통 튀는" 인상을 만든다. 라운드는 작은 칩 4px부터 풀 필 999px까지 폭넓게 운용하되, 카드 12px / 큰 패널 18–24px이 디폴트다.

**Key Characteristics:**
- 라이트 베이스 (`#F6F7FA` / `#FFFFFF`) + 다크 헤더(`#14161B`) — 데이터 사이트 친화 ↔ 게임 헤더의 대비
- 브랜드 옐로우(`#FFC60A`)는 액션·활성 상태 전용, 장식적 사용 금지
- 4-패밀리 타입 시스템(KoBrawl / Lilita / Pretendard / Bricolage) — 디스플레이 / 본문 / 숫자 역할 분리
- 스티커 그림자(`0 3px 0` 오프셋) — 시그니처 깊이 표현
- `--ease-pop` 오버슈트 이징을 인터랙션 디폴트로
- 클래스·랭크·희귀도마다 채도 높은 게임 컬러 매핑(레드/퍼플/그린/시안 등) — UI 액센트가 아니라 **데이터 카테고리**로 기능
- 1,280px 최대 컨테이너 + 8px 베이스 스페이싱 — op.gg 같은 정보 밀도

## 2. Color Palette & Roles

### Primary Brand
- **Brand Yellow** (`#FFC60A`): 로고 별, 1차 CTA, 활성 탭, 포커스 보더
- **Brand Yellow Deep** (`#E8A900`): 프레스 / 하단 그림자
- **Brand Yellow Soft** (`#FFE082`): 옐로우 틴트 배경
- **Brand Black** (`#14161B`): 톱 네비, 인버스 표면, 두꺼운 보더
- **Brand Ink** (`#0B0C10`): 가장 깊은 다크 면

### Surface & Text (Light theme — default)
- **bg** (`#F6F7FA`): 페이지 베이스
- **bg-elev-1 / 2** (`#FFFFFF`): 카드, 다이얼로그
- **bg-hero** (`#14161B`): 히어로 / 헤더 — 항상 다크
- **fg** (`#14161B`): 1차 텍스트
- **fg-muted** (`#5C6271`): 보조 텍스트
- **fg-subtle** (`#8A90A0`): 캡션, 비활성
- **fg-inverse** (`#FFFFFF`): 다크 면 위 텍스트
- **border** (`#D8DCE5`): 카드 보더
- **border-strong** (`#14161B`): 청키 스티커 아웃라인
- **border-focus** (`#FFC60A`): 포커스 링

### Semantic
- **success** (`#2BD576`): 승리(WIN), 성공
- **danger** (`#FF3A3A`): 패배(LOSS), 에러
- **warning** (`#FF8A1F`): 경고
- **info** (`#2E7CF6`): 안내, 링크
- **draw** (`#5C6271`): 무승부

### 게임 데이터 컬러 (데이터 카테고리 매핑)
- **Class**: damage `#FF3A3A` · tank `#9146FF` · marksman `#FF8A1F` · assassin `#FF5DAA` · support `#2BD576` · controller `#3DE0FF` · artillery `#FFC60A`
- **Rank Tier**: bronze `#C77545` · silver `#9FAFC5` · gold `#F2B93B` · diamond `#58D7E8` · legendary `#B83BFF` · masters `#FF3A3A` · pro `#FFD447`
- **Rarity**: common `#B4B9C7` · rare `#2BD576` · super-rare `#3DE0FF` · epic `#9146FF` · mythic `#FF3A3A` · legendary `#FFC60A`

### Neutrals (50→950)
`#F6F7FA` → `#ECEEF3` → `#D8DCE5` → `#B4B9C7` → `#8A90A0` → `#5C6271` → `#3B404D` → `#2A2E38` → `#1E2128` → `#14161B` → `#0B0C10`

### Shadows
- **sticker** (`0 3px 0 0 rgba(0,0,0,0.35)`): 칩·뱃지·작은 카드 — 시그니처
- **sticker-lg** (`0 6px 0 0 rgba(0,0,0,0.35)`): 히어로 카드
- **card** (`0 1px 2px / 0 4px 12px`): 표준 카드
- **card-lg** (`0 2px 4px / 0 12px 32px`): 모달, 부유 패널
- **glow-yellow** (`0 0 0 4px rgba(255,198,10,0.35)`): 옐로우 포커스 링
- **inset-hi / inset-lo**: 버튼 하이라이트·프레스 디테일

## 3. Typography Rules

### Font Families
- **Display (KO)**: `KoBrawl Gothic` — 한국어 디스플레이, 두툼한 게임 글꼴
- **Display (EN)**: `Lilita One` — 영문 디스플레이 페어
- **Body**: `Pretendard Variable` — 한·영 본문
- **Numeric**: `Bricolage Grotesque` (tabular-nums) — 트로피, 승률, 랭킹 등 모든 수치
- **Mono**: `JetBrains Mono` — 플레이어 태그, 코드

### Hierarchy

| Role | Family | Size | Weight | Line | Tracking | Notes |
|---|---|---|---|---|---|---|
| t-display | display | 84px | 400 | 1.1 | 0.04em | uppercase, 히어로 |
| t-h1 | title | 60px | 400 | 1.1 | -0.02em | 페이지 타이틀 |
| t-h2 | title | 44px | 400 | 1.25 | -0.02em | 섹션 헤더 |
| t-h3 | title | 32px | 400 | 1.25 | normal | 하위 섹션 |
| t-h4 | body | 24px | 700 | 1.25 | -0.02em | 카드 타이틀 (강조) |
| t-h5 | body | 20px | 700 | 1.25 | normal | 카드 타이틀 |
| t-body | body | 16px | 400 | 1.5 | normal | 표준 본문 |
| t-body-sm | body | 14px | 400 | 1.5 | normal | 보조 본문 |
| t-caption | body | 12px | 500 | 1.25 | normal | 캡션 |
| t-overline | body | 12px | 700 | 1 | 0.12em | UPPERCASE 라벨 |
| t-num | num | inherit | 700 | — | -0.02em | tabular-nums |
| t-num-xl | num | 44px | 800 | 1 | -0.02em | 큰 스코어 |
| t-mono | mono | 14px | 400 | — | — | 태그 |

### 원칙
- **역할별 폰트 분리가 핵심**: 디스플레이는 두툼하게(KoBrawl/Lilita), 본문은 깨끗하게(Pretendard), 숫자는 정렬되게(Bricolage tabular). 한 화면 안에서 세 결의 폰트가 공존하는 것이 ImStarGG다움.
- **숫자는 항상 t-num**: 트로피·승률·픽률·랭킹은 예외 없이 Bricolage tabular-nums. 표 안에서 자릿수가 정렬되어야 op.gg 같은 인상이 산다.
- **한글 친화 스케일**: 본문은 16px, 보조 14px가 디폴트. 12px 미만은 메타데이터·뱃지에 한정.
- **uppercase는 영문/숫자에만**: t-overline, 클래스 라벨 등에서 사용. 한국어는 절대 uppercase 처리하지 않는다.

## 4. Component Stylings

### Buttons
- **`.btn` (기본)**: `#ECEEF3` 배경, 1px `#D8DCE5` 보더, 8px radius, 13px / 700, 8×14px 패딩
- **`.btn-primary`**: 옐로우 배경(`#FFC60A`), 잉크 텍스트, 하단 `box-shadow: 0 1px 0 #E8A900`로 두툼한 키캡 느낌
- **`.btn:disabled`**: opacity 0.55, 그림자 제거
- 모든 버튼 hover는 한 단계 어두워지는 식 — 색조는 유지

### Pills / Chips (`.pill`)
- 11px / 700 / uppercase, 4px radius, 3×8px 패딩
- 변형: `yellow`(브랜드 옐로우 솔리드), `green`(WIN), `red`(LOSS), `blue`(info), `dark`(인버스)
- 결과 표시(WIN/LOSS/DRAW), 카테고리 태그, 작은 메타에 사용

### Cards (`.card`)
- `#FFFFFF` 배경, 1px `#D8DCE5` 보더, 12px radius
- `.card-head`: 14×18 패딩, 하단 1px 디바이더, 좌측 타이틀 + 우측 액션
- `.card-title`: 15px / 700 / -0.01em
- `.card-body`: 16×18 패딩
- 호버 시 미세한 lift만, 큰 그림자는 모달·드롭다운에 한정

### Tabs — 두 가지 구현 공존
- **커스텀 `.tabs` / `.tab`** (랭킹·이벤트 상세 등 데이터 밀도 높은 페이지): 하단 1px 보더 라인, 액티브 탭만 옐로우 2px 언더라인. 14px / 600, 비액티브 `--fg-muted` / 액티브 `--fg`. 모바일에선 가로 스크롤 + flex-wrap-nowrap.
- **shadcn `<Tabs>`** (플레이어 상세 등 인터랙티브 폼 페이지): `src/components/ui/tabs.tsx`. Radix 기반으로 키보드 네비·ARIA 자동.
- 선택 가이드: 표/리스트 헤더처럼 디자인 시그니처가 강한 곳은 커스텀 `.tabs`, 일반 인터랙티브 영역은 shadcn `<Tabs>`.

### TopNav (`.topnav`)
- 다크(`#14161B`) 풀 너비, 하단 3px 옐로우 보더
- 60px 높이, 1,280px max-width 컨테이너
- 로고(좌) — 메뉴 링크 — flex-spacer — 검색(`#PLAYERTAG` 입력) (우)
- 검색 인풋: 반투명 화이트 배경(`rgba(255,255,255,0.08)`), 포커스 시 옐로우 보더 + 한 단계 밝아짐
- 검색 드롭다운(최근 검색): 280px, 흰 카드, `--shadow-card-lg` 변종

### Mobile TabBar (`.mobile-tabbar`)
- 모바일에서만 등장(<640px), 60px 높이, 다크 배경 + 상단 2px 옐로우 보더
- 5개 탭(홈/브롤러/이벤트/랭킹/클럽), 아이콘 18px + 라벨 10px / 700 / 0.04em
- 액티브 탭은 옐로우 텍스트

### RenewalButton — 시그니처 컴포넌트
플레이어 페이지의 핵심 액션. 백엔드 갱신 상태에 따라 활성/비활성과 라벨이 바뀌는 단순 버튼.
- **활성**: 옐로우 1차 버튼 + ⟳ 아이콘, "갱신"
- **갱신 중**: 비활성 + ⟳ 회전 + "갱신 중…", 백그라운드 폴링이 끝날 때까지 유지
- **쿨다운/불가**: 자동 비활성
- **완료/실패 피드백**: 토스트로 통보 (직접 색을 바꾸지 않는다)

> 클럽 페이지에서도 동일한 시각/위치를 공유하지만, 클럽 갱신 동작은 도메인 spec 에서 구현 예정 — 현재는 항상 비활성 상태의 placeholder.

### Toast
- 라이트·다크 모드 자동 전환되는 풀 컬러 토스트(`richColors`) 사용
- 위치는 화면 상단 중앙, `--ease-pop` 등으로 자연스러운 슬라이드인
- 종류: `info`(요청 접수), `success`(완료), `error`(실패), `warning`. 결과 색은 토큰 기반

### Skeleton (`.skel`)
- 200% 폭의 그라디언트 시머, 1.5s 무한 — `#ECEEF3 → #D8DCE5 → #ECEEF3`

### 게임 도메인 전용 컴포넌트
- **BrawlerPortrait**: 사각형 라운드 8px, 두 컬러 그라디언트(브롤러별 c1→c2) + 첫 글자만. (이미지 fallback 없음 — `portraitImageUrl` 을 직접 보여주는 곳은 `<Image unoptimized>` 로 별도 처리.) `showFrame` 시 희귀도 컬러 인셋 보더(`box-shadow: inset 0 0 0 2px <rarity>`)
- **ClassIcon**: 18px 원형, 클래스 컬러 채움 + 화이트 SVG 글리프
- **Trophy**: 트로피 PNG + 숫자(`var(--font-num)` / 700) 인라인
- **PowerPill**: P11 = 옐로우→오렌지 그라디언트, 그 외 다크 그레이. 11px / 800 / num
- **ResultPill**: WIN(green) / LOSS(red) / DRAW(neutral), 쇼다운은 `#1`–`#10` 랭크 칩

### Result Bar (`.result-bar`)
- 4px 폭 세로 바, 행 좌측에 붙어 카드 안의 결과를 한눈에. win=`#2BD576`, loss=`#FF3A3A`, draw=`#8A90A0`

## 5. Layout Principles

### Spacing
- **Base unit**: 8px
- **Scale**: `0 / 4 / 8 / 12 / 16 / 20 / 24 / 32 / 40 / 48 / 64 / 80`
- 카드 내부는 16–18px, 카드 사이는 12–16px, 섹션 사이는 24–32px

### Grid
- **Max container**: 1,280px (`.main`, `.topnav-inner`)
- **Page padding**: desktop 24px / tablet 16px / mobile 12px
- 플레이어 페이지: `aside 320px` + 콘텐츠 1fr (모바일에서 스택)
- 브롤러 그리드: 7–8 col(role 탭) → 5 → 3 → 2 → 1
- 표 레이아웃은 모두 inline-style grid-template-columns로 정의 후, `responsive.css`에서 `[style*=...]` 셀렉터로 모바일 변환

### Whitespace
- **데이터 페이지의 밀도**: op.gg 트랙처럼 행 패딩 8–10px / 14–18px로 타이트하게. 휴식 공간은 카드 사이 거터로만.
- **히어로의 여백**: 플레이어 / 브롤러 / 이벤트 디테일 상단 카드는 28×24px 큰 패딩으로 호흡.

### Border Radius Scale
- `--radius-xs` 4px — 칩, 작은 뱃지
- `--radius-sm` 8px — 인풋, 작은 컨트롤, 브롤러 포트레잇
- `--radius-md` 12px — **카드 디폴트**
- `--radius-lg` 18px — 큰 패널
- `--radius-xl` 24px — 다이얼로그
- `--radius-2xl` 32px — 히어로 블록
- `--radius-pill` 999px — 풀 필 버튼·검색 인풋 변종

## 6. Depth & Elevation

| Level | 처리 | 사용처 |
|---|---|---|
| 0 (Base) | `#F6F7FA` 페이지 배경 | 페이지 베이스 |
| 1 (Surface) | `#FFFFFF` + 1px `#D8DCE5` | 표준 카드 |
| 2 (Sticker) | `0 3px 0 rgba(0,0,0,0.35)` | 칩·뱃지·풀 필 — **시그니처** |
| 3 (Card) | `0 4px 12px rgba(20,22,27,0.08)` + `0 1px 2px` | 호버 / 떠있는 카드 |
| 4 (Card-lg) | `0 12px 32px rgba(20,22,27,0.12)` | 드롭다운, 부유 패널 |
| 5 (Modal) | `0 8px 24px rgba(0,0,0,0.5)` | 모달, 토스트 |
| Hero | `bg-hero: #14161B` + 하단 3px 옐로우 보더 | 톱 네비 |
| Focus Ring | `0 0 0 4px rgba(255,198,10,0.35)` | 포커스 가능 요소 |

**그림자 철학**: 두 가지 그림자 언어를 의도적으로 공존시킨다.
1. **스티커 그림자** (오프셋만, 블러 0) — 게임 굿즈처럼 통통 튀는 작은 요소.
2. **소프트 그림자** (블러 큰 표준 그림자) — 데이터 카드, 모달 등 진지한 면.

작은 요소는 1번, 정보를 담는 큰 카드는 2번으로 — 결을 섞지 않는다.

## 7. Do's and Don'ts

### Do
- 옐로우(`#FFC60A`)는 액션·활성·포커스에만 — 데이터 카드 배경에 절대 쓰지 않는다
- 숫자는 무조건 `var(--font-num)` + tabular-nums — 정렬이 op.gg 다움의 핵심
- WIN/LOSS는 색뿐 아니라 행 좌측 4px `result-bar`로도 표시 — 색약 사용자 배려
- 클래스·랭크·희귀도 컬러는 정해진 매핑을 따른다 — 새로 만들지 않는다
- 한국어는 KoBrawl Gothic, 영문/숫자는 Lilita One/Bricolage — 페어링을 깨지 않는다
- 카드 호버는 미세한 lift만, 강한 그림자는 모달 전용
- 모바일에선 표를 그리드 행 단위로 압축하고 부수 컬럼을 `nth-child` 숨김 — 가로 스크롤 표 금지
- 모든 인터랙션의 디폴트 이징은 `--ease-pop` — 게임스러운 통통함 유지

### Don't
- ❌ 옐로우를 큰 면적에 칠하지 마라 — 톱 네비 보더 3px / 버튼 / 액티브 인디케이터 외엔 쓰지 않는다
- ❌ 한국어를 uppercase 처리하지 마라 — overline·라벨은 영문/숫자 한정
- ❌ 시스템 폰트 fallback에 의존하지 마라 — KoBrawl과 Pretendard 로딩이 ImStarGG의 뼈대다
- ❌ 클래스 컬러를 카테고리 외 용도로 쓰지 마라 — purple은 항상 tank/epic, red는 damage/mythic/loss다
- ❌ 카드에 그라디언트를 깔지 마라 — 그라디언트는 BrawlerPortrait·PowerPill의 P11 등 캐릭터 표현 전용
- ❌ 그림자 반경을 키우지 마라 — 스티커 그림자의 핵심은 블러 0의 단단함
- ❌ 1,280px max-width를 깨지 마라 — 모든 페이지가 같은 컨테이너 위에 산다

## 8. Responsive Behavior

### Breakpoints
| Name | Width | 변화 |
|---|---|---|
| Desktop | ≥ 960px | 기본 레이아웃, sidebar + main |
| Tablet | 640–959px | 사이드바·히어로 그리드를 1열로 스택, 표 컬럼 일부 축소 |
| Mobile | < 640px | TopNav 링크 숨김 + 하단 탭바 노출, 표 → 압축 그리드 |
| Mini | < 380px | 로고 축소, 3-col 그리드 → 2-col |

### Collapsing Strategy
- **TopNav**: 데스크탑은 로고 + 메뉴 4개 + 검색. 모바일은 로고 + 검색만, 메뉴는 하단 `mobile-tabbar`로 이동
- **PlayerPage**: `aside 320px + 1fr` → 모바일에서 column 스택
- **표(랭킹·플레이어 브롤러·메타·멤버)**: 데스크탑 5–8 컬럼 → 모바일 3–4 컬럼으로 압축, 부수 컬럼은 `nth-child` 숨김
- **카드 그리드**: `repeat(3, 1fr)` / `repeat(4, 1fr)` → 모바일 `repeat(2, 1fr)`. 5-col 쇼다운만 유지(컴팩트 갭)
- **히어로 블록**: 260/180px 포트레잇 + 메타 → 1열 스택, 좌우 보더는 하단 보더로 변환

## 9. Agent Prompt Guide

### Quick Reference
- 페이지 배경: `#F6F7FA`
- 카드: `#FFFFFF` + 1px `#D8DCE5` + radius 12px
- 1차 텍스트: `#14161B`, 보조: `#5C6271`
- 액센트: `#FFC60A` (옐로우)
- 다크 헤더: `#14161B` + 하단 3px 옐로우
- 본문: Pretendard 16/400, 숫자: Bricolage 700 tabular-nums

### Example Prompts
- "카드 만들어줘: `#FFFFFF` 배경, 12px radius, 1px `#D8DCE5` 보더. head는 14×18 패딩에 하단 1px 디바이더. 타이틀 15px/700, body 16×18 패딩."
- "WIN 칩: `.pill green`, 11px/700/uppercase, `rgba(43,213,118,0.15)` 배경, `#1a9550` 텍스트, 4px radius."
- "갱신 버튼: 옐로우 1차 버튼, 잉크 텍스트, 하단 `0 1px 0 #E8A900` 그림자로 키캡 느낌. 갱신 중에는 비활성 + ⟳ 회전 + '갱신 중…', 결과는 토스트로."
- "TopNav: 다크 `#14161B`, 60px 높이, 1,280px 컨테이너, 하단 3px 옐로우 보더. 로고+링크4+검색(#PLAYERTAG)."
- "랭킹 표 행: grid-template-columns로 60/60/1fr/1fr/120, 모바일에선 40/1fr/auto + nth-child로 컬럼 숨김. 좌측 4px result-bar."

### Iteration Guide
1. 페이지는 항상 `#F6F7FA` 위 흰 카드 — op.gg 같은 정보 표면
2. 헤더만 다크(`#14161B`) — 그 외엔 라이트
3. 옐로우는 액션·활성에만, 데이터 카테고리는 클래스/랭크/희귀도 매핑 컬러로
4. 숫자는 무조건 Bricolage tabular-nums — 자릿수 정렬이 통계 사이트의 신뢰감
5. 작은 요소는 스티커 그림자(`0 3px 0`), 큰 카드는 소프트 그림자 — 결을 섞지 마라
6. 인터랙션은 `--ease-pop` 오버슈트 — 게임스러운 통통함을 잃지 마라
7. 모바일은 표를 압축하되 가로 스크롤로 도망가지 마라 — `nth-child`로 부수 컬럼을 숨겨라
