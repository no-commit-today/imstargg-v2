# imstargg-frontend 클럽 도메인 설계

작성일: 2026-04-28
대상: `imstargg-frontend`
범위: 클럽 검색 / 상세 / 멤버 목록 / 갱신 구현
선행: `docs/superpowers/specs/2026-04-27-frontend-foundation-design.md` (Foundation)

> 본 spec은 Foundation에서 확립한 패턴(레이어 분리, RSC + initialData, TanStack Query, 1초 폴링 갱신, MSW 통합 테스트, 표준 SEO)을 클럽 도메인에 그대로 미러링합니다. 새로운 패턴을 도입하지 않으며, 클럽 도메인 특화 결정만 본 문서에서 다룹니다.

---

## 1. 목표와 비목표

### 목표

- `app/clubs/page.tsx` (검색)와 `app/clubs/[tag]/page.tsx` (상세)의 mock을 백엔드 API와 연동된 실제 구현으로 교체
- 클럽 갱신(`POST /v1/clubs/{tag}/renewal`)을 플레이어와 동일한 1초 폴링 UX로 구현
- 멤버 목록을 백엔드 응답으로 렌더하고, 행 클릭으로 `/players/{tag}`로 이동
- 트로피 분포 차트를 `members[]` 클라이언트 계산으로 유지
- 표준 SEO (metadata + canonical) 적용

### 비목표

- **클럽 이름 검색**: 백엔드에 이름→태그 lookup API 부재 → 본 spec 범위 외
- **클럽 글로벌 랭킹 / 시즌 랭킹**: ranking 도메인 spec
- **클럽 활동 내역(배틀 로그)**: battle 도메인 spec
- **클럽 자산 이미지 매핑**(`badgeId`, `iconId` → URL): 자산 spec까지 placeholder 유지
- **최근 검색 기능**: Foundation player 검색과 일관성 유지 — 현재 mock의 "최근 검색" 섹션은 제거
- **동적 sitemap에 클럽 포함**: ranking 도메인 spec에서 처리

---

## 2. 핵심 결정사항

| 결정 | 선택 | 이유 |
|---|---|---|
| 백엔드 API 사용 범위 | `GET /v1/clubs/{tag}`, `GET/POST /v1/clubs/{tag}/renewal` | 이미 존재. 추가 백엔드 변경 불필요 |
| 첫 갱신(record 없음) 처리 | `emptyRenewal(tag)` 재사용 | Foundation에서 확정된 패턴 |
| Mock UI 정리 | "글로벌 랭킹 #142", "5분 전 갱신" 박스 **제거** / 트로피 분포 차트 **유지(클라이언트 계산)** | 백엔드 응답에 없는 항목은 제거. 분포는 추가 API 없이 정확히 계산 가능 |
| 멤버 정렬 | 클라이언트 안정 정렬: role(PRESIDENT > VICE_PRESIDENT > SENIOR > MEMBER > null) → trophies desc | 백엔드 순서 의존 회피, 화면 일관성 |
| Search Input 분리 | `PlayerSearchInput`을 일반화하지 않고 별도 `ClubSearchInput` | 라우팅·placeholder·예시 태그가 다름. props 분기보다 명확 |
| Badge / 멤버 아이콘 | `/assets/club_league.png` 고정 + 멤버는 mock의 hsl 그라디언트 + 첫 글자 | 자산 매핑 spec까지 placeholder |

---

## 3. 파일 구조

```text
app/clubs/
  page.tsx                          (정적, ClubSearchPage 렌더)
  _components/
    ClubSearchPage.tsx              (현재 파일 — mock 제거 후 재작성)
    ClubSearchInput.tsx             (신규)
  [tag]/
    page.tsx                        (RSC, generateMetadata, fetch + initialData)
    error.tsx                       (신규)
    not-found.tsx                   (신규)
    _components/
      ClubContent.tsx               (클라이언트 진입점, useClub initialData)
      ClubHeader.tsx                (히어로 카드 + RenewalButton)
      ClubMembers.tsx               (멤버 표)
      ClubTrophyDistribution.tsx    (분포 차트)

src/api/
  endpoints/
    club.ts                         (신규: clubApi.get(tag, opts))
    renewal.ts                      (수정: getClub / renewClub 추가)
  types.ts                          (수정: ClubResponse, ClubMemberResponse, ClubMemberRole, ClubType 추가)

src/domain/club/
  types.ts                          (신규: Club, ClubMember 도메인 타입)
  mappers.ts                        (신규: toClub, toClubMember)
  members.ts                        (신규: sortMembers, trophyDistribution — 순수 함수)

src/hooks/
  queries/queryKeys.ts              (수정: club.all / club.detail(tag) / club.renewal(tag))
  queries/useClub.ts                (신규)
  useClubRenewal.ts                 (신규: usePlayerRenewal과 동형)

tests/
  unit/domain/club/
    mappers.test.ts                 (신규)
    members.test.ts                 (신규)
  integration/club/
    clubRenewal.test.tsx            (신규: playerRenewal.test.tsx 미러)
  e2e/
    club.spec.ts                    (신규)
```

기존 `app/clubs/[tag]/_components/ClubDetailPage.tsx`는 신규 컴포넌트군으로 대체되며 삭제합니다.

---

## 4. API / 도메인 레이어

### 4.1 DTO 타입 (`src/api/types.ts`)

```typescript
export type ClubType =
  | "OPEN"
  | "INVITE_ONLY"
  | "CLOSED"
  | "UNKNOWN";

export type ClubMemberRole =
  | "PRESIDENT"
  | "VICE_PRESIDENT"
  | "SENIOR"
  | "MEMBER"
  | "UNKNOWN";

export interface ClubMemberResponse {
  tag: string;
  name: string;
  nameColor: string | null;
  role: ClubMemberRole | null;
  trophies: number;
  iconId: number;
}

export interface ClubResponse {
  tag: string;
  name: string;
  description: string;
  type: ClubType | null;
  badgeId: number;
  requiredTrophies: number;
  trophies: number;
  members: ClubMemberResponse[];
}
```

### 4.2 endpoint (`src/api/endpoints/club.ts`)

```typescript
import { apiFetch } from "../client";
import type { ClubResponse } from "../types";
import { toUrlPath } from "@/lib/tag";

export const clubApi = {
  get: (tag: string, opts?: { signal?: AbortSignal }) =>
    apiFetch<ClubResponse>(`/v1/clubs/${toUrlPath(tag)}`, opts),
};
```

### 4.3 renewal endpoint 확장 (`src/api/endpoints/renewal.ts`)

`getPlayer` / `renewPlayer`와 동일한 형태로 `getClub(tag, opts)` / `renewClub(tag)` 추가.

### 4.4 도메인 매퍼 (`src/domain/club/mappers.ts`)

DTO → 도메인 변환에서 처리:

- `tag` 정규화 (`normalizeTag`)
- `type === null` → `"UNKNOWN"`
- `member.role === null` → `"UNKNOWN"`
- `nameColor`는 그대로 (null 가능) — 표시 단계에서 처리
- 도메인 타입은 nullable을 없애고 `"UNKNOWN"` 리터럴로 통일하여 컴포넌트가 분기를 줄일 수 있게 함

### 4.5 멤버 유틸 (`src/domain/club/members.ts`)

순수 함수로 분리:

```typescript
export const ROLE_RANK: Record<ClubMemberRole, number> = {
  PRESIDENT: 0,
  VICE_PRESIDENT: 1,
  SENIOR: 2,
  MEMBER: 3,
  UNKNOWN: 4,
};

export function sortMembers(members: ClubMember[]): ClubMember[];

export interface TrophyBucket {
  label: string;
  min: number;
  max: number | null;
  count: number;
}

export function trophyDistribution(members: ClubMember[]): TrophyBucket[];
// buckets: 90k+, 80–90k, 70–80k, 60–70k, <60k (현재 mock과 동일)
```

---

## 5. 훅 레이어

### 5.1 query keys (`src/hooks/queries/queryKeys.ts`)

```typescript
club: {
  all: ["club"] as const,
  detail: (tag: string) => ["club", "detail", normalizeTag(tag)] as const,
  renewal: (tag: string) => ["club", "renewal", normalizeTag(tag)] as const,
},
```

### 5.2 `useClub`

`usePlayer`와 동형. `initialData` 받음, `staleTime: 60_000`.

### 5.3 `useClubRenewal`

`usePlayerRenewal`과 동형:

- `useQuery` `queryFn`: `renewalApi.getClub(tag)`. `NotFoundError` → `emptyRenewal(tag)`
- `refetchInterval: q => q.state.data?.progressing ? 1000 : false`
- progressing `true → false` 전환 감지 → `qc.invalidateQueries({ queryKey: queryKeys.club.all })` + `toast.success("갱신 완료")`
- `useMutation`: `renewalApi.renewClub(tag)`, onSuccess에서 cache set + `toast.info("갱신 요청됨")`, onError `toast.error("갱신 요청 실패")`
- 반환값: `{ renewal, progressing, canRenew, trigger, triggering }`

`RenewalButton kind="club"`은 이미 컴포넌트에 분기가 있으므로 `useClubRenewal`을 호출하도록 본 spec에서 연결합니다.

---

## 6. 페이지 / 컴포넌트

### 6.1 `app/clubs/page.tsx`

정적. `<ClubSearchPage />`만 렌더. metadata는 `buildMetadata({ title: "클럽 검색" })`.

### 6.2 `ClubSearchPage` (재작성)

- 헤더 ("CLUBS" overline + "클럽 검색" h1 + 보조 텍스트) — 기존 mock의 카피 유지
- `<ClubSearchInput />` 단일 입력
- "최근 검색" 섹션 제거

### 6.3 `ClubSearchInput`

- `useState<string>` + `<form onSubmit>` (`SubmitEvent<HTMLFormElement>`)
- `isValidTag(input)` 통과 시 `router.push("/clubs/" + stripHash(normalized))`
- 실패 시 인라인 에러 (placeholder "예: 2U8CVLQQ9")

### 6.4 `app/clubs/[tag]/page.tsx` (RSC)

```text
1. params.tag → normalizeTag → encoded
2. apiFetch<ClubResponse>(/v1/clubs/{enc}, { next: { revalidate: 60 } })
   - NotFoundError → notFound()
   - 그 외 에러 → throw (error.tsx로)
3. apiFetch<RenewalResponse>(/v1/clubs/{enc}/renewal)
   - NotFoundError → emptyRenewal(tag)
4. generateMetadata: buildMetadata({
     title: `${club.name} (${tag}) — 클럽`,
     description: `${trophies.toLocaleString()}🏆, 멤버 ${members.length}/30. ${type 라벨}.`,
     canonical: `/clubs/${stripHash(tag)}`,
     // og 이미지는 logo-full.png 기본
   })
5. <ClubContent initialClub={club} initialRenewal={renewal} tag={tag} />
```

### 6.5 `ClubContent` (client)

```text
useClub(tag, { initialData: initialClub })
useClubRenewal(tag) — initialRenewal 데이터를 query cache에 시드 (queryClient.setQueryData)
seed 시점: useEffect 가 아닌 useState 초기화 단계에서 1회 (Foundation player 패턴과 동일)

레이아웃:
  <ClubHeader club={club} renewal={renewal} ... />
  grid 2col:
    <ClubMembers members={sortMembers(club.members)} />
    aside: <ClubTrophyDistribution members={club.members} />
```

### 6.6 `ClubHeader`

기존 mock 히어로 카드 디자인 유지하되:

- 글로벌 랭킹 박스 **제거**
- 별도 "마지막 갱신 5분 전" 박스 **제거** (RenewalButton 자체가 상태 노출)
- 클럽 타입 라벨: `OPEN` / `INVITE_ONLY` / `CLOSED` / `UNKNOWN` → `"가입 가능"` / `"가입 신청 필요"` / `"비공개"` / `"-"`
- 트로피 / 멤버 / 필요 트로피 / 평균 트로피 4개 stat 표시 (평균은 `members[]` 계산)
- 우측 컬럼: 타입 칩 + `<RenewalButton kind="club" tag={tag} />`

### 6.7 `ClubMembers`

- `sortMembers(club.members)` 결과를 표로 렌더
- 컬럼: `# / 이름(아바타+태그) / 역할 / 트로피 / →전적`
- 행 클릭 → `router.push("/players/" + stripHash(member.tag))`
- role label 매핑: PRESIDENT→"회장"(yellow), VICE_PRESIDENT→"부회장"(purple), SENIOR→"시니어"(blue), MEMBER→"멤버"(gray), UNKNOWN→칩 미표시
- "나" 표시는 인증 미구현이므로 제거

### 6.8 `ClubTrophyDistribution`

`trophyDistribution(members)` 결과 렌더 — mock의 시각 디자인 유지(가로 바 차트, top bucket 옐로우).

### 6.9 error.tsx / not-found.tsx

`app/players/[tag]/error.tsx`, `not-found.tsx`를 거의 그대로 복제하되 카피만 클럽으로.

---

## 7. 첫 갱신 처리 (Foundation 패턴 그대로)

플레이어와 동일하게 클럽도 백엔드는 `GET /v1/clubs/{tag}/renewal`에 대해 갱신 record가 없으면 404를 반환합니다. POST는 첫 갱신 생성을 허용합니다. 따라서:

- `useClubRenewal`의 `queryFn`에서 `NotFoundError` catch → `emptyRenewal(tag)` 반환
- RSC `app/clubs/[tag]/page.tsx`에서도 동일 처리
- 통합 테스트로 `클럽 첫 갱신` 시나리오 보장

---

## 8. SEO

- `buildMetadata` 재사용
- canonical은 `/clubs/{stripHash(tag)}`
- 클럽 404 페이지(`not-found.tsx`)는 robots `noindex` 메타
- 동적 sitemap 포함은 ranking 도메인 spec까지 미룸

---

## 9. 테스트 전략

원칙은 Foundation과 동일: **네트워크 경계만 모킹**(MSW). 우리 코드는 모킹 금지.

### 9.1 단위 테스트

- `tests/unit/domain/club/mappers.test.ts`
  - `tag` 정규화, `type === null → UNKNOWN`, `role === null → UNKNOWN`
- `tests/unit/domain/club/members.test.ts`
  - `sortMembers`: role 우선순위, 같은 role 내 트로피 desc, 안정 정렬 검증
  - `trophyDistribution`: 경계값(60000, 80000, 90000), 빈 배열, 모두 같은 buckets 케이스

### 9.2 통합 테스트 (MSW + Testing Library)

`tests/integration/club/clubRenewal.test.tsx` — `playerRenewal.test.tsx` 미러:

1. **갱신 클릭 → 폴링 → 완료 토스트**: GET `/v1/clubs/{ENC}/renewal`이 progressing=true 응답 → 1.5초 후 false. POST → 토스트 sequence 검증.
2. **첫 갱신(record 없음)**: GET 첫 호출 404 → 버튼 활성화 → POST → progressing=true → 완료.

테스트 태그: `#2U8CVLQQ9` / `%232U8CVLQQ9`.

### 9.3 E2E (Playwright, prod API)

`tests/e2e/club.spec.ts`:

- `/clubs/2U8CVLQQ9` 진입 → 클럽 이름 표시, 멤버 행 ≥ 1 표시
- 멤버 행 클릭 → `/players/{...}` 이동
- SEO: `<title>`에 클럽 이름 포함, canonical 태그 존재
- 갱신 클릭 플로우는 `test.skip` (CORS 배포 후 활성화 — Foundation player 정책과 동일)

---

## 10. 구현 순서 (권장)

플랜 단계에서 확정하지만, 순서 전제:

1. DTO 타입 + endpoint + renewal endpoint 확장
2. 도메인 타입 / 매퍼 / members 유틸 (TDD: members 단위 테스트 먼저)
3. queryKeys + useClub + useClubRenewal (RenewalButton kind="club" 연결)
4. RSC page.tsx + generateMetadata + error/not-found
5. ClubContent / ClubHeader / ClubMembers / ClubTrophyDistribution
6. ClubSearchPage 재작성 + ClubSearchInput
7. 통합 테스트 (clubRenewal.test.tsx)
8. E2E (club.spec.ts)
9. 기존 ClubDetailPage.tsx 삭제, 사용처 정리, typecheck/lint/build/test 통과 확인

각 단계 끝에 frequent commit. Foundation 도메인 spec 패턴과 동일하게 phase별 subagent 디스패치로 실행 가능.

---

## 11. 의존 / 위험

- **백엔드 의존**: 변경 없음. CORS는 Foundation에서 이미 설정.
- **자산 의존**: badge / member icon URL 매핑이 부재 → 자산 매핑 spec 전까지 placeholder 유지. 이는 디자인적으로 수용 가능하다고 판단.
- **테스트 데이터**: prod에서 클럽 `#2U8CVLQQ9` 가 항상 살아있어야 E2E 통과. 테스트 작성 시점에 1회 검증.

---

## 12. 완료 기준

- `pnpm typecheck` / `pnpm lint` / `pnpm build` / `pnpm test:run` 모두 통과
- `/clubs` 검색 → 유효 태그 입력 → `/clubs/{tag}` 도달
- `/clubs/{tag}` 상세에서: 클럽 정보 + 멤버 표 + 트로피 분포 표시
- 갱신 버튼 클릭 → "갱신 요청됨" → "갱신 완료" 토스트
- 첫 갱신(record 없음) 클럽도 갱신 버튼이 활성화되어 첫 갱신 트리거 가능
- 멤버 행 클릭 → `/players/{tag}` 이동
- E2E club spec 통과 (갱신 클릭 플로우는 skip 상태 허용)
