# 피드백 재설계 — 카테고리 → 5단계 만족도

- 작성일: 2026-05-22
- 도메인: `operation.Feedback`
- 선행 spec: [2026-05-17-user-feedback-design.md](./2026-05-17-user-feedback-design.md) — 본 spec 으로 대체

## 1. 배경 (Context)

기존 피드백은 **버그 신고 / 기능 제안 / 기타** 3종 카테고리 + 자유 의견 + 선택 이메일 구조였다. 운영해 본 결과 카테고리 구분의 가치보다 의견 자체와 사용자의 만족 수준이 더 중요한 신호로 판단됐다. 카테고리는 분석에 거의 활용되지 않았고, 이메일은 응답 약속 없이 받아 두어 사용자 기대를 모호하게 만들었다.

피드백을 단순화한다.

- **만족도 (5단계)** + **자유 의견** 만 받는다.
- 이메일 / 카테고리 입력란을 제거한다.
- 도메인 우산(`operation`)과 클래스 이름(`Feedback`)은 유지한다.

새 사용자 카피:
- 다이얼로그 타이틀: **"ImStarGG에 피드백을 전달해주세요."**
- 부제: **"주신 의견은 빠짐없이 확인하고 감사히 받아들이겠습니다."**

## 2. 도메인 이름 결정 — `Feedback` 유지

만족도(rating) + 자유 의견(content)의 결합은 일반적인 사용자 피드백의 한 형태이다. 사용자 카피 자체가 "피드백" 용어를 쓰고 있고, 패키지(`operation`) / 풋터 링크 / 어드민 메뉴 / 배치 잡 이름 / DB 테이블이 모두 `feedback` 으로 정렬돼 있어 rename 비용 대비 의미상 이득이 없다.

검토한 대안:
- `SatisfactionFeedback` — 장황. `Feedback` 으로 충분.
- `UserOpinion` / `Voice` / `Review` — 의미가 좁거나(opinion) 도메인 결과 어긋남(review).
- `Survey` — 다중 문항 뉘앙스. 1점 만족도 + 1코멘트 구조엔 과함.

→ **`Feedback` 유지**, 만족도 enum 신설.

## 3. 도메인 모델 & DB 스키마

### 3.1 신규 enum

`core-enum` / `com.imstargg.core.enums.operation.FeedbackRating`

```kotlin
enum class FeedbackRating {
    VERY_SATISFIED,    // 매우 만족
    SATISFIED,         // 만족
    NEUTRAL,           // 보통
    DISSATISFIED,      // 불만족
    VERY_DISSATISFIED, // 매우 불만족
}
```

기존 `FeedbackCategory` 는 삭제.

### 3.2 테이블

기존 `feedback` 테이블을 **drop & 재생성** 한다. 운영 데이터는 폐기 — 카테고리/이메일 기반 row 는 rating 으로 변환할 수 없고, 모순 데이터를 잔존시키기보다 깨끗한 재시작이 단순하다 (사용자 결정).

**시간 컬럼 설계**: 도메인 의미의 "작성일시"는 audit 용 `created_at` 과 분리하여 `submitted_at` 전용 컬럼으로 둔다. 애플리케이션이 `Clock` 으로 명시 세팅하여 테스트에서 `Clock.fixed(...)` 로 결정적 시간 제어가 가능하다. 모든 조회/정렬은 `submitted_at` 기준.

```sql
-- V260522__redesign_feedback_to_rating.sql
drop table if exists feedback;

create table feedback (
    id           bigserial   primary key,
    rating       varchar(32) not null,
    content      text        not null,
    checked      boolean     not null default false,
    submitted_at timestamptz not null,            -- 도메인 작성일시 (app Clock)
    created_at   timestamptz not null default now(),  -- audit (BaseTimeEntity)
    updated_at   timestamptz not null default now()   -- audit (BaseTimeEntity)
);

-- 관리자 목록은 (checked 필터 → submitted_at desc 정렬) 단일 경로만 사용.
-- 배치 디제스트 (24h window) 도 submitted_at 범위 스캔 + checked 무관 — 같은 인덱스로 커버 가능.
create index ix_feedback__checked__submittedat
    on feedback (checked, submitted_at desc);
```

`schema.sql` 도 신규 정의로 동기화. 기존의 `ix_feedback__createdat` / `ix_feedback__checked` 단일 인덱스는 폐기, `rating` 인덱스는 만들지 않는다 (rating 값 cardinality 5 — 필터 선택도가 낮아 인덱스 이득 없음, 정렬 키도 아님).

### 3.3 엔티티

`db-core` / `FeedbackEntity` (변경 가능 컬럼 패턴 유지)

```kotlin
@Entity
@Table(name = "feedback")
class FeedbackEntity(
    @Enumerated(EnumType.STRING)
    @Column(name = "rating", updatable = false, nullable = false, length = 32)
    val rating: FeedbackRating,
    @Column(name = "content", updatable = false, nullable = false, columnDefinition = "text")
    val content: String,
    @Column(name = "submitted_at", updatable = false, nullable = false)
    val submittedAt: Instant,
    checked: Boolean = false,
) : BaseTimeEntity() {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null

    @Column(name = "checked", nullable = false)
    var checked: Boolean = checked
        protected set

    fun check()   { this.checked = true }
    fun uncheck() { this.checked = false }
}
```

- `email` / `category` 필드 제거
- `submittedAt: Instant` 신규 — 생성자에서 받아 `val`(불변). 호출자(`FeedbackRepository`)가 도메인 명령(`FeedbackCreate.submittedAt`)을 전달
- `BaseTimeEntity` 의 `createdAt` / `updatedAt` 은 그대로 두되 (audit 목적), 어떤 조회/정렬에도 사용하지 않는다
- `checked` 의 mutable 패턴 유지

### 3.4 검증 규칙

- `rating` 필수 — enum 값 중 하나
- `content` 필수 — trim 후 1~2000자

## 4. core-api 변경

### 4.1 도메인

```kotlin
// core/domain/operation/Feedback.kt
data class Feedback(
    val id: Long,
    val rating: FeedbackRating,
    val content: String,
    val submittedAt: Instant,
    val checked: Boolean,
)

// core/domain/operation/FeedbackCreate.kt
data class FeedbackCreate(
    val rating: FeedbackRating,
    val content: String,
    val submittedAt: Instant,
)
```

### 4.2 서비스 / 리포지토리

```kotlin
@Service
class FeedbackService(
    private val repository: FeedbackRepository,
    private val clock: Clock,
) {
    fun submit(rating: FeedbackRating, content: String): Feedback {
        val command = FeedbackCreate(
            rating = rating,
            content = content.trim(),
            submittedAt = clock.instant(),
        )
        return repository.save(command)
    }
}
```

- `Clock` 은 기존 `core-api` 빈 등록(다른 서비스에서도 사용 중)을 그대로 주입받는다. 별도 빈 추가 없음
- `FeedbackRepository.save(command)`: entity 생성자에 `submittedAt = command.submittedAt` 전달, `email` 인자 제거
- `toDomain()` 매퍼: `email` / `category` 제거, `rating` / `submittedAt` 매핑 추가
- 시간 결정은 서비스 계층의 책임. 컨트롤러는 시간을 모른다. 테스트는 `Clock.fixed(...)` 를 주입해 결정적으로 검증한다.

### 4.3 컨트롤러

```kotlin
@RestController
class FeedbackController(private val feedbackService: FeedbackService) {
    @PostMapping("/v1/feedback")
    fun create(@Valid @RequestBody request: FeedbackCreateRequest)
        : ResponseEntity<FeedbackCreateResponse> {
        val feedback = feedbackService.submit(rating = request.rating, content = request.content)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(FeedbackCreateResponse.of(feedback))
    }
}

data class FeedbackCreateRequest(
    val rating: FeedbackRating,
    @field:NotBlank @field:Size(max = 2000)
    val content: String,
)
```

엔드포인트 경로(`/v1/feedback`)와 응답(`{ id }`)은 유지.

## 5. core-admin 변경

- `FeedbackSearchRequest`: `category: FeedbackCategory?` → `rating: FeedbackRating?`
- `AdminFeedbackRepository.search`:
  - QueryDSL predicate `category.eq` → `rating.eq`
  - `orderBy(feedbackEntity.createdAt.desc())` → `orderBy(feedbackEntity.submittedAt.desc())` — 신규 인덱스 `(checked, submitted_at desc)` 활용
- `AdminFeedbackService.search` 시그니처도 `rating` 으로 교체
- `FeedbackResponse`: `category` → `rating`, `email` 필드 제거, `createdAt` → `submittedAt` 으로 교체 (도메인 작성일시)
- `AdminFeedbackController`: `/feedbacks`, `/feedbacks/{id}/check` 경로 그대로

## 6. core-batch 변경

`FeedbackDailyDigestJobConfig` + `FeedbackDigestMessage`:

- 조회 메서드: `findAllByCreatedAtGreaterThanEqualAndCreatedAtLessThan` → `findAllBySubmittedAtGreaterThanEqualAndSubmittedAtLessThan` (JpaRepository 쿼리 메서드 네이밍)
- `countByCategory` → `countByRating: Map<FeedbackRating, Long>`
- 이메일 카운트 라인 제거
- 알림 본문 예시:
  ```
  📨 지난 24시간 피드백 12건
  • 매우 만족 4건
  • 만족 5건
  • 보통 2건
  • 불만족 1건
  ```
- 한국어 라벨은 enum → 표시 라벨 매핑(`FeedbackRatingDisplay` 또는 인라인 `when`)을 batch 모듈에 둔다. core-enum 은 라벨링 책임을 두지 않는다 (다른 모듈도 자기 컨텍스트 라벨을 가짐).

## 7. 프론트엔드 — `imstargg-frontend`

### 7.1 다이얼로그 카피

- `FeedbackDialog.tsx`:
  - 타이틀: `"ImStarGG에 피드백을 전달해주세요."`
  - 부제(`DialogDescription`): `"주신 의견은 빠짐없이 확인하고 감사히 받아들이겠습니다."`

### 7.2 폼 (DESIGN.md 토큰 기준)

**5단계 만족도 — 가로 세그먼티드 라디오 그룹**

```
😄 매우만족   🙂 만족   😐 보통   🙁 불만족   😡 매우불만족
```

- 칩 1개당: `radius-sm`(8px), 1px `#D8DCE5` 보더, 흰 배경
- 호버: 보더 `#B4B9C7` + 스티커 그림자(`0 3px 0`)
- 선택(checked): 옐로우 보더(`#FFC60A`) + `glow-yellow` 링 + 스티커 그림자, 라벨 `#14161B` / 700
- 키보드: `role="radiogroup"` + 화살표키 네비 (네이티브 `<input type="radio">` 활용)
- 이모지 20px / 라벨 12px / Pretendard 700 — uppercase 처리 안 함
- 모바일(<640px): flex 5등분, 라벨 줄바꿈 허용 (이모지+라벨 그대로 표시)
- 미선택 상태로 제출 시: 인라인 에러 `"만족도를 선택해주세요"`

**의견 textarea**

- 6 rows, max 2000자, 카운터 유지 (기존 그대로)
- 라벨: `의견 *` — placeholder 없음

**버튼**

- "보내기" (primary 옐로우) / "취소" (outline) — 기존 그대로
- 제출 중: "보내는 중..." + disabled

**삭제 항목**

- 유형 라디오 그룹 (카테고리)
- 이메일 input

### 7.3 파일 변경

| 파일 | 액션 |
|---|---|
| `src/components/feedback/feedback-category.ts` | **삭제** |
| `src/components/feedback/feedback-rating.ts` | **신설** — `FEEDBACK_RATINGS`, `FEEDBACK_RATING_LABELS`, `FEEDBACK_RATING_EMOJIS` |
| `src/api/operation/feedback.ts` | 요청 타입 `{ rating, content }` 로 교체. `FeedbackCategory` 타입 제거 |
| `src/components/feedback/FeedbackDialog.tsx` | 타이틀/부제 갱신, `DialogDescription` 추가 |
| `src/components/feedback/FeedbackForm.tsx` | zod 스키마(rating + content)·필드·UI 교체. email 제거 |
| `FeedbackFooterLink` / `feedback-dialog-store` / `FeedbackDialogMount` / `use-feedback-dialog` | 변경 없음 (링크 카피 "의견 보내기" 유지) |

### 7.4 zod 스키마

```ts
const schema = z.object({
  rating: z.enum([
    "VERY_SATISFIED", "SATISFIED", "NEUTRAL",
    "DISSATISFIED", "VERY_DISSATISFIED",
  ]),
  content: z.string().trim()
    .min(1, "의견을 입력해주세요")
    .max(2000, "최대 2000자까지 입력 가능합니다"),
});
```

`defaultValues.rating` 은 비워둔다 (기존엔 `BUG` 디폴트였지만, 만족도는 사용자의 명시적 선택이 의미 있음).

### 7.5 신규 파일 예시

`src/components/feedback/feedback-rating.ts`
```ts
import type { FeedbackRating } from "@/api/operation/feedback";

export const FEEDBACK_RATINGS: FeedbackRating[] = [
  "VERY_SATISFIED",
  "SATISFIED",
  "NEUTRAL",
  "DISSATISFIED",
  "VERY_DISSATISFIED",
];

export const FEEDBACK_RATING_LABELS: Record<FeedbackRating, string> = {
  VERY_SATISFIED: "매우 만족",
  SATISFIED: "만족",
  NEUTRAL: "보통",
  DISSATISFIED: "불만족",
  VERY_DISSATISFIED: "매우 불만족",
};

export const FEEDBACK_RATING_EMOJIS: Record<FeedbackRating, string> = {
  VERY_SATISFIED: "😄",
  SATISFIED: "🙂",
  NEUTRAL: "😐",
  DISSATISFIED: "🙁",
  VERY_DISSATISFIED: "😡",
};
```

## 8. 프론트엔드 — `imstargg-admin-frontend`

### 8.1 타입

`lib/types.ts`:
```ts
export type FeedbackRating =
  | "VERY_SATISFIED" | "SATISFIED" | "NEUTRAL"
  | "DISSATISFIED" | "VERY_DISSATISFIED";

export interface FeedbackItem {
  id: number;
  rating: FeedbackRating;
  content: string;
  checked: boolean;
  submittedAt: string;  // ISO-8601, 도메인 작성일시 (BE 응답 키 변경: createdAt → submittedAt)
}

export interface FeedbackSearchParams {
  rating?: FeedbackRating;
  checked?: boolean;
  page?: number;
  size?: number;
}
```

`FeedbackCategory` 와 `email` 필드 모두 삭제.

### 8.2 API

`lib/api/operation/feedback.ts`:
- 쿼리스트링 `category` → `rating`
- `getFeedbacks` / `updateFeedbackCheck` 시그니처는 동일

### 8.3 UI

**`FeedbackFilters.tsx`**: 카테고리 셀렉트 → rating 셀렉트
- 옵션: 전체 / 매우 만족 / 만족 / 보통 / 불만족 / 매우 불만족

**`FeedbackTable.tsx`**: 컬럼 재정렬
- 컬럼: `체크 / 만족도 / 작성일시 / 내용` (이메일 컬럼 제거) — 작성일시 값은 `submittedAt` 필드에서 조회 (기존 `createdAt` 참조 모두 교체)
- 만족도 셀: 이모지 + 라벨 + 색조 뱃지
  ```
  VERY_SATISFIED   → green   (#2BD576 톤)
  SATISFIED        → lime    (옅은 green)
  NEUTRAL          → neutral (#8A90A0)
  DISSATISFIED     → orange  (#FF8A1F)
  VERY_DISSATISFIED → red    (#FF3A3A)
  ```

`FeedbackContentCell.tsx`, `FeedbackCheckToggle.tsx`: 변경 없음.

## 9. API 계약 (Breaking change)

```http
POST /v1/feedback
Content-Type: application/json

{ "rating": "VERY_SATISFIED" | "SATISFIED" | "NEUTRAL"
         | "DISSATISFIED" | "VERY_DISSATISFIED",
  "content": "string (1~2000)" }
→ 201 { "id": number }
```

```http
GET /feedbacks?rating=<enum>&checked=<bool>&page=&size=
→ 200 { content: FeedbackResponse[], page, size, totalElements, totalPages }
PUT /feedbacks/{id}/check  { "checked": bool }  → 204
```

구버전 클라이언트가 새 API 를 호출하면 `rating` 누락으로 400. **백엔드(`core-api`/`core-admin`/`core-batch`/`db-core`) + 프론트 2개 동시 배포** 필요.

## 10. 테스트

### 10.1 백엔드 (갱신)

- `FeedbackServiceTest` — rating 기반 submit 시나리오 + `Clock.fixed(...)` 주입으로 `submittedAt` 결정성 검증 (저장된 도메인의 `submittedAt` 이 fixed clock 시각과 같은지)
- `FeedbackControllerTest` — request body / 검증 케이스 (rating 누락, content 빈 값 / 2000자 초과). 컨트롤러는 시간을 다루지 않으므로 Clock 검증은 서비스 테스트에 위임
- `FeedbackEntityTest` — entity 생성 + checked 토글 + `submittedAt` val 검증
- `FeedbackDigestMessageTest` — `countByRating` 기반 메시지 생성 / 0건 null 반환
- `FeedbackDailyDigestJobConfig` 관련 (Job 자체 테스트가 있다면) — `submittedAt` 범위 쿼리로 갱신
- `AdminFeedbackControllerTest` — rating 필터 / 페이징 / 체크 토글 / 응답 필드(`submittedAt`) 검증

`AdminFeedbackServiceTest` 가 없다면 추가하지 않는다 (현재 컨벤션상 컨트롤러 테스트로 커버).

### 10.2 프론트 (갱신)

- `FeedbackForm` 의 컴포넌트 단위 테스트는 현재 코드베이스에 없음 — spec 단계에서 새로 추가하지 않음 (변경 범위 최소화). 검증은 prod E2E 호출 영역 — 별도 E2E 추가도 보류 (필요 시 follow-up).

## 11. 마이그레이션 / 호환성

- 운영 DB `feedback` 테이블: **drop & 재생성** (사용자 결정)
- 마이그레이션 파일 1개 (`V260522__redesign_feedback_to_rating.sql`)
- `schema.sql` 동기화
- 구버전 데이터 백업이 필요한 경우 배포 전 별도 dump 수행 (운영 책임)

## 12. 릴리즈 / 배포

`release-management` 스킬 기준 영향 앱:

| 앱 | bump 종류 |
|---|---|
| `core-api` | minor (요청 스키마 breaking) |
| `core-admin` | minor (응답/필터 breaking) |
| `core-batch` | minor (digest 로직 변경) |
| `imstargg-frontend` | minor (API 클라이언트 + 폼 UI 변경) |
| `imstargg-admin-frontend` | 관리 대상 외 — 별도로 갱신 (스킬 무관) |

배포 순서: **DB 마이그레이션 → 백엔드 3개 동시 → 프론트 2개**. 짧은 다운타임을 허용한다 (피드백은 비핵심 기능).

## 13. 작업 범위 요약 (체크리스트용)

**core-enum**
- [ ] `FeedbackRating` 신설
- [ ] `FeedbackCategory` 삭제

**db-core**
- [ ] 마이그레이션 SQL 작성 (drop + 재생성, 단일 `(checked, submitted_at desc)` 인덱스)
- [ ] `schema.sql` 갱신
- [ ] `FeedbackEntity` 교체 (`submittedAt: Instant` val 추가, `email`/`category` 제거)
- [ ] `FeedbackEntityTest` 갱신
- [ ] QueryDSL Q-class 재생성 확인 (KSP)

**core-api**
- [ ] `Feedback` / `FeedbackCreate` 도메인 교체 (`submittedAt` 포함)
- [ ] `FeedbackService` — `Clock` 주입, `submit` 에서 `clock.instant()` 로 `submittedAt` 세팅
- [ ] `FeedbackRepository.save` — entity 생성 시 `submittedAt = command.submittedAt` 전달
- [ ] `FeedbackController` / `FeedbackCreateRequest` 교체
- [ ] `FeedbackCreateResponse` (변경 없음 — `id` 만)
- [ ] 단위/컨텍스트 테스트 갱신 (`Clock.fixed` 주입 패턴)

**core-admin**
- [ ] `FeedbackSearchRequest` / `FeedbackResponse` 교체 (응답 시간 필드 `submittedAt`)
- [ ] `AdminFeedbackService` / `AdminFeedbackRepository` 교체 — QueryDSL `orderBy(submittedAt.desc())` + `rating.eq` predicate
- [ ] `AdminFeedbackController` (시그니처 정합성만)
- [ ] `AdminFeedbackControllerTest` 갱신

**core-batch**
- [ ] `FeedbackDigestMessage` rating 기반 재작성
- [ ] `FeedbackDailyDigestJobConfig` 교체 (조회 메서드를 `submittedAt` 범위로 교체)
- [ ] `FeedbackJpaRepository` 의 24h 조회 메서드 이름 변경 (`findAllBySubmittedAt…`)
- [ ] `FeedbackDigestMessageTest` 갱신

**imstargg-frontend**
- [ ] `feedback-category.ts` 삭제, `feedback-rating.ts` 신설
- [ ] `api/operation/feedback.ts` 타입 교체
- [ ] `FeedbackDialog.tsx` 카피 갱신 + `DialogDescription` 추가
- [ ] `FeedbackForm.tsx` 스키마/UI 교체
- [ ] `pnpm typecheck` / `pnpm lint` 통과

**imstargg-admin-frontend**
- [ ] `lib/types.ts` 타입 교체 (`createdAt` → `submittedAt`)
- [ ] `lib/api/operation/feedback.ts` 쿼리 키 교체
- [ ] `FeedbackFilters` rating 셀렉트
- [ ] `FeedbackTable` 만족도 셀 + 이메일 컬럼 제거 + 작성일시 셀이 `submittedAt` 참조

**릴리즈**
- [ ] `release-management bump` (영향 앱들)
