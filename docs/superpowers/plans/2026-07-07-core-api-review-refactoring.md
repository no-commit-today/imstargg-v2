# core-api 검토·리팩터링 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** [스펙](../specs/2026-07-07-core-api-review-refactoring-design.md)의 19개 승인 항목(A 3, B 3, S 6, C 6, D 1)을 4개 Phase 로 구현한다.

**Architecture:** 기존 계층(Controller → QueryComposer → Service → application Repository(`*WithCache`) → JPA)을 유지한 타깃 수정. 핵심 규칙 2가지 — ① 조회·매핑은 리포지토리, 계산·집계는 도메인. ② 불일치가 곧 버그인 규칙만 통합, 우연히 닮은 코드는 중복 유지 + 테스트 고정 (신규 공용 추상화 금지).

**Tech Stack:** Spring Boot 4 + Kotlin (Java 21), JPA/QueryDSL(KSP), Caffeine + Redis 캐시, JUnit5 + AssertJ + MockK(springmockk), PostgreSQL(Docker).

**전제 조건:**
- 모든 명령은 `imstargg-backend/` 디렉터리에서 실행.
- context 테스트(`CoreApiContextTest` 상속)는 로컬 인프라 필요: `docker-compose up -d` (PostgreSQL·Valkey).
- 테스트 클래스에 `@Tag("context")` 를 직접 붙이지 말 것 — `CoreApiContextTest` 베이스가 이미 포함.
- 테스트 컨벤션: 한글 백틱 메서드명, `sut`, `// given // when // then`, AssertJ, 픽스처(`aPlayerEntity()` 등), 타입 추론.
- API 응답 스키마는 절대 바꾸지 않는다 (프론트엔드 무수정이 성공 기준).

---

### Task 0: 브랜치·환경 준비

**Files:** 없음 (git/인프라)

- [ ] **Step 1: 로컬 인프라 기동**

Run: `docker-compose up -d && docker-compose ps`
Expected: postgres·valkey(redis)·floci 컨테이너 Up

- [ ] **Step 2: develop 과 동기화 상태 확인**

```bash
git fetch origin
git log --oneline origin/develop..HEAD   # 우리 커밋(docs 스펙/플랜)만 나와야 함
git log --oneline HEAD..origin/develop | head   # develop 이 앞서 있으면 출력됨
```

develop 이 앞서 있으면: `git merge origin/develop` (충돌 시 해결 후 계속). docs 커밋만 앞서 있으면 그대로 진행.

- [ ] **Step 3: 베이스라인 확인**

Run: `./gradlew :core:core-api:unitTest ktlintCheck`
Expected: BUILD SUCCESSFUL (시작 전 그린 확인)

---

## Phase 1 — 정합성 + 도메인 경계

### Task 1: A-1 — offset 보존 `OffsetBasedPageRequest` 도입

**Files:**
- Test(추가): `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/application/brawlstars/PlayerRepositorySearchTest.kt`
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/infrastructure/jpa/OffsetBasedPageRequest.kt`
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/infrastructure/jpa/OffsetLimitExtensions.kt`
- Create: `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/infrastructure/jpa/OffsetBasedPageRequestTest.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/OffsetLimit.kt` (toPageable 제거)
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/brawlstars/PlayerRepository.kt:57,69` / `ClubRepository.kt:45,57` / `BattleRepository.kt:46` (import 만 변경)

- [ ] **Step 1: 실패하는 컨텍스트 테스트 작성** — `PlayerRepositorySearchTest`의 `SearchByTag` nested 클래스에 추가:

```kotlin
@Test
fun `offset 이 limit 의 배수가 아니어도 정확한 위치부터 반환한다`() {
    // given - 트로피 100~500 인 5명
    (1..5).forEach { i ->
        playerJpaRepository.save(
            aPlayerEntity()
                .withBrawlStarsTag("#A$i")
                .withName("플레이어$i")
                .withTrophies(i * 100)
                .build(),
        )
    }

    // when - 트로피 desc 정렬에서 offset=3 → 4번째(200), 5번째(100)
    val result = sut.searchByTag(BrawlStarsTag("#A"), OffsetLimit(offset = 3, limit = 2))

    // then
    assertThat(result.content).hasSize(2)
    assertThat(result.content[0].trophies).isEqualTo(200)
    assertThat(result.content[1].trophies).isEqualTo(100)
    assertThat(result.hasNext).isFalse
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.application.brawlstars.PlayerRepositorySearchTest"`
Expected: FAIL — 현재 `PageRequest.of(3/2, 2)` = page 1 → 300, 200 이 반환되고 hasNext=true

- [ ] **Step 3: `OffsetBasedPageRequest` 단위 테스트 작성** — `OffsetBasedPageRequestTest.kt` 신규:

```kotlin
package com.imstargg.core.infrastructure.jpa

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OffsetBasedPageRequestTest {
    @Test
    fun `offset 을 그대로 보존한다`() {
        // given
        val sut = OffsetBasedPageRequest(offset = 7, limit = 5)

        // when // then
        assertThat(sut.offset).isEqualTo(7L)
        assertThat(sut.pageSize).isEqualTo(5)
        assertThat(sut.pageNumber).isEqualTo(1)
    }

    @Test
    fun `next 는 offset 에 limit 을 더한다`() {
        // given
        val sut = OffsetBasedPageRequest(offset = 3, limit = 2)

        // when
        val next = sut.next()

        // then
        assertThat(next.offset).isEqualTo(5L)
        assertThat(next.pageSize).isEqualTo(2)
    }

    @Test
    fun `previousOrFirst 는 offset 이 limit 미만이면 first 를 반환한다`() {
        // given
        val sut = OffsetBasedPageRequest(offset = 1, limit = 5)

        // when
        val previous = sut.previousOrFirst()

        // then
        assertThat(previous.offset).isEqualTo(0L)
    }

    @Test
    fun `withPage 는 pageNumber 곱하기 limit 을 offset 으로 사용한다`() {
        // given
        val sut = OffsetBasedPageRequest(offset = 3, limit = 10)

        // when
        val paged = sut.withPage(2)

        // then
        assertThat(paged.offset).isEqualTo(20L)
    }
}
```

- [ ] **Step 4: 구현** — `OffsetBasedPageRequest.kt` 신규:

```kotlin
package com.imstargg.core.infrastructure.jpa

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

/**
 * offset 을 그대로 보존하는 Pageable.
 * `PageRequest.of(offset / limit, limit)` 는 offset 이 limit 의 배수가 아니면 위치가 어긋난다.
 */
class OffsetBasedPageRequest(
    private val offset: Long,
    private val limit: Int,
) : Pageable {
    init {
        require(offset >= 0) { "offset must not be negative" }
        require(limit >= 1) { "limit must not be less than one" }
    }

    constructor(offset: Int, limit: Int) : this(offset.toLong(), limit)

    override fun getPageNumber(): Int = (offset / limit).toInt()

    override fun getPageSize(): Int = limit

    override fun getOffset(): Long = offset

    override fun getSort(): Sort = Sort.unsorted()

    override fun next(): Pageable = OffsetBasedPageRequest(offset + limit, limit)

    override fun previousOrFirst(): Pageable = if (hasPrevious()) OffsetBasedPageRequest(offset - limit, limit) else first()

    override fun first(): Pageable = OffsetBasedPageRequest(0L, limit)

    override fun withPage(pageNumber: Int): Pageable = OffsetBasedPageRequest(pageNumber.toLong() * limit, limit)

    override fun hasPrevious(): Boolean = offset >= limit
}
```

`OffsetLimitExtensions.kt` 신규:

```kotlin
package com.imstargg.core.infrastructure.jpa

import com.imstargg.core.domain.OffsetLimit
import org.springframework.data.domain.Pageable

fun OffsetLimit.toPageable(): Pageable = OffsetBasedPageRequest(offset = offset, limit = limit)
```

`domain/OffsetLimit.kt` 를 순수 도메인으로 축소:

```kotlin
package com.imstargg.core.domain

data class OffsetLimit(
    val offset: Int,
    val limit: Int,
)
```

호출부 3개 파일(`PlayerRepository`, `ClubRepository`, `BattleRepository`)에 import 추가 — 호출 코드는 그대로:

```kotlin
import com.imstargg.core.infrastructure.jpa.toPageable
```

- [ ] **Step 5: 테스트 통과 확인**

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.infrastructure.jpa.OffsetBasedPageRequestTest" --tests "com.imstargg.core.application.brawlstars.PlayerRepositorySearchTest" --tests "com.imstargg.core.application.brawlstars.ClubRepositorySearchTest" --tests "com.imstargg.core.application.brawlstars.BattleServiceTest"`
Expected: PASS

- [ ] **Step 6: 커밋**

```bash
git add -A imstargg-backend/core/core-api
git commit -m "fix(core-api): offset 이 limit 배수가 아닐 때 페이지 위치 왜곡 수정

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

### Task 2: B-1 — domain 패키지의 Spring Data `Slice` 의존 제거

**Files:**
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/infrastructure/jpa/SpringDataSliceExtensions.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/Slice.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/brawlstars/PlayerRepository.kt:59,71` / `ClubRepository.kt:70`

- [ ] **Step 1: 사용처 전수 확인**

Run: `grep -rn "Slice.from(" core/core-api/src/main/kotlin`
Expected: `PlayerRepository.kt` 2곳, `ClubRepository.kt` 1곳 (`SliceResponse.from` 은 별개 — 대상 아님)

- [ ] **Step 2: 확장 함수 신설** — `SpringDataSliceExtensions.kt`:

```kotlin
package com.imstargg.core.infrastructure.jpa

import com.imstargg.core.domain.Slice
import org.springframework.data.domain.Slice as SpringDataSlice

fun <T : Any> SpringDataSlice<T>.toSlice(): Slice<T> =
    Slice(
        content = content,
        hasNext = hasNext(),
    )
```

`domain/Slice.kt` 에서 companion(`from`)과 Spring import 제거:

```kotlin
package com.imstargg.core.domain

data class Slice<T>(
    val content: List<T>,
    val hasNext: Boolean,
) {
    fun <R> map(transform: (T) -> R): Slice<R> =
        Slice(
            content = content.map(transform),
            hasNext = hasNext,
        )
}
```

호출부 교체 (import `com.imstargg.core.infrastructure.jpa.toSlice` 추가):
- `PlayerRepository.kt:59,71`: `return Slice.from(slice.map { toPlayer(it) })` → `return slice.map { toPlayer(it) }.toSlice()`
- `ClubRepository.kt:70`: `return Slice.from(slice.map { toClub(it, membersByClubId[it.id] ?: emptyList()) })` → `return slice.map { toClub(it, membersByClubId[it.id] ?: emptyList()) }.toSlice()`

- [ ] **Step 3: domain 순수성 검증 + 테스트**

Run: `grep -rn "org.springframework" core/core-api/src/main/kotlin/com/imstargg/core/domain/ ; ./gradlew :core:core-api:test --tests "com.imstargg.core.application.brawlstars.*"`
Expected: grep 결과 없음(exit 1), 테스트 PASS

- [ ] **Step 4: 커밋**

```bash
git add -A imstargg-backend/core/core-api
git commit -m "refactor(core-api): domain 패키지의 Spring Data 의존 제거

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

### Task 3: A-2 — 페이지네이션 파라미터 검증 추가

**Files:**
- Create: `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/api/controller/OffsetLimitRequestValidationTest.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/OffsetLimitRequest.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/PlayerController.kt:44` / `ClubController.kt:32` / `BattleController.kt:21`

- [ ] **Step 1: 실패하는 테스트 작성** — `OffsetLimitRequestValidationTest.kt` 신규 (`StatisticsControllerTest` 의 MockMvc 패턴):

```kotlin
package com.imstargg.core.api.controller

import com.imstargg.CoreApiContextTest
import com.imstargg.core.application.brawlstars.PlayerService
import com.imstargg.core.domain.Slice
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@AutoConfigureMockMvc
class OffsetLimitRequestValidationTest(
    @Autowired private val mockMvc: MockMvc,
) : CoreApiContextTest() {
    @MockkBean
    private lateinit var playerService: PlayerService

    @BeforeEach
    fun setUp() {
        every { playerService.search(any(), any()) } returns Slice(content = emptyList(), hasNext = false)
    }

    @Test
    fun `limit 이 100 을 초과하면 400`() {
        mockMvc
            .get("/v1/player/search") {
                param("q", "test")
                param("limit", "101")
            }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `limit 이 1 미만이면 400`() {
        mockMvc
            .get("/v1/player/search") {
                param("q", "test")
                param("limit", "0")
            }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `offset 이 음수면 400`() {
        mockMvc
            .get("/v1/player/search") {
                param("q", "test")
                param("offset", "-1")
            }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `offset 이 10000 을 초과하면 400`() {
        mockMvc
            .get("/v1/player/search") {
                param("q", "test")
                param("offset", "10001")
            }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `경계값 offset 10000 limit 100 은 200`() {
        mockMvc
            .get("/v1/player/search") {
                param("q", "test")
                param("offset", "10000")
                param("limit", "100")
            }.andExpect { status { isOk() } }
    }

    @Test
    fun `파라미터 생략 시 기본값으로 200`() {
        mockMvc
            .get("/v1/player/search") {
                param("q", "test")
            }.andExpect { status { isOk() } }
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.api.controller.OffsetLimitRequestValidationTest"`
Expected: FAIL — 400 기대 케이스들이 200 반환

- [ ] **Step 3: 구현** — `OffsetLimitRequest.kt`:

```kotlin
package com.imstargg.core.api.controller

import com.imstargg.core.domain.OffsetLimit
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min

data class OffsetLimitRequest(
    @field:Min(0)
    @field:Max(10_000)
    val offset: Int = 0,
    @field:Min(1)
    @field:Max(100)
    val limit: Int = 25,
) {
    fun toOffsetLimit(): OffsetLimit = OffsetLimit(offset, limit)
}
```

컨트롤러 3곳에 `@Valid` 추가 (import `jakarta.validation.Valid`):
- `PlayerController.kt:44`: `@Valid @ModelAttribute offsetLimit: OffsetLimitRequest,`
- `ClubController.kt:32`: `@Valid @ModelAttribute offsetLimit: OffsetLimitRequest,`
- `BattleController.kt:21`: `@Valid @ModelAttribute request: OffsetLimitRequest,`

- [ ] **Step 4: 통과 확인**

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.api.controller.OffsetLimitRequestValidationTest"`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add -A imstargg-backend/core/core-api
git commit -m "feat(core-api): 페이지네이션 파라미터 offset·limit 검증 추가

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

### Task 4: B-2 — `Brawlers` Map 인덱싱

**Files:**
- Create: `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/domain/brawlstars/brawler/BrawlersTest.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/brawlstars/brawler/Brawlers.kt`

- [ ] **Step 1: 동작 고정(characterization) 테스트 작성** — `BrawlersTest.kt` 신규 (`Brawler` 생성은 최소 필드 — `Brawler(id, name, rarity, role, portraitImageUrl)`):

```kotlin
package com.imstargg.core.domain.brawlstars.brawler

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.Messages
import com.imstargg.core.enums.BrawlerRarity
import com.imstargg.core.enums.BrawlerRole
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BrawlersTest {
    private fun brawler(id: Long) =
        Brawler(
            id = BrawlStarsId(id),
            name = Messages(emptyMap()),
            rarity = BrawlerRarity.RARE,
            role = BrawlerRole.TANK,
            portraitImageUrl = null,
        )

    @Test
    fun `id 로 브롤러를 찾는다`() {
        // given
        val sut = Brawlers(listOf(brawler(1L), brawler(2L)))

        // when
        val result = sut.find(BrawlStarsId(2L))

        // then
        assertThat(result).isEqualTo(brawler(2L))
    }

    @Test
    fun `없는 id 는 null 을 반환한다`() {
        // given
        val sut = Brawlers(listOf(brawler(1L)))

        // when
        val result = sut.find(BrawlStarsId(99L))

        // then
        assertThat(result).isNull()
    }
}
```

- [ ] **Step 2: 테스트 실행 (현재 구현으로 PASS — 동작 고정)**

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.domain.brawlstars.brawler.BrawlersTest"`
Expected: PASS

- [ ] **Step 3: Map 인덱싱으로 리팩터링** — `Brawlers.kt`:

```kotlin
package com.imstargg.core.domain.brawlstars.brawler

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class Brawlers(
    private val brawlers: List<Brawler>,
) {
    private val brawlersById: Map<BrawlStarsId, Brawler> = brawlers.associateBy { it.id }

    fun find(id: BrawlStarsId): Brawler? = brawlersById[id]
}
```

- [ ] **Step 4: 통과 확인**

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.domain.brawlstars.brawler.BrawlersTest"`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add -A imstargg-backend/core/core-api
git commit -m "refactor(core-api): Brawlers 조회를 Map 인덱스로 변경

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

### Task 5: B-3 — bounded context 구조 명문화

**Files:**
- Modify: `imstargg-backend/CLAUDE.md` (### Key Patterns 섹션 바로 아래에 신규 h3 삽입)

- [ ] **Step 1: `imstargg-backend/CLAUDE.md` 의 `### Cache Key Conventions` 제목 바로 위에 다음 섹션 삽입**

```markdown
### Bounded Context & 도메인 모델

- **core-api `domain`** — 조회(read) 컨텍스트의 도메인 모델. JPA 엔티티와 격리된 불변 모델이며 Spring 프레임워크에 의존하지 않는다 (Pageable/Slice 변환은 `core-api` 의 `infrastructure/jpa` 확장 함수 담당).
- **core-worker `domain`** — 수집·동기화(write) 컨텍스트의 도메인 모델. BrawlStars API 응답과 DB 상태를 diff·갱신하기 위해 JPA 엔티티를 직접 보유하는 것이 **의도된 설계**다. core-api 모델과 이름이 같아도(예: `Player`) 책임이 다른 별개 bounded context 모델이므로 통합·공유 모듈 추출을 하지 않는다.
- **통합·추상화 판정 기준**: 사본 간 불일치가 곧 버그가 되는 비즈니스 규칙(예: 픽률 분모 = 총 게임 수 계산)만 통합한다. 우연히 닮은 코드(예: Pair/Enemy 통계)는 중복을 유지하고 단위 테스트로 각 사본을 고정한다.
- **통계 로직 위치 규칙**: 조회·매핑은 리포지토리, 계산·집계는 도메인(`create()` 등).
```

- [ ] **Step 2: 커밋**

```bash
git add imstargg-backend/CLAUDE.md
git commit -m "docs: bounded context 구조와 통합 판정 기준 명문화

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Phase 2 — statistics 유지보수성

### Task 6: A-3 — 총 게임 수 계산을 `GameSlots` 도메인으로 단일화

**Files:**
- Test(추가): `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/domain/statistics/GameSlotsTest.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/GameSlots.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/BrawlerResultStatistics.kt:37-43`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepository.kt:182-207`

- [ ] **Step 1: 실패하는 도메인 테스트 작성** — `GameSlotsTest` 에 추가:

```kotlin
@Test
fun `totalGameCount 는 이벤트별 슬롯 합을 게임 수로 환산해 합산한다`() {
    // given - 3v3(slots=6) 360슬롯 → 60게임, 솔로 쇼다운(slots=10) 500슬롯 → 50게임
    val sut =
        GameSlots(
            mapOf(
                BrawlStarsId(1L) to GameSlot(listOf(3, 3)),
                BrawlStarsId(2L) to GameSlot(List(10) { 1 }),
            ),
        )

    // when
    val result = sut.totalGameCount(mapOf(BrawlStarsId(1L) to 360L, BrawlStarsId(2L) to 500L))

    // then
    assertThat(result).isEqualTo(110L)
}

@Test
fun `totalGameCount 는 미등록 이벤트를 default 슬롯으로 환산한다`() {
    // given - default = 3v3 (slots=6)
    val sut = GameSlots(emptyMap())

    // when
    val result = sut.totalGameCount(mapOf(BrawlStarsId(9L) to 12L))

    // then
    assertThat(result).isEqualTo(2L)
}

@Test
fun `totalGameCount 는 빈 입력이면 0`() {
    // when
    val result = GameSlots(emptyMap()).totalGameCount(emptyMap())

    // then
    assertThat(result).isEqualTo(0L)
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.domain.statistics.GameSlotsTest"`
Expected: FAIL (컴파일 에러 — totalGameCount 미정의)

- [ ] **Step 3: 도메인 구현** — `GameSlots.kt`:

```kotlin
package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

/**
 * 이벤트별 [GameSlot] 묶음. 등록되지 않은 eventId 는 [GameSlot.default] 로 폴백한다.
 */
class GameSlots(
    private val byEventId: Map<BrawlStarsId, GameSlot>,
) {
    operator fun get(eventId: BrawlStarsId): GameSlot = byEventId[eventId] ?: GameSlot.default()

    /**
     * 조회 범위의 총 게임 수 = Σ (이벤트별 Result 슬롯 합 ÷ [GameSlot.resultSlotsPerGame]).
     *
     * Pair / Enemy 통계의 분모도 동일한 게임 수를 사용하므로 항상 Result 슬롯 합 기준으로 계산한다.
     * 수학적으로 `pair_slot/pairSlotsPerGame == enemy_slot/enemySlotsPerGame == result_slot/resultSlotsPerGame == gameCount`
     * 가 보장되므로 Result 테이블을 단일 진실 출처로 사용한다.
     * 분모 의미 정정 이력은 [StatsRate.pickRate] 참조.
     */
    fun totalGameCount(resultSlotSumByEventId: Map<BrawlStarsId, Long>): Long =
        resultSlotSumByEventId.entries.sumOf { (eventId, slotSum) ->
            slotSum / this[eventId].resultSlotsPerGame
        }
}
```

- [ ] **Step 4: 사용처 2곳을 도메인 호출로 교체**

`BrawlerResultStatistics.kt` 의 `create()` 에서 `totalBattleCount` 계산부(37-43행)를 교체:

```kotlin
            val totalBattleCount: Long =
                gameSlots.totalGameCount(
                    rows
                        .groupBy { it.eventId }
                        .mapValues { (_, eventRows) -> eventRows.sumOf { it.resultCount.totalCount } },
                )
```

`BattleResultStatisticsRepository.kt` 의 `computeTotalGameCount()`(182-207행)를 조회+위임으로 교체 (KDoc 은 도메인으로 이동했으므로 축소):

```kotlin
    /** 총 게임 수 — 계산 규칙은 [GameSlots.totalGameCount] 가 단일 진실 출처. */
    private fun computeTotalGameCount(
        eventId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Long {
        val slotSumByEventId =
            battleResultJpaRepository
                .findEventTotalSlotSums(
                    eventBrawlStarsId = eventId?.value,
                    tierRange = tierRange,
                    startDate = startDate,
                    endDate = endDate,
                ).associate { BrawlStarsId(it.eventBrawlStarsId) to it.totalSlotCount }
        val gameSlots = gameSlotRepository.findAllByEventIds(slotSumByEventId.keys.toList())
        return gameSlots.totalGameCount(slotSumByEventId)
    }
```

- [ ] **Step 5: 통과 확인 (기존 테스트 = 동작 보존 검증)**

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.domain.statistics.GameSlotsTest" --tests "com.imstargg.core.domain.statistics.BrawlerResultStatisticsTest" --tests "com.imstargg.core.application.statistics.BattleResultStatisticsRepositoryTest"`
Expected: PASS

- [ ] **Step 6: 커밋**

```bash
git add -A imstargg-backend/core/core-api
git commit -m "refactor(core-api): 총 게임 수 계산을 GameSlots 도메인으로 단일화

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

### Task 7: S-1a — 솔로 순위 통계 집계를 도메인 `create()` 로 이동

**Files:**
- Create: `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/domain/statistics/RankCountTest.kt`
- Create: `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/domain/statistics/BrawlerRankStatisticsTest.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/RankCount.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/BrawlerRankStatistics.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleRankStatisticsRepository.kt:22-43`

- [ ] **Step 1: 실패하는 도메인 테스트 작성** — `RankCountTest.kt` 신규:

```kotlin
package com.imstargg.core.domain.statistics

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.Test

class RankCountTest {
    @Test
    fun `merge 는 순위별 카운트를 합산한다`() {
        // given
        val sut = RankCount(mapOf(1 to 2L, 3 to 1L))

        // when
        val result = sut.merge(RankCount(mapOf(1 to 1L, 2 to 4L)))

        // then
        assertThat(result).isEqualTo(RankCount(mapOf(1 to 3L, 2 to 4L, 3 to 1L)))
    }

    @Test
    fun `averageRank 는 가중 평균이다`() {
        // given - 1위 2회, 3위 1회 → (1*2 + 3*1) / 3
        val sut = RankCount(mapOf(1 to 2L, 3 to 1L))

        // when // then
        assertThat(sut.averageRank).isCloseTo(5.0 / 3.0, Offset.offset(1e-9))
    }

    @Test
    fun `빈 카운트의 averageRank 는 0`() {
        // when // then
        assertThat(RankCount(emptyMap()).averageRank).isEqualTo(0.0)
    }
}
```

`BrawlerRankStatisticsTest.kt` 신규:

```kotlin
package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.Test

class BrawlerRankStatisticsTest {
    @Test
    fun `create 는 같은 브롤러의 count 를 병합해 item 을 만든다`() {
        // given
        val counts =
            listOf(
                BrawlerRankCount(BrawlStarsId(10L), RankCount(mapOf(1 to 2L))),
                BrawlerRankCount(BrawlStarsId(10L), RankCount(mapOf(3 to 1L))),
                BrawlerRankCount(BrawlStarsId(20L), RankCount(mapOf(2 to 2L))),
            )

        // when
        val sut = BrawlerRankStatistics.create(counts)

        // then
        assertThat(sut.items).hasSize(2)
        val item10 = sut.items.first { it.brawlerId == BrawlStarsId(10L) }
        assertThat(item10.totalBattleCount).isEqualTo(3L)
        assertThat(item10.averageRank).isCloseTo(5.0 / 3.0, Offset.offset(1e-9))
        val item20 = sut.items.first { it.brawlerId == BrawlStarsId(20L) }
        assertThat(item20.averageRank).isEqualTo(2.0)
    }

    @Test
    fun `빈 입력은 빈 items`() {
        // when
        val sut = BrawlerRankStatistics.create(emptyList())

        // then
        assertThat(sut.items).isEmpty()
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.domain.statistics.RankCountTest" --tests "com.imstargg.core.domain.statistics.BrawlerRankStatisticsTest"`
Expected: FAIL (컴파일 에러 — merge/create 미정의)

- [ ] **Step 3: 도메인 구현**

`RankCount.kt` 에 merge 추가:

```kotlin
package com.imstargg.core.domain.statistics

data class RankCount(
    val counts: Map<Int, Long>,
) {
    val totalCount: Long get() = counts.values.sum()

    val averageRank: Double
        get() =
            if (totalCount > 0) {
                counts.entries.sumOf { it.key.toLong() * it.value }.toDouble() / totalCount
            } else {
                0.0
            }

    fun merge(other: RankCount): RankCount {
        val merged = counts.toMutableMap()
        other.counts.forEach { (rank, count) -> merged.merge(rank, count, Long::plus) }
        return RankCount(merged)
    }
}
```

`BrawlerRankStatistics.kt` 에 create 추가:

```kotlin
package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerRankStatisticsItem(
    val brawlerId: BrawlStarsId,
    val totalBattleCount: Long,
    val averageRank: Double,
)

data class BrawlerRankStatistics(
    val items: List<BrawlerRankStatisticsItem>,
) {
    companion object {
        fun create(counts: List<BrawlerRankCount>): BrawlerRankStatistics {
            val items =
                counts
                    .groupBy { it.brawlerId }
                    .map { (brawlerId, grouped) ->
                        BrawlerRankCount(
                            brawlerId = brawlerId,
                            rankCount = grouped.map { it.rankCount }.reduce(RankCount::merge),
                        ).toStatisticsItem()
                    }
            return BrawlerRankStatistics(items = items)
        }
    }
}
```

- [ ] **Step 4: 리포지토리를 조회+매핑만 남기고 위임** — `BattleRankStatisticsRepository.kt` 의 `findBrawlerRankStatistics`(22-43행) 교체:

```kotlin
    fun findBrawlerRankStatistics(
        eventId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerRankStatistics {
        val counts =
            soloBattleRankJpaRepository
                .findAllByEventBrawlStarsIdAndTierRangeAndBattleDateBetween(
                    eventId.value,
                    tierRange,
                    startDate,
                    endDate,
                ).map { entity ->
                    BrawlerRankCount(
                        brawlerId = BrawlStarsId(entity.brawlerBrawlStarsId),
                        rankCount = entity.rankCounts.toRankCount(),
                    )
                }
        return BrawlerRankStatistics.create(counts)
    }
```

- [ ] **Step 5: 통과 확인**

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.domain.statistics.RankCountTest" --tests "com.imstargg.core.domain.statistics.BrawlerRankStatisticsTest" --tests "com.imstargg.core.application.statistics.BattleRankStatisticsRepositoryTest"`
Expected: PASS

- [ ] **Step 6: 커밋**

```bash
git add -A imstargg-backend/core/core-api
git commit -m "refactor(core-api): 솔로 순위 통계 집계를 도메인 create 로 이동

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

### Task 8: S-1b — pair 순위 통계 평균 계산을 도메인으로 이동

**Files:**
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/BrawlerPairRankCount.kt`
- Create: `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/domain/statistics/BrawlerPairRankStatisticsTest.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/BrawlerPairRankStatistics.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleRankStatisticsRepository.kt` (findBrawlerPairRankStatistics)

- [ ] **Step 1: 실패하는 테스트 작성** — `BrawlerPairRankStatisticsTest.kt` 신규:

```kotlin
package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.Test

class BrawlerPairRankStatisticsTest {
    @Test
    fun `create 는 weightedRankSum 을 totalCount 로 나눠 averageRank 를 계산한다`() {
        // given - 1위 2회 + 3위 1회 → weightedRankSum=5, totalCount=3
        val counts =
            listOf(
                BrawlerPairRankCount(
                    brawlerId = BrawlStarsId(10L),
                    pairBrawlerId = BrawlStarsId(20L),
                    totalCount = 3L,
                    weightedRankSum = 5L,
                ),
            )

        // when
        val sut = BrawlerPairRankStatistics.create(totalBattleCount = 3L, totalElementCount = 1L, counts = counts)

        // then
        assertThat(sut.totalBattleCount).isEqualTo(3L)
        assertThat(sut.totalElementCount).isEqualTo(1L)
        val item = sut.items.single()
        assertThat(item.totalBattleCount).isEqualTo(3L)
        assertThat(item.averageRank).isCloseTo(5.0 / 3.0, Offset.offset(1e-9))
    }

    @Test
    fun `totalCount 0 이면 averageRank 0`() {
        // given
        val counts =
            listOf(
                BrawlerPairRankCount(
                    brawlerId = BrawlStarsId(10L),
                    pairBrawlerId = BrawlStarsId(20L),
                    totalCount = 0L,
                    weightedRankSum = 0L,
                ),
            )

        // when
        val sut = BrawlerPairRankStatistics.create(totalBattleCount = 0L, totalElementCount = 0L, counts = counts)

        // then
        assertThat(sut.items.single().averageRank).isEqualTo(0.0)
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.domain.statistics.BrawlerPairRankStatisticsTest"`
Expected: FAIL (컴파일 에러)

- [ ] **Step 3: 도메인 구현**

`BrawlerPairRankCount.kt` 신규:

```kotlin
package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerPairRankCount(
    val brawlerId: BrawlStarsId,
    val pairBrawlerId: BrawlStarsId,
    val totalCount: Long,
    val weightedRankSum: Long,
) {
    val averageRank: Double
        get() = if (totalCount > 0) weightedRankSum.toDouble() / totalCount else 0.0
}
```

`BrawlerPairRankStatistics.kt` 에 create 추가:

```kotlin
package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

data class BrawlerPairRankStatisticsItem(
    val brawlerId: BrawlStarsId,
    val pairBrawlerId: BrawlStarsId,
    val totalBattleCount: Long,
    val averageRank: Double,
)

data class BrawlerPairRankStatistics(
    val totalBattleCount: Long,
    val totalElementCount: Long,
    val items: List<BrawlerPairRankStatisticsItem>,
) {
    companion object {
        fun create(
            totalBattleCount: Long,
            totalElementCount: Long,
            counts: List<BrawlerPairRankCount>,
        ): BrawlerPairRankStatistics =
            BrawlerPairRankStatistics(
                totalBattleCount = totalBattleCount,
                totalElementCount = totalElementCount,
                items =
                    counts.map { count ->
                        BrawlerPairRankStatisticsItem(
                            brawlerId = count.brawlerId,
                            pairBrawlerId = count.pairBrawlerId,
                            totalBattleCount = count.totalCount,
                            averageRank = count.averageRank,
                        )
                    },
            )
    }
}
```

- [ ] **Step 4: 리포지토리 위임** — `BattleRankStatisticsRepository.kt` 의 `findBrawlerPairRankStatistics` 를 교체 (items 인라인 계산 삭제):

```kotlin
    fun findBrawlerPairRankStatistics(
        eventId: BrawlStarsId,
        brawlerId: BrawlStarsId,
        pairBrawlerId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerPairRankStatistics {
        val counts =
            pairBattleRankJpaRepository
                .findAllByConditions(
                    eventBrawlStarsId = eventId.value,
                    brawlerBrawlStarsId = brawlerId.value,
                    pairBrawlerBrawlStarsId = pairBrawlerId?.value,
                    tierRange = tierRange,
                    startDate = startDate,
                    endDate = endDate,
                ).map { projection ->
                    BrawlerPairRankCount(
                        brawlerId = BrawlStarsId(projection.brawlerBrawlStarsId),
                        pairBrawlerId = BrawlStarsId(projection.pairBrawlerBrawlStarsId),
                        totalCount = projection.totalCount,
                        weightedRankSum = projection.weightedRankSum,
                    )
                }
        val totalBattleCount =
            pairBattleRankJpaRepository.countTotalBattleCount(
                eventBrawlStarsId = eventId.value,
                brawlerBrawlStarsId = brawlerId.value,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
            )
        val totalElementCount =
            pairBattleRankJpaRepository.countDistinctPairBrawlers(
                eventBrawlStarsId = eventId.value,
                brawlerBrawlStarsId = brawlerId.value,
                pairBrawlerBrawlStarsId = pairBrawlerId?.value,
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
            )
        return BrawlerPairRankStatistics.create(
            totalBattleCount = totalBattleCount,
            totalElementCount = totalElementCount,
            counts = counts,
        )
    }
```

불필요해진 import(`BrawlerPairRankStatisticsItem`) 제거.

- [ ] **Step 5: 통과 확인**

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.domain.statistics.BrawlerPairRankStatisticsTest" --tests "com.imstargg.core.application.statistics.BattleRankStatisticsRepositoryTest"`
Expected: PASS (기존 컨텍스트 테스트의 averageRank 5/3 검증이 동작 보존을 증명)

- [ ] **Step 6: 커밋**

```bash
git add -A imstargg-backend/core/core-api
git commit -m "refactor(core-api): pair 순위 통계 평균 계산을 도메인으로 이동

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

### Task 9: S-5 — 일별 통계 집계 idiom 통일

**Files:**
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepository.kt` (findBrawlerDailyResultStatistics)

- [ ] **Step 1: 인프라 `+` 합산 → 도메인 매핑 후 merge 로 교체** — `findBrawlerDailyResultStatistics` 내부의 resultCount 계산부:

기존:

```kotlin
                                resultCount =
                                    grouped
                                        .map { it.resultCount }
                                        .reduce { acc, r -> acc + r }
                                        .toResultCount(),
```

교체:

```kotlin
                                resultCount =
                                    grouped
                                        .map { it.resultCount.toResultCount() }
                                        .reduce(ResultCount::merge),
```

- [ ] **Step 2: 통과 확인 (동작 보존)**

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.application.statistics.BattleResultStatisticsRepositoryTest"`
Expected: PASS

- [ ] **Step 3: 커밋**

```bash
git add -A imstargg-backend/core/core-api
git commit -m "refactor(core-api): 일별 통계 집계를 도메인 매핑 후 merge idiom 으로 통일

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

### Task 10: S-2′ + S-6a — Pair/Enemy 사본 고정 테스트와 KDoc 정본화

**Files:**
- Create: `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/domain/statistics/BrawlerPairResultStatisticsTest.kt`
- Create: `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/domain/statistics/BrawlerEnemyResultStatisticsTest.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/StatsRate.kt` (KDoc 정본)
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/statistics/BrawlerResultStatistics.kt` / `BrawlerPairResultStatistics.kt` / `BrawlerEnemyResultStatistics.kt` (클래스 KDoc → @see 참조)

- [ ] **Step 1: 사본 고정 테스트 작성 (현재 동작 기준 — 즉시 PASS 해야 함)**

`BrawlerPairResultStatisticsTest.kt`:

```kotlin
package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BrawlerPairResultStatisticsTest {
    @Test
    fun `create 는 pair 슬롯 수를 battleCount 로, 게임 수를 픽률 분모로 사용한다`() {
        // given - pair 슬롯 40 (승30 패10), 전체 100게임
        val counts =
            listOf(
                BrawlerPairResultCount(
                    brawlerId = BrawlStarsId(10L),
                    pairBrawlerId = BrawlStarsId(20L),
                    resultCount = ResultCount(victoryCount = 30, defeatCount = 10, drawCount = 0),
                ),
            )

        // when
        val sut = BrawlerPairResultStatistics.create(totalBattleCount = 100L, totalElementCount = 1L, counts = counts)

        // then
        val item = sut.items.single()
        assertThat(item.battleCount).isEqualTo(40L)
        assertThat(item.pickRate).isEqualTo(0.4)
        assertThat(item.winRate).isEqualTo(0.75)
    }

    @Test
    fun `게임 수 0 이면 pickRate 0`() {
        // given
        val counts =
            listOf(
                BrawlerPairResultCount(
                    brawlerId = BrawlStarsId(10L),
                    pairBrawlerId = BrawlStarsId(20L),
                    resultCount = ResultCount(0, 0, 0),
                ),
            )

        // when
        val sut = BrawlerPairResultStatistics.create(totalBattleCount = 0L, totalElementCount = 0L, counts = counts)

        // then
        assertThat(sut.items.single().pickRate).isEqualTo(0.0)
    }
}
```

`BrawlerEnemyResultStatisticsTest.kt` (동일 구조 — 사본이므로 테스트도 독립 사본):

```kotlin
package com.imstargg.core.domain.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BrawlerEnemyResultStatisticsTest {
    @Test
    fun `create 는 매치업 슬롯 수를 battleCount 로, 게임 수를 픽률 분모로 사용한다`() {
        // given - 매치업 슬롯 40 (승30 패10), 전체 100게임
        val counts =
            listOf(
                BrawlerEnemyResultCount(
                    brawlerId = BrawlStarsId(10L),
                    enemyBrawlerId = BrawlStarsId(20L),
                    resultCount = ResultCount(victoryCount = 30, defeatCount = 10, drawCount = 0),
                ),
            )

        // when
        val sut = BrawlerEnemyResultStatistics.create(totalBattleCount = 100L, totalElementCount = 1L, counts = counts)

        // then
        val item = sut.items.single()
        assertThat(item.battleCount).isEqualTo(40L)
        assertThat(item.pickRate).isEqualTo(0.4)
        assertThat(item.winRate).isEqualTo(0.75)
    }

    @Test
    fun `게임 수 0 이면 pickRate 0`() {
        // given
        val counts =
            listOf(
                BrawlerEnemyResultCount(
                    brawlerId = BrawlStarsId(10L),
                    enemyBrawlerId = BrawlStarsId(20L),
                    resultCount = ResultCount(0, 0, 0),
                ),
            )

        // when
        val sut = BrawlerEnemyResultStatistics.create(totalBattleCount = 0L, totalElementCount = 0L, counts = counts)

        // then
        assertThat(sut.items.single().pickRate).isEqualTo(0.0)
    }
}
```

- [ ] **Step 2: PASS 확인**

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.domain.statistics.BrawlerPairResultStatisticsTest" --tests "com.imstargg.core.domain.statistics.BrawlerEnemyResultStatisticsTest"`
Expected: PASS

- [ ] **Step 3: KDoc 정본화**

`StatsRate.kt` 의 `pickRate` KDoc 을 정본으로 확장:

```kotlin
    /**
     * 픽률 = 슬롯(브롤러/조합 등장) 수 / 전투(게임) 수.
     *
     * **분모·분자 의미의 단일 정본** (Result/Pair/Enemy 통계 공통):
     * - 분모 `totalBattleCount` 는 **게임 수** 여야 한다. 이전에는 슬롯 합으로 잘못 사용됨 — 2026-05-19 정정.
     *   슬롯 합 ↔ 게임 수 환산 규칙은 [GameSlots.totalGameCount] 참조.
     * - item 단위 `battleCount` 는 **해당 항목의 슬롯 수** 다. 정정 당시 item 에 전역 게임 수를 복사해
     *   전 항목 전투수가 동일 표시됐던 버그가 있었음 — 2026-06-06 정정.
     *
     * Pair/Enemy 통계 클래스는 구조가 닮았지만 의미가 다른 별개 모델로 **중복 유지가 의도된 설계**다.
     * 이 규칙을 고칠 때는 각 사본의 단위 테스트(BrawlerResultStatisticsTest,
     * BrawlerPairResultStatisticsTest, BrawlerEnemyResultStatisticsTest)를 함께 갱신할 것.
     */
```

세 통계 클래스의 클래스 KDoc 을 참조형으로 축소 (아이템 필드 KDoc 은 유지):

`BrawlerResultStatistics.kt`:

```kotlin
/**
 * 결과형 통계 - 브롤러 단위. 분모·battleCount 의미와 정정 이력은 [StatsRate.pickRate] 가 정본.
 */
```

`BrawlerPairResultStatistics.kt`:

```kotlin
/**
 * 결과형 Pair(아군 조합) 통계 — winRate 는 "함께일 때 승률".
 * 분모·battleCount 의미와 정정 이력은 [StatsRate.pickRate] 가 정본.
 * Enemy 통계와 구조가 닮았지만 의미가 다른 별개 모델 — 통합하지 말 것 (중복 유지 + 테스트 고정).
 */
```

`BrawlerEnemyResultStatistics.kt`:

```kotlin
/**
 * 결과형 Enemy(상대) 통계 — winRate 는 "상대로 만났을 때 승률".
 * 분모·battleCount 의미와 정정 이력은 [StatsRate.pickRate] 가 정본.
 * Pair 통계와 구조가 닮았지만 의미가 다른 별개 모델 — 통합하지 말 것 (중복 유지 + 테스트 고정).
 */
```

- [ ] **Step 4: 컴파일·테스트 확인**

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.domain.statistics.*"`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add -A imstargg-backend/core/core-api
git commit -m "test(core-api): Pair/Enemy 통계 사본 고정 테스트 추가 및 KDoc 정본화

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

### Task 11: S-3′ — 통계 캐시 키 회귀 테스트 + 경고 주석

**Files:**
- Create: `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/application/statistics/StatisticsCacheKeyTest.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/BattleResultStatisticsRepositoryWithCache.kt` / `BattleRankStatisticsRepositoryWithCache.kt` (클래스 KDoc)
- Modify: `imstargg-backend/CLAUDE.md` (Cache Key Conventions 에 한 줄)

- [ ] **Step 1: 캐시 키 회귀 테스트 작성** — `StatisticsCacheKeyTest.kt` 신규 (단위 테스트 — kotlin-reflect 사용):

```kotlin
package com.imstargg.core.application.statistics

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.cache.annotation.Cacheable
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.valueParameters

class StatisticsCacheKeyTest {
    private val targets =
        listOf(
            BattleResultStatisticsRepositoryWithCache::class,
            BattleRankStatisticsRepositoryWithCache::class,
        )

    @Test
    fun `Cacheable key 표현식은 메서드의 모든 파라미터를 포함한다`() {
        targets.forEach { klass ->
            klass.declaredFunctions.forEach { fn ->
                val cacheable = fn.findAnnotation<Cacheable>() ?: return@forEach
                fn.valueParameters.forEach { param ->
                    assertThat(cacheable.key)
                        .withFailMessage(
                            "%s.%s 의 캐시 키에 #%s 가 없습니다 — 파라미터를 추가했다면 key 도 갱신하세요",
                            klass.simpleName,
                            fn.name,
                            param.name,
                        ).contains("#${param.name}")
                }
            }
        }
    }

    @Test
    fun `캐시 키 스냅샷 - 형식 변경 시 메서드 단위 v prefix 범프를 검토할 것`() {
        // given
        val keys =
            targets
                .flatMap { klass ->
                    klass.declaredFunctions.mapNotNull { fn ->
                        fn.findAnnotation<Cacheable>()?.let { "${klass.simpleName}.${fn.name}" to it.key }
                    }
                }.toMap()

        // then
        assertThat(keys).isEqualTo(
            mapOf(
                "BattleResultStatisticsRepositoryWithCache.findBrawlerResultStatistics" to
                    "'v2:brawlerResultStats:' + #eventId + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
                "BattleResultStatisticsRepositoryWithCache.findBrawlerPairResultStatistics" to
                    "'v2:brawlerPairResultStats:' + #eventId + ':' + #brawlerId + ':' + #pairBrawlerId" +
                    " + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
                "BattleResultStatisticsRepositoryWithCache.findBrawlerEnemyResultStatistics" to
                    "'v2:brawlerEnemyResultStats:' + #eventId + ':' + #brawlerId + ':' + #tierRange" +
                    " + ':' + #startDate + ':' + #endDate",
                "BattleResultStatisticsRepositoryWithCache.findBrawlerDailyResultStatistics" to
                    "'v1:brawlerDailyResultStats:' + #brawlerId + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
                "BattleRankStatisticsRepositoryWithCache.findBrawlerRankStatistics" to
                    "'v1:brawlerRankStats:' + #eventId + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
                "BattleRankStatisticsRepositoryWithCache.findBrawlerPairRankStatistics" to
                    "'v1:brawlerPairRankStats:' + #eventId + ':' + #brawlerId + ':' + #pairBrawlerId" +
                    " + ':' + #tierRange + ':' + #startDate + ':' + #endDate",
            ),
        )
    }
}
```

- [ ] **Step 2: PASS 확인 (현재 키가 이미 파라미터를 전부 포함하는지 검증)**

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.application.statistics.StatisticsCacheKeyTest"`
Expected: PASS. (FAIL 이면 스냅샷의 문자열을 소스의 실제 key 리터럴과 일치하도록 수정 — 소스가 정답)

- [ ] **Step 3: 두 WithCache 클래스에 경고 KDoc 추가** (클래스 선언 위):

```kotlin
/**
 * 주의: @Cacheable 메서드에 파라미터를 추가/삭제하면 key 표현식도 함께 갱신할 것 —
 * 누락은 [StatisticsCacheKeyTest] 가 잡는다. 키 형식 변경 시 해당 메서드의 `v{N}:` prefix 를 범프한다.
 */
```

- [ ] **Step 4: CLAUDE.md 의 `### Cache Key Conventions` 목록 끝에 추가**

```markdown
- **키-파라미터 동기화**: `@Cacheable` 메서드의 파라미터 변경 시 key 표현식도 함께 갱신 — 누락·형식 변경은 `StatisticsCacheKeyTest` 가 검증
```

- [ ] **Step 5: 커밋**

```bash
git add -A imstargg-backend
git commit -m "test(core-api): 통계 캐시 키 회귀 테스트 및 경고 주석 추가

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

### Task 12: S-4′ — 통계 CustomImpl 파일 내 반복 where 절 정리

**Files:**
- Modify: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/statistics/repository/BrawlerBattleResultStatsJpaRepositoryCustomImpl.kt`
- Modify: 동일 디렉터리의 `BrawlerPairBattleResultStatsJpaRepositoryCustomImpl.kt`, `BrawlerEnemyBattleResultStatsJpaRepositoryCustomImpl.kt`, `BrawlerPairBattleRankStatsJpaRepositoryCustomImpl.kt`

각 파일에 **그 파일 전용** private `conditions()` 를 만들고 각 메서드의 `.where(...)` 를 `.where(*conditions(...))` 로 교체한다. 파일 밖 공유 헬퍼는 만들지 않는다 (스펙 S-4′ 판정).

- [ ] **Step 1: `BrawlerBattleResultStatsJpaRepositoryCustomImpl` 정리** — 클래스 끝에 추가:

```kotlin
    private fun conditions(
        eventBrawlStarsId: Long?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ) = arrayOf(
        eventBrawlStarsId?.let { brawlerBattleResultStatsEntity.eventBrawlStarsId.eq(it) },
        brawlerBattleResultStatsEntity.tierRange.eq(tierRange),
        brawlerBattleResultStatsEntity.battleDate.goe(startDate),
        brawlerBattleResultStatsEntity.battleDate.loe(endDate),
    )
```

두 메서드의 where 를 교체: `.where(*conditions(eventBrawlStarsId, tierRange, startDate, endDate))`

- [ ] **Step 2: `BrawlerPairBattleResultStatsJpaRepositoryCustomImpl` 정리**

```kotlin
    private fun conditions(
        eventBrawlStarsId: Long?,
        brawlerBrawlStarsId: Long,
        pairBrawlerBrawlStarsId: Long?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ) = arrayOf(
        eventBrawlStarsId?.let { brawlerPairBattleResultStatsEntity.eventBrawlStarsId.eq(it) },
        brawlerPairBattleResultStatsEntity.brawlerBrawlStarsId.eq(brawlerBrawlStarsId),
        pairBrawlerBrawlStarsId?.let { brawlerPairBattleResultStatsEntity.pairBrawlerBrawlStarsId.eq(it) },
        brawlerPairBattleResultStatsEntity.tierRange.eq(tierRange),
        brawlerPairBattleResultStatsEntity.startBattleDate.goe(startDate),
        brawlerPairBattleResultStatsEntity.endBattleDate.loe(endDate),
    )
```

두 메서드 where 교체: `.where(*conditions(eventBrawlStarsId, brawlerBrawlStarsId, pairBrawlerBrawlStarsId, tierRange, startDate, endDate))`

- [ ] **Step 3: `BrawlerEnemyBattleResultStatsJpaRepositoryCustomImpl` 정리** (pair 없는 5개 조건, 두 메서드 교체):

```kotlin
    private fun conditions(
        eventBrawlStarsId: Long?,
        brawlerBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ) = arrayOf(
        eventBrawlStarsId?.let { brawlerEnemyBattleResultStatsEntity.eventBrawlStarsId.eq(it) },
        brawlerEnemyBattleResultStatsEntity.brawlerBrawlStarsId.eq(brawlerBrawlStarsId),
        brawlerEnemyBattleResultStatsEntity.tierRange.eq(tierRange),
        brawlerEnemyBattleResultStatsEntity.startBattleDate.goe(startDate),
        brawlerEnemyBattleResultStatsEntity.endBattleDate.loe(endDate),
    )
```

- [ ] **Step 4: `BrawlerPairBattleRankStatsJpaRepositoryCustomImpl` 정리** (3개 메서드 — `countTotalBattleCount` 는 pair 조건이 원래 없으므로 `pairBrawlerBrawlStarsId = null` 로 호출해 동일 predicate 유지):

```kotlin
    private fun conditions(
        eventBrawlStarsId: Long,
        brawlerBrawlStarsId: Long,
        pairBrawlerBrawlStarsId: Long?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ) = arrayOf(
        brawlerPairBattleRankStatsEntity.eventBrawlStarsId.eq(eventBrawlStarsId),
        brawlerPairBattleRankStatsEntity.brawlerBrawlStarsId.eq(brawlerBrawlStarsId),
        pairBrawlerBrawlStarsId?.let { brawlerPairBattleRankStatsEntity.pairBrawlerBrawlStarsId.eq(it) },
        brawlerPairBattleRankStatsEntity.tierRange.eq(tierRange),
        brawlerPairBattleRankStatsEntity.startBattleDate.goe(startDate),
        brawlerPairBattleRankStatsEntity.endBattleDate.loe(endDate),
    )
```

- `findAllByConditions`/`countDistinctPairBrawlers`: `.where(*conditions(eventBrawlStarsId, brawlerBrawlStarsId, pairBrawlerBrawlStarsId, tierRange, startDate, endDate))`
- `countTotalBattleCount`: `.where(*conditions(eventBrawlStarsId, brawlerBrawlStarsId, null, tierRange, startDate, endDate))`

- [ ] **Step 5: 동작 보존 확인**

Run: `./gradlew :infrastructure:db-core:compileKotlin :core:core-api:test --tests "com.imstargg.core.application.statistics.BattleResultStatisticsRepositoryTest" --tests "com.imstargg.core.application.statistics.BattleRankStatisticsRepositoryTest"`
Expected: PASS

- [ ] **Step 6: 커밋**

```bash
git add -A imstargg-backend/infrastructure/db-core
git commit -m "refactor(db-core): 통계 CustomImpl 의 반복 where 절을 파일 내 사설 함수로 정리

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Phase 3 — 성능 위생

### Task 13: C-1 — 이벤트 맵 이름 검색을 캐시된 목록 필터로 전환

**Files:**
- Test(수정): `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/application/brawlstars/BattleEventServiceTest.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/brawlstars/BattleEventRepository.kt` (searchByMapName → findAll)
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/brawlstars/BattleEventRepositoryWithCache.kt` (findAll 캐시 추가)
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/brawlstars/BattleEventService.kt` (search 전환)
- Modify: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/brawlstars/repository/MessageJpaRepository.kt` (미사용 쿼리 삭제)

- [ ] **Step 1: 검색 매칭 테스트 추가 (전환 전 — 현재 구현으로 PASS 확인)** — `BattleEventServiceTest` 에 추가. 생성자에 `private val messageJpaRepository: MessageJpaRepository,` 파라미터 추가, import 추가 (`MessageEntity`, `MessageType` — `com.imstargg.infrastructure.db.core.brawlstars.*`, `MessageJpaRepository` — `...brawlstars.repository.*`, `Language` — `com.imstargg.core.enums.Language`, `BattleEventEntity` — `...brawlstars.event.BattleEventEntity`):

```kotlin
    @Test
    fun `search 는 맵 이름 메시지에 부분 일치(대소문자 무시)하는 이벤트를 반환한다`() {
        // given
        battleEventJpaRepository.save(
            BattleEventEntity(brawlStarsId = 15_000_777L, mode = null, mapName = "Hard Rock Mine"),
        )
        battleEventJpaRepository.save(
            BattleEventEntity(brawlStarsId = 15_000_778L, mode = null, mapName = "Snake Prairie"),
        )
        messageJpaRepository.save(
            MessageEntity(
                type = MessageType.BATTLE_EVENT_NAME,
                brawlStarsId = 15_000_777L,
                lang = Language.DEFAULT,
                content = "Hard Rock Mine",
            ),
        )
        messageJpaRepository.save(
            MessageEntity(
                type = MessageType.BATTLE_EVENT_NAME,
                brawlStarsId = 15_000_778L,
                lang = Language.DEFAULT,
                content = "Snake Prairie",
            ),
        )

        // when
        val result = sut.search("rock")

        // then
        assertThat(result).hasSize(1)
        assertThat(result.first().id.value).isEqualTo(15_000_777L)
    }
```

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.application.brawlstars.BattleEventServiceTest"`
Expected: PASS (동작 고정 — 전환 후에도 이 테스트가 그대로 통과해야 함)

- [ ] **Step 2: `BattleEventRepository` 에 `findAll()` 추가, `searchByMapName()` 삭제** — `find(id)` 메서드 위에 추가:

```kotlin
    fun findAll(): List<BattleEvent> {
        val eventEntities = battleEventJpaRepository.findAll().toList()
        if (eventEntities.isEmpty()) return emptyList()

        val eventIds = eventEntities.map { it.brawlStarsId }
        val modeIds = eventEntities.mapNotNull { it.mode }.map { it.brawlStarsId }

        val mapNameMessages =
            messageJpaRepository
                .findAllByTypeAndBrawlStarsIdIn(MessageType.BATTLE_EVENT_NAME, eventIds)
                .groupBy { it.brawlStarsId }
        val modeNameMessages =
            messageJpaRepository
                .findAllByTypeAndBrawlStarsIdIn(MessageType.BATTLE_EVENT_MODE_NAME, modeIds)
                .groupBy { it.brawlStarsId }
        val modeIconImages =
            brawlStarsImageJpaRepository
                .findAllByTypeAndBrawlStarsIdIn(BrawlStarsImageType.BATTLE_EVENT_MODE_ICON, modeIds)
                .associateBy { it.brawlStarsId }

        return eventEntities.map { eventEntity ->
            toBattleEvent(eventEntity, mapNameMessages, modeNameMessages, modeIconImages)
        }
    }
```

`searchByMapName()` 메서드(28-58행) 전체 삭제.

- [ ] **Step 3: WithCache 에 캐시 추가** — `BattleEventRepositoryWithCache` 의 `find` 아래:

```kotlin
    @Cacheable(cacheNames = [CacheNames.BATTLE_EVENT], key = "'all'")
    fun findAll(): List<BattleEvent> = battleEventRepository.findAll()
```

- [ ] **Step 4: Service 전환** — `BattleEventService`: 생성자에서 `battleEventRepository` 제거, search 교체:

```kotlin
    fun search(name: String): List<BattleEvent> =
        battleEventRepositoryWithCache
            .findAll()
            .filter { it.mapName.containsIgnoreCase(name) }
```

- [ ] **Step 5: 미사용 JPA 쿼리 삭제** — 사용처 확인 후 `MessageJpaRepository.findAllByTypeAndContentContainingIgnoreCase` 제거:

Run: `grep -rn "findAllByTypeAndContentContainingIgnoreCase" --include="*.kt" . | grep -v build/`
Expected: `MessageJpaRepository.kt` 선언부만 남음 → 해당 메서드 삭제

- [ ] **Step 6: 통과 확인**

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.application.brawlstars.BattleEventServiceTest"`
Expected: PASS (Step 1 테스트 포함 — full scan 없이 동일 결과)

- [ ] **Step 7: 커밋**

```bash
git add -A imstargg-backend
git commit -m "refactor(core-api): 이벤트 맵 이름 검색을 캐시된 목록 메모리 필터로 전환

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

### Task 14: C-2 — `GameSlotRepository` 루프 조회를 벌크 조회로 전환

**Files:**
- Test(수정): `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/application/statistics/GameSlotRepositoryTest.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/statistics/GameSlotRepository.kt:21`

- [ ] **Step 1: 테스트에 캐시 격리 추가** — `GameSlotRepositoryTest` 생성자에 `private val cacheManager: CacheManager,` 추가 (import `org.springframework.cache.CacheManager`, `com.imstargg.core.config.CacheNames`), `@BeforeEach` 추가 (import `org.junit.jupiter.api.BeforeEach`):

```kotlin
    @BeforeEach
    fun setUp() {
        cacheManager.getCache(CacheNames.BATTLE_EVENT)?.clear()
    }
```

- [ ] **Step 2: 벌크 구현으로 교체** — `GameSlotRepository.findAllByEventIds`:

```kotlin
    fun findAllByEventIds(eventIds: List<BrawlStarsId>): GameSlots {
        val slotByEventId =
            battleEventRepositoryWithCache
                .findAll()
                .associate { event -> event.id to (event.mode?.playerCounts?.let(::GameSlot) ?: GameSlot.default()) }
        return GameSlots(
            eventIds
                .distinct()
                .mapNotNull { id -> slotByEventId[id]?.let { id to it } }
                .toMap(),
        )
    }
```

(`find(eventId)` 단건 메서드는 그대로 유지 — 미존재 이벤트는 `GameSlots.get` 의 default 폴백이 처리)

- [ ] **Step 3: 통과 확인 (기존 findAllByEventIds 테스트가 동작 보존 검증)**

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.application.statistics.GameSlotRepositoryTest" --tests "com.imstargg.core.application.statistics.BattleResultStatisticsRepositoryTest"`
Expected: PASS

- [ ] **Step 4: 커밋**

```bash
git add -A imstargg-backend/core/core-api
git commit -m "refactor(core-api): GameSlot 조회를 이벤트별 루프에서 캐시 벌크 조회로 전환

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

### Task 15: C-3 — 이미지 전체 로드 후 메모리 필터 제거

**Files:**
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/brawlstars/BrawlerRepository.kt:57-61,105`

- [ ] **Step 1: `findAll()` 의 초상화 조회 교체** (57-61행):

```kotlin
        val portraits =
            brawlStarsImageJpaRepository
                .findAllByTypeIn(listOf(BrawlStarsImageType.BRAWLER_PORTRAIT))
                .associateBy { it.brawlStarsId }
```

- [ ] **Step 2: `findAllBrawlerItems()` 의 이미지 조회 교체** (105행) + 클래스 하단에 companion 추가:

```kotlin
        val imagesByKey =
            brawlStarsImageJpaRepository
                .findAllByTypeIn(ITEM_IMAGE_TYPES)
                .associateBy { it.type to it.brawlStarsId }
```

```kotlin
    companion object {
        private val ITEM_IMAGE_TYPES =
            listOf(
                BrawlStarsImageType.GADGET,
                BrawlStarsImageType.GEAR,
                BrawlStarsImageType.STARPOWER,
                BrawlStarsImageType.HYPERCHARGE,
                BrawlStarsImageType.BUFFY_GADGET,
                BrawlStarsImageType.BUFFY_STAR_POWER,
                BrawlStarsImageType.BUFFY_HYPER_CHARGE,
            )
    }
```

- [ ] **Step 3: 통과 확인**

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.application.brawlstars.BrawlerServiceTest" --tests "com.imstargg.core.application.brawlstars.PlayerServiceTest"`
Expected: PASS

- [ ] **Step 4: 커밋**

```bash
git add -A imstargg-backend/core/core-api
git commit -m "refactor(core-api): 이미지 전체 로드를 타입 조건 조회로 교체

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

### Task 16: C-4 — gzip 응답 압축 활성화

**Files:**
- Modify: `imstargg-backend/core/core-api/src/main/resources/application.yml:21-27`

- [ ] **Step 1: server 블록에 compression 추가** (기본 문서 — 전 프로파일 적용):

```yaml
server:
  shutdown: graceful
  compression:
    enabled: true
    mime-types: application/json
    min-response-size: 1024
  tomcat:
    max-connections: 20000
    threads:
      max: 600
      min-spare: 100
```

- [ ] **Step 2: 로컬 검증** (docker 인프라 필요):

```bash
./gradlew :core:core-api:bootRun --args='--spring.profiles.active=local' &
sleep 25
curl -sI -H "Accept-Encoding: gzip" "http://localhost:8080/v1/brawlers" | grep -i "content-encoding"
kill %1
```

Expected: `content-encoding: gzip` (응답이 1KB 미만이면 통계 엔드포인트로 재시도하거나 min-response-size 동작으로 간주)

- [ ] **Step 3: 커밋**

```bash
git add imstargg-backend/core/core-api/src/main/resources/application.yml
git commit -m "feat(core-api): gzip 응답 압축 활성화

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

### Task 17: C-5 — 정적 성격 GET 에 `Cache-Control` 헤더 부여

**Files:**
- Test(수정): `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/config/WebMvcConfigTest.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/config/WebMvcConfig.kt`

- [ ] **Step 1: 실패하는 테스트 작성** — `WebMvcConfigTest` 에 추가 (import 추가: `org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get`):

```kotlin
    @Test
    fun `브롤러 목록 GET 응답에 Cache-Control 헤더가 붙는다`() {
        mockMvc
            .perform(get("/v1/brawlers"))
            .andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=300, stale-while-revalidate=60"))
    }

    @Test
    fun `브롤러 통계 GET 응답에 Cache-Control 헤더가 붙는다`() {
        mockMvc
            .perform(
                get("/v1/statistics/brawler/result")
                    .param("tierRange", "TROPHY_1000_PLUS")
                    .param("dateRange", "THIS_WEEK"),
            ).andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=1800, stale-while-revalidate=300"))
    }

    @Test
    fun `플레이어 조회 응답에는 Cache-Control 헤더가 없다`() {
        mockMvc
            .perform(get("/v1/players/%23NOPE"))
            .andExpect(header().doesNotExist(HttpHeaders.CACHE_CONTROL))
    }
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.config.WebMvcConfigTest"`
Expected: FAIL — Cache-Control 헤더 부재

- [ ] **Step 3: 구현** — `WebMvcConfig` 에 인터셉터 추가:

```kotlin
package com.imstargg.core.config

import org.springframework.context.annotation.Configuration
import org.springframework.http.CacheControl
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.mvc.WebContentInterceptor
import java.time.Duration

@Configuration
class WebMvcConfig(
    private val cors: CorsProperties,
) : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        if (cors.allowedOrigins.isEmpty()) return
        registry
            .addMapping("/v1/**")
            .allowedOrigins(*cors.allowedOrigins.toTypedArray())
            .allowedMethods("GET", "POST", "OPTIONS")
            .allowCredentials(false)
            .maxAge(3600)
    }

    /**
     * 정적 성격 GET(브롤러·이벤트·브롤러 통계)에만 Cache-Control 부여.
     * 서버 캐시 TTL(Caffeine 5분 / Redis statistics 30분)과 일치시킨다.
     * 플레이어 데이터(프로필·전투·갱신·플레이어 통계)는 갱신 직후 최신성이 중요하므로 제외.
     */
    override fun addInterceptors(registry: InterceptorRegistry) {
        val cacheControlInterceptor =
            WebContentInterceptor().apply {
                addCacheMapping(
                    CacheControl.maxAge(Duration.ofMinutes(5)).staleWhileRevalidate(Duration.ofMinutes(1)),
                    "/v1/brawlers/**",
                    "/v1/brawler/**",
                    "/v1/events/**",
                    "/v1/event/**",
                )
                addCacheMapping(
                    CacheControl.maxAge(Duration.ofMinutes(30)).staleWhileRevalidate(Duration.ofMinutes(5)),
                    "/v1/statistics/brawler/**",
                )
            }
        registry
            .addInterceptor(cacheControlInterceptor)
            .addPathPatterns(
                "/v1/brawlers/**",
                "/v1/brawler/**",
                "/v1/events/**",
                "/v1/event/**",
                "/v1/statistics/brawler/**",
            )
    }
}
```

- [ ] **Step 4: 통과 확인**

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.config.WebMvcConfigTest" --tests "com.imstargg.core.api.controller.v1.StatisticsControllerTest"`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add -A imstargg-backend/core/core-api
git commit -m "feat(core-api): 브롤러·이벤트·통계 GET 에 Cache-Control 헤더 부여

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

### Task 18: C-6 — Renewal 요청 흐름 왕복 축소 + 경합 매핑

**Files:**
- Test(수정): `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/application/renewal/RenewalServiceTest.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/renewal/RenewalRepository.kt` (request 재설계)
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/renewal/RenewalService.kt` (doRenew 정리)

- [ ] **Step 1: 신규 테스트 추가 (stale 재요청 경로)** — `RenewalServiceTest` 의 `RenewPlayer` nested 에 추가:

```kotlin
        @Test
        fun `stale 상태의 기존 갱신은 재요청되어 PENDING 이 된다`() {
            // given - 임계(10초)를 넘긴 오래된 요청
            val tag = BrawlStarsTag("#PSTL")
            renewalJpaRepository.save(
                aRenewalEntity()
                    .withBrawlStarsTag(tag.value)
                    .withType(RenewalType.PLAYER)
                    .withRequestedAt(now.minus(Duration.ofHours(1)))
                    .build(),
            )

            // when
            val result = sut.renewPlayer(tag)

            // then
            assertThat(result.status).isEqualTo(RenewalStatus.PENDING)
            assertThat(result.requestedAt).isEqualTo(now)
            verify { renewalProducer.producePlayerRenewal(tag) }
        }
```

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.application.renewal.RenewalServiceTest"`
Expected: PASS (현행 동작 고정 — 리팩터링 후에도 전부 통과해야 함)

- [ ] **Step 2: `RenewalRepository.request` 를 "체크+쓰기 단일 트랜잭션, 불가 시 null" 로 재설계**:

```kotlin
    /**
     * 갱신 요청을 저장한다. 진행 중(재요청 불가) 갱신이 이미 있으면 null.
     * 최초 요청 동시 경합은 uk_renewal__brawlstarstag_type 제약 위반(DataIntegrityViolationException)으로
     * 드러나며 호출부(RenewalService)가 RENEWAL_ALREADY_IN_PROGRESS 로 매핑한다.
     */
    @Transactional
    fun request(
        tag: BrawlStarsTag,
        type: RenewalType,
        requestedAt: Instant,
    ): Renewal? {
        val entity = renewalJpaRepository.findByBrawlStarsTagAndType(tag.value, type)
        if (entity != null) {
            if (!toRenewal(entity).canRenew(requestedAt)) return null
            entity.reRequest(requestedAt)
            return toRenewal(entity)
        }
        val newEntity =
            RenewalEntity(
                brawlStarsTag = tag.value,
                type = type,
                requestedAt = requestedAt,
            )
        renewalJpaRepository.save(newEntity)
        return toRenewal(newEntity)
    }
```

- [ ] **Step 3: `RenewalService.doRenew` 정리** (사전 find 제거, 경합 매핑 — import `org.springframework.dao.DataIntegrityViolationException` 추가):

```kotlin
    private fun doRenew(
        tag: BrawlStarsTag,
        type: RenewalType,
        publishAction: () -> Unit,
    ): Renewal {
        if (brawlStarsApiMaintenanceRepository.isUnderMaintenance()) {
            throw CoreException(ErrorType.BRAWLSTARS_API_UNDER_MAINTENANCE)
        }

        val now = clock.instant()
        val renewal =
            try {
                renewalRepository.request(tag, type, now)
            } catch (e: DataIntegrityViolationException) {
                null
            } ?: throw CoreException(ErrorType.RENEWAL_ALREADY_IN_PROGRESS, "tag=$tag, type=$type")

        publishAction()

        log.info("[갱신 요청] type={} tag={}", type, tag.value)
        return renewalRepository.toPending(renewal)
    }
```

주: 경합(DataIntegrityViolation) 경로는 동시성 없이는 재현 불가라 통합 테스트를 두지 않는다 — 매핑 코드가 자명하고, 유니크 제약 자체는 스키마가 보장.

- [ ] **Step 4: 통과 확인 (기존 4개 + 신규 1개 전부)**

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.application.renewal.RenewalServiceTest"`
Expected: PASS — 같은 행 조회가 3회→2회로 줄고 체크·쓰기가 한 트랜잭션이 됨

- [ ] **Step 5: 커밋**

```bash
git add -A imstargg-backend/core/core-api
git commit -m "refactor(core-api): Renewal 요청의 중복 조회 축소 및 경합을 409 로 매핑

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Phase 4 — 캐시 메트릭

### Task 19: D-2 — 캐시 히트율 메트릭 노출

**Files:**
- Create: `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/config/CacheMetricsTest.kt`
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/config/CacheMetricsConfig.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/config/CacheConfig.kt:66` (enableStatistics)

- [ ] **Step 1: 실패하는 컨텍스트 테스트 작성** — `CacheMetricsTest.kt` 신규:

```kotlin
package com.imstargg.core.config

import com.imstargg.CoreApiContextTest
import io.micrometer.core.instrument.MeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.cache.CacheManager
import org.springframework.data.redis.cache.RedisCache
import org.springframework.data.redis.cache.RedisCacheManager

class CacheMetricsTest(
    private val meterRegistry: MeterRegistry,
    private val cacheManager: CacheManager,
    private val redisCacheManager: RedisCacheManager,
) : CoreApiContextTest() {
    @Test
    fun `caffeine 캐시 메트릭이 등록되어 있다`() {
        // given - 캐시 접근으로 통계 발생
        cacheManager.getCache(CacheNames.BRAWLER)!!.also {
            it.put("probe", "value")
            it.get("probe")
        }

        // when // then
        assertThat(meterRegistry.find("cache.gets").tag("cache", CacheNames.BRAWLER).meters).isNotEmpty()
    }

    @Test
    fun `redis statistics 캐시의 통계 수집이 활성화되어 있다`() {
        // given
        val cache = redisCacheManager.getCache(CacheNames.STATISTICS) as RedisCache

        // when
        cache.get("metrics-probe")

        // then
        assertThat(cache.statistics.gets).isGreaterThan(0)
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.config.CacheMetricsTest"`
Expected: FAIL (caffeine meter 미등록 그리고/또는 redis gets == 0)
주: 첫 번째 테스트가 이미 PASS 라면 Boot 자동 바인딩이 동작하는 것 — 그 경우 Step 3 의 `CacheMetricsConfig` 생성은 건너뛰고 enableStatistics 만 적용한다.

- [ ] **Step 3: 구현**

`CacheConfig.kt` 의 statisticsConfig 에 통계 활성화 추가:

```kotlin
        val statisticsConfig =
            RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .computePrefixWith { cacheName -> "$cacheName::" }
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(serializer),
                ).disableCachingNullValues()
```

→ `RedisCacheManager.builder(...)` 체인에 `.enableStatistics()` 추가:

```kotlin
        return RedisCacheManager
            .builder(connectionFactory)
            .cacheDefaults(statisticsConfig)
            .initialCacheNames(setOf(CacheNames.STATISTICS))
            .disableCreateOnMissingCache()
            .enableStatistics()
            .build()
```

`CacheMetricsConfig.kt` 신규 (CompositeCacheManager 는 Boot 자동 바인딩 대상이 아닐 수 있어 Caffeine 을 명시 바인딩):

```kotlin
package com.imstargg.core.config

import com.github.benmanes.caffeine.cache.Cache
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CacheMetricsConfig {
    @Bean
    fun caffeineCacheMetricsBinder(caffeineCacheManager: CaffeineCacheManager): MeterBinder =
        MeterBinder { registry ->
            caffeineCacheManager.cacheNames.forEach { name ->
                @Suppress("UNCHECKED_CAST")
                val nativeCache = caffeineCacheManager.getCache(name)?.nativeCache as? Cache<Any, Any> ?: return@forEach
                CaffeineCacheMetrics.monitor(registry, nativeCache, name)
            }
        }
}
```

- [ ] **Step 4: 통과 확인**

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.config.CacheMetricsTest" --tests "com.imstargg.core.config.CacheConfigSerializerTest"`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
git add -A imstargg-backend/core/core-api
git commit -m "feat(core-api): 캐시 히트율 메트릭 노출 (Caffeine 바인딩·Redis 통계 활성화)

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 20: 최종 검증 및 마무리

**Files:** 없음 (검증)

- [ ] **Step 1: 린트 정리**

Run: `./gradlew ktlintFormat ktlintCheck`
Expected: BUILD SUCCESSFUL (포맷 변경이 생겼으면 `chore(core-api): ktlint 포맷 정리` 로 별도 커밋)

- [ ] **Step 2: 백엔드 전체 테스트**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL — core-api 외 모듈(worker/batch/admin) 무영향 확인

- [ ] **Step 3: 스펙 성공 기준 대조** ([스펙 §7](../specs/2026-07-07-core-api-review-refactoring-design.md))

```bash
# 기준 2: domain 패키지에 Spring import 없음
grep -rn "org.springframework" imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/ && echo "FAIL" || echo "OK"
# 기준 1: 19개 항목 매핑 — Task 1(A-1), 3(A-2), 6(A-3), 1·2(B-1), 4(B-2), 5(B-3),
#          7·8(S-1), 10(S-2·S-6a), 11(S-3·S-6b), 12(S-4), 9(S-5), 7·8·10(S-6),
#          13(C-1), 14(C-2), 15(C-3), 16(C-4), 17(C-5), 18(C-6), 19(D-2)
git log --oneline develop..HEAD 2>/dev/null || git log --oneline -25
```

Expected: grep OK, 커밋이 Task 별로 존재

- [ ] **Step 4: PR 준비 (사용자 확인 후 진행)**

- 브랜치를 `feature/core-api-review-refactoring` 이름으로 push: `git push -u origin HEAD:refs/heads/feature/core-api-review-refactoring`
- PR 은 `develop` 대상. gh 는 Cho-D-YoungRae 계정 + `-R` 리포 명시 필요 (리모트가 SSH 별칭).
- 버전 bump(core-api SNAPSHOT patch)는 PR 생성 시점에 `/release` 스킬로 처리.

---

## 부록: 작업 중 발견되는 편차 처리

- 이 계획의 코드는 2026-07-07 시점 소스 기준이다. 라인 번호가 어긋나면 **코드 내용**으로 위치를 찾는다.
- 계획과 실제 시그니처/상수명이 다르면(예: enum 상수) 컴파일 에러 메시지에 따라 실제 소스에 맞추되, 스펙의 의도(항목 A/B/S/C/D)를 유지한다.
- 새 공용 추상화(파라미터 객체·크로스 파일 헬퍼·제네릭 빌더)를 추가하고 싶어지면 멈춘다 — 스펙 §4 Phase 2 판정 기준 위반.
