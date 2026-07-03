# 브롤스타즈 API 스펙 추적 정규화 설계

- 날짜: 2026-07-03
- 상태: 승인됨
- 대상: `imstargg-backend` `core-batch` — `BrawlStarsApiSpecTrackJob`

## 배경 / 문제

`BrawlStarsApiSpecTrackTask` 는 브롤스타즈 API 스펙(`GET /v1`, swagger 2.0 YAML)을 수집해
직전 스냅샷과 **원문 문자열 그대로** 비교한다. 브롤스타즈 서버는 요청마다 YAML 의
**맵 키 순서를 다르게** 직렬화하기 때문에(어드민 diff 로 확인 — 값이 바뀌는 필드는 없음),
매 실행이 "스펙 변경"으로 판정되어 오탐 알림이 반복 발생하고, 어드민 diff 페이지도
재정렬 노이즈로 가득 차 실제 변경을 식별하기 어렵다.

## 목표

1. 의미가 같은(순서만 다른) 응답에 대해 알림이 발생하지 않는다.
   (저장도 발생하지 않되, 배포 직후 레거시 raw 스냅샷의 정규화본 이관 1회는 예외)
2. 실제 스펙 변경 시에는 지금처럼 알림이 오고, 어드민 diff 에 **바뀐 부분만** 보인다.
3. 신규 의존성 없음, DB 스키마·어드민 API/프론트 변경 없음.

## 결정: 정규화(canonicalize) 후 비교·저장

YAML 을 파싱해 맵 키를 재귀 정렬한 뒤 재직렬화한 **정규화본**을 비교·저장 기준으로 삼는다.

기각한 대안:

- **의미 비교만 (Map equals)**: `Yaml().load(a) == Yaml().load(b)` 로 비교 코드는 최소지만,
  저장이 raw 그대로라 실제 변경 시 어드민 diff 에 재정렬 노이즈가 남는다. 목표 2 미달.
- **구조적 diff 엔진 + 알림 요약**: 알림에 변경 요약(추가/삭제 path 등)까지 포함.
  구현 비용이 크고, 정규화만으로 어드민 텍스트 diff 가 깨끗해지므로 과잉(YAGNI).

## 상세 설계

### 컴포넌트

- **신규** `YamlCanonicalizer` (Kotlin `object`, `com.imstargg.core.batch.job.brawlstars.task` 패키지, 파일 1개)
  - 공개 API: `canonicalize(content: String): String`
  - 동작: SnakeYAML `SafeConstructor` 로 파싱(외부 응답이므로 임의 객체 인스턴스화 차단)
    → 맵 키를 재귀 정렬(정렬 비교만 문자열 기준, 키 타입 자체는 보존 — swagger 의
    `responses` 상태코드 같은 정수 키 대비), **리스트 순서는 보존**(파라미터 순서 등은 의미 있음)
    → BLOCK 스타일로 재직렬화.
  - SnakeYAML 2.5 는 Spring Boot 를 통해 이미 core-batch 클래스패스에 존재.
- **수정** `BrawlStarsApiSpecTrackTask`: 비교·저장을 정규화본 기준으로 변경 (아래 흐름 참고).
- **변경 없음**: `BrawlStarsApiSpecEntity`, DB 스키마, `core-admin` API, admin-frontend.
  content 컬럼에 정규화본이 저장될 뿐이며 어드민 diff 는 기존 방식 그대로 동작한다.

### 데이터 흐름 (`track()`)

1. 원문 수신 → 빈 응답 `check`(기존 유지) → `canonical = YamlCanonicalizer.canonicalize(content)`
2. `previous == null` → `canonical` 저장, 알림 없음 (최초 수집, 기존과 동일)
3. `canonicalize(previous.content) == canonical` (의미 동일):
   - `previous.content != canonical` 이면(배포 직후 레거시 raw 행) → **알림 없이 `canonical` 1회 저장**
     (이후 diff 가 영구히 정규화본끼리 비교되도록 이관)
   - 같으면 로그만 남기고 종료 (기존과 동일)
4. 다르면 → `canonical` 저장 + 변경 알림 (기존과 동일)

전환 시나리오: 배포 후 첫 실행에서 DB 마지막 행은 raw 지만 비교 시점에 정규화하므로
오탐 알림이 발생하지 않고, 조용히 정규화본으로 1회 이관된다.

### 에러 처리

- 응답이 유효한 YAML 이 아니면 파싱 예외를 **그대로 전파해 잡 실패**로 처리한다.
  "형식 자체가 바뀜"은 스펙 변경보다 큰 이상 신호이므로 기존 잡 실패 모니터링으로 드러낸다.
  (`BrawlStarsApiTasklet` 은 점검 `ServiceUnavailable` 만 삼키므로 별도 코드 불필요)
  단, JSON·HTML 오류 페이지처럼 **YAML 로 파싱은 되는** 비정상 응답은 예외가 아니라
  일반 변경 감지 경로로 흘러 변경 알림이 발생한다 — 사람이 확인하게 되므로 허용.
- 빈 응답 검증은 기존 `check` 그대로 유지 (정규화 이전 수행).

### 테스트

- `YamlCanonicalizerTest` (신규, 단위):
  - 키 순서만 다른 두 입력 → 동일한 출력
  - 중첩 맵도 정렬됨
  - 리스트 순서는 보존됨
  - 유효하지 않은 YAML → 예외 발생
- `BrawlStarsApiSpecTrackTaskTest` (확장):
  - 순서만 다른 응답 → 저장·알림 없음 (핵심 회귀 테스트)
  - 레거시 raw previous(의미 동일) → 알림 없이 정규화본 저장
  - 의미 있는 변경 → 정규화본 저장 + 알림
  - 기존 테스트 기대값을 정규화본 기준으로 조정
    (SnakeYAML dump 시 quote 스타일이 원문과 달라질 수 있음)

## 실데이터 검증 (2026-07-03, prod 스냅샷 15개 전수)

prod `brawlstars_api_spec` 의 스냅샷 15개(2026-06-17 ~ 07-01, 매일 1건 저장 = 매일 오탐)를
읽기 전용 계정으로 조회해 검증했다.

- 15개 모두 길이 동일(33,149 bytes), md5 는 전부 상이 — 내용은 같고 배치만 다름.
- 연속 쌍 14개 전부 **의미 동등**(맵 순서 무시·리스트 순서 유지 비교) — 추적 시작 이후
  실제 스펙 변경은 한 번도 없었고 모든 알림이 오탐이었음.
- 본 설계의 키 정렬 정규화를 시뮬레이션한 결과 **15개 정규화본이 전부 동일 문자열** —
  정규화만으로 관찰된 오탐 100% 제거 확인.
- 재정렬은 `definitions` (모델 정의 맵과 그 `properties` 맵, 실행당 약 10개 노드)에서만 발생.
  리스트 순서 변동 없음, 정수 키 없음, 15개 모두 SafeConstructor 상당(safe_load) 파싱 성공.

## 범위 외

- 알림 메시지 내용 변경, 어드민 diff UI 개선, 스펙 파싱 기반 구조적 분석.
- 값이 매번 바뀌는 필드 제외 처리 — 현재 관찰상 존재하지 않음. 정규화 적용 후에도
  오탐 알림이 재발하면 그때는 diff 가 작아져 원인 필드가 바로 드러나므로 후속 대응.
