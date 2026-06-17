# 계획 1 — 백엔드: core-admin 생성 API 원자적 확장 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** core-admin 의 브롤러/기어/이벤트/이벤트 모드 생성 API 4종을 확장해, 이름·세부 설정을 한 트랜잭션에 함께 저장한다.

**Architecture:** 컨트롤러는 변경하지 않는다(요청 DTO를 그대로 서비스에 전달). 각 요청 DTO에 필드를 추가하고, 각 서비스의 `createXxx`가 엔티티 저장 후 기존 private `doUpdateMessages`(공백 필터 포함)와 직접 저장 로직으로 이름·기어 등을 같은 트랜잭션에 저장한다. 이미지는 본 계획 범위 밖(기존 분리된 `PUT .../image` 유지).

**Tech Stack:** Kotlin, Spring Boot, JPA, JUnit5 + AssertJ, `CoreAdminContextTest`(@SpringBootTest + 실 DB) + `CleanUp.all()`.

**작업 디렉터리:** 모든 gradle 명령은 `imstargg-backend/` 에서 실행. 파일 경로는 저장소 루트 기준.

**공통 규칙:** 모든 커밋 메시지 끝에 트레일러를 추가 — `-m "<title>" -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"`.

---

## File Structure

**수정 (요청 DTO):**
- `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/request/BrawlerCreateRequest.kt`
- `.../request/GearCreateRequest.kt`
- `.../request/BattleEventCreateRequest.kt`
- `.../request/BattleEventModeCreateRequest.kt`

**수정 (서비스):**
- `.../application/BrawlerService.kt` — `createBrawler`
- `.../application/GearService.kt` — `create`
- `.../application/BattleEventService.kt` — `createEvent`, `createMode`

**생성 (테스트):**
- `imstargg-backend/core/core-admin/src/test/kotlin/com/imstargg/core/admin/application/BrawlerServiceTest.kt`
- `.../application/GearServiceTest.kt`
- `.../application/BattleEventServiceTest.kt`

**변경 없음:** 컨트롤러 4종(`BrawlerController`, `GearController`, `BattleEventController`, `BattleEventModeController`), 엔티티, 마이그레이션/스키마.

---

## Task 1: 브롤러 생성 API 확장 (이름 + 기어)

**Files:**
- Modify: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/request/BrawlerCreateRequest.kt`
- Modify: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/application/BrawlerService.kt:178-181`
- Test: `imstargg-backend/core/core-admin/src/test/kotlin/com/imstargg/core/admin/application/BrawlerServiceTest.kt` (생성)

- [ ] **Step 1: 요청 DTO에 필드 추가**

`BrawlerCreateRequest.kt` 전체를 아래로 교체:

```kotlin
package com.imstargg.core.admin.request

import com.imstargg.core.enums.BrawlerRarity
import com.imstargg.core.enums.BrawlerRole
import com.imstargg.core.enums.Language

data class BrawlerCreateRequest(
    val brawlStarsId: Long,
    val rarity: BrawlerRarity,
    val role: BrawlerRole,
    val names: Map<Language, String> = emptyMap(),
    val gearBrawlStarsIds: Set<Long> = emptySet(),
)
```

- [ ] **Step 2: 실패하는 테스트 작성**

`BrawlerServiceTest.kt` 생성:

```kotlin
package com.imstargg.core.admin.application

import com.imstargg.CoreAdminContextTest
import com.imstargg.core.admin.request.BrawlerCreateRequest
import com.imstargg.core.enums.BrawlerRarity
import com.imstargg.core.enums.BrawlerRole
import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.brawlstars.MessageType
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.BrawlerGearJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.BrawlerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.repository.MessageJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BrawlerServiceTest
    @Autowired
    constructor(
        private val sut: BrawlerService,
        private val brawlerJpaRepository: BrawlerJpaRepository,
        private val brawlerGearJpaRepository: BrawlerGearJpaRepository,
        private val messageJpaRepository: MessageJpaRepository,
        private val cleanUp: CleanUp,
    ) : CoreAdminContextTest() {
        @AfterEach
        fun tearDown() {
            cleanUp.all()
        }

        @Test
        fun `브롤러 등록 시 이름과 기어가 함께 저장된다`() {
            // given
            val request =
                BrawlerCreateRequest(
                    brawlStarsId = 16000000L,
                    rarity = BrawlerRarity.MYTHIC,
                    role = BrawlerRole.DAMAGE_DEALER,
                    names = mapOf(Language.KOREAN to "셸리", Language.ENGLISH to "Shelly"),
                    gearBrawlStarsIds = setOf(62000000L, 62000001L),
                )

            // when
            sut.createBrawler(request)

            // then
            assertThat(brawlerJpaRepository.findByBrawlStarsId(16000000L)).isNotNull
            val names =
                messageJpaRepository
                    .findAllByTypeAndBrawlStarsIdIn(MessageType.BRAWLER_NAME, listOf(16000000L))
                    .associate { it.lang to it.content }
            assertThat(names)
                .containsExactlyInAnyOrderEntriesOf(
                    mapOf(Language.KOREAN to "셸리", Language.ENGLISH to "Shelly"),
                )
            val gearIds =
                brawlerGearJpaRepository.findAllByBrawlerBrawlStarsId(16000000L).map { it.gearBrawlStarsId }
            assertThat(gearIds).containsExactlyInAnyOrder(62000000L, 62000001L)
        }
    }
```

- [ ] **Step 3: 테스트 실행 → 실패 확인**

Run: `cd imstargg-backend && ./gradlew :core:core-admin:test --tests "com.imstargg.core.admin.application.BrawlerServiceTest"`
Expected: FAIL — `names` 가 비어 있고 `gearIds` 가 비어 있음(현재 `createBrawler` 는 브롤러만 저장).

- [ ] **Step 4: 서비스 구현**

`BrawlerService.kt` 의 `createBrawler`(178-181행)를 교체:

```kotlin
    @Transactional
    fun createBrawler(request: BrawlerCreateRequest) {
        brawlerJpaRepository.save(BrawlerEntity(request.brawlStarsId, request.rarity, request.role))
        doUpdateMessages(MessageType.BRAWLER_NAME, request.brawlStarsId, request.names)
        request.gearBrawlStarsIds.forEach { gearId ->
            brawlerGearJpaRepository.save(BrawlerGearEntity(request.brawlStarsId, gearId))
        }
    }
```

(`doUpdateMessages`, `brawlerGearJpaRepository`, `BrawlerGearEntity`, `MessageType` 는 이미 이 파일에서 import/사용 중이므로 추가 import 불필요.)

- [ ] **Step 5: 테스트 실행 → 통과 확인**

Run: `cd imstargg-backend && ./gradlew :core:core-admin:test --tests "com.imstargg.core.admin.application.BrawlerServiceTest"`
Expected: PASS

- [ ] **Step 6: 커밋**

```bash
cd imstargg-backend && ./gradlew ktlintFormat
git add core/core-admin/src/main/kotlin/com/imstargg/core/admin/request/BrawlerCreateRequest.kt \
        core/core-admin/src/main/kotlin/com/imstargg/core/admin/application/BrawlerService.kt \
        core/core-admin/src/test/kotlin/com/imstargg/core/admin/application/BrawlerServiceTest.kt
git commit -m "feat(core-admin): 브롤러 생성 시 이름·기어 원자적 저장" \
           -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 2: 기어 생성 API 확장 (이름)

**Files:**
- Modify: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/request/GearCreateRequest.kt`
- Modify: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/application/GearService.kt:50-53`
- Test: `imstargg-backend/core/core-admin/src/test/kotlin/com/imstargg/core/admin/application/GearServiceTest.kt` (생성)

- [ ] **Step 1: 요청 DTO에 필드 추가**

`GearCreateRequest.kt` 전체를 아래로 교체:

```kotlin
package com.imstargg.core.admin.request

import com.imstargg.core.enums.GearRarity
import com.imstargg.core.enums.Language

data class GearCreateRequest(
    val brawlStarsId: Long,
    val rarity: GearRarity,
    val names: Map<Language, String> = emptyMap(),
)
```

- [ ] **Step 2: 실패하는 테스트 작성**

`GearServiceTest.kt` 생성:

```kotlin
package com.imstargg.core.admin.application

import com.imstargg.CoreAdminContextTest
import com.imstargg.core.admin.request.GearCreateRequest
import com.imstargg.core.enums.GearRarity
import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.brawlstars.MessageType
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.GearJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.repository.MessageJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class GearServiceTest
    @Autowired
    constructor(
        private val sut: GearService,
        private val gearJpaRepository: GearJpaRepository,
        private val messageJpaRepository: MessageJpaRepository,
        private val cleanUp: CleanUp,
    ) : CoreAdminContextTest() {
        @AfterEach
        fun tearDown() {
            cleanUp.all()
        }

        @Test
        fun `기어 등록 시 이름이 함께 저장된다`() {
            // given
            val request =
                GearCreateRequest(
                    brawlStarsId = 62000000L,
                    rarity = GearRarity.SUPER_RARE,
                    names = mapOf(Language.KOREAN to "속도", Language.ENGLISH to "Speed"),
                )

            // when
            sut.create(request)

            // then
            assertThat(gearJpaRepository.findAll().map { it.brawlStarsId }).contains(62000000L)
            val names =
                messageJpaRepository
                    .findAllByTypeAndBrawlStarsIdIn(MessageType.GEAR_NAME, listOf(62000000L))
                    .associate { it.lang to it.content }
            assertThat(names)
                .containsExactlyInAnyOrderEntriesOf(
                    mapOf(Language.KOREAN to "속도", Language.ENGLISH to "Speed"),
                )
        }
    }
```

- [ ] **Step 3: 테스트 실행 → 실패 확인**

Run: `cd imstargg-backend && ./gradlew :core:core-admin:test --tests "com.imstargg.core.admin.application.GearServiceTest"`
Expected: FAIL — `names` 가 비어 있음(현재 `create` 는 기어만 저장).

- [ ] **Step 4: 서비스 구현**

`GearService.kt` 의 `create`(50-53행)를 교체:

```kotlin
    @Transactional
    fun create(request: GearCreateRequest) {
        gearJpaRepository.save(GearEntity(request.brawlStarsId, request.rarity))
        doUpdateMessages(MessageType.GEAR_NAME, request.brawlStarsId, request.names)
    }
```

(`doUpdateMessages`, `MessageType`, `GearEntity` 모두 이 파일에서 이미 사용 중 — 추가 import 불필요.)

- [ ] **Step 5: 테스트 실행 → 통과 확인**

Run: `cd imstargg-backend && ./gradlew :core:core-admin:test --tests "com.imstargg.core.admin.application.GearServiceTest"`
Expected: PASS

- [ ] **Step 6: 커밋**

```bash
cd imstargg-backend && ./gradlew ktlintFormat
git add core/core-admin/src/main/kotlin/com/imstargg/core/admin/request/GearCreateRequest.kt \
        core/core-admin/src/main/kotlin/com/imstargg/core/admin/application/GearService.kt \
        core/core-admin/src/test/kotlin/com/imstargg/core/admin/application/GearServiceTest.kt
git commit -m "feat(core-admin): 기어 생성 시 이름 원자적 저장" \
           -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 3: 이벤트 생성 API 확장 (맵 이름)

**Files:**
- Modify: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/request/BattleEventCreateRequest.kt`
- Modify: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/application/BattleEventService.kt:112-116`
- Test: `imstargg-backend/core/core-admin/src/test/kotlin/com/imstargg/core/admin/application/BattleEventServiceTest.kt` (생성)

- [ ] **Step 1: 요청 DTO에 필드 추가**

`BattleEventCreateRequest.kt` 전체를 아래로 교체:

```kotlin
package com.imstargg.core.admin.request

import com.imstargg.core.enums.Language

data class BattleEventCreateRequest(
    val brawlStarsId: Long,
    val modeId: Long?,
    val mapName: String?,
    val mapNames: Map<Language, String> = emptyMap(),
)
```

- [ ] **Step 2: 실패하는 테스트 작성**

`BattleEventServiceTest.kt` 생성 (Task 4의 모드 테스트도 이 파일에 추가되므로 import 를 미리 포함):

```kotlin
package com.imstargg.core.admin.application

import com.imstargg.CoreAdminContextTest
import com.imstargg.core.admin.request.BattleEventCreateRequest
import com.imstargg.core.admin.request.BattleEventModeCreateRequest
import com.imstargg.core.enums.BattleEventModeResultType
import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.brawlstars.MessageType
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventModeJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.repository.MessageJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BattleEventServiceTest
    @Autowired
    constructor(
        private val sut: BattleEventService,
        private val battleEventJpaRepository: BattleEventJpaRepository,
        private val battleEventModeJpaRepository: BattleEventModeJpaRepository,
        private val messageJpaRepository: MessageJpaRepository,
        private val cleanUp: CleanUp,
    ) : CoreAdminContextTest() {
        @AfterEach
        fun tearDown() {
            cleanUp.all()
        }

        @Test
        fun `이벤트 등록 시 맵 이름이 함께 저장된다`() {
            // given
            val request =
                BattleEventCreateRequest(
                    brawlStarsId = 15000000L,
                    modeId = null,
                    mapName = "Gem Fort",
                    mapNames = mapOf(Language.KOREAN to "젬 요새", Language.ENGLISH to "Gem Fort"),
                )

            // when
            sut.createEvent(request)

            // then
            assertThat(battleEventJpaRepository.findAll().toList().map { it.brawlStarsId }).contains(15000000L)
            val names =
                messageJpaRepository
                    .findAllByTypeAndBrawlStarsIdIn(MessageType.BATTLE_EVENT_NAME, listOf(15000000L))
                    .associate { it.lang to it.content }
            assertThat(names)
                .containsExactlyInAnyOrderEntriesOf(
                    mapOf(Language.KOREAN to "젬 요새", Language.ENGLISH to "Gem Fort"),
                )
        }
    }
```

- [ ] **Step 3: 테스트 실행 → 실패 확인**

Run: `cd imstargg-backend && ./gradlew :core:core-admin:test --tests "com.imstargg.core.admin.application.BattleEventServiceTest"`
Expected: FAIL — `names` 가 비어 있음(현재 `createEvent` 는 이벤트만 저장).

- [ ] **Step 4: 서비스 구현**

`BattleEventService.kt` 의 `createEvent`(112-116행)를 교체:

```kotlin
    @Transactional
    fun createEvent(request: BattleEventCreateRequest) {
        val mode = request.modeId?.let { battleEventModeJpaRepository.findByBrawlStarsId(it) }
        battleEventJpaRepository.save(BattleEventEntity(request.brawlStarsId, mode, request.mapName))
        doUpdateMessages(MessageType.BATTLE_EVENT_NAME, request.brawlStarsId, request.mapNames)
    }
```

(`doUpdateMessages`, `MessageType`, `BattleEventEntity` 모두 이 파일에서 이미 사용 중 — 추가 import 불필요.)

- [ ] **Step 5: 테스트 실행 → 통과 확인**

Run: `cd imstargg-backend && ./gradlew :core:core-admin:test --tests "com.imstargg.core.admin.application.BattleEventServiceTest"`
Expected: PASS

- [ ] **Step 6: 커밋**

```bash
cd imstargg-backend && ./gradlew ktlintFormat
git add core/core-admin/src/main/kotlin/com/imstargg/core/admin/request/BattleEventCreateRequest.kt \
        core/core-admin/src/main/kotlin/com/imstargg/core/admin/application/BattleEventService.kt \
        core/core-admin/src/test/kotlin/com/imstargg/core/admin/application/BattleEventServiceTest.kt
git commit -m "feat(core-admin): 이벤트 생성 시 맵 이름 원자적 저장" \
           -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 4: 이벤트 모드 생성 API 확장 (이름 + 인원수 + 색상)

**Files:**
- Modify: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/request/BattleEventModeCreateRequest.kt`
- Modify: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/application/BattleEventService.kt:78-87`
- Test: `imstargg-backend/core/core-admin/src/test/kotlin/com/imstargg/core/admin/application/BattleEventServiceTest.kt` (Task 3에서 생성한 파일에 테스트 추가)

- [ ] **Step 1: 요청 DTO에 필드 추가**

`BattleEventModeCreateRequest.kt` 전체를 아래로 교체:

```kotlin
package com.imstargg.core.admin.request

import com.imstargg.core.enums.BattleEventModeResultType
import com.imstargg.core.enums.Language

data class BattleEventModeCreateRequest(
    val brawlStarsId: Long,
    val brawlStarsName: String,
    val resultType: BattleEventModeResultType = BattleEventModeResultType.RESULT,
    val names: Map<Language, String> = emptyMap(),
    // 아래 기본값은 BattleEventModeEntity 의 DEFAULT_PLAYER_COUNTS / DEFAULT_COLOR 와 일치
    val playerCounts: List<Int> = listOf(3, 3),
    val color: String = "#2A2E38",
)
```

- [ ] **Step 2: 실패하는 테스트 추가**

`BattleEventServiceTest.kt` 의 클래스 본문(마지막 `}` 직전)에 두 테스트를 추가:

```kotlin
        @Test
        fun `이벤트 모드 등록 시 이름_인원수_색상이 함께 저장된다`() {
            // given
            val request =
                BattleEventModeCreateRequest(
                    brawlStarsId = 48000000L,
                    brawlStarsName = "gemGrab",
                    resultType = BattleEventModeResultType.RESULT,
                    names = mapOf(Language.KOREAN to "젬 그랩", Language.ENGLISH to "Gem Grab"),
                    playerCounts = listOf(3, 3),
                    color = "#1A2B3C",
                )

            // when
            sut.createMode(request)

            // then
            val mode = battleEventModeJpaRepository.findByBrawlStarsId(48000000L)
            assertThat(mode).isNotNull
            assertThat(mode!!.playerCounts).containsExactly(3, 3)
            assertThat(mode.color).isEqualTo("#1A2B3C")
            val names =
                messageJpaRepository
                    .findAllByTypeAndBrawlStarsIdIn(MessageType.BATTLE_EVENT_MODE_NAME, listOf(48000000L))
                    .associate { it.lang to it.content }
            assertThat(names)
                .containsExactlyInAnyOrderEntriesOf(
                    mapOf(Language.KOREAN to "젬 그랩", Language.ENGLISH to "Gem Grab"),
                )
        }

        @Test
        fun `이벤트 모드 등록 시 잘못된 색상이면 예외가 발생한다`() {
            // given
            val request =
                BattleEventModeCreateRequest(
                    brawlStarsId = 48000001L,
                    brawlStarsName = "bounty",
                    color = "not-a-color",
                )

            // when
            val thrown =
                catchThrowableOfType(IllegalArgumentException::class.java) { sut.createMode(request) }

            // then
            assertThat(thrown).isNotNull
        }
```

- [ ] **Step 3: 테스트 실행 → 실패 확인**

Run: `cd imstargg-backend && ./gradlew :core:core-admin:test --tests "com.imstargg.core.admin.application.BattleEventServiceTest"`
Expected: FAIL — `이름_인원수_색상` 테스트에서 `color` 가 기본 `#2A2E38` 이고 이름이 비어 있음(현재 `createMode` 는 `playerCounts·color·names` 를 무시).

- [ ] **Step 4: 서비스 구현**

`BattleEventService.kt` 의 `createMode`(78-87행)를 교체:

```kotlin
    @Transactional
    fun createMode(request: BattleEventModeCreateRequest) {
        battleEventModeJpaRepository.save(
            BattleEventModeEntity(
                brawlStarsId = request.brawlStarsId,
                brawlStarsName = request.brawlStarsName,
                resultType = request.resultType,
                playerCounts = request.playerCounts,
                color = request.color,
            ),
        )
        doUpdateMessages(MessageType.BATTLE_EVENT_MODE_NAME, request.brawlStarsId, request.names)
    }
```

(`BattleEventModeEntity` 생성자는 `playerCounts·color` 를 받고 init 에서 검증한다 — 잘못된 값은 여기서 `IllegalArgumentException`. `doUpdateMessages`, `MessageType` 모두 이미 사용 중.)

- [ ] **Step 5: 테스트 실행 → 통과 확인**

Run: `cd imstargg-backend && ./gradlew :core:core-admin:test --tests "com.imstargg.core.admin.application.BattleEventServiceTest"`
Expected: PASS (3개 테스트 모두)

- [ ] **Step 6: 커밋**

```bash
cd imstargg-backend && ./gradlew ktlintFormat
git add core/core-admin/src/main/kotlin/com/imstargg/core/admin/request/BattleEventModeCreateRequest.kt \
        core/core-admin/src/main/kotlin/com/imstargg/core/admin/application/BattleEventService.kt \
        core/core-admin/src/test/kotlin/com/imstargg/core/admin/application/BattleEventServiceTest.kt
git commit -m "feat(core-admin): 이벤트 모드 생성 시 이름·인원수·색상 원자적 저장" \
           -m "Co-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>"
```

---

## Task 5: 전체 검증

**Files:** 없음(검증만).

- [ ] **Step 1: 린트 검사**

Run: `cd imstargg-backend && ./gradlew :core:core-admin:ktlintCheck`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: core-admin 전체 테스트**

Run: `cd imstargg-backend && ./gradlew :core:core-admin:test`
Expected: BUILD SUCCESSFUL — 신규 3개 서비스 테스트 + 기존 컨트롤러 테스트 모두 통과.

- [ ] **Step 3: (이상 시) 수정 후 재실행**

실패가 있으면 메시지를 읽고 해당 Task 로 돌아가 수정 → 본 Task Step 1-2 재실행.

---

## 참고 — 후속 계획

- **계획 2(프론트 등록 폼 + 순차 이미지 업로드)** 는 이 확장된 생성 API에 의존한다. `createBrawler/create/createEvent/createMode` 가 `names`(+ `gearBrawlStarsIds` / `playerCounts` / `color`)를 받는다는 전제로 작성한다.
- 컨트롤러는 변경하지 않았으므로 HTTP 계약(경로/메서드/응답 `200 OK`)은 동일하다. 요청 본문 필드만 추가된다.
