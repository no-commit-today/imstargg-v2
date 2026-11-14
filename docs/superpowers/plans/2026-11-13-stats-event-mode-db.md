# 통계 수집 모드 DB 관리 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 통계 수집 모드 목록을 코드 enum `StatsEventMode` 에서 통계 쪽 DB 테이블 `stats_event_mode` 로 옮긴다. 관리자는 배포 없이 모드 편집 화면에서 수집 유형을 정한다.

**Architecture:**
- 모드마다 다르던 유형·팀 구성은 고정된 수집 유형 enum 넷(`StatsEventModeType`)으로 묶는다. DB 에는 모드 ID → 유형 대응만 둔다.
- core-worker 는 테이블을 메모리 스냅샷으로 들고 주기적으로 새로 읽는다.
- core-batch 는 테이블을 읽지 않고, 원본 전투의 팀 구성으로 유형을 정한다.
- core-api·core-admin 은 brawlstars 코드가 통계를 모르도록, 응답을 합치는 계층(컨트롤러)에서 유형을 붙인다.

**Tech Stack:** Kotlin, Spring Boot, Spring Data JPA, Caffeine(core-api 로컬 캐시), MockK·springmockk, H2(MODE=PostgreSQL, 테스트), PostgreSQL(운영), Next.js(관리자 프론트)

**Spec:** `docs/superpowers/specs/2026-11-13-stats-event-mode-db-design.md` (스펙 A: `docs/superpowers/specs/2026-11-03-stats-event-mode-whitelist-design.md`)

## Global Constraints

- 모든 경로는 저장소 루트 기준이다. Gradle 명령은 `imstargg-backend/` 에서 `./gradlew ...` 로 돌린다.
- 커밋
  - Conventional Commits(`feat(core-enum): …` 등)로 쓴다.
  - 메시지 끝에 `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` 를 넣는다.
  - 커밋 시각이 09~18시(KST) 안이면 PR·병합 전에 18시 이후나 08시 이전으로 날짜만 옮긴다(트리·메시지는 그대로).
  - 푸시와 PR 은 사용자가 요청할 때만 한다.
- 테스트 클래스에 `@Tag("context")` 를 쓰지 않는다. `CoreWorkerContextTest`·`CoreApiContextTest`·`CoreAdminContextTest` 가 이미 갖고 있다.
- 테스트 DB 는 H2 다. 엔티티로 테이블이 생기고 `schema.sql`·마이그레이션은 실행되지 않는다. CI 에는 Redis 가 없다.
- 마이그레이션은 운영에 수동으로 적용하고 멱등으로 쓴다(`create … if not exists`, `on conflict do nothing`). 파일명은 `V{구현일 yyMMdd}__create_stats_event_mode.sql` 이고, 머리말 형식은 `V261102b__add_status_to_stats_player.sql` 을 따른다. `schema.sql` 의 인덱스는 엔티티 `@Table` 에도 적는다.
- superdomain 컨벤션을 따른다.
  - core-api 는 application 리포지토리가 엔티티를 도메인으로 바꾸고, 트랜잭션은 리포지토리에 건다.
  - core-worker·core-admin·core-batch 는 JPA 엔티티를 직접 써도 된다.
  - 모킹은 외부 자원(DB·HTTP)에만 쓴다. 컨트롤러 테스트는 application 계층을 모킹해도 된다.
- 이름은 용어 사전(`docs/superglossary/glossary.md`)을 따른다.
  - 이 계획이 새로 쓰는 단어 `reload`(다시 읽기)·`match`(일치)는 사전에 없다. Task 1 과 Task 4 에서 `/superglossary:glossary` 로 등록한다.
  - `refresh` 는 플레이어 새로고침 전용이라 쓰지 않는다.
- 시드 12개(모드 ID → 유형). 테스트 픽스처도 이 값을 그대로 쓴다.
  - `RESULT_3VS3`: 0, 2, 3, 5, 17, 20, 22, 45, 48
  - `SOLO_SHOWDOWN`: 6
  - `DUO_SHOWDOWN`: 9
  - `TRIO_SHOWDOWN`: 38
- 관리자 프론트는 CI 테스트가 없다. `pnpm lint`·`pnpm build` 를 로컬에서 돌린다. Volta 의 `pnpm` 이 "env: node: No such file" 로 죽으면 `/opt/homebrew/bin/pnpm` 을 직접 부른다.
- 버전 올리기(`/release`)는 PR 단계에서 한다. 이 계획의 태스크는 버전을 건드리지 않는다.

## Review Focus

- **관리자가 구성이 다른 모드에 유형을 잘못 매긴다**(예: 5대5 인 31 에 `RESULT_3VS3`). 그 모드만 조용히 수집이 0 이 되어야 하고, 다른 모드나 통계를 오염시키면 안 된다. Task 4 가 테스트로 고정하고, Task 7 은 선택지에 구성을 보여 준다.
- **스냅샷을 새로 읽는 도중에 수집이 돈다.** 수집은 이전 목록이나 새 목록 중 하나만 봐야 하고, 반쯤 바뀐 목록을 보면 안 된다. Task 4 에서 불변 맵을 `@Volatile` 참조 하나로 통째로 바꾼다. 테스트로 재현하기 어려우니 리뷰에서 확인한다.
- **관리자가 같은 모드에 유형을 다시 지정한다.** 행은 하나로 남고 마지막 유형이어야 한다. Task 6 이 테스트로 고정한다.
- **원본에 들어온 뒤 모드를 끈다.** 배치는 모드 ID 와 관계없이 구성으로 판정해 계속 집계한다(설계 §3.5·§3.8a 의 의도). Task 3 이 테스트로 고정한다.
- **core-api 새 캐시 이름을 Caffeine 목록에 빠뜨린다.** 런타임에 "Cannot find cache" 로 이벤트·로테이션·전적 응답이 모두 실패한다. Task 5 의 컨텍스트 테스트가 실제 캐시를 거쳐 고정한다.

## File Structure

| 모듈 | 파일 | 책임 |
|---|---|---|
| core-enum | `core/enums/StatsEventModeType.kt` (생성) | 수집 유형 넷과 구성 → 유형 조회, `RESULT_SLOTS_PER_GAME` |
| core-enum | `core/enums/StatsEventMode.kt`·`StatsEventModeTest.kt` (Task 8 에서 삭제) | 옛 모드별 목록 |
| db-core | `statistics/StatsEventModeEntity.kt`, `statistics/repository/StatsEventModeJpaRepository.kt` (생성) | 테이블 매핑 |
| db-core | `resources/schema.sql`, `resources/db/migration/V{yyMMdd}__create_stats_event_mode.sql` | DDL·시드 |
| core-batch | `domain/statistics/StatsBotBattlePolicy.kt`·`StatsBotLobbyMonitor.kt`·`BrawlerBattleRankStatsAggregator.kt` | 구성 기반 유형 판정 |
| core-worker | `application/stats/StatsEventModeSnapshot.kt`, `schedule/StatsEventModeReloadScheduler.kt` (생성), `application/stats/StatsBattleAggregateApplier.kt` | 스냅샷과 수집 필터 |
| core-api | `domain/statistics/StatsEventModeTypes.kt`, `application/statistics/StatsEventModeRepository.kt`·`StatsEventModeService.kt` (생성) | 모드 ID → 유형 조회(캐시) |
| core-api | `domain/brawlstars/event/BattleEventMode.kt`, `application/brawlstars/BattleEventRepository.kt`, `api/controller/v1/response/*`, `api/controller/v1/BattleEventController.kt`·`BattleController.kt`, `config/CacheNames.kt`·`CacheConfig.kt`, `domain/statistics/StatsRate.kt` | brawlstars 에서 통계 제거, 응답에서 합치기 |
| core-admin | `application/statistics/StatsEventModeService.kt`, `controller/statistics/StatsEventModeController.kt`, `request/statistics/StatsEventModeTypeUpdateRequest.kt` (생성), `application/BattleEventService.kt`, `controller/BattleEventModeController.kt`, `response/BattleEventModeResponse.kt` | 유형 지정 API, 목록에 유형 붙이기 |
| admin-frontend | `lib/types.ts`, `lib/api.ts`, `app/event-modes/page.tsx`, `app/event-modes/[brawlStarsId]/edit/page.tsx` | 유형 표시·선택 |
| 문서·기타 | `imstargg-frontend/app/events/_components/modeStyles.ts`(주석), `docs/superdomain/DOMAIN.md`, `imstargg-backend/docs/stats-battle-data-strategy.md`, 스펙 A 머리, `UnregisteredRotationEventAlertTask.kt` KDoc | 문서 |

core-enum·db-core·core-batch·core-worker·core-api 의 Kotlin 경로 앞부분은 각각 다음과 같다.
- `imstargg-backend/core/core-enum/src/main/kotlin/com/imstargg/`
- `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/`
- `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/`
- `imstargg-backend/core/core-worker/src/main/kotlin/com/imstargg/core/worker/`
- `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/`

core-admin 은 `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/` 이다. 테스트는 같은 패키지의 `src/test` 에 둔다.

---

### Task 0: 시작 전 준비

- [ ] **Step 1: 설계·계획 문서를 먼저 커밋한다**(사용자가 커밋을 허락한 뒤). 이 두 파일이 커밋되지 않으면, 격리된 워크트리의 서브에이전트가 볼 수 없다. Task 8 의 커밋에 섞여 들어가지도 않게 한다.

```bash
git add docs/superpowers/specs/2026-11-13-stats-event-mode-db-design.md docs/superpowers/plans/2026-11-13-stats-event-mode-db.md
git commit -m "docs: 통계 수집 모드 DB 관리 설계와 구현 계획을 더한다"
```

- [ ] **Step 2: 브랜치 이름을 확인한다.** AGENTS.md 는 develop 으로 가는 PR 에 `feature/<설명>` 을 쓴다. 지금 브랜치(`claude/…`)를 PR 전에 `feature/stats-event-mode-db` 로 바꿀지 사용자에게 확인한다.

### Task 1: 수집 유형 enum (core-enum)

**Files:**
- Create: `imstargg-backend/core/core-enum/src/main/kotlin/com/imstargg/core/enums/StatsEventModeType.kt`
- Test: `imstargg-backend/core/core-enum/src/test/kotlin/com/imstargg/core/enums/StatsEventModeTypeTest.kt`

**Interfaces:**
- Produces:
  - `enum class StatsEventModeType(val resultType: BattleEventModeResultType, val teamCount: Int, val teamSize: Int)`: `RESULT_3VS3(RESULT, 2, 3)`, `SOLO_SHOWDOWN(RANK, 10, 1)`, `DUO_SHOWDOWN(RANK, 5, 2)`, `TRIO_SHOWDOWN(RANK, 4, 3)` 순서.
  - `fun matches(teamSizes: List<Int>): Boolean`: 팀 수가 `teamCount` 이고 모든 팀이 `teamSize` 명이면 true.
  - `companion object { const val RESULT_SLOTS_PER_GAME = 6; fun find(resultType: BattleEventModeResultType, teamSizes: List<Int>): StatsEventModeType? }`

- [ ] **Step 1: 실패하는 테스트를 쓴다**

```kotlin
class StatsEventModeTypeTest {
    @Test
    fun `유형과 구성은 넷으로 고정한다`() {
        assertThat(StatsEventModeType.entries)
            .extracting("resultType", "teamCount", "teamSize")
            .containsExactly(
                tuple(RESULT, 2, 3), tuple(RANK, 10, 1), tuple(RANK, 5, 2), tuple(RANK, 4, 3),
            )
    }

    @Test
    fun `결과형 유형의 1게임당 슬롯 수는 RESULT_SLOTS_PER_GAME 과 같다`() {
        assertThat(StatsEventModeType.entries.filter { it.resultType == RESULT })
            .allSatisfy { assertThat(it.teamCount * it.teamSize).isEqualTo(StatsEventModeType.RESULT_SLOTS_PER_GAME) }
    }

    @Test
    fun `팀 구성으로 유형을 찾는다`() {
        assertThat(StatsEventModeType.find(RESULT, listOf(3, 3))).isEqualTo(StatsEventModeType.RESULT_3VS3)
        assertThat(StatsEventModeType.find(RANK, List(10) { 1 })).isEqualTo(StatsEventModeType.SOLO_SHOWDOWN)
        assertThat(StatsEventModeType.find(RANK, List(5) { 2 })).isEqualTo(StatsEventModeType.DUO_SHOWDOWN)
        assertThat(StatsEventModeType.find(RANK, List(4) { 3 })).isEqualTo(StatsEventModeType.TRIO_SHOWDOWN)
    }

    @Test
    fun `어느 유형에도 맞지 않는 구성이면 null 이다`() {
        // 20인 솔로(75), 10팀 듀오(86), 5대5, 듀얼 1대1, 3팀, 3대2, 결과형 구성의 순위 전투, 솔로 9팀
        listOf(
            RANK to List(20) { 1 }, RANK to List(10) { 2 }, RESULT to listOf(5, 5), RESULT to listOf(1, 1),
            RESULT to listOf(3, 3, 3), RESULT to listOf(3, 2), RANK to listOf(3, 3), RANK to List(9) { 1 },
        ).forEach { (resultType, teamSizes) ->
            assertThat(StatsEventModeType.find(resultType, teamSizes)).describedAs("$resultType $teamSizes").isNull()
        }
    }
}
```

- [ ] **Step 2: 테스트를 돌려 실패를 확인한다**

Run: `./gradlew :core:core-enum:test --tests "com.imstargg.core.enums.StatsEventModeTypeTest"`
Expected: 컴파일 실패(`StatsEventModeType` 없음)

- [ ] **Step 3: `StatsEventModeType` 을 구현한다**

- `find` 는 `entries.find { it.resultType == resultType && it.matches(teamSizes) }` 다.
- 지금 `StatsEventMode` 에서 다음 둘을 옮겨 KDoc 을 쓴다.
  - 결정 이력. 2026-11 비중, 하키·아레나·농구를 넣은 이유, 와이프아웃·듀얼·5v5 를 뺀 이유, `battle.mode` 문자열을 믿지 않는 이유.
  - `RESULT_SLOTS_PER_GAME` 의 KDoc.
- 모드를 추가하는 절차는 설계 §3.1 의 네 단계로 새로 적는다.
- "모드 → 유형 대응은 DB `stats_event_mode` 에 있고, 관리자 화면에서 정한다" 를 첫 줄에 적는다.
- `StatsEventMode` 는 이 태스크에서 지우지 않는다(Task 8).

- [ ] **Step 4: 테스트를 돌려 통과를 확인한다**

Run: `./gradlew :core:core-enum:test`
Expected: PASS(`StatsEventModeTest` 포함)

- [ ] **Step 5: 용어 사전에 `match`(일치)를 등록한다** — `/superglossary:glossary` 로 "일치 | match | | | 팀 구성이 유형과 맞음" 을 더한다.

- [ ] **Step 6: 커밋한다**

```bash
git add imstargg-backend/core/core-enum docs/superglossary/glossary.md
git commit -m "feat(core-enum): 통계 수집 유형 StatsEventModeType 을 더한다"
```

### Task 2: 통계 수집 모드 테이블 (db-core)

**Files:**
- Create: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/StatsEventModeEntity.kt`
- Create: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/repository/StatsEventModeJpaRepository.kt`
- Modify: `imstargg-backend/infrastructure/db-core/src/main/resources/schema.sql` (`stats_player` 블록 뒤, 지금 356행 근처)
- Create: `imstargg-backend/infrastructure/db-core/src/main/resources/db/migration/V{yyMMdd}__create_stats_event_mode.sql`

**Interfaces:**
- Consumes: `StatsEventModeType` (Task 1)
- Produces:
  - `class StatsEventModeEntity(eventModeBrawlStarsId: Long, type: StatsEventModeType) : BaseTimeEntity()`: `@Table(name = "stats_event_mode", indexes = [Index(name = "uk_stats_event_mode__event_mode_brawlstars_id", columnList = "event_mode_brawlstars_id", unique = true)])`.
    - `val id: Long?`(`stats_event_mode_id`, IDENTITY)
    - `val eventModeBrawlStarsId: Long`(updatable=false)
    - `var type: StatsEventModeType`(`@Enumerated(STRING)`, length 25, `protected set`)
    - `fun updateType(type: StatsEventModeType)`
  - `interface StatsEventModeJpaRepository : JpaRepository<StatsEventModeEntity, Long> { fun findByEventModeBrawlStarsId(eventModeBrawlStarsId: Long): StatsEventModeEntity? }`

엔티티 단위 테스트는 두지 않는다(컨벤션: 라이브러리·DB 기능은 테스트하지 않는다). 매핑은 Task 4·5·6 의 컨텍스트 테스트가 H2 로 거친다.

- [ ] **Step 1: 엔티티와 JPA 리포지토리를 쓴다.** 형식은 `statistics/battle/StatsPlayerEntity.kt`·`StatsPlayerJpaRepository.kt` 를 따른다.

- [ ] **Step 2: `schema.sql` 에 테이블·인덱스·시드를 더한다.**
  - DDL 은 설계 §3.2 의 블록 그대로다.
  - 바로 뒤에 시드를 둔다: `insert into stats_event_mode (event_mode_brawlstars_id, type) values (0, 'RESULT_3VS3'), … , (38, 'TRIO_SHOWDOWN') on conflict (event_mode_brawlstars_id) do nothing;`. 값은 Global Constraints 의 12개다.
  - 시드 위 주석: "새 환경도 운영과 같은 12개로 시작한다. 비면 core-worker 가 아무것도 수집하지 않는다".

- [ ] **Step 3: 마이그레이션 파일을 쓴다.** 본문은 Step 2 와 같은 DDL + 시드다. 머리말에는 다음을 적는다.
  - 배경: 수집 모드를 배포 없이 관리. 설계 문서 경로.
  - 배포 순서: 앱보다 먼저. 스펙 A 와 같은 릴리즈라 V261103 은 앱 배포·확인 뒤(설계 §3.9).
  - 영향: 새 테이블이라 구 앱 영향 없음.
  - 롤백: 테이블은 남겨도 무해.
  - 멱등.

- [ ] **Step 4: 컴파일과 lint 를 확인한다**

Run: `./gradlew :infrastructure:db-core:compileKotlin :infrastructure:db-core:ktlintCheck`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: PostgreSQL 에서 멱등과 시드를 확인한다**

```bash
docker run -d --rm --name stats-event-mode-pg -e POSTGRES_PASSWORD=pg postgres:17-alpine
sleep 3
docker exec -i stats-event-mode-pg psql -U postgres -v ON_ERROR_STOP=1 < imstargg-backend/infrastructure/db-core/src/main/resources/db/migration/V{yyMMdd}__create_stats_event_mode.sql
docker exec -i stats-event-mode-pg psql -U postgres -v ON_ERROR_STOP=1 < imstargg-backend/infrastructure/db-core/src/main/resources/db/migration/V{yyMMdd}__create_stats_event_mode.sql
docker exec stats-event-mode-pg psql -U postgres -tAc "select string_agg(event_mode_brawlstars_id || '=' || type, ',' order by event_mode_brawlstars_id) from stats_event_mode"
```

Expected: 두 번 모두 오류가 없다. 마지막 줄은 `0=RESULT_3VS3,2=RESULT_3VS3,3=RESULT_3VS3,5=RESULT_3VS3,6=SOLO_SHOWDOWN,9=DUO_SHOWDOWN,17=RESULT_3VS3,20=RESULT_3VS3,22=RESULT_3VS3,38=TRIO_SHOWDOWN,45=RESULT_3VS3,48=RESULT_3VS3` 다.

이어서 같은 컨테이너에서 `drop table stats_event_mode;` 를 한다. 그다음 `schema.sql` 의 `stats_event_mode` 블록(DDL+시드)만 잘라 적용하고 같은 조회 결과가 나오는지 본다. 끝나면 `docker stop stats-event-mode-pg` 다.

- [ ] **Step 6: 커밋한다**

```bash
git add imstargg-backend/infrastructure/db-core
git commit -m "feat(db-core): 통계 수집 모드 테이블 stats_event_mode 와 시드 12개를 더한다"
```

### Task 3: 봇 판정·순위 집계를 팀 구성으로 (core-batch)

**Files:**
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/domain/statistics/StatsBotBattlePolicy.kt` (KDoc 머리, `resultTeams`, `rankLobby`)
- Modify: `…/domain/statistics/StatsBotLobbyMonitor.kt` (`ModeSpec`, `record`, `SPECS`, `MODES`)
- Modify: `…/domain/statistics/BrawlerBattleRankStatsAggregator.kt` (KDoc 의 모드 항목, `aggregate` 첫 줄)
- Test: `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/domain/statistics/StatsBattleFixtures.kt`·`StatsBotBattlePolicyTest.kt`·`StatsBotLobbyMonitorTest.kt`·`BrawlerBattleRankStatsAggregatorTest.kt`, `…/job/statistics/battlerank/BrawlerBattleRankStatsTaskTest.kt`

**Interfaces:**
- Consumes: `StatsEventModeType.find(resultType, teamSizes)`, `RESULT_3VS3`·`SOLO_SHOWDOWN`·`DUO_SHOWDOWN`·`TRIO_SHOWDOWN` (Task 1)
- Produces: 테스트 픽스처
  - `showdown(modeType: StatsEventModeType, teamTrophies: List<List<Int>>, battleRank: Int = 1, type: String = BattleType.RANKED.code)`. 지금 `mode: StatsEventMode` 자리(첫 인자)에 유형을 받는다. 모드 ID 는 픽스처 안의 `SOLO_SHOWDOWN → 6, DUO_SHOWDOWN → 9, TRIO_SHOWDOWN → 38` 으로 정한다. 호출부는 첫 인자를 위치로 넘기므로 `StatsEventMode.X` → `StatsEventModeType.X` 만 바꾸면 된다.
  - `statsResultBattle`·`threeVsThree` 의 기본 모드 ID 는 `0L`, `statsRankBattle` 은 `6L` 이다.

- [ ] **Step 1: 테스트를 새 기대로 고친다(실패하는 테스트)**
  - 모든 `StatsEventMode.X` 사용을 `StatsEventModeType.X` 로 바꾼다. `StatsEventMode.GEM_GRAB.modeBrawlStarsId` 같은 ID 참조는 숫자로 바꾼다.
  - `StatsBotBattlePolicyTest.ThreeVsThree` 의 `결과형 허용 모드가 아니면 false 다` 를 다음으로 바꾼다.

    ```kotlin
    @Test
    fun `2×3 결과형이면 모드 ID 와 관계없이 판정 범위에 든다`() {
        // 수집 모드 목록은 DB 에 있고 배치는 읽지 않는다. 원본에 들어온 전투는 팀 구성으로 판정한다
        listOf(0L, 25L, 999L).forEach { modeId ->
            val battle = threeVsThree(listOf(1000, 1100, 1200), listOf(1050, 1080, 1090), eventModeBrawlStarsId = modeId)
            assertThat(P.appliesTo(battle)).isTrue()
            assertThat(P.isBotBattle(battle)).isTrue()
        }
    }
    ```

  - `Showdown` 의 `순위형 허용 모드는 모두 판정 범위에 들고…` 는 다음으로 바꾼다. 테스트 이름의 "허용 모드" 를 "유형" 으로 고친다.
    - 반복 대상: `StatsEventModeType.entries.filter { it.resultType == RANK }`
    - 전투: `showdown(type, List(type.teamCount) { List(type.teamSize) { 1000 } })`
  - `트로피전 쇼다운의 정해진 팀 구성이 아니면 false 다`
    - 목록에서 `notShowdown` 을 뺀다.
    - 다음 둘을 더한다.
      - `statsRankBattle(teams = (1..20).map { listOf(statsPlayer(it)) }, eventModeBrawlStarsId = 75)`
      - `statsRankBattle(teams = (0 until 10).map { t -> listOf(statsPlayer(2 * t + 1), statsPlayer(2 * t + 2)) }, eventModeBrawlStarsId = 86)`
  - 새 테스트 `순위형 판정은 모드 ID 가 아니라 팀 구성으로 정한다`
    - 전투: `statsRankBattle(duo(*clustered).teams, eventModeBrawlStarsId = 0L)`. `clustered` 는 같은 클래스 안의 기존 배열과 같다.
    - 기대: `appliesTo` 와 `isBotLobby` 가 모두 true.
  - `StatsBotLobbyMonitorTest`
    - `순위형 허용 모드는 모두 지표에 세진다` 를 위와 같은 방식으로 유형 기준으로 바꾼다.
    - 나머지 `showdown(StatsEventMode.X, …)` 는 유형으로 바꾼다.
  - `BrawlerBattleRankStatsAggregatorTest`
    - 모드 75 테스트
      - 75 전투: `statsRankBattle(teams = (101..120).map { listOf(statsPlayer(it)) }, battleRank = 19, eventModeBrawlStarsId = 75)`
      - 같은 날짜의 솔로 전투: `showdown(SOLO_SHOWDOWN, listOf(listOf(1500)) + List(9) { listOf(2100) })`
      - 기대: `containsOnly(1L)`
    - 모드 86 테스트: 위의 10×2 구성으로 바꾸고 `isEmpty()` 다.
    - 새 테스트 `쇼다운 구성이면 모드 ID 와 관계없이 집계한다`: `showdown(SOLO_SHOWDOWN, …)` 의 팀으로 `eventModeBrawlStarsId = 999` 인 전투를 만들고 `isNotEmpty()` 다.
  - `BrawlerBattleRankStatsTaskTest.rankBattle()`: 팀을 10×1 로 바꾼다. 수집자 `#TAG1`(index 1)과 상대 9명이고, 상대는 index 2~10, 트로피 2100 이다. 단순화된 1팀 1명 구성은 이제 집계에서 빠지기 때문이다.

- [ ] **Step 2: 테스트를 돌려 실패를 확인한다**

Run: `./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.domain.statistics.*" --tests "com.imstargg.core.batch.job.statistics.battlerank.*"`
Expected: 컴파일 실패(픽스처 시그니처) 또는 새 기대에서 FAIL

- [ ] **Step 3: 운영 코드를 바꾼다**
  - **`StatsBotBattlePolicy.resultTeams`**
    - `StatsEventMode.find` 와 resultType·2×3 검사 세 줄을 `StatsEventModeType.find(BattleEventModeResultType.RESULT, battle.teams.map { it.size }) ?: return null` 하나로 바꾼다.
    - `RESULT_TEAM_SIZE` 는 창 계산에 남는다.
  - **`StatsBotBattlePolicy.rankLobby`**
    - `val type = StatsEventModeType.find(BattleEventModeResultType.RANK, battle.teams.map { it.size }) ?: return null` 로 유형을 구한다.
    - 임계값은 `when (type)` 으로 고른다. `SOLO_SHOWDOWN`·`DUO_SHOWDOWN`·`TRIO_SHOWDOWN` 은 각 상수, `RESULT_3VS3 -> return null` 이고 `else` 는 두지 않는다. 그래야 유형을 더하면 컴파일 오류가 난다.
    - 따로 하던 구성 검사 줄은 지운다.
  - **`StatsBotBattlePolicy` KDoc**
    - "결과형 허용 모드" 를 "2×3 결과형 구성" 으로, 쇼다운 적용 범위를 "솔로·듀오·트리오 쇼다운 유형의 팀 구성" 으로 고친다.
    - 결정 이력에 한 줄을 더한다: "모드 ID 가 아니라 팀 구성으로 유형을 정한다. 수집 모드 목록은 DB 에 있고 배치는 읽지 않는다(2026-11-13 통계 수집 모드 DB 관리 설계 §3.5)".
  - **`StatsBotLobbyMonitor`**
    - `ModeSpec.mode` 를 `type: StatsEventModeType` 로 바꾼다.
    - `record` 는 `StatsEventModeType.find(RANK, battle.teams.map { it.size }) ?: return` 로 유형을 구한다.
    - `SPECS` 의 키와 `MODES` 도 유형으로 바꾼다.
  - **`BrawlerBattleRankStatsAggregator`**
    - `aggregate` 첫 줄을 `if (StatsEventModeType.find(BattleEventModeResultType.RANK, battle.teams.map { it.size }) == null) return` 로 바꾼다.
    - KDoc 의 모드 항목에서 "[StatsEventMode] 의 순위형 모드만" 을 "솔로·듀오·트리오 쇼다운 유형의 팀 구성인 전투만" 으로 고친다. 75·86 설명은 남긴다.

- [ ] **Step 4: 테스트를 돌려 통과를 확인한다**

Run: `./gradlew :core:core-batch:test :core:core-batch:ktlintCheck`
Expected: PASS. `rg "StatsEventMode\b" imstargg-backend/core/core-batch/src` 에는 `UnregisteredRotationEventAlertTask.kt` 의 KDoc 만 남는다(Task 8).

- [ ] **Step 5: 커밋한다**

```bash
git add imstargg-backend/core/core-batch
git commit -m "refactor(core-batch): 봇 판정과 순위 집계가 수집 모드 목록 대신 팀 구성으로 유형을 정한다"
```

### Task 4: 수집 필터를 DB 스냅샷으로 (core-worker)

**Files:**
- Create: `imstargg-backend/core/core-worker/src/main/kotlin/com/imstargg/core/worker/application/stats/StatsEventModeSnapshot.kt`
- Create: `imstargg-backend/core/core-worker/src/main/kotlin/com/imstargg/core/worker/schedule/StatsEventModeReloadScheduler.kt`
- Modify: `…/application/stats/StatsBattleAggregateApplier.kt` (생성자, `findStatsEventMode`·`canCreateRankBattle`·`canCreateResultBattle`, 지금 58~98행)
- Modify: `imstargg-backend/core/core-worker/src/main/resources/application.yml` (첫 문서의 `app.schedule`, 지금 43행)
- Test: `imstargg-backend/core/core-worker/src/test/kotlin/com/imstargg/core/worker/application/stats/StatsBattleAggregateApplierTest.kt` (수정), `…/application/stats/StatsEventModeSnapshotTest.kt`·`…/schedule/StatsEventModeReloadSchedulerTest.kt` (생성)

**Interfaces:**
- Consumes: `StatsEventModeType.matches`·`resultType` (Task 1), `StatsEventModeEntity`·`StatsEventModeJpaRepository` (Task 2)
- Produces:
  - `@Component class StatsEventModeSnapshot(private val statsEventModeJpaRepository: StatsEventModeJpaRepository)`
    - `fun find(eventModeBrawlStarsId: Long): StatsEventModeType?`
    - `fun reload()`
      - 모든 행을 읽어 `Map<Long, StatsEventModeType>` 로 만들고, `@Volatile` 참조를 한 번에 바꾼다.
      - 읽기가 실패하면 예외를 그대로 던지고 참조는 바꾸지 않는다.
      - 결과가 비면 WARN 을 남긴다.
      - 내용이 바뀌면(첫 읽기 포함) INFO 로 모드 수와 `id=TYPE` 목록을 남긴다.
    - `@PostConstruct fun init()`: `reload()` 를 부른다. 실패하면 기동이 실패한다.
  - `@Component @ConditionalOnProperty(prefix = "app.schedule.stats-event-mode-reload", name = ["enabled"], havingValue = "true") class StatsEventModeReloadScheduler(private val statsEventModeSnapshot: StatsEventModeSnapshot)`
    - `@Scheduled(fixedDelayString = "\${app.schedule.stats-event-mode-reload.interval}", initialDelayString = "\${app.schedule.stats-event-mode-reload.interval}") fun reload()`
    - `Exception` 을 잡아 WARN("통계 수집 모드 다시 읽기 실패, 이전 목록을 유지한다")만 남긴다.
  - 로거는 `KotlinLogging.logger {}` 다(`AsyncExceptionHandler.kt` 와 같다).

- [ ] **Step 1: 실패하는 테스트를 쓴다**
  - `StatsBattleAggregateApplierTest`
    - 생성자에 `StatsEventModeJpaRepository`·`StatsEventModeSnapshot` 을 더한다.
    - `@BeforeEach` 에서 Global Constraints 의 시드 12개를 `saveAll` 하고 `snapshot.reload()` 를 부른다. 기존 테스트 기대는 그대로 둔다.
  - 같은 클래스에 `@Nested inner class \`apply 수집 모드 목록\``. 전투는 기존 헬퍼 `createResultBattle(battleKey, battleAt = recentBattleAt(), playerBrawlStarsTag, teams, modeBrawlStarsId)` 로 만든다. 기본 `teams` 는 `createTeam3v3()` 이다.
    - `목록에서 지운 모드는 다시 읽은 뒤부터 저장하지 않는다`: 모드 20 행을 지우고 `reload()` → 모드 20 결과 전투 → `statsResultBattleRepository.count() == 0`
    - `목록에 더한 모드는 다시 읽은 뒤부터 저장한다`: `(25, RESULT_3VS3)` 저장, `reload()` → 모드 25 결과 전투 → count 1
    - `다시 읽기 전에는 이전 목록으로 판정한다`: `(25, RESULT_3VS3)` 저장, `reload()` 없음 → count 0
    - `유형 구성과 다른 모드에 유형을 매기면 그 모드의 전투만 저장하지 않는다`
      - `(31, RESULT_3VS3)` 저장, `reload()`
      - `apply` 에 두 전투를 함께 넘긴다.
        - `createResultBattle(battleKey = "k31", …, playerBrawlStarsTag = "#T0P0", teams = createTeams(2, 5), modeBrawlStarsId = 31L)`
        - `createResultBattle(battleKey = "k20", …, modeBrawlStarsId = 20L)`
      - 기대: `statsResultBattleRepository.findAll().map { it.eventModeBrawlStarsId } == listOf(20L)`
  - `StatsEventModeSnapshotTest`(단위, `mockk<StatsEventModeJpaRepository>()`)
    - `읽은 행으로 모드 ID 의 유형을 찾는다`: findAll → `[StatsEventModeEntity(20, RESULT_3VS3), StatsEventModeEntity(6, SOLO_SHOWDOWN)]` → `find(20) == RESULT_3VS3`, `find(6) == SOLO_SHOWDOWN`, `find(25) == null`
    - `다시 읽기에 실패하면 예외를 던지고 이전 목록을 그대로 쓴다`: 첫 findAll 은 위 목록, 둘째는 `RuntimeException` → `assertThrows<RuntimeException> { reload() }` → `find(20) == RESULT_3VS3`
    - `init 에서 읽기에 실패하면 예외가 밖으로 나간다`: findAll 이 예외 → `assertThrows { init() }`
    - `빈 목록을 읽으면 아무 모드도 찾지 않는다`: 시드 뒤 빈 목록으로 `reload()` → `find(20) == null`
  - `StatsEventModeReloadSchedulerTest`(단위): 진짜 `StatsEventModeSnapshot` 에 모킹한 리포지토리를 쓴다. 첫 읽기 성공 뒤 둘째 읽기가 예외 → `scheduler.reload()` 가 예외 없이 끝나고 `snapshot.find(20) == RESULT_3VS3`.

- [ ] **Step 2: 테스트를 돌려 실패를 확인한다**

Run: `./gradlew :core:core-worker:test --tests "com.imstargg.core.worker.application.stats.*" --tests "com.imstargg.core.worker.schedule.*"`
Expected: 컴파일 실패(`StatsEventModeSnapshot` 없음)

- [ ] **Step 3: 스냅샷·스케줄러를 쓰고 적용기를 바꾼다**
  - **적용기**
    - 생성자에 `private val statsEventModeSnapshot: StatsEventModeSnapshot` 을 더한다.
    - `findStatsEventMode` 는 `StatsEventModeType?` 를 돌려준다.
      - `statsEventModeSnapshot.find(event.modeBrawlStarsId) ?: return null`
      - `if (!type.matches(battle.teams.map { it.size })) return null`
    - 두 `canCreate…` 는 `?.resultType` 으로 비교한다.
    - 함수 이름은 `findStatsEventModeType` 으로 바꾸고 KDoc 의 modeId 설명은 유지한다.
  - **`application.yml`** 첫 문서 `app.schedule` 아래에 다음을 더한다.

    ```yaml
        stats-event-mode-reload:
          enabled: true
          # 관리자가 바꾼 통계 수집 모드가 반영되기까지의 최대 지연. core-api 로컬 캐시 만료(5분)와 같다
          interval: 5m
    ```

    테스트는 `src/test/resources/application.yml` 이 `app.schedule` 을 두지 않아 스케줄러가 생기지 않는다. 그대로 둔다.
  - 스냅샷 KDoc 에 설계 §3.3 의 동작을 적는다. 기동 실패, 다시 읽기 실패 시 유지, 빈 목록 경고, 반영 지연이다.

- [ ] **Step 4: 테스트를 돌려 통과를 확인한다**

Run: `./gradlew :core:core-worker:test :core:core-worker:ktlintCheck`
Expected: PASS. `rg "StatsEventMode\b" imstargg-backend/core/core-worker/src` 결과가 없다.

- [ ] **Step 5: 용어 사전에 `reload`(다시 읽기)를 등록한다** — "다시 읽기 | reload | | | 메모리에 든 기준 데이터를 DB 에서 다시 읽음. 새로고침(refresh)과 다르다".

- [ ] **Step 6: 커밋한다**

```bash
git add imstargg-backend/core/core-worker docs/superglossary/glossary.md
git commit -m "feat(core-worker): 통계 수집 모드를 DB 스냅샷에서 찾고 5분마다 다시 읽는다"
```

### Task 5: 이벤트 모드 응답에 유형을 붙이는 위치를 옮긴다 (core-api)

**Files:**
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/StatsEventModeTypes.kt`
- Create: `…/core/application/statistics/StatsEventModeRepository.kt`·`StatsEventModeService.kt`
- Modify: `…/core/config/CacheNames.kt`·`CacheConfig.kt`(Caffeine 이름 목록)
- Modify: `…/core/domain/brawlstars/event/BattleEventMode.kt`(`resultType` 삭제), `…/core/application/brawlstars/BattleEventRepository.kt`(89·112행 근처 `resultType = …` 와 import 삭제)
- Modify: `…/core/api/controller/v1/response/BattleEventModeResponse.kt`·`BattleEventResponse.kt`·`RotationBattleEventResponse.kt`·`BattleResponse.kt`
- Modify: `…/core/api/controller/v1/BattleEventController.kt`·`BattleController.kt`
- Modify: `…/core/domain/statistics/StatsRate.kt`(import·KDoc 의 `StatsEventMode.RESULT_SLOTS_PER_GAME` → `StatsEventModeType.RESULT_SLOTS_PER_GAME`)
- Test: `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/api/controller/v1/BattleEventControllerTest.kt`(수정), `…/core/api/controller/v1/response/BattleEventModeResponseTest.kt`·`…/core/application/statistics/StatsEventModeRepositoryTest.kt`(생성)

**Interfaces:**
- Consumes: `StatsEventModeType` (Task 1), `StatsEventModeJpaRepository`·`StatsEventModeEntity` (Task 2)
- Produces:
  - `class StatsEventModeTypes(private val typesByModeId: Map<BrawlStarsId, StatsEventModeType>) { fun resultTypeOf(modeId: BrawlStarsId): BattleEventModeResultType? }`(domain, 순수 Kotlin)
  - `@Repository class StatsEventModeRepository(private val statsEventModeJpaRepository: StatsEventModeJpaRepository)`
    - `@Cacheable(cacheNames = [CacheNames.STATS_EVENT_MODE], key = "'all'") @Transactional(readOnly = true) fun findAll(): StatsEventModeTypes`
  - `@Service class StatsEventModeService(private val statsEventModeRepository: StatsEventModeRepository) { fun getTypes(): StatsEventModeTypes }`
  - `CacheNames.STATS_EVENT_MODE = "statsEventMode"`. `CaffeineCacheManager(…)` 이름 목록에 더한다.
  - 응답 팩토리
    - `BattleEventModeResponse.of(mode: BattleEventMode, statsEventModeTypes: StatsEventModeTypes)`
    - `BattleEventResponse.of(event: BattleEvent, statsEventModeTypes: StatsEventModeTypes)`
    - `RotationBattleEventResponse.of(rotationBattleEvent: RotationBattleEvent, statsEventModeTypes: StatsEventModeTypes)`
    - `BattleResponse.of(battle: Battle, statsEventModeTypes: StatsEventModeTypes)`
  - 두 컨트롤러는 생성자로 `StatsEventModeService` 를 받는다. 요청마다 `getTypes()` 를 한 번 불러 팩토리에 넘긴다.
  - `BattleEventMode(id, name, iconImageUrl)` 에서 `resultType` 이 빠진다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**
  - `BattleEventModeResponseTest`(단위)
    - `types = StatsEventModeTypes(mapOf(BrawlStarsId(20) to RESULT_3VS3, BrawlStarsId(6) to SOLO_SHOWDOWN))`
    - `수집 모드는 그 유형의 resultType 을 낸다`: 모드 20 → `RESULT`, 모드 6 → `RANK`
    - `수집하지 않는 모드는 resultType 이 null 이다`: 모드 25 → `null`
  - `StatsEventModeRepositoryTest`(`CoreApiContextTest`, 실제 H2 와 실제 캐시)
    - `CleanUp` 으로 뒷정리한다.
    - 컨텍스트를 다른 테스트와 나눠 쓰므로 `@BeforeEach`·`@AfterEach` 에서 `cacheManager.getCache(CacheNames.STATS_EVENT_MODE)?.clear()` 를 부른다(`BrawlerServiceTest` 39행과 같은 방식).
    - `수집 모드 목록을 캐시를 거쳐 읽는다`
      - `(20, RESULT_3VS3)`·`(6, SOLO_SHOWDOWN)` 저장
      - `sut.findAll().resultTypeOf(BrawlStarsId(20)) == RESULT`, `resultTypeOf(BrawlStarsId(6)) == RANK`, `resultTypeOf(BrawlStarsId(25)) == null`
      - 캐시 이름이 Caffeine 목록에 없으면 이 호출이 `IllegalArgumentException` 으로 실패한다.
  - `BattleEventControllerTest`
    - `@MockkBean(relaxed = true) private lateinit var statsEventModeService: StatsEventModeService` 를 더한다. 기본으로 `every { getTypes() } returns StatsEventModeTypes(mapOf(BrawlStarsId(20) to RESULT_3VS3))` 다.
    - 기존 테스트의 `BattleEventMode(... resultType = null)` 에서 인자를 뺀다(모드 25 → 여전히 `"resultType":null`).
    - 새 테스트 `수집 모드 이벤트는 로테이션 응답에도 resultType 을 낸다`
      - `battleEventService.getCurrentRotationEvents()` 가 모드 20 이벤트 하나를 담은 `RotationBattleEvent` 를 돌려준다.
      - `/v1/events/rotation` → `string(containsString("\"resultType\":\"RESULT\""))`

- [ ] **Step 2: 테스트를 돌려 실패를 확인한다**

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.api.controller.v1.*" --tests "com.imstargg.core.application.statistics.StatsEventModeRepositoryTest"`
Expected: 컴파일 실패

- [ ] **Step 3: 구현한다**
  - **추가**: 도메인·리포지토리·서비스를 더하고, 캐시 이름을 `CacheNames`·`CacheConfig` 에 등록한다.
  - **brawlstars 에서 지우기**
    - `BattleEventMode.resultType` 과 `BattleEventRepository` 의 `StatsEventMode` 사용을 지운다.
    - `src/main` 과 `src/test` 의 `BattleEventMode(` 생성 지점에서 `resultType` 인자를 지운다(`rg "BattleEventMode\(" imstargg-backend/core/core-api`).
  - **응답 팩토리와 컨트롤러**: 위 시그니처대로 바꾼다.
  - **KDoc**
    - 응답 `BattleEventModeResponse.resultType` 의 "통계 수집 유형. 통계를 모으지 않는 모드는 본래 결과형이어도 null." 은 유지한다.
    - `BattleEventModeResponse.of` KDoc 에 "brawlstars 의 이벤트 모델은 통계를 모른다. 유형은 응답을 합치는 이곳에서 붙인다(DOMAIN.md: 응답을 합치는 계층은 도메인 사이 의존으로 세지 않는다)" 를 적는다.

- [ ] **Step 4: 테스트를 돌려 통과를 확인한다**

Run: `./gradlew :core:core-api:test :core:core-api:ktlintCheck`
Expected: PASS. `rg "StatsEventMode\b" imstargg-backend/core/core-api/src` 결과가 없다.

- [ ] **Step 5: 커밋한다**

```bash
git add imstargg-backend/core/core-api
git commit -m "refactor(core-api): 이벤트 모드 응답의 통계 수집 유형을 brawlstars 가 아니라 응답을 합치는 컨트롤러에서 붙인다"
```

### Task 6: 관리자 유형 지정 API (core-admin)

**Files:**
- Create: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/application/statistics/StatsEventModeService.kt`
- Create: `…/core/admin/controller/statistics/StatsEventModeController.kt`
- Create: `…/core/admin/request/statistics/StatsEventModeTypeUpdateRequest.kt`
- Modify: `…/core/admin/application/BattleEventService.kt`(`findAllModes` 의 `StatsEventMode` 사용 삭제), `…/core/admin/response/BattleEventModeResponse.kt`, `…/core/admin/controller/BattleEventModeController.kt`(`getAll`)
- Test: `imstargg-backend/core/core-admin/src/test/kotlin/com/imstargg/core/admin/application/statistics/StatsEventModeServiceTest.kt`(생성), `…/application/BattleEventServiceTest.kt`(85행 근처 수정), `…/controller/BattleEventModeControllerTest.kt`(생성)

**Interfaces:**
- Consumes: `StatsEventModeType` (Task 1), `StatsEventModeEntity`·`StatsEventModeJpaRepository` (Task 2), `BattleEventModeJpaRepository.findByBrawlStarsId`(기존)
- Produces:
  - `@Service class StatsEventModeService(statsEventModeJpaRepository, battleEventModeJpaRepository)`
    - `@Transactional(readOnly = true) fun findAllTypes(): Map<Long, StatsEventModeType>`
    - `@Transactional fun updateType(eventModeBrawlStarsId: Long, type: StatsEventModeType?)`
      - 모드가 `battle_event_mode` 에 없으면 `ResponseStatusException(HttpStatus.NOT_FOUND, …)` 이다.
      - null 이면 행을 지운다(없으면 아무것도 하지 않는다).
      - 값이 있으면 행이 있을 때 `updateType`, 없을 때 새로 저장한다.
  - `data class StatsEventModeTypeUpdateRequest(val type: StatsEventModeType?)`
  - `PUT /event-modes/{brawlStarsId}/stats-type` 본문 `{"type": "RESULT_3VS3" | … | null}` → 200 빈 응답
  - `BattleEventModeResponse.resultType` 대신 `statsType: StatsEventModeType? = null` 을 둔다. KDoc 은 "통계 수집 유형. 없으면 수집하지 않는다. 응답을 합치는 컨트롤러가 채운다" 다.
  - `BattleEventModeController` 는 생성자로 `StatsEventModeService` 를 받는다. `getAll()` 은 `val types = statsEventModeService.findAllTypes()` 를 구한 뒤 `battleEventService.findAllModes().map { it.copy(statsType = types[it.brawlStarsId]) }` 를 돌려준다.

- [ ] **Step 1: 실패하는 테스트를 쓴다**
  - `StatsEventModeServiceTest`(`CoreAdminContextTest`, 실제 H2, `CleanUp`). 모드 행 `BattleEventModeEntity(20, "knockout")` 을 먼저 저장한다.
    - `유형을 지정하면 행이 생긴다`: `updateType(20, RESULT_3VS3)` → `findAllTypes() == mapOf(20L to RESULT_3VS3)`
    - `같은 모드에 다시 지정하면 행은 하나이고 마지막 유형이다`: RESULT_3VS3 뒤 SOLO_SHOWDOWN → `statsEventModeJpaRepository.count() == 1`, `findAllTypes()[20] == SOLO_SHOWDOWN`
    - `null 로 지정하면 행을 지운다`: 지정 뒤 `updateType(20, null)` → `findAllTypes().isEmpty()`
    - `행이 없을 때 null 로 지정해도 오류가 없다`
    - `등록되지 않은 모드면 404 다`: `assertThrows<ResponseStatusException> { updateType(999, RESULT_3VS3) }.statusCode == HttpStatus.NOT_FOUND`
  - `BattleEventServiceTest`: `모드 목록의 resultType 은 통계 수집 유형이다` 를 지우고, 그 기대는 아래 컨트롤러 테스트로 옮긴다. 서비스는 이제 유형을 모른다.
  - `BattleEventModeControllerTest`(MockMvc, `AdminFeedbackControllerTest` 의 설정을 따른다. 실제 서비스·H2)
    - `모드 목록에 통계 수집 유형을 붙인다`
      - 모드 0·6·24 를 저장하고, `stats_event_mode` 에 `(0, RESULT_3VS3)`·`(6, SOLO_SHOWDOWN)` 를 저장한다.
      - `GET /event-modes` → 모드 0 은 `"statsType":"RESULT_3VS3"`, 6 은 `"SOLO_SHOWDOWN"`, 24 는 `"statsType":null`
    - `유형 지정 API 는 본문의 유형으로 행을 바꾼다`: 먼저 `BattleEventModeEntity(20, "knockout")` 을 저장한다(없으면 404). `PUT /event-modes/20/stats-type` 본문 `{"type":"RESULT_3VS3"}` → 200, 행 생김. 본문 `{"type":null}` → 200, 행 없음.

- [ ] **Step 2: 테스트를 돌려 실패를 확인한다**

Run: `./gradlew :core:core-admin:test --tests "com.imstargg.core.admin.application.*" --tests "com.imstargg.core.admin.controller.*"`
Expected: 컴파일 실패

- [ ] **Step 3: 구현한다.** 위 시그니처대로 쓰고, `BattleEventService` 에서 `StatsEventMode` import 와 `resultType = …` 를 지운다.

- [ ] **Step 4: 테스트를 돌려 통과를 확인한다**

Run: `./gradlew :core:core-admin:test :core:core-admin:ktlintCheck`
Expected: PASS. `rg "StatsEventMode\b" imstargg-backend/core/core-admin/src` 결과가 없다.

- [ ] **Step 5: 커밋한다**

```bash
git add imstargg-backend/core/core-admin
git commit -m "feat(core-admin): 이벤트 모드의 통계 수집 유형을 지정하는 API 와 목록 표시를 더한다"
```

### Task 7: 관리자 화면 (imstargg-admin-frontend)

**Files:**
- Modify: `imstargg-admin-frontend/lib/types.ts`(79~87행), `imstargg-admin-frontend/lib/api.ts`(Event Mode 묶음, 219~238행)
- Modify: `imstargg-admin-frontend/app/event-modes/page.tsx`(30행 `STATS_TYPE_LABELS`, 166행 셀)
- Modify: `imstargg-admin-frontend/app/event-modes/[brawlStarsId]/edit/page.tsx`

**Interfaces:**
- Consumes: Task 6 의 `GET /event-modes` 응답 `statsType`, `PUT /event-modes/{brawlStarsId}/stats-type`
- Produces:
  - `lib/types.ts`
    - `export type StatsEventModeType = "RESULT_3VS3" | "SOLO_SHOWDOWN" | "DUO_SHOWDOWN" | "TRIO_SHOWDOWN";`
    - `export const STATS_EVENT_MODE_TYPE_LABELS: Record<StatsEventModeType, string>`
      - `RESULT_3VS3: "3대3 결과형 (2팀 × 3명)"`
      - `SOLO_SHOWDOWN: "솔로 쇼다운 (10팀 × 1명)"`
      - `DUO_SHOWDOWN: "듀오 쇼다운 (5팀 × 2명)"`
      - `TRIO_SHOWDOWN: "트리오 쇼다운 (4팀 × 3명)"`
    - `BattleEventMode.resultType` 을 `statsType: StatsEventModeType | null` 로 바꾼다. 쓰는 곳이 없어지면 `BattleEventModeResultType` 은 지운다.
  - `lib/api.ts`: `export const updateEventModeStatsType = (brawlStarsId: number, type: StatsEventModeType | null) => request<void>(\`/event-modes/${brawlStarsId}/stats-type\`, { method: "PUT", body: JSON.stringify({ type }) });`

- [ ] **Step 1: 타입과 API 함수를 바꾼다.**

- [ ] **Step 2: 목록 화면을 고친다.**
  - 로컬 `STATS_TYPE_LABELS` 를 지우고, 셀을 `m.statsType ? STATS_EVENT_MODE_TYPE_LABELS[m.statsType] : "수집 안 함"` 으로 바꾼다.
  - 열 머리 문구가 "유형" 이면 "통계 수집" 으로 바꾼다.

- [ ] **Step 3: 편집 화면에 "통계 수집" 카드를 더한다.** 위치는 "아이콘" 카드 뒤다.
  - `components/ui/select.tsx` 로 선택지 다섯(수집 안 함 + 네 유형 라벨)을 보여 준다. 사용 방식은 `app/events/page.tsx` 의 Select 를 따른다.
  - 초깃값은 `eventMode.statsType` 이다.
  - "저장" 버튼은 `updateEventModeStatsType(brawlStarsId, 선택값)` 을 부른다.
    - 성공: `toast.success("통계 수집 유형이 저장되었습니다. 수집 서버에는 최대 5분 뒤 반영되고, 켠 모드의 통계는 다음 날 배치 뒤부터 보입니다.")` 뒤 `loadEventMode()`
    - 실패: `toast.error("저장에 실패했습니다.")`
  - 카드 설명 한 줄: "팀 구성이 다른 모드에 유형을 매기면 그 모드는 수집되지 않습니다."

- [ ] **Step 4: lint 와 build 를 확인한다**

Run: `cd imstargg-admin-frontend && pnpm lint && pnpm build`
Expected: 오류 없음

- [ ] **Step 5: 화면을 직접 확인한다.**
  - 로컬 core-admin(Task 6 코드)과 `pnpm dev` 를 띄운다.
  - 모드 목록에 "통계 수집" 열이 보이는지 확인한다.
  - 편집 화면에서 유형을 저장하고, 다시 "수집 안 함" 으로 저장한 뒤 목록에 반영되는지 확인한다.
  - 로컬 DB 를 쓸 수 없으면 이 확인을 건너뛰었다고 보고한다.

- [ ] **Step 6: 커밋한다**

```bash
git add imstargg-admin-frontend
git commit -m "feat(admin-frontend): 이벤트 모드 편집 화면에서 통계 수집 유형을 정한다"
```

### Task 8: 옛 enum 삭제와 문서 (정리)

**Files:**
- Delete: `imstargg-backend/core/core-enum/src/main/kotlin/com/imstargg/core/enums/StatsEventMode.kt`, `imstargg-backend/core/core-enum/src/test/kotlin/com/imstargg/core/enums/StatsEventModeTest.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/task/UnregisteredRotationEventAlertTask.kt`(KDoc 24·32행 근처), 그 밖에 `rg "StatsEventMode\b" imstargg-backend` 에 남는 KDoc·주석(db-core `BrawlerBattleResultStatsEntity`·`BrawlerBattleRankStatsEntity` 포함)
- Modify: `imstargg-frontend/app/events/_components/modeStyles.ts`(21행 주석)
- Modify: `docs/superdomain/DOMAIN.md`, `imstargg-backend/docs/stats-battle-data-strategy.md`(78·84·180·195·330행 근처), `docs/superpowers/specs/2026-11-03-stats-event-mode-whitelist-design.md`(머리)

- [ ] **Step 1: 옛 enum 과 테스트를 지운다.** `rg "StatsEventMode\b" imstargg-backend` 로 남은 참조를 모두 `StatsEventModeType` 이나 "통계 수집 모드 테이블" 설명으로 고친다.

- [ ] **Step 2: 프론트 주석을 고친다.**
  - 대상: `modeStyles.ts` 의 "백엔드 `StatsEventMode` 에 모드를 추가할 때 여기에도 함께 추가한다."
  - 새 문구: "관리자 화면에서 통계 수집을 켠 모드는 여기에 색을 더한다. 없으면 기본 회색이다."

- [ ] **Step 3: 문서를 고친다**(설계 §5).
  - **`DOMAIN.md` statistics 규칙**
    - "허용 목록의 모드" 앞에 "관리자가 정한" 을 더한다.
    - "그래서 허용 목록의 결과형 모드는 모두 3대3이어야 한다" 를 "결과형 수집 유형은 3대3 하나뿐이다" 로 고친다.
    - 클래스 이름은 넣지 않는다.
  - **`DOMAIN.md` statistics 코드 목록**: "core-admin 의 `statistics` 패키지" 를 더한다.
  - **`DOMAIN.md` brawlstars 코드 목록**: "core-admin(operation 제외)" 을 "core-admin(operation·statistics 제외)" 로 고친다.
  - **`stats-battle-data-strategy.md`**: `StatsEventMode` 를 단일 출처로 적은 곳을 "모드 → 수집 유형은 `stats_event_mode` 테이블, 유형별 팀 구성은 `StatsEventModeType`" 으로 고친다. 봇 판정 절은 "2×3 결과형 구성" 으로 고친다.
  - **스펙 A 머리**: "- 변경: 2026-11-13 — 결정 2(허용 목록은 코드 enum)는 [통계 수집 모드 DB 관리 설계](2026-11-13-stats-event-mode-db-design.md)로 대체했다. 결정 10(컬럼 삭제 시점)도 그 설계 §3.9 가 바꿨다." 한 줄을 더한다.

- [ ] **Step 4: 전체 검증을 돌린다**

Run: `./gradlew ktlintCheck test`(`imstargg-backend/`), `cd imstargg-frontend && pnpm lint`
Expected: 모두 통과. `rg "StatsEventMode\b" imstargg-backend imstargg-frontend imstargg-admin-frontend docs/superdomain` 결과가 없다.

- [ ] **Step 5: 용어를 점검한다** — `/superglossary:check` 를 이번 브랜치 변경에 돌려 위반을 고친다.

- [ ] **Step 6: 커밋한다**

```bash
git add -A imstargg-backend imstargg-frontend/app/events/_components/modeStyles.ts docs/superdomain/DOMAIN.md docs/superpowers/specs/2026-11-03-stats-event-mode-whitelist-design.md
git commit -m "refactor: 옛 통계 수집 모드 enum 을 지우고 문서를 DB 관리 기준으로 고친다"
```

---

## 배포 (계획 밖, 참고)

배포는 설계 §3.9 의 순서를 따른다. 이 계획의 커밋은 스펙 A 와 같은 develop → main 릴리즈에 실려 나간다.

1. 배포 직전 순위형 구성을 조회한다(운영 DB 읽기, 사용자 허락).
2. `V{yyMMdd}__create_stats_event_mode.sql` 을 적용하고 12행을 확인한다.
3. 앱을 배포한다.
4. 배포 후 확인을 한다.
5. V261103 을 적용한다.
6. core-admin 을 다시 띄운다.
