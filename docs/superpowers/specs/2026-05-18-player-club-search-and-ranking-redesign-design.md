# 플레이어/클럽 검색 & 랭킹 재배치 디자인

- **작성일**: 2026-05-18
- **대상 앱**: `core-api`, `imstargg-frontend`
- **관련 도메인**: 검색, 랭킹, 갱신(renewal), 검색 내역

## 1. 배경 / 문제

현재 imstargg-frontend 는 다음과 같은 제약을 가지고 있다.

- **검색은 태그(`#XXX`)로만** 가능. 이름으로 검색이 불가능해 정확한 태그를 모르면 진입 자체가 어렵다.
- 랭킹은 `/ranking/players`, `/ranking/clubs` 두 라우트로 **별도 탭에 분리**되어 있어, 플레이어/클럽 탭과 자연스럽게 연결되지 않는다.
- **플레이어 탭 자체가 없다**. 플레이어 도메인의 진입점은 검색 결과로 직접 이동하는 형태밖에 없다.
- 검색 내역(`localStorage`)은 **결과가 없어도 저장**되며 태그만 보여줘 식별이 어렵다.

목표는 검색 경험을 이름까지 확장하고, 랭킹을 각 도메인 탭(플레이어/클럽/브롤러 상세)으로 통합하며, 검색 내역을 의미있는 정보로 강화하는 것이다.

## 2. 목표 / 비목표

### 목표

1. 이름/태그 통합 검색 API 신설 (`core-api`)
2. 플레이어 탭 신설 + 플레이어/클럽 탭에 랭킹 통합
3. 브롤러 상세 페이지에 "랭킹" 탭 신설
4. 라이브 검색(타이핑 시 드롭다운) + 키보드 네비게이션
5. 검색 결과 페이지 + 갱신(renewal) 컴포넌트 통합
6. 검색 내역 강화 (결과 클릭 시에만 저장, 이름/트로피 표시)

### 비목표

- 검색 결과 정렬 옵션화 (트로피 desc 고정)
- 부분 문자열(substring) 매칭 — `startsWith` 만 지원
- `pg_trgm` GIN 인덱스 도입
- 기존 `/ranking/*` 라우트의 redirect 처리 (단순 제거)
- 클럽 검색에 즐겨찾기 기능 추가 (현행 유지)

## 3. 핵심 설계 결정

| 항목 | 결정 | 근거 |
|---|---|---|
| 검색 결과 페이지 라우트 | `/players/search?q=...` / `/clubs/search?q=...` 분리 | 결과 페이지에 도메인별 갱신 컴포넌트가 필요하고, 검색이 발생한 탭의 컨텍스트 유지 |
| 브롤러 상세의 랭킹 | 별도 "랭킹" 탭 신설 (해당 브롤러의 트로피/파워 랭킹) | 데이터량이 많아 인라인보다 탭이 자연스러움 |
| `/ranking/*` 처리 | redirect 없이 단순 제거 | 사용자 요구사항 |
| 이름 검색 대소문자 | PostgreSQL `LOWER()` 표현식 인덱스 | citext 컬럼 변경보다 침습성 적음 |
| 인덱스 종류 | B-tree (`varchar_pattern_ops`) | `startsWith` 매칭에 충분, GIN 불필요 |
| 정렬 | 트로피 desc | 단순함, 사용자 기대치 일치 |
| 페이징 | offset/limit | 사용자 요구사항 |
| 라이브 검색 limit | offset=0, limit=5 | 사용자 요구사항 |
| 결과 페이지 limit | limit=20 | 일반적 페이징 단위 |
| 디바운스 | 250ms | 입력 중간값 호출 억제 |
| 최소 입력 길이 | 2자 | 1자 검색은 결과 폭발 위험 |
| 키보드 | ↑↓ 이동, Enter 선택. Esc 미지원 | 사용자 요구사항 |
| 검색 내역 저장 시점 | 결과 항목을 **클릭(상세 페이지 이동)** 했을 때만 | "결과가 존재한 것만" 요구사항 충족 |

## 4. Backend (core-api) 설계

### 4.1 API

> **경로 컨벤션**: `imstargg-backend/CLAUDE.md` 에 따라 ID 가 뒤따르지 않는 경로는 단수형. 따라서 `/v1/player/search`, `/v1/club/search`.

#### `GET /v1/player/search`

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `q` | string | (필수) | 검색어. `#` 시작 시 태그, 아니면 이름(prefix) |
| `offset` | int | 0 | offset (>=0) |
| `limit` | int | 20 | limit (1..50) |

**응답** `PlayerSearchResponse`
```json
{
  "results": [ /* PlayerResponse 그대로 재사용 */ ],
  "hasNext": true
}
```

- `results` 항목은 기존 `PlayerResponse` 를 그대로 사용 (단건 조회 API 와 동일 구조).
- 검색 결과에 직접 사용하지 않는 필드(`victories3vs3` 등)도 포함되지만, 타입 일관성 이득이 더 큼.

#### `GET /v1/club/search`

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `q` | string | (필수) | 검색어. `#` 시작 시 태그, 아니면 이름(prefix) |
| `offset` | int | 0 | offset (>=0) |
| `limit` | int | 20 | limit (1..50) |

**응답** `ClubSearchResponse`
```json
{
  "results": [ /* ClubResponse 그대로 재사용 (members 포함) */ ],
  "hasNext": true
}
```

- `results` 항목은 기존 `ClubResponse` 를 그대로 사용. 단건 조회와 동일 구조.
- `members: List<ClubMemberResponse>` 가 포함되어 페이로드가 무겁다 — limit=20 × 멤버 30명 = 최대 600 객체. 위험 섹션 참조.

### 4.2 검색 로직

```kotlin
fun search(q: String, offset: Int, limit: Int): SearchPage<T> {
    val normalized = q.trim()
    return if (normalized.startsWith("#")) {
        // 태그 정규화 (대문자, # 포함) 후 단건 exact 조회
        val tag = normalizeTag(normalized)   // 기존 로직 재사용
        searchByTag(tag).toSinglePage(offset, limit)
    } else {
        // 이름 prefix 검색
        val pattern = normalized.lowercase() + "%"
        searchByNamePrefix(pattern, offset, limit + 1)   // +1 로 hasNext 판정
    }
}
```

- 태그 검색은 단건이라 hasNext 는 항상 false.
- 이름 검색은 `limit+1` 가져와서 `hasNext` 판정 (count 쿼리 회피).
- 정렬: `trophies DESC, brawlstars_tag ASC` (tie-break)
- `status` 필터링은 적용하지 않음 (현재 검색 결과에서 비활성 플레이어/클럽을 굳이 제외할 이유 없음).
  비활성 데이터를 노출하지 않는 정책이 필요해지면 partial index 와 함께 추가.

### 4.3 DB 마이그레이션 (Flyway)

```sql
-- V{next}__add_player_club_name_search_index.sql
CREATE INDEX idx_player__lower_name
  ON player (LOWER(name) varchar_pattern_ops);

CREATE INDEX idx_club__lower_name
  ON club (LOWER(name) varchar_pattern_ops);
```

- `varchar_pattern_ops` 는 PostgreSQL 의 prefix `LIKE 'xxx%'` 쿼리에 최적화된 opclass.
- `LOWER(name)` 표현식 인덱스로 대소문자 무관 검색. 쿼리 측도 `LOWER(name) LIKE ?` 형태로 일치시켜야 인덱스 사용됨.
- 한글은 PostgreSQL `LOWER()` 가 무영향이라 안전.

### 4.4 Repository / Service / Controller 구조

- `PlayerJpaRepository` 에 `findByLowerNameStartsWith(prefix, offset, limit)` 추가
  - JPA `@Query(nativeQuery = true)` 또는 QueryDSL 둘 다 가능. 기존 패턴 따름.
- `PlayerSearchService` / `ClubSearchService` 신설 (or 기존 PlayerService/ClubService 확장)
- `PlayerController` / `ClubController` 에 `searchPlayers` / `searchClubs` 핸들러 추가
- 응답 DTO: **신규는 `PlayerSearchResponse`, `ClubSearchResponse` 두 개만**. 항목은 기존 `PlayerResponse` / `ClubResponse` 그대로 재사용

### 4.5 검증 / 에러

- `q` 가 비어있거나(`trim` 후 empty) → `400` 빈 결과 대신 명시적 에러
- `limit > 50` 또는 `< 1` → `400`
- `offset < 0` → `400`
- 태그 정규화 실패 (`#` 시작했는데 잘못된 형식) → `400`

### 4.6 테스트

- `CoreApiContextTest` 기반 통합 테스트
  - 태그 검색 (정확 일치, 정규화)
  - 이름 prefix 검색 (정확, 대소문자 무시, 한글)
  - 빈 결과
  - hasNext 동작 (limit 경계)
  - 잘못된 파라미터 400
- 인덱스 사용 여부는 운영 EXPLAIN 으로 별도 확인 (자동 테스트 불필요)

## 5. Frontend (imstargg-frontend) 설계

### 5.1 라우트 변경

| 라우트 | 변경 | 비고 |
|---|---|---|
| `/players` | **신설** | 검색창 + 플레이어 트로피 랭킹 (국가 셀렉터 포함) |
| `/clubs` | 수정 | 기존 검색창 아래에 클럽 트로피 랭킹 추가 |
| `/players/search?q=...` | **신설** | 플레이어 검색 결과 + 갱신 카드 |
| `/clubs/search?q=...` | **신설** | 클럽 검색 결과 + 갱신 카드 |
| `/players/[tag]` | 변경 없음 | |
| `/clubs/[tag]` | 변경 없음 | |
| `/brawlers/[id]` | 수정 | "랭킹" 탭 신설 |
| `/ranking/*` | **삭제** | redirect 없음 |

### 5.2 공통 컴포넌트

#### `SearchableInput<T>` (신규, 제네릭)

플레이어/클럽 양쪽 공용. 책임:

- 입력값 디바운스 (250ms)
- 최소 길이(2자) 이상 시 fetch (offset=0, limit=5)
- 드롭다운 렌더 (아이템 컴포넌트는 prop 으로 받음)
- 키보드 네비 (↑↓ 이동, Enter 선택). Esc 미지원
- 외부 클릭/blur 로 드롭다운 닫기
- 검색 기록(history) 통합
- 라이브 결과 항목 선택 → 상세 페이지 이동 + history 저장
- Enter (선택 항목 없을 때) → 검색 결과 페이지 이동

Props:
```ts
type SearchableInputProps<T> = {
  kind: "player" | "club";
  placeholder: string;
  fetchResults: (q: string) => Promise<T[]>;
  renderItem: (item: T, selected: boolean) => ReactNode;
  toDetailHref: (item: T) => string;
  toSearchHref: (q: string) => string;
  toHistoryEntry: (item: T) => SearchEntry;
};
```

#### `PlayerSearchInput`, `ClubSearchInput` (수정)

`SearchableInput` 의 thin wrapper. 기존 API 호환을 위해 위치만 유지.

#### `SearchHistoryDropdown` (수정)

- 항목 표시: 이름 (large) / 태그 (small, mono) / 트로피 (right-aligned)
- 즐겨찾기 섹션 / 최근 검색 섹션 (기존 구조 유지)
- 클럽은 즐겨찾기 미지원 (현행 유지)

#### `RenewalCard` (신규)

- 검색 결과 페이지에 노출
- "원하는 결과가 없나요? 태그를 입력해 직접 갱신할 수 있습니다." 안내
- 태그 입력 + 갱신 버튼 → `POST /v1/{players|clubs}/{tag}/renewal`
- prefill: 현재 검색 쿼리가 태그(`#`로 시작)면 자동 입력
- 갱신 상태 polling 또는 즉시 응답 처리 (기존 RenewalController 응답 구조 따름)

### 5.3 검색 결과 페이지

`/players/search?q=...` (`/clubs/search` 도 동일 패턴)

레이아웃:

```
┌─────────────────────────────────────────┐
│ [검색창 (현재 쿼리 prefill)]              │
│                                         │
│ 검색 결과: "야"                          │
│ ┌─────────────────────────────────────┐ │
│ │ [아이콘] 야옹이                       │ │
│ │         #2U8CVLQQ9 · 52,000 🏆      │ │
│ │         Star Force                  │ │
│ ├─────────────────────────────────────┤ │
│ │ ...                                 │ │
│ └─────────────────────────────────────┘ │
│ [페이지네이션 (이전/다음 또는 더보기)]      │
│                                         │
│ ─────────────────                       │
│ [RenewalCard]                           │
│  "원하는 결과가 없나요?"                  │
│  [#태그 입력] [갱신]                     │
└─────────────────────────────────────────┘
```

- 페이징은 `hasNext` 기반 "더보기" 버튼 (단순함). 또는 offset 기반 query param.
- 결과 항목 클릭 → 상세 페이지 이동 + 검색 내역 추가
- 결과 0건이어도 RenewalCard 는 항상 노출

### 5.4 랭킹 통합

#### `/players` 페이지

```
┌─────────────────────────────────────────┐
│ [PlayerSearchInput]                     │
│                                         │
│ ─────────────────                       │
│ 플레이어 랭킹                            │
│ [CountrySelector] [RankingList]         │
└─────────────────────────────────────────┘
```

기존 `/ranking/players` 의 `CountrySelector` + `PlayerRankingList` 컴포넌트를 그대로 옮긴다.

#### `/clubs` 페이지

```
┌─────────────────────────────────────────┐
│ [ClubSearchInput]                       │
│                                         │
│ ─────────────────                       │
│ 클럽 랭킹                                │
│ [CountrySelector] [RankingList]         │
└─────────────────────────────────────────┘
```

#### 브롤러 상세 (`/brawlers/[id]`)

기존 탭 구조(브롤러 카드/통계 등)에 "랭킹" 탭을 추가.

- 탭 내부: `CountrySelector` + 브롤러 트로피 랭킹 리스트
- 데이터 소스는 기존 브롤러별 랭킹 API 재사용 (이미 존재한다면)

### 5.5 검색 내역 (`searchHistory.ts`)

#### 스키마 변경

```ts
// 기존
type SearchEntry = { kind: "player"|"club"; tag: string; label?: string; addedAt: number };

// 신규
type SearchEntry = {
  kind: "player" | "club";
  tag: string;
  name: string;
  trophies: number;
  addedAt: number;
};
```

#### 마이그레이션

- localStorage 키를 `imstargg.search.recents.v2` 로 변경하여 기존 데이터 자연 폐기.
- 또는 기존 키 유지하고 항목별로 `name`/`trophies` 없는 항목 필터링 → **단순함 위해 v2 키 사용 권장**.

#### 저장 시점

- 라이브 검색 드롭다운에서 결과 선택 → `addRecent(item)` 호출
- 검색 결과 페이지에서 결과 항목 클릭 → `addRecent(item)` 호출
- **단순 태그 입력 후 상세 페이지 직접 진입**(검색 결과 페이지 거치지 않음) 시: 그 자체로 검색 결과가 존재했음을 의미하므로 상세 페이지에서 로딩 성공 시 `addRecent` (또는 기존 동작 유지)
- 결과 0건인 검색은 저장하지 않음 (`SearchableInput` 이 결과를 받기 전엔 history 에 안 들어감)

#### 표시

```
┌─────────────────────────────────────┐
│ [⭐] 야옹이                           │
│      #2U8CVLQQ9          52,000 🏆  │
├─────────────────────────────────────┤
│ [⭐] Star Force                      │
│      #XYZ              1,200,000 🏆 │
└─────────────────────────────────────┘
```

## 6. 데이터 흐름 (요약)

### 라이브 검색

```
사용자 타이핑 → debounce(250ms)
  → GET /v1/{player|club}/search?q=...&offset=0&limit=5
  → 드롭다운 렌더 (키보드 네비 가능)
  → 항목 선택 → addRecent() → router.push(/{players|clubs}/{tag})
```

### Enter / 검색 버튼

```
Enter 누름 (선택 항목 없음)
  → router.push(/{players|clubs}/search?q=...)
  → 결과 페이지에서 GET /v1/{player|club}/search?q=...&offset=0&limit=20
  → 결과 항목 클릭 → addRecent() → 상세 페이지로
  → 또는 RenewalCard 사용 → POST renewal
```

> Backend API 는 단수형(`/v1/player/search`), Frontend 라우트는 복수형(`/players/search`). 두 도메인의 컨벤션이 다르므로 의도적으로 구분.

### 갱신 카드

```
쿼리가 #으로 시작 → 태그 입력 prefill
사용자 갱신 클릭 → POST /v1/{players|clubs}/{tag}/renewal
응답 처리 (기존 RenewalController 응답 구조 따름)
```

## 7. 컴포넌트 / 파일 변경 목록 (예상)

### Backend

| 파일 | 변경 |
|---|---|
| `db/migration/V{n}__add_player_club_name_search_index.sql` | 신규 |
| `PlayerJpaRepository.kt` | `findByLowerNameStartsWith` 추가 |
| `ClubJpaRepository.kt` | `findByLowerNameStartsWith` 추가 |
| `PlayerSearchService.kt` (또는 PlayerService 확장) | 신규 |
| `ClubSearchService.kt` (또는 ClubService 확장) | 신규 |
| `PlayerController.kt` | `/search` 엔드포인트 추가 |
| `ClubController.kt` | `/search` 엔드포인트 추가 |
| `PlayerSearchResponse.kt`, `ClubSearchResponse.kt` | 신규 DTO (results 항목은 기존 `PlayerResponse` / `ClubResponse` 재사용) |
| 통합 테스트 | 신규 |

### Frontend

| 파일 | 변경 |
|---|---|
| `src/lib/api/players.ts` / `clubs.ts` (or 동등 위치) | `searchPlayers` / `searchClubs` 클라이언트 추가 |
| `src/components/shared/SearchableInput.tsx` | 신규 (제네릭 공용) |
| `src/components/shared/PlayerSearchInput.tsx` | 리팩토링 (SearchableInput wrap) |
| `app/clubs/_components/ClubSearchInput.tsx` | 리팩토링 (SearchableInput wrap) |
| `src/components/shared/SearchHistoryDropdown.tsx` | 항목 렌더 강화 |
| `src/lib/searchHistory.ts` | SearchEntry 스키마 + v2 키 |
| `src/components/shared/RenewalCard.tsx` | 신규 |
| `app/players/page.tsx` | 신규 |
| `app/players/search/page.tsx` | 신규 |
| `app/clubs/page.tsx` | 랭킹 섹션 추가 |
| `app/clubs/search/page.tsx` | 신규 |
| `app/brawlers/[id]/page.tsx` | 랭킹 탭 추가 |
| `app/ranking/**` | **삭제** |
| 네비게이션 메뉴 | "랭킹" 탭 제거 / "플레이어" 탭 추가 |

## 8. 위험 / 고려사항

- **인덱스 효율**: `LOWER(name) varchar_pattern_ops` 인덱스는 prefix 매칭에 한정. 운영 EXPLAIN 으로 사용 확인 필요. row 수에 따라 성능 평가.
- **`ClubResponse` 페이로드 부담**: 검색 결과에 `members: List<ClubMemberResponse>` 가 포함되어 limit=20 시 최대 600 멤버 객체 전송. N+1 회피 위해 ClubResponse 변환 시 멤버 fetch 가 단일 쿼리/배치 쿼리로 이루어지는지 반드시 확인. 운영 응답 사이즈/지연 모니터링 후 슬림 응답 분리 고려.
- **트래픽 폭증**: 라이브 검색은 타이핑마다 API 호출. 디바운스 250ms + 최소 2자 정책으로 완화하지만, 필요 시 백엔드 캐시 또는 rate limit 검토.
- **localStorage 마이그레이션**: v2 키 도입으로 기존 사용자는 검색 내역이 사라짐. 일회성 UX 저하지만 단순함을 우선.
- **검색 정렬의 동률 처리**: 트로피 같은 플레이어가 다수일 때 결과 페이지 사이 일관성을 위해 `tag ASC` tie-break 필수.
- **브롤러 랭킹 탭의 데이터 소스**: 기존 브롤러별 랭킹 API 가 있는지 별도 확인 필요. 없다면 추가 작업 발생 — 별도 spec 으로 분리 가능.
- **태그 검색의 정규화 누락 시 인덱스 미사용**: `BRAWLSTARS_TAG` 컬럼은 이미 unique index 가 있으므로 정확 일치만 보장하면 됨.

## 9. 릴리즈 영향

- `core-api` minor bump (신규 엔드포인트)
- `imstargg-frontend` minor bump (신규 라우트, 컴포넌트, 검색 내역 스키마 변경)
- DB 마이그레이션 1건 (인덱스 추가 — `CONCURRENTLY` 옵션 검토)

## 10. 구현 순서 (제안)

1. **Backend**
   - Flyway 마이그레이션 (인덱스 추가)
   - Repository / Service / Controller / DTO
   - 통합 테스트
2. **Frontend 공통 컴포넌트**
   - `SearchableInput` (제네릭)
   - 기존 `PlayerSearchInput`/`ClubSearchInput` 리팩토링
   - `SearchHistoryDropdown` 항목 강화
   - `searchHistory.ts` 스키마 변경 (v2 키)
3. **검색 결과 페이지**
   - `RenewalCard` 신규
   - `/players/search`, `/clubs/search`
4. **랭킹 통합**
   - `/players` 신설 (랭킹 통합)
   - `/clubs` 랭킹 섹션 추가
5. **브롤러 상세 랭킹 탭**
   - `/brawlers/[id]` 탭 추가
6. **정리**
   - `/ranking/*` 삭제
   - 네비게이션 메뉴 수정
7. **릴리즈**
   - core-api / frontend minor bump
