# Admin 통합 등록 & 브롤러 상세 단일 페이지 — 설계

- 날짜: 2026-06-17
- 대상: `core-admin`(백엔드) · `imstargg-admin-frontend`(관리자 프론트)
- 상태: 설계 확정(구현 계획 대기)

## 배경

관리자 도구에서 게임 메타데이터(브롤러/기어/이벤트/이벤트 모드)를 등록할 때, 현재는 최소 식별 정보만 등록하고 나머지(이름·세부 설정·이미지)는 상세/편집 화면에서 따로 입력한다. 또 브롤러 상세는 `기본정보·스타파워·가젯·하이퍼차지·기어` 5개 탭으로 분리되어 있는데, 하위 항목 개수가 적어(스타파워≈2, 가젯≈2, 하이퍼차지≈1) 한 페이지에 모두 담는 편이 낫다.

> 참고: "피드백 체크" 기능은 코드상 이미 완전히 구현되어 있어(목록·필터·체크 토글·백엔드 `GET/PUT /feedbacks`·사이드바 링크) 본 작업 범위에서 제외한다.

## 목표

1. **브롤러 상세 단일 페이지화** — 탭 제거, 한 페이지에 모든 섹션, 기어를 이미지 그리드로 선택.
2. **통합 등록** — 브롤러/기어/이벤트/이벤트 모드를 등록할 때 (이미지 포함) 필요한 데이터를 한 폼에서 모두 입력. 백엔드 생성 API는 데이터를 원자적으로 받도록 확장하고, 이미지는 분리된 기존 업로드 API를 프론트가 순차 호출.

## 비목표 (Non-goals)

- 하위 항목(스타파워/가젯/하이퍼차지/버피)을 브롤러 **등록 시점**에 입력하는 것 — 각자 고유 ID·이미지가 필요하므로 등록 직후 상세 페이지에서 추가한다.
- 기본 기어 자동 선택 — 별도 `default` 식별 장치를 두지 않는다. 등록 폼의 기어는 **사전 선택 없이** 운영자가 수동 선택.
- 편집(수정) 화면의 구조 변경 — 기어/이벤트/모드 편집은 이미 단일 페이지 + 섹션별 granular 저장이며 그대로 유지.
- 이미지 업로드를 생성 API에 multipart로 합치는 것 — 의도적으로 분리 유지.

---

## 설계 1 — 브롤러 상세 단일 페이지 (프론트 전용)

대상 파일: `imstargg-admin-frontend/app/brawlers/[brawlStarsId]/page.tsx`

상세 조회 API(`GET /brawlers/{brawlStarsId}`)가 이미 `names·rarity·role·imageUrl·buffies·starPowers·gadgets·hyperCharges·linkedGearBrawlStarsIds·allGears`를 한 번에 내려준다. 따라서 **백엔드 변경 없이** 레이아웃만 바꾼다.

### 변경 내용

- shadcn `Tabs` 제거 → 섹션 카드를 단일 컬럼으로 수직 스택.
- 섹션 순서(확정): **이름/기본정보 → 기어 → 가젯 → 스타파워 → 하이퍼차지 → 버피**.
- **기어 섹션**: 현재의 텍스트 토글 버튼 → **이미지 타일 그리드**. 각 타일은 `GearResponse.imageUrl` 썸네일 + 한글명 + 선택 표시(체크). 클릭으로 토글, "기어 연결 저장" 버튼으로 `PUT /brawlers/{id}/gears` 1회 호출(기존 동작).
- **버피 섹션 분리**: 현재 기본정보 안에 묶여 있던 버피(가젯/스파/하차)를 **맨 아래 독립 섹션**으로. 각 버피는 이미지 + 등록/삭제(기존 엔드포인트).
- 저장 모델: 섹션별 **granular 저장 유지**(이름/이미지/기어연결/각 하위 항목 = 기존 엔드포인트 그대로). 단일 "전체 저장" 폼으로 바꾸지 않는다.

### 영향 없음

- 백엔드 API/DTO 변경 없음. 기존 상세 조회·각 섹션 저장 엔드포인트 재사용.

---

## 설계 2 — 통합 등록 (백엔드 + 프론트)

### 2.1 공통 원칙

- **데이터는 원자적으로**: 각 `POST` 생성 엔드포인트가 이름·세부 설정을 한 트랜잭션에 저장하도록 확장(현재처럼 생성 후 별도 PUT로 쪼개지 않음).
- **이미지는 프론트가 순차 호출**: 생성 API 성공 후, 사용자가 폼에서 선택한 이미지 파일이 있으면 프론트가 기존 이미지 업로드 PUT을 이어서 호출. 백엔드 이미지 API는 변경하지 않는다.
- 생성 엔드포인트의 식별자(`brawlStarsId`)는 요청 본문에 포함되어 클라이언트가 이미 알고 있으므로, 이미지 PUT의 경로 변수로 그대로 사용한다(생성 응답 본문 변경 불필요, 현행 `200 OK` 유지).

### 2.2 백엔드 API 변경 (요청 DTO + 서비스)

모듈: `core-admin`. 컨트롤러 경로/메서드는 동일, 요청 DTO와 서비스 저장 로직만 확장. 이름은 공백 값 필터링 후 `MessageEntity`로 저장(기존 `updateXxxMessages` 로직 재사용).

#### 브롤러 — `POST /brawlers`

```kotlin
// 현재
data class BrawlerCreateRequest(
    val brawlStarsId: Long,
    val rarity: BrawlerRarity,
    val role: BrawlerRole,
)
// 변경
data class BrawlerCreateRequest(
    val brawlStarsId: Long,
    val rarity: BrawlerRarity,
    val role: BrawlerRole,
    val names: Map<Language, String>,        // 추가 (BRAWLER_NAME)
    val gearBrawlStarsIds: Set<Long>,        // 추가 (BrawlerGearEntity, 사전선택 없음 → 빈 Set 허용)
)
```
`BrawlerService.createBrawler`: 한 트랜잭션에 `BrawlerEntity` 저장 + `MessageEntity(BRAWLER_NAME)` 저장 + `BrawlerGearEntity` 저장.

#### 기어 — `POST /gears`

```kotlin
// 현재: { brawlStarsId, rarity }
// 변경: + names: Map<Language, String>   // GEAR_NAME
```
`GearService.create`: `GearEntity` 저장 + `MessageEntity(GEAR_NAME)` 저장.

#### 이벤트 — `POST /events`

```kotlin
// 현재: { brawlStarsId, modeId?, mapName? }
// 변경: + mapNames: Map<Language, String>   // BATTLE_EVENT_NAME (맵 이름 번역; mapName 있을 때 의미)
```
`BattleEventService.createEvent`: `BattleEventEntity` 저장 + (mapNames 있으면) `MessageEntity(BATTLE_EVENT_NAME)` 저장. **이벤트는 이미지 없음.**

#### 이벤트 모드 — `POST /event-modes`

```kotlin
// 현재
data class BattleEventModeCreateRequest(
    val brawlStarsId: Long,
    val brawlStarsName: String,
    val resultType: BattleEventModeResultType = BattleEventModeResultType.RESULT,
)
// 변경
data class BattleEventModeCreateRequest(
    val brawlStarsId: Long,
    val brawlStarsName: String,
    val resultType: BattleEventModeResultType = BattleEventModeResultType.RESULT,
    val names: Map<Language, String> = emptyMap(),       // BATTLE_EVENT_MODE_NAME
    val playerCounts: List<Int> = listOf(3, 3),          // 기존 기본값과 동일
    val color: String = "#2A2E38",                       // 기존 기본값과 동일
)
```
`BattleEventService.createMode`: `BattleEventModeEntity`를 `playerCounts·color` 포함해 저장 + `MessageEntity(BATTLE_EVENT_MODE_NAME)` 저장. 검증은 기존 규칙 재사용 — `color`는 `^#[0-9A-Fa-f]{6}$`, `playerCounts`는 비어있지 않고 모든 값 ≥ 1.

### 2.3 프론트 등록 흐름 (admin-frontend)

각 등록 폼은 모든 데이터 필드 + (해당되면) 이미지 파일 선택을 포함한다. 제출 시:

```text
1) await createX(data)                       // POST — 데이터 원자적 생성
2) if (imageFile) await uploadXImage(id, imageFile)   // PUT multipart — 이미지(선택)
3) 성공 → 목록 또는 상세로 이동
```

- 이미지 파일은 **선택사항**. 없으면 2)를 건너뜀.
- 1) 실패 → 이미지 시도하지 않고 생성 에러 노출.
- 1) 성공 + 2) 실패 → 엔티티는 생성된 상태. "등록은 완료, 이미지 업로드 실패 — 상세에서 다시 업로드" 토스트 후 상세/목록으로 이동(롤백하지 않음).

이미지 필드가 있는 폼: **브롤러(포트레이트)·기어·이벤트 모드(아이콘)**. 이벤트는 이미지 단계 없음.

### 2.4 폼 표현 (기본 가정)

- **브롤러 등록 → 전용 페이지** `app/brawlers/new` (기어 이미지 그리드가 공간을 차지).
- **기어/이벤트/이벤트 모드 → 현행 다이얼로그 확장**(목록 페이지 내 모달에 전체 필드 + 이미지).
- 등록 폼의 입력 컴포넌트(이름 KO/EN, 이미지 업로드 등)는 상세/편집의 것과 표현 컴포넌트를 공유할 수 있다(저장 동작만 상이: 등록=순차 2콜, 편집=granular).

---

## 설계 3 — 편집 정책

- 기어/이벤트/이벤트 모드 편집 화면은 **이미 단일 페이지 + 섹션별 granular 저장**이므로 변경하지 않는다.
- 브롤러의 편집 surface는 설계 1의 단일 페이지 상세이다.
- 즉 "편집 통합"은 사실상 완료 상태이며, 신규 작업은 **등록(생성)** 경험과 **브롤러 상세 레이아웃**에 한정된다.

---

## 데이터 흐름 (등록, 이미지 포함 예: 브롤러)

```text
[등록 폼] --(submit)-->
  POST /brawlers { brawlStarsId, rarity, role, names, gearBrawlStarsIds }
    -> BrawlerEntity + MessageEntity(BRAWLER_NAME) + BrawlerGearEntity  (1 트랜잭션)
  (imageFile 있으면) PUT /brawlers/{brawlStarsId}/image  (multipart)
    -> S3 업로드 + BrawlStarsImageEntity(BRAWLER_PORTRAIT)
  -> 상세 페이지로 이동
```

## 에러 처리

- **생성 검증 실패**(중복 ID, 잘못된 color/playerCounts 등): 백엔드 `IllegalArgumentException` → 프론트 토스트로 메시지 노출, 이미지 단계 미진입.
- **부분 실패**(생성 성공·이미지 실패): 위 2.3대로 안내 + 이동, 상세에서 이미지 재업로드.
- 기존 검증 규칙(color 정규식, playerCounts ≥ 1)은 생성 시점에도 동일 적용.

## 테스트 전략

- **백엔드(core-admin)**: `CoreAdminContextTest` 기반 통합 테스트로 확장된 4개 생성 서비스 검증.
  - 이름(KO/EN)·기어 링크·playerCounts·color가 생성과 함께 한 트랜잭션에 저장되는지.
  - 공백 이름 필터링, 빈 `gearBrawlStarsIds` 허용, color/playerCounts 검증 실패 케이스.
  - 컨트롤러 레벨은 기존 `Admin*ControllerTest` 패턴을 따른다.
- **프론트(admin)**: 등록→이미지 순차 호출 흐름과 부분 실패 분기를 수동 검증(필요 시 admin-frontend의 기존 테스트 관행을 따른다). 브롤러 상세 단일 페이지는 시각/동작 수동 확인.

## 영향 파일 (개략)

**백엔드 `core-admin`**
- `request/.../BrawlerCreateRequest`, `GearCreateRequest`, `BattleEventCreateRequest`, `BattleEventModeCreateRequest` — 필드 확장
- `application/...` — `BrawlerService.createBrawler`, `GearService.create`, `BattleEventService.createEvent`, `BattleEventService.createMode` — 저장 로직 확장(기존 messages/gears 저장 로직 재사용)

**프론트 `imstargg-admin-frontend`**
- `app/brawlers/[brawlStarsId]/page.tsx` — 탭 제거, 단일 페이지 재구성, 기어 이미지 그리드, 버피 섹션 분리
- `app/brawlers/new/` — 신규 등록 페이지(전체 필드 + 이미지)
- `app/gears/page.tsx`, `app/events/page.tsx`, `app/event-modes/page.tsx` — 등록 다이얼로그 확장(전체 필드 + 이미지)
- `lib/api/...` — `createBrawler/createGear/createEvent/createEventMode` 요청 타입 확장 + 등록 후 이미지 업로드 순차 호출
- `lib/types.ts` — 요청 타입 확장

> AGENTS.md 규칙: 백엔드 API 스펙 변경이므로 `core-admin` ↔ `imstargg-admin-frontend` 동기화 필수(본 설계에 포함됨). DB 변경은 없음(엔티티/스키마 무변경 — 기존 테이블에 저장).

## 가정 / 미해결

- 브롤스타즈 공용 기어 ≈ `SUPER_RARE`이지만, 본 설계는 기본 기어 자동 선택을 하지 않으므로 영향 없음.
- 폼 표현(브롤러=전용 페이지 / 그 외=다이얼로그)은 기본 가정이며 구현 중 조정 가능.
- 각 서비스의 정확한 내부 재사용 지점(messages/gears 저장 헬퍼)은 구현 계획 단계에서 코드 확인 후 확정.
- `imstargg-admin-frontend`는 표준과 다른 Next.js 버전을 사용한다(`AGENTS.md`). 프론트 구현 전 `node_modules/next/dist/docs/`의 관련 가이드를 확인할 것.
