# 사용자 피드백(Feedback) 기능 설계

- 작성일: 2026-05-17
- 도메인 우산: `operation` (운영용 기능 모음)
- 첫 기능: `feedback` (버그 신고 / 기능 제안 / 기타)

## 1. 배경 (Context)

이용자와의 소통 창구가 없어 다음 정보를 받지 못하고 있다.

- 버그 신고
- 기능 추가 요청
- 단순 피드백·소감

이를 통칭하는 용어로 **feedback** 을 채택한다. 카테고리는 `BUG`, `FEATURE_REQUEST`, `ETC` 3종. 본문(content) 하나만 받고 제목은 두지 않는다. 이메일은 답변이 필요한 경우 선택적으로 입력. 어뷰징·도배는 자동 차단이 아니라 관리자가 직접 판단하도록 IP 해시를 보관한다.

운영성 기능들이 앞으로 더 추가될 수 있으므로 `operation` 도메인(패키지) 우산 아래 둔다. operation 안에 feedback 서브 패키지를 따로 만들지 않고 평탄하게 배치하며, operation 도메인이 커지면 그때 분리한다.

## 2. 도메인 모델 & DB 스키마

### 2.1 테이블

단일 테이블에 카테고리 컬럼을 둔다. 카테고리별 스키마 차이가 없어 분리할 이유가 없고, 통합 조회·24시간 카운트 쿼리가 단순해진다.

```sql
create table if not exists feedback (
  id         bigserial   primary key,
  category   varchar(32) not null,   -- BUG | FEATURE_REQUEST | ETC
  content    text        not null,   -- 1~2000자
  email      varchar(254),           -- nullable (선택 입력)
  ip_hash    varchar(64) not null,   -- SHA-256(hex) of salt + ip
  checked    boolean     not null default false,
  -- BaseEntity
  created_at timestamp   not null,
  updated_at timestamp   not null,
  deleted    boolean     not null default false
);

create index if not exists idx_feedback_created_at on feedback (created_at desc);
create index if not exists idx_feedback_checked    on feedback (checked);
create index if not exists idx_feedback_ip_hash    on feedback (ip_hash);
```

마이그레이션 파일: `infrastructure/db-core/src/main/resources/db/migration/V260517__add_feedback_table.sql` (+ `schema.sql` 갱신).

### 2.2 엔티티

- `FeedbackCategory` (enum, `core-enum`): `BUG`, `FEATURE_REQUEST`, `ETC`
- `FeedbackEntity` (`db-core`, `BaseEntity` 상속)
  - 생성자: `category`, `content`, `email`, `ipHash` 모두 `val` (불변)
  - `var checked: Boolean = false` + `protected set`
  - `check()` / `uncheck()` 메서드로 상태 변경

`updated_at` 은 BaseEntity 가 자동 갱신하므로 별도 `checked_at` 컬럼은 두지 않는다.

### 2.3 검증 규칙

- `category` 필수, enum 값 중 하나
- `content` 필수, trim 후 1~2000자
- `email` 입력 시 RFC 형식, 최대 254자

### 2.4 IP 해시

- 알고리즘: `SHA-256(hex(salt + ip))`
- salt: `app.operation.feedback.ip-salt` 프로퍼티 (환경변수로 주입)
- 위치: `core-api` 의 application 레이어 (`IpHasher`, `IpHasherProperties`). 해시는 제출 시점에만 발생하므로 db-core 가 아닌 core-api 가 적합. 어드민은 해시 문자열을 그대로 노출만 한다.

## 3. 모듈 구조 & 패키지 배치

```
db-core/
  com/imstargg/db/core/operation/
    FeedbackEntity.kt
    FeedbackJpaRepository.kt

core-enum/
  com/imstargg/core/enums/operation/
    FeedbackCategory.kt

core-api/
  com/imstargg/core/api/controller/v1/operation/
    FeedbackController.kt
    FeedbackCreateRequest.kt
  com/imstargg/core/api/controller/v1/response/operation/
    FeedbackCreateResponse.kt
  com/imstargg/core/application/operation/
    FeedbackService.kt
    FeedbackRepository.kt          # Application Layer Repository 패턴
    IpHasher.kt
    IpHasherProperties.kt
  com/imstargg/core/domain/operation/
    Feedback.kt
  com/imstargg/core/api/support/
    ClientIpResolver.kt

core-admin/
  com/imstargg/core/admin/controller/operation/
    AdminFeedbackController.kt
  com/imstargg/core/admin/request/operation/
    FeedbackSearchRequest.kt
  com/imstargg/core/admin/response/operation/
    FeedbackListResponse.kt
    FeedbackResponse.kt
  com/imstargg/core/admin/application/operation/
    AdminFeedbackService.kt
    AdminFeedbackRepository.kt     # QueryDSL 검색·페이지네이션

core-batch/
  com/imstargg/core/batch/job/operation/
    FeedbackDailyDigestJobConfig.kt
    FeedbackDailyDigestTasklet.kt
```

프로젝트 컨벤션 적합성: 파일당 1개 클래스, `@OneToMany` 미사용, BaseEntity 상속, Application Layer Repository, Domain ID Reference 원칙 모두 부합.

## 4. API 명세

### 4.1 사용자 API (core-api)

**POST `/v1/feedback`** — 피드백 제출

요청
```json
{
  "category": "BUG | FEATURE_REQUEST | ETC",
  "content": "string (1~2000)",
  "email":   "string? (선택, RFC 형식, ≤254)"
}
```

응답 (201 Created)
```json
{ "id": 12345 }
```

처리 흐름
1. `@Valid` Bean Validation
2. `ClientIpResolver` 로 IP 추출 (`X-Forwarded-For` 첫 토큰 → `RemoteAddr` 폴백)
3. `IpHasher.hash(ip)` → `ipHash`
4. `FeedbackEntity` 생성·저장
5. `id` 반환

검증 실패는 기존 `core-api` 에러 핸들러 규약에 따라 400.

### 4.2 어드민 API (core-admin)

**GET `/feedbacks`** — 목록 조회

쿼리 파라미터
- `category`: `BUG | FEATURE_REQUEST | ETC` (선택)
- `checked`: `true | false` (선택)
- `page`: 0-base, 기본 0
- `size`: 기본 20, 최대 100

응답
```json
{
  "content": [
    {
      "id": 12345,
      "category": "BUG",
      "content": "...",
      "email": "user@example.com",
      "ipHash": "a3f1...e9",
      "checked": false,
      "createdAt": "2026-05-17T13:24:11"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 137,
  "totalPages": 7
}
```

기본 정렬 `createdAt DESC`. 필터는 QueryDSL 로 동적 조립.

**PUT `/feedbacks/{id}/check`** — 체크 상태 설정 (멱등)

요청
```json
{ "checked": true }
```

응답: 204 No Content. 존재하지 않으면 404. `FeedbackEntity.check()` / `uncheck()` 호출.

### 4.3 인증

- 사용자 API: 공개 (기존 `/v1/*` 정책 동일)
- 어드민 API: 기존 `core-admin` 인증 그대로

## 5. 배치 — 디스코드 일일 알림

- **잡 이름**: `feedbackDailyDigestJob`
- **스케줄**: 매일 21:00 KST (`0 0 21 * * *`, `Asia/Seoul`)
- **윈도우**: `[now − 24h, now)`
- **0건이면 미발송**

처리 흐름
1. 24시간 윈도우 내 `feedback` 조회 → 카테고리별 카운트, 이메일 보유 건수 집계
2. 총합 0 → 종료 (알림 없음)
3. 총합 ≥ 1 → 메시지 빌드 후 `AlertManager.send(...)` (기존 `DiscordAlertManager` 재사용)

메시지 예시
```
📨 지난 24시간 피드백 5건
• BUG 2건
• FEATURE_REQUEST 1건
• ETC 2건
✉️ 이메일 응답 요청 1건
```

0인 카테고리 줄은 생략. 이메일 보유 0건이면 마지막 줄 생략.

구현
- `core-batch` 의 단일 Step + Tasklet (chunk 불필요, 집계만 수행)
- 읽기 전용 트랜잭션
- 디스코드 호출 실패 시 잡 실패로 기록. 재시도 없음 (다음 날 알림으로 보강)

## 6. 사용자 프론트엔드 (imstargg-frontend)

### 6.1 트리거

- `SiteHeader`, `SiteFooter` 양쪽에 "피드백" 진입점 (이미 전역 적용된 `SiteFooter` 활용)
  - 헤더: 데스크탑 = 아이콘 + 라벨, 모바일 = 아이콘
  - 푸터: 텍스트 링크
- 두 트리거 모두 동일한 `FeedbackDialog` 오픈 (단일 컴포넌트 재사용)

### 6.2 컴포넌트 구조

```
src/components/feedback/
  FeedbackDialog.tsx          # shadcn Dialog 래퍼
  FeedbackForm.tsx            # 폼 본체 (react-hook-form + zod)
  feedback-category.ts        # 카테고리 라벨 매핑
src/api/operation/
  feedback.ts                 # POST /v1/feedback
src/hooks/
  use-feedback-dialog.ts      # 전역 오픈 상태 (zustand 기존 패턴 활용)
```

### 6.3 다이얼로그 UI

- 카테고리 라디오 그룹: 버그 신고 / 기능 제안 / 기타
- 본문 textarea (1~2000자, 카운터 표시)
- 이메일 input (선택, 플레이스홀더 "답변이 필요한 경우 입력")
- 취소 / 보내기 버튼

### 6.4 검증 (zod)

```ts
const schema = z.object({
  category: z.enum(['BUG', 'FEATURE_REQUEST', 'ETC']),
  content:  z.string().trim().min(1, '내용을 입력해주세요').max(2000),
  email:    z.union([z.literal(''), z.string().email('이메일 형식이 아닙니다').max(254)])
            .transform(v => v === '' ? undefined : v),
})
```

### 6.5 상태 처리

- 성공: 다이얼로그 닫기 + `toast.success('의견이 접수되었습니다. 감사합니다!')`
- 검증 실패: 인라인 에러 메시지
- 서버 오류: `toast.error('전송에 실패했습니다. 잠시 후 다시 시도해주세요')`
- 제출 중: 보내기 버튼 disabled + 스피너

### 6.6 접근성

- Dialog: ESC 닫기, 포커스 트랩, 초기 포커스는 카테고리 라디오
- 라벨-입력 결합(`htmlFor`/`id`), `aria-invalid` 로 검증 실패 표시

## 7. 어드민 프론트엔드 (imstargg-admin-frontend)

### 7.1 라우트 & 파일

```
app/feedbacks/
  page.tsx                    # 목록 페이지
  _components/
    FeedbackTable.tsx
    FeedbackFilters.tsx
    FeedbackCheckToggle.tsx
    FeedbackContentCell.tsx
lib/api/operation/
  feedback.ts                 # GET /feedbacks, PUT /feedbacks/{id}/check
```

### 7.2 테이블 컬럼

| 컬럼 | 동작 |
|---|---|
| 체크 | shadcn `Checkbox` — 토글 시 `PUT /feedbacks/{id}/check`, 낙관적 업데이트 |
| 유형 | 카테고리 뱃지 (BUG=red, FEATURE_REQUEST=blue, ETC=gray) |
| 작성일시 | `MM-DD HH:mm` (호버 시 풀 ISO) |
| 내용 | 한 줄 truncate, 행 클릭 시 전체 본문 표시 |
| 이메일 | 있으면 `mailto:` 링크, 없으면 `-` |
| ipHash | 앞 8자 + 호버 시 전체. 화면 내 동일 ipHash 가 여럿이면 같은 색 배경으로 그룹핑 (도배 어뷰징 시각 보조) |

### 7.3 필터 & URL 동기화

- 필터/페이지를 URL `searchParams` 에 반영 (`?category=BUG&checked=false&page=0`)
- 새로고침/공유 시 동일 화면 재현
- React Query 캐시 (기존 어드민 패턴)

### 7.4 체크 토글 흐름

1. 체크박스 클릭
2. 낙관적 UI 업데이트
3. `PUT /feedbacks/{id}/check`
4. 실패 시 롤백 + `toast.error`
5. 성공 시 캐시 갱신

### 7.5 인증

기존 어드민 인증 미들웨어가 모든 경로 보호 → 별도 작업 없음.

## 8. 검증 (Verification)

기능별 검증 방법.

### 8.1 백엔드 단위·통합 테스트

- `FeedbackEntityTest` — `check()` / `uncheck()` 동작
- `FeedbackServiceTest` — IP 해시 호출, 저장 결과 검증
- `IpHasherTest` — 동일 IP 가 동일 해시, 다른 IP 가 다른 해시, salt 변경 시 다른 결과
- `FeedbackControllerTest` (`CoreApiContextTest`) — 검증 실패 400, 정상 제출 201
- `AdminFeedbackControllerTest` (`CoreAdminContextTest`) — 필터·페이지네이션, PUT 멱등성, 404
- `AdminFeedbackRepositoryTest` (`DbCoreContextTest`) — QueryDSL 동적 필터
- `FeedbackDailyDigestTaskletTest` — 24h 윈도우 경계 데이터 무시, 카테고리/이메일 카운트, 0건 시 `AlertManager.send` 미호출 (Mockito verify)

### 8.2 수동 E2E

1. `docker-compose up -d` → `./gradlew :core:core-api:bootRun :core:core-admin:bootRun`
2. 프론트엔드 dev 서버 → 헤더와 푸터의 "피드백" 버튼 클릭 → 카테고리 선택 → 본문 입력 → 이메일 비워서 제출 → 201 + 토스트
3. 동일 동작을 이메일 입력해서 1회 더 제출
4. 어드민 프론트엔드 `/feedbacks` → 방금 제출한 2건 노출 확인, 카테고리 뱃지/이메일 컬럼/ipHash 앞 8자 확인, 동일 ipHash 그룹핑 색 확인
5. 체크박스 토글 → 새로고침 후 상태 유지 확인
6. 카테고리·체크상태 필터 변경 → URL 파라미터 동기화 확인
7. 배치: `feedbackDailyDigestJob` 수동 실행 (개발 프로필) → 디스코드 알림 수신 확인. 0건 시나리오로 윈도우 밀어서 미발송 확인

## 9. 향후 확장 여지

- 카테고리 추가: enum 항목 추가 + 마이그레이션 없이 (DB 는 varchar) + 프론트 라벨 추가
- operation 도메인이 커지면 `operation/feedback/` 으로 서브 패키지 분리
- 알림 채널 추가: 기존 `AlertManager` 추상화 활용
- 카테고리별 디스코드 멘션 룰: `BUG` 만 별도 채널/멘션 등
