# core-api 검토·리팩터링 설계

- 날짜: 2026-07-07
- 대상: `imstargg-backend/core/core-api` (+ 도메인 모델 관점에서 걸치는 db-core·core-worker 발견사항)
- 상태: 설계 승인 완료 (섹션별 승인), 구현 전

## 1. 배경과 전제

core-api 전반(아키텍처·도메인 모델·성능)의 예방적 검토를 수행하고, 발견사항 중 승인된
항목을 구현까지 진행한다. 브레인스토밍 문답으로 확정한 전제:

| 항목 | 결정 |
|---|---|
| 동기 | 체감 문제 없음 — 예방적 종합 검토 |
| 서비스 규모 | 소규모 (일 방문자 수백~수천) — 성능 항목은 예방 관점으로 우선순위 판단 |
| 모듈 범위 | core-api 중심 + 도메인 모델 관점에서 걸치는 이슈(db-core·core-worker)는 발견사항에 포함 |
| 변경 허용 | API 스펙 변경 허용 — 단, 검토 결과 실질적 스펙 변경이 필요한 항목 없음 |
| 산출물 | 본 설계 문서 + 승인 항목 구현 |
| 접근법 | 구조 재설계 포함 검토 → 검토 결과 "bounded context 유지 + 경계 결함 제거" 채택 (§3) |

## 2. 총평

core-api는 선언된 컨벤션(Controller → QueryComposer → Service → application Repository(`*WithCache`) → JPA)과
실제 코드가 거의 일치하는 잘 관리된 코드베이스다. 대개편은 불필요하다.

유지할 강점:

- 도메인 모델이 JPA 엔티티로부터 격리되어 있고 Domain ID Reference 원칙이 지켜짐
- OSIV 비활성화, `default_batch_fetch_size: 300`, 검색·통계 쿼리와 인덱스 매칭 양호
- Caffeine(로컬) + Redis(통계) 컴포지트 캐시, Redis 장애 시 fallback 에러 핸들러, `@Cacheable(sync = true)`로 thundering herd 방지
- `ProblemDetail` 기반 에러 응답, 도메인 KDoc으로 정정 이력 보존 문화

발견사항 요약: 정합성 트랩(A) 3건, 도메인 경계(B) 3건, statistics 유지보수성(S) 6건,
성능 위생(C) 6건, 선택 제안(D) 3건 중 1건 채택 — 총 19개 구현 항목.

statistics 도메인은 사용자 요청으로 유지보수성 관점의 추가 심층 검토를 수행했고(§4 Phase 2),
"새 통계 종류 하나 추가 = 13개 파일 생성·수정"인 현재 구조의 반복·분산 문제를 구조적으로 줄인다.

## 3. 아키텍처 구조 결정 (승인: 옵션 2)

### 판정: core-worker Player는 "중복"이 아니라 별도 bounded context의 write 모델

- `core/core-api/.../domain/brawlstars/player/Player.kt` — 조회(read) 컨텍스트의 불변 read model. JPA 격리 잘됨.
- `core/core-worker/.../worker/domain/Player.kt` — BrawlStars API 응답과 DB 상태를 diff·갱신하는 수집(write) 컨텍스트의 mutable 애그리거트. JPA 엔티티를 직접 보유.

이름만 같을 뿐 책임이 다르므로 모델 통합·공유 모듈 추출은 오히려 조회 모양과 수집 모양을
결합시켜 역효과다.

### 검토한 옵션

| 옵션 | 내용 | 판단 |
|---|---|---|
| 1. 공유 core-domain 모듈 추출 | 도메인 모델을 별도 모듈로 공유 | 기각 — 실제 공유 가능 코드가 거의 없음(worker 도메인 4클래스). 모듈 관리 비용(Dockerfile prefetch 목록 등)만 증가 |
| **2. bounded context 유지 + 경계 결함 제거** | 컨텍스트별 모델 유지, 아래 결함만 제거 | **채택** |
| 3. 헥사고날(포트/어댑터) 전환 | domain에 리포지토리 인터페이스 도입 | 기각 — 소규모에 세리머니 과다. application Repository가 이미 JPA 격리 실익 제공 |

### 옵션 2의 실행 항목

1. core-api `domain` 패키지의 Spring Data 의존 제거 (§4 B-1)
2. 리포지토리로 샌 도메인 로직 회수 (§4 A-3)
3. bounded context 구조 명문화 — `imstargg-backend/CLAUDE.md`에 "core-api domain = read 컨텍스트,
   core-worker domain = 수집 write 컨텍스트(엔티티 기반이 의도된 설계)"를 기록해 향후 혼동 방지

## 4. 구현 백로그

### Phase 1 — 정합성 + 도메인 경계 (A-1, A-2, B-1~B-3)

**A-1. `OffsetLimit.toPageable()`의 offset 왜곡 수정**
- 근거: `core/core-api/.../domain/OffsetLimit.kt:10` — `PageRequest.of(offset / limit, limit)`은
  offset이 limit의 배수일 때만 정확. 예: `offset=30, limit=25` → page=1 → 실제 25행부터 반환.
- 현재 프론트엔드는 배수로만 호출(무한 스크롤)하므로 잠재 상태이나, 공개 API라 임의 호출엔 조용히 틀린 결과.
- 수정: offset을 그대로 보존하는 커스텀 `Pageable` 구현으로 교체. 사용처 3곳
  (`PlayerRepository`, `ClubRepository`, `BattleRepository`의 search/battles 조회) 일괄 적용.
- 검증: offset이 배수가 아닌 케이스의 리포지토리 컨텍스트 테스트 추가.

**A-2. `OffsetLimitRequest` 검증 추가**
- 근거: `core/core-api/.../api/controller/OffsetLimitRequest.kt` — `@Min/@Max` 부재.
  공개 API에서 `?limit=1000000`이 그대로 DB까지 전달.
- 수정: `0 ≤ offset ≤ 10,000`, `1 ≤ limit ≤ 100` 검증. 프론트(25 고정) 무영향.
- 참고: 비정상 호출이 400으로 바뀌는 것 외 API 스펙 변화 없음.

**B-1. domain 패키지의 Spring Data 의존 제거**
- 근거: `core/core-api/.../domain/Slice.kt:3`(Spring Data `Slice` import),
  `core/core-api/.../domain/OffsetLimit.kt:3-4`(`PageRequest`/`Pageable` import).
- 수정: `Slice.from(...)`·`toPageable()` 변환을 application 계층(매퍼/확장 함수)으로 이동.
  A-1의 커스텀 Pageable 도입과 한 묶음으로 진행.

**B-2. `Brawlers` 값 객체 Map 인덱싱**
- 근거: `core/core-api/.../domain/brawlstars/brawler/Brawlers.kt:8` — `find()`가 선형 탐색.
  `StatisticsQueryComposer`의 페어/상대 통계 루프에서 아이템마다 2회 호출.
- 수정: 생성 시 `associateBy { it.id }`로 인덱스 보유. 동작 불변.

**B-3. bounded context 구조 명문화**
- §3 실행 항목 3의 구체화: `imstargg-backend/CLAUDE.md`에 core-api domain(read 컨텍스트)과
  core-worker domain(수집 write 컨텍스트, 엔티티 기반이 의도된 설계)의 관계를 기록.

### Phase 2 — statistics 유지보수성 (A-3, S-1~S-6)

사용자 요청으로 수행한 statistics 도메인 심층 검토 결과. 진단된 문제:

1. **계산 로직 배치 불일치** — 결과형은 도메인 `create()`에서 집계하지만, 순위형은 리포지토리에
   그룹핑·`averageRank` 계산이 인라인(`BattleRankStatisticsRepository.kt:35-41, 67-72`).
   `BrawlerRankStatistics`는 빈 홀더라 순위 계산의 단위 테스트가 불가능(도메인 테스트가 결과형에만 존재).
2. **Pair/Enemy 복붙 쌍둥이** — `BrawlerPairResultStatistics`/`BrawlerEnemyResultStatistics`가
   필드명 하나 차이로 51줄 동일. 2026-05/06 픽률 버그 정정을 양쪽 KDoc에 중복 기록해야 했음.
   db-core에 `BrawlerTrioBattleResultStatsEntity`가 이미 존재 — Trio 노출 시 세 번째 복사본 위험.
3. **파라미터 튜플 반복** — `(eventId?, tierRange, startDate, endDate)`가 13개 이상 메서드
   시그니처(Service·WithCache·Repository)와 6개 QueryDSL CustomImpl where 절에 반복.
4. **캐시 키 수작업 조립** — WithCache SpEL 키의 파라미터 문자열 연결. 파라미터 추가 시 키 갱신을
   빠뜨리면 컴파일러가 못 잡는 잘못된 캐시 히트 발생. (메서드별 `v{N}:` prefix는 의도된 컨벤션 — 유지)
5. **집계 idiom 불일치** — 도메인 매핑 후 `reduce(ResultCount::merge)` vs 인프라 embeddable
   `+` 합산 후 `toResultCount()` 변환이 혼재.

적용 규칙: **"조회·매핑은 리포지토리, 계산·집계는 도메인"**.

**A-3. 총 게임 수 계산 로직 단일화**
- 근거: 동일 수식(이벤트별 슬롯 합 ÷ `resultSlotsPerGame`)이 두 곳에 이중 구현 —
  도메인 `core/core-api/.../domain/statistics/BrawlerResultStatistics.kt:37-43`과
  리포지토리 `core/core-api/.../application/statistics/BattleResultStatisticsRepository.kt:190-207`.
  픽률 분모 정정 이력(2026-05-19, 06-06 KDoc)이 있는 민감 로직이라 재발 표면이 두 배.
- 수정: `GameSlots`(도메인)에 총 게임 수 계산을 두고 양쪽이 사용. `computeTotalGameCount()`는
  데이터 조회만 남기고 계산은 도메인 호출로 위임.
- 검증: 기존 통계 단위 테스트 + 도메인 계산 단위 테스트.

**S-1. 순위형 통계의 도메인 로직 회수**
- `BrawlerRankStatistics.create(counts)` / `BrawlerPairRankStatistics.create(...)`를 신설해
  그룹핑·평균 순위 계산을 리포지토리에서 도메인으로 이동. 결과형과 대칭 구조 확보, 단위 테스트 가능화.

**S-2. Pair/Enemy 결과 통계의 공통 집계 추출**
- 도메인 타입명은 유지(pair/enemy는 도메인 언어)하되 동일한 items 생성 로직(battleCount·pickRate·winRate
  산출)을 공용 함수로 추출. 픽률 분모 히스토리 KDoc도 한 곳으로 통합. 향후 Trio 노출 시 재사용.

**S-3. 통계 조회 조건 값 객체 도입 (`StatsQuery` 류)**
- `eventId?/brawlerId?/pairBrawlerId?/tierRange/기간`을 담는 도메인 값 객체로
  Controller→QueryComposer→Service→WithCache→Repository 시그니처 통일. Service는 `StatsDateRange`→기간
  해석을 유지한 채 객체를 완성해 하위로 전달.
- 캐시 키는 값 객체가 파생(예: `#query.cacheKey`) — 필드 추가 시 키가 자동 반영되어 문제 4를 구조적으로 차단.
  메서드별 `v{N}:` prefix 컨벤션은 유지하며, 키 형식 변경이므로 해당 메서드들의 v를 범프.
- 경계: db-core는 core-api 도메인을 모르므로 application Repository에서 JPA/QueryDSL 파라미터로 언팩.
- 적용 범위: 결과형·순위형 통계 조회 경로. 아이템 소유율 통계(`brawlerId`·`trophyRange`)와
  플레이어 통계는 파라미터 세트가 달라 제외.

**S-4. QueryDSL 공통 조건 헬퍼 (db-core)**
- 6개 CustomImpl에 반복되는 nullable eventId·tierRange·날짜 범위 predicate를 db-core 통계
  리포지토리 패키지 내 헬퍼로 추출.

**S-5. 집계 idiom 통일**
- "projection/entity → 도메인 Count 매핑 → 도메인에서 merge"로 단일화. S-1·S-2 작업에 포함해 진행.

**S-6. 도메인 단위 테스트 보강**
- Pair/Enemy/Rank `create()` 단위 테스트 신설(정정 이력이 있는 민감 계산 커버),
  S-3의 캐시 키 파생 로직 테스트.

### Phase 3 — 성능 위생 (C)

**C-1. 이벤트 맵 이름 검색 풀스캔 제거**
- 근거: `core/core-api/.../application/brawlstars/BattleEventRepository.kt`의 `searchByMapName()`이
  `message.content`에 `ContainingIgnoreCase`(중간 일치) 사용. message 인덱스는
  `(type, brawlstars_id, lang)`뿐이라 content 조건은 풀스캔.
- 수정: 이벤트 ~100개 규모이므로 캐시된 이벤트 목록에서 메모리 필터로 전환 —
  `BrawlerService.search`와 동일 패턴으로 일관성 확보. DB 인덱스 추가보다 단순.

**C-2. `GameSlotRepository.findAllByEventIds()` 루프 조회 제거**
- 근거: `core/core-api/.../application/statistics/GameSlotRepository.kt:21` — 이벤트별 `find()` 반복.
  캐시 히트 시 무해하나 콜드 캐시에서 이벤트 수만큼 조회 증폭, 통계 요청 1건이 이 경로를 2회 통과.
- 수정: 캐시된 이벤트 전체 조회(또는 배치 조회)를 활용한 벌크화.

**C-3. 이미지 전체 로드 후 메모리 필터 제거**
- 근거: `core/core-api/.../application/brawlstars/BrawlerRepository.kt:57-61, 105` —
  `brawlStarsImageJpaRepository.findAll()` 후 타입 필터.
- 수정: 타입 조건 쿼리 메서드(`findAllByType...`)로 교체.

**C-4. gzip 응답 압축 활성화**
- 근거: `server.compression` 미설정. 통계 JSON 응답 크기가 큼.
- 수정: core-api `application.yml`에 `server.compression.enabled: true` + `min-response-size` 설정.

**C-5. HTTP `Cache-Control` 헤더 부여**
- 근거: 모든 GET 응답에 캐시 제어 헤더 없음.
- 수정: 통계·브롤러·이벤트 GET에 짧은 `max-age`(+`stale-while-revalidate`).
  서버 캐시 TTL(Caffeine 5분/Redis 30분)과 일관된 값으로. 헤더 추가는 스펙 파괴 없음.

**C-6. Renewal 흐름 정리**
- 근거: `core/core-api/.../application/renewal/RenewalService.kt:54-76` +
  `RenewalRepository.kt` — 요청 1건이 같은 renewal 행을 3회 조회(find → request 내 find → toPending 내 find).
  `canRenew` 체크와 저장이 트랜잭션 분리된 check-then-act로, 경합 시 유니크 제약
  (`uk_renewal__brawlstarstag_type`, schema.sql:150) 위반이 500으로 노출될 수 있음.
- 수정: 조회 결과 재사용으로 왕복 축소, 제약 위반 예외를 `RENEWAL_ALREADY_IN_PROGRESS`로 매핑.
  SQS 발행 실패 시 REQUESTED로 남고 `canRenew`의 STALE_THRESHOLD로 자가 회복하는 현 구조는 유지.

### Phase 4 — 추가 채택 항목 (D)

**D-2. 캐시 메트릭 노출**
- 근거: Caffeine `recordStats()`가 이미 활성(`CacheConfig.kt:37`), monitoring 모듈(Actuator/Prometheus) 보유.
- 수정: Caffeine/Redis 캐시 히트율이 Prometheus 메트릭으로 노출되는지 확인, 미노출 시
  `CacheMetricsRegistrar` 연동. 이후 TTL 튜닝의 근거 데이터 확보 목적.

## 5. 기록만 하는 발견사항 (이번에 실행하지 않음)

- **read model의 빈약함은 문제 아님**: 조회 컨텍스트의 data class는 적절한 설계. 계산이 필요한
  통계 도메인은 이미 리치(`BrawlerResultStatistics.create`, `Renewal.canRenew`, `BrawlerTierScore.tier`).
  무리한 rich화는 하지 않는다 (YAGNI).
- **`FeedbackEntity.check/uncheck`의 도메인 로직**: core-admin 컨텍스트 소관(admin은 의도적으로
  엔티티 직접 사용). core-api의 `Feedback` read model엔 불필요.
- **core-worker Player의 내부 계층**: write 모델이 JPA 엔티티를 직접 보유하는 것은 수집 컨텍스트의
  실용적 선택으로 판정. 순수화(자체 타입 + 매퍼)는 worker 스코프의 후속 과제.
- **플레이어 브롤러 조회의 5회 배치 쿼리**(`PlayerRepository.kt:82-106`): 아이템 타입별 테이블이
  달라 이미 N+1 없는 적정 구조. 통합 실익 낮음.
- **Hikari pool=10**: 소규모에 적정. 부하 근거 없이 증설하지 않음 (tomcat 스레드와의 비율은
  규모 성장 시 재검토).
- **deep offset**: 실사용(무한 스크롤, 얕은 페이지)에서 해당 없음. keyset 전환은 규모 성장 시.
- **D-1. springdoc OpenAPI**: 프론트 동기화 안전망으로 유효하나 이번 범위에서 제외.
- **D-3. Renewal rate limit**: `canRenew` 쿨다운이 1차 방어 중. 남용 정황 발생 시 도입.
- **`domain/statistics` 하위 패키지 그룹화**(result/rank/item): 탐색성 개선 효과는 있으나 기계적
  이동이라 이번 리팩터링과 분리. Phase 2 이후 파일 구성을 보고 재판단.
- **PlayerBrawlerStats의 명시적 cache-aside**(`PlayerBrawlerStatsCacheStore`): `@Cacheable` 대신
  수작업 캐시를 쓰는 것은 core-worker와 캐시를 공유하기 위한 의도된 설계 — 문서화만.
- **Trio 통계 엔티티 미노출**: db-core에 `BrawlerTrioBattle*StatsEntity`가 존재하나 core-api 미노출.
  향후 노출 시 S-2의 공용 집계를 재사용할 것.
- **Enemy/Pair 통계 테이블의 날짜 필드명 차이**(`startBattleDate/endBattleDate` vs `battleDate`):
  사전 집계 단위 차이에서 온 것 — 스키마 변경 없이 유지.

## 6. 실행 계획

- 순서: Phase 1 → Phase 2 → Phase 3 → Phase 4. 각 Phase는 독립 커밋(Conventional Commits) 단위.
- Phase 2(statistics)는 동작 보존 리팩터링 — 기존 통계 응답이 리팩터링 전후 동일함을 기존
  Repository 컨텍스트 테스트 + 신설 도메인 단위 테스트로 확인. S-3의 캐시 키 형식 변경은
  컨벤션대로 v prefix 범프로 처리(구 키는 Redis TTL 30분 후 자연 소멸).
- 브랜치: Git 전략 준수 — `develop` 기준 feature 브랜치에서 작업, 완료 후 `develop` PR
  (`pr-test.yml`이 backend 테스트 수행).
- 테스트 게이트: 변경 영역마다 단위/컨텍스트 테스트 추가(내부 데이터 계층은 실 DB
  `CoreApiContextTest` + `CleanUp.all()`, 외부 시스템은 MockK, 시간은 `Clock.fixed`).
  전체 `ktlintCheck` + `unitTest`/`contextTest` 통과 확인.
- 프론트엔드 영향: 없음 (API 스펙 실질 변경 없음 — A-2는 비정상 호출만 400 처리).
- 버전: 구현 완료 시점에 `/release` 스킬로 core-api patch/minor 판단 (내부 리팩터링 중심이므로
  patch 예상).

## 7. 성공 기준

1. Phase 1~4의 19개 항목(A 3건, B 3건, S 6건, C 6건, D 1건)이 구현되고 코드 변경 항목은 각각 테스트로 검증됨
2. `domain` 패키지에 Spring 프레임워크 import가 남지 않음 (core-api 기준)
3. 기존 전체 테스트 스위트(`unitTest`, `contextTest`)와 ktlint 통과
4. API 응답 스키마 변화 없음 (프론트엔드 무수정)
5. backend CLAUDE.md에 bounded context 구조가 명문화됨
6. statistics 도메인에서 "조회·매핑은 리포지토리, 계산·집계는 도메인" 규칙이 성립 —
   순위형 포함 모든 통계 계산이 도메인 `create()`에 있고 단위 테스트로 커버됨
