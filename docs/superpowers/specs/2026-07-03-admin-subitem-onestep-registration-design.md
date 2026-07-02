# 관리자 등록 흐름 통합 — 하위 아이템 원스텝 등록 설계

- 날짜: 2026-07-03
- 상태: 사용자 승인됨 (설계 단계)
- 대상: `imstargg-backend/core/core-admin`, `imstargg-admin-frontend`

## 배경

admin 의 등록 흐름은 최근 통합 작업(이벤트 모드 폼 통합, 이벤트 맵 이름 통합, `/brawlers/new` 전용 페이지)으로 대부분 "생성 API 1회 + 이미지 업로드 1회" 패턴으로 정리됐다. 남아 있는 분할 등록 흐름은 다음 3개다.

| 흐름 | 현재 | 문제 |
|---|---|---|
| 가젯/스타파워/하이퍼차지 추가 (브롤러 상세) | ID 만 입력해 생성 → 목록 행에서 이름 저장(`PUT .../messages`)·이미지 업로드(`PUT .../image`) 별도 | 3단계 |
| 브롤러 신규 등록 (`/brawlers/new`) | 하위 아이템 등록 불가 — 등록 후 상세로 이동해 개별 추가 | 페이지 이동 + 아이템당 3단계 |
| 버피 등록 (브롤러 상세) | 등록 클릭(생성) → 이미지 업로드 별도 | 2단계 |

원인: 하위 아이템 생성 요청 `BrawlerSubItemCreateRequest` 만 `brawlStarsId` 하나를 받는다. 브롤러(`BrawlerCreateRequest`)·기어(`GearCreateRequest`) 생성은 이미 `names` 를 받아 같은 트랜잭션에서 `MessageEntity` 까지 저장한다.

## 목표

- 가젯/스타파워/하이퍼차지 추가를 한 번의 사용자 액션(ID+이름+이미지)으로.
- 브롤러 신규 등록 폼에서 하위 아이템까지 함께 등록.
- 버피 등록을 이미지 선택 포함 한 번의 액션으로.
- 완료 시 admin 전체에서 분할 등록 흐름 제거 (기어·이벤트·이벤트모드·점검은 이미 통합 완료 확인).

## 비목표

- 이미지 업로드 API 통합 — S3 라이프사이클 분리 유지, 별도 `PUT .../image` 그대로 사용.
- 브롤러+하위 아이템을 단일 요청으로 받는 대통합 API — 이미지가 어차피 별도 N회이고, 상세 페이지의 개별 추가 API 와 이원화되므로 채택하지 않음 (검토 후 기각).
- 기존 수정 흐름(`SubItemRow` 의 이름 저장/이미지 재업로드, `PUT .../messages`) 변경.
- `/brawlers/new` 에 버피 포함 — 버피는 이미지 전용 특수 항목으로 상세에서만 등록.

## 설계

### 1. core-admin: 하위 아이템 생성 시 이름 저장 (유일한 API 변경)

`BrawlerSubItemCreateRequest` 에 선택 필드 `names` 추가:

```kotlin
data class BrawlerSubItemCreateRequest(
    val brawlStarsId: Long,
    val names: Map<Language, String> = emptyMap(),
)
```

`BrawlerService.createStarPower` / `createGadget` / `createHyperCharge` 는 엔티티 저장 후 **같은 트랜잭션**에서 이름을 `MessageEntity` 로 저장한다 (각각 `STARPOWER_NAME` / `GADGET_NAME` / `HYPERCHARGE_NAME`). `createBrawler`·`GearService.create` 의 기존 패턴을 그대로 따르고, 빈 문자열 값은 저장하지 않는다.

- 엔드포인트·경로·상태코드 변경 없음: `POST /brawlers/{id}/gadgets` 등 3개의 요청 본문만 확장.
- `names` 기본값이 빈 맵이므로 기존 호출과 하위 호환.

### 2. admin-frontend: 브롤러 상세 — 하위 아이템 추가 다이얼로그 확장

파일: `app/brawlers/[brawlStarsId]/page.tsx` (`SubItemSection` AddDialog), `lib/api.ts`

- 다이얼로그 필드: BrawlStars ID(필수) + 이름 KO/EN(선택) + 이미지(선택, `PendingImageInput`).
- `createGadget`/`createStarPower`/`createHyperCharge` 시그니처를 `names` 포함으로 변경.
- 등록 클릭 시:
  1. 생성 호출(`{brawlStarsId, names}`) — 실패 시 토스트 후 중단.
  2. 이미지가 선택돼 있으면 업로드 — 실패해도 아이템은 등록된 상태이므로 "등록됐지만 이미지 업로드에 실패했습니다. 아래 목록에서 다시 업로드하세요." 토스트 (브롤러 신규 등록의 기존 문구 패턴).
  3. `loadDetail()` 후 다이얼로그 닫기·초기화.
- `SubItemRow`(수정 용도)는 그대로 유지.

### 3. admin-frontend: `/brawlers/new` — 하위 아이템 섹션 추가

파일: `app/brawlers/new/page.tsx`

- 가젯/스타파워/하이퍼차지 3개 섹션. 각 섹션은 "행 추가"로 pending 아이템 `{ brawlStarsId, ko, en, imageFile }` 을 복수 개 쌓고 행 삭제 가능. 3개 타입 마크업이 동일하므로 페이지 내 로컬 컴포넌트 하나로 렌더링.
- 제출 시 호출 순서: `createBrawler` → 브롤러 이미지 업로드 → 각 pending 아이템 생성 + 이미지 업로드(순차).
- 실패 처리:
  - 브롤러 생성 실패 → 폼 유지 (현행과 동일).
  - 이후 단계 실패 → 계속 진행하고 실패 항목을 토스트로 요약한 뒤 상세 페이지(`/brawlers/{id}`)로 이동. 상세 페이지가 모든 항목의 보완·재시도 UI 를 이미 갖추고 있으므로 복구 경로가 보장된다.
- pending 아이템 ID 미입력 등 검증은 제출 전 수동 검증 (프로젝트 컨벤션: react-hook-form 미사용).

### 4. admin-frontend: 버피 등록 통합 (백엔드 무변경)

파일: `app/brawlers/[brawlStarsId]/page.tsx` (버피 카드)

- 미등록 버피 행에 `PendingImageInput` 을 두고 "등록" 클릭 한 번으로 `createBrawlerBuffy` → (이미지 선택 시) `uploadBrawlerBuffyImage` 연속 호출 → `loadDetail()`.
- 이미지 없이 등록하는 현행 동작도 유지 (이미지는 선택).
- 등록된 버피의 이미지 교체는 기존 `ImageUploadField` 그대로.

### 5. 에러 처리 원칙 (공통)

생성 성공 후 후속 호출(이름·이미지) 실패는 치명적이지 않다 — 결과 상태가 기존 2단계 흐름의 중간 상태와 동일하고, 상세 페이지의 기존 편집 UI 로 항상 복구 가능하다. 실패는 토스트로 명시하고 진행한다. 기존 브롤러/기어/이벤트모드 등록과 같은 원칙.

## 검증

- 백엔드: `./gradlew :core:core-admin:test` — 하위 아이템 생성 시 `names` 저장을 검증하는 테스트 추가 (기존 테스트 구조 준수).
- 프론트: `pnpm lint && pnpm exec tsc --noEmit && pnpm build` — admin-frontend 는 테스트 프레임워크 없음, CI(pr-test.yml) 미포함이므로 로컬 검증 필수.
- API 스펙 동기화 규칙 준수: core-admin 요청 DTO 변경 → admin-frontend `lib/api.ts` 시그니처 동시 반영 (이 설계 자체가 해당 작업).
