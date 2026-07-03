# 브롤스타즈 API 스펙 추적 정규화 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 브롤스타즈 API 스펙(`GET /v1`) 응답 YAML 의 맵 키 순서가 매 요청 달라져 발생하는 오탐 변경 알림을, 키 정렬 정규화본 비교·저장으로 제거한다.

**Architecture:** `core-batch` 에 순수 함수 유틸 `YamlCanonicalizer`(SnakeYAML SafeConstructor 파싱 → 맵 키 재귀 정렬 → BLOCK 재직렬화)를 추가하고, `BrawlStarsApiSpecTrackTask` 의 비교·저장 기준을 원문에서 정규화본으로 바꾼다. 직전 스냅샷이 정규화 도입 전 원문이면 알림 없이 정규화본으로 1회 이관 저장한다. DB 스키마·어드민은 변경하지 않는다.

**Tech Stack:** Kotlin, Spring Boot 4 (Java 21), SnakeYAML 2.5 (이미 클래스패스에 존재, Spring Boot BOM 관리), JUnit 5 + AssertJ + MockK

**Spec:** `docs/superpowers/specs/2026-07-03-api-spec-track-normalization-design.md` (prod 스냅샷 15개 전수 검증 완료 — 전 구간 의미 동일, 정규화본 전부 일치)

## Global Constraints

- 모든 명령은 별도 표기 없으면 레포 루트 기준. Gradle 명령은 `imstargg-backend/` 에서 실행.
- 파일당 1개 클래스 원칙 (imstargg-backend/CLAUDE.md).
- snakeyaml 의존성은 버전 없이 선언: `implementation("org.yaml:snakeyaml")` — 버전은 Spring Boot BOM 이 관리.
- 테스트 컨벤션: 메서드명 한글 백틱, `// given` `// when` `// then` 주석, AssertJ `assertThat`, 예외는 `catchThrowableOfType(Class, ThrowingCallable)` (Class 가 첫 인자), 타입 추론 사용, 시간은 `Clock.fixed` 주입, 외부 시스템(HTTP client·알림)만 MockK.
- 인자 3개 이상 호출은 named arguments 사용.
- 알림 메시지 문구, DB 스키마(schema.sql·마이그레이션), `core-admin`, `admin-frontend` 는 변경 금지.
- 커밋은 Conventional Commits. 커밋 메시지 끝에 `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>` 푸터.
- 각 태스크 종료 시 ktlint 통과 필수 (`ktlintFormat` 으로 자동 수정 가능).

---

### Task 1: YamlCanonicalizer — YAML 맵 키 정렬 정규화 유틸

**Files:**
- Modify: `imstargg-backend/core/core-batch/build.gradle.kts` (snakeyaml 명시 선언)
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/task/YamlCanonicalizer.kt`
- Test: `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/brawlstars/task/YamlCanonicalizerTest.kt`

**Interfaces:**
- Consumes: 없음 (SnakeYAML 라이브러리만 사용)
- Produces: `object YamlCanonicalizer` — `fun canonicalize(content: String): String`. 패키지 `com.imstargg.core.batch.job.brawlstars.task`. 유효하지 않은 YAML 입력 시 `org.yaml.snakeyaml.error.YAMLException` (하위 타입 포함) 전파. Task 2 가 이 시그니처를 그대로 사용한다.

- [ ] **Step 1: snakeyaml 의존성 명시 선언**

직접 import 하는 라이브러리이므로 전이 의존에 기대지 않고 명시한다 (버전은 BOM 관리라 생략).

`imstargg-backend/core/core-batch/build.gradle.kts` 의 dependencies 블록에서:

```kotlin
    implementation("org.springframework.boot:spring-boot-starter-batch")
    implementation("org.springframework:spring-web")
```

를 아래로 교체:

```kotlin
    implementation("org.springframework.boot:spring-boot-starter-batch")
    implementation("org.springframework:spring-web")
    implementation("org.yaml:snakeyaml")
```

- [ ] **Step 2: 실패하는 테스트 작성**

`imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/brawlstars/task/YamlCanonicalizerTest.kt` 생성:

```kotlin
package com.imstargg.core.batch.job.brawlstars.task

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.error.YAMLException

class YamlCanonicalizerTest {
    @Test
    fun `맵 키 순서만 다른 두 YAML 은 동일한 정규화본이 된다`() {
        // given
        val yamlA = "swagger: \"2.0\"\ninfo:\n  title: Brawl Stars API\n  version: v1"
        val yamlB = "info:\n  version: v1\n  title: Brawl Stars API\nswagger: \"2.0\""

        // when
        val canonicalA = YamlCanonicalizer.canonicalize(yamlA)
        val canonicalB = YamlCanonicalizer.canonicalize(yamlB)

        // then
        assertThat(canonicalA).isEqualTo(canonicalB)
    }

    @Test
    fun `중첩 맵 키까지 재귀 정렬하고 스칼라 표기를 보존한다`() {
        // given
        val yaml = "b:\n  d: 2\n  c: 1\na: 3\nswagger: \"2.0\""

        // when
        val canonical = YamlCanonicalizer.canonicalize(yaml)

        // then
        assertThat(canonical).isEqualTo("a: 3\nb:\n  c: 1\n  d: 2\nswagger: '2.0'\n")
    }

    @Test
    fun `리스트 순서는 보존한다`() {
        // given
        val yamlA = "schemes:\n  - https\n  - http"
        val yamlB = "schemes:\n  - http\n  - https"

        // when
        val canonicalA = YamlCanonicalizer.canonicalize(yamlA)
        val canonicalB = YamlCanonicalizer.canonicalize(yamlB)

        // then
        assertThat(canonicalA).isEqualTo("schemes:\n- https\n- http\n")
        assertThat(canonicalA).isNotEqualTo(canonicalB)
    }

    @Test
    fun `유효하지 않은 YAML 이면 예외가 발생한다`() {
        // when
        val thrown =
            catchThrowableOfType(YAMLException::class.java) {
                YamlCanonicalizer.canonicalize("key: [unclosed")
            }

        // then
        assertThat(thrown).isNotNull()
    }
}
```

- [ ] **Step 3: 테스트가 실패하는지 확인**

Run (in `imstargg-backend/`):

```bash
./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.job.brawlstars.task.YamlCanonicalizerTest"
```

Expected: **BUILD FAILED** — 컴파일 에러 `unresolved reference: YamlCanonicalizer` (클래스가 아직 없으므로 컴파일 단계에서 실패하는 것이 정상 red 신호).

- [ ] **Step 4: 최소 구현 작성**

`imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/task/YamlCanonicalizer.kt` 생성:

```kotlin
package com.imstargg.core.batch.job.brawlstars.task

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

/**
 * YAML 문서를 의미 보존 정규형(맵 키 재귀 정렬)으로 재직렬화한다.
 *
 * 브롤스타즈 API 스펙(`GET /v1`) 응답은 요청마다 맵 키 순서가 달라지므로, 원문 대신
 * 정규화본을 비교·저장 기준으로 삼는다. 정렬 비교는 문자열 기준이지만 키 타입 자체는
 * 보존하고, 리스트 순서는 의미가 있으므로(파라미터 순서 등) 정렬하지 않는다.
 * 유효한 YAML 이 아니면 SnakeYAML 예외를 그대로 전파한다 — 응답 형식 자체의 변경은
 * 잡 실패로 시끄럽게 드러나는 것이 의도된 동작이다.
 */
object YamlCanonicalizer {
    fun canonicalize(content: String): String {
        val root = Yaml(SafeConstructor(LoaderOptions())).load<Any?>(content)
        val dumperOptions =
            DumperOptions().apply {
                defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
                isAllowUnicode = true
                width = 4096
            }
        return Yaml(dumperOptions).dump(sortMapKeys(root))
    }

    private fun sortMapKeys(node: Any?): Any? =
        when (node) {
            is Map<*, *> ->
                node.entries
                    .sortedBy { it.key.toString() }
                    .associate { it.key to sortMapKeys(it.value) }
            is List<*> -> node.map { sortMapKeys(it) }
            else -> node
        }
}
```

구현 노트:
- `SafeConstructor` — 외부 API 응답이므로 임의 객체 인스턴스화를 차단한다.
- `associate` 는 `LinkedHashMap` 을 반환하므로 정렬된 삽입 순서가 dump 에 그대로 반영된다.
- `width = 4096` — 긴 스칼라(설명 문장·URL)의 줄바꿈을 막아 어드민 diff 를 줄 단위로 깨끗하게 유지한다.

- [ ] **Step 5: 테스트 통과 확인**

Run (in `imstargg-backend/`):

```bash
./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.job.brawlstars.task.YamlCanonicalizerTest"
```

Expected: **BUILD SUCCESSFUL**, 4 tests passed.

만약 `중첩 맵 키까지 재귀 정렬하고 스칼라 표기를 보존한다` 또는 `리스트 순서는 보존한다` 가 기대 문자열 불일치로 실패하면: 실제 SnakeYAML 출력(테스트 실패 메시지의 actual)을 확인하고, 의미가 동일한 표기 차이(따옴표·들여쓰기)라면 테스트의 기대 문자열을 actual 로 수정한다. 정렬·순서 자체가 다르면 구현 버그이므로 `sortMapKeys` 를 수정한다.

- [ ] **Step 6: ktlint 확인 후 커밋**

Run (in `imstargg-backend/`):

```bash
./gradlew :core:core-batch:ktlintCheck
```

Expected: BUILD SUCCESSFUL (실패 시 `./gradlew :core:core-batch:ktlintFormat` 후 재확인).

레포 루트에서:

```bash
git add imstargg-backend/core/core-batch/build.gradle.kts \
  imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/task/YamlCanonicalizer.kt \
  imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/brawlstars/task/YamlCanonicalizerTest.kt
git commit -m "feat(core-batch): YAML 맵 키 정렬 정규화 유틸 YamlCanonicalizer 추가

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 2: BrawlStarsApiSpecTrackTask 정규화본 비교·저장 전환

**Files:**
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/task/BrawlStarsApiSpecTrackTask.kt`
- Modify: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/brawlstars/system/BrawlStarsApiSpecEntity.kt` (KDoc 만)
- Test: `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/brawlstars/task/BrawlStarsApiSpecTrackTaskTest.kt`

**Interfaces:**
- Consumes: Task 1 의 `YamlCanonicalizer.canonicalize(content: String): String` (같은 패키지라 import 불필요)
- Produces: 없음 (잡 동작 변경만 — 외부에 노출되는 시그니처 변화 없음. `BrawlStarsApiSpecJpaRepository`, `AlertManager` 사용 방식 기존과 동일)

- [ ] **Step 1: 테스트를 새 동작 기준으로 재작성**

`imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/brawlstars/task/BrawlStarsApiSpecTrackTaskTest.kt` 전체를 아래로 교체 (기존 4개 중 2개는 기대값 변경, 2개 신규 추가 — 정규화 문자열은 Task 1 테스트에서 검증된 표기를 사용):

```kotlin
package com.imstargg.core.batch.job.brawlstars.task

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.infrastructure.db.core.brawlstars.system.BrawlStarsApiSpecEntity
import com.imstargg.infrastructure.db.core.brawlstars.system.repository.BrawlStarsApiSpecJpaRepository
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class BrawlStarsApiSpecTrackTaskTest {
    private val fixedInstant = Instant.parse("2026-06-06T07:00:00Z")
    private val clock = Clock.fixed(fixedInstant, ZoneOffset.UTC)
    private val brawlStarsClient = mockk<BrawlStarsClient>()
    private val brawlStarsApiSpecJpaRepository = mockk<BrawlStarsApiSpecJpaRepository>()
    private val alertManager = mockk<AlertManager>()
    private val sut =
        BrawlStarsApiSpecTrackTask(
            clock = clock,
            brawlStarsClient = brawlStarsClient,
            brawlStarsApiSpecJpaRepository = brawlStarsApiSpecJpaRepository,
            alertManager = alertManager,
        )

    @Test
    fun `최초 수집이면 정규화본 스냅샷을 저장하고 알림을 보내지 않는다`() {
        // given
        every { brawlStarsClient.getApiSpec() } returns "swagger: \"2.0\""
        every { brawlStarsApiSpecJpaRepository.findTopByOrderByCollectedAtDesc() } returns null
        val savedSlot = slot<BrawlStarsApiSpecEntity>()
        every { brawlStarsApiSpecJpaRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        // when
        sut.track()

        // then
        assertThat(savedSlot.captured.content).isEqualTo("swagger: '2.0'\n")
        assertThat(savedSlot.captured.collectedAt).isEqualTo(fixedInstant)
        verify(exactly = 0) { alertManager.alert(any()) }
    }

    @Test
    fun `직전 스냅샷과 정규화본이 같으면 저장도 알림도 하지 않는다`() {
        // given
        every { brawlStarsClient.getApiSpec() } returns "a: 1\nb: 2"
        every { brawlStarsApiSpecJpaRepository.findTopByOrderByCollectedAtDesc() } returns
            BrawlStarsApiSpecEntity(content = "a: 1\nb: 2\n", collectedAt = fixedInstant.minusSeconds(86400))

        // when
        sut.track()

        // then
        verify(exactly = 0) { brawlStarsApiSpecJpaRepository.save(any()) }
        verify(exactly = 0) { alertManager.alert(any()) }
    }

    @Test
    fun `맵 키 순서만 다른 응답은 변경으로 판정하지 않는다`() {
        // given — 원문 문자열 비교였다면 오탐 알림이 났을 입력
        every { brawlStarsClient.getApiSpec() } returns "b: 2\na: 1"
        every { brawlStarsApiSpecJpaRepository.findTopByOrderByCollectedAtDesc() } returns
            BrawlStarsApiSpecEntity(content = "a: 1\nb: 2\n", collectedAt = fixedInstant.minusSeconds(86400))

        // when
        sut.track()

        // then
        verify(exactly = 0) { brawlStarsApiSpecJpaRepository.save(any()) }
        verify(exactly = 0) { alertManager.alert(any()) }
    }

    @Test
    fun `직전 스냅샷이 정규화 이전 원문이면 알림 없이 정규화본으로 이관 저장한다`() {
        // given — 의미는 같지만 저장형이 비정렬 원문인 레거시 행
        every { brawlStarsClient.getApiSpec() } returns "a: 1\nb: 2"
        every { brawlStarsApiSpecJpaRepository.findTopByOrderByCollectedAtDesc() } returns
            BrawlStarsApiSpecEntity(content = "b: 2\na: 1", collectedAt = fixedInstant.minusSeconds(86400))
        val savedSlot = slot<BrawlStarsApiSpecEntity>()
        every { brawlStarsApiSpecJpaRepository.save(capture(savedSlot)) } answers { savedSlot.captured }

        // when
        sut.track()

        // then
        assertThat(savedSlot.captured.content).isEqualTo("a: 1\nb: 2\n")
        assertThat(savedSlot.captured.collectedAt).isEqualTo(fixedInstant)
        verify(exactly = 0) { alertManager.alert(any()) }
    }

    @Test
    fun `응답이 비어있으면 예외가 발생하고 저장하지 않는다`() {
        // given
        every { brawlStarsClient.getApiSpec() } returns " "

        // when
        val thrown = catchThrowableOfType(IllegalStateException::class.java) { sut.track() }

        // then
        assertThat(thrown).isNotNull()
        verify(exactly = 0) { brawlStarsApiSpecJpaRepository.save(any()) }
        verify(exactly = 0) { alertManager.alert(any()) }
    }

    @Test
    fun `정규화본이 다르면 새 스냅샷을 저장하고 변경 알림을 보낸다`() {
        // given
        every { brawlStarsClient.getApiSpec() } returns "b: 2\na: 1\nnewField: true"
        every { brawlStarsApiSpecJpaRepository.findTopByOrderByCollectedAtDesc() } returns
            BrawlStarsApiSpecEntity(content = "a: 1\nb: 2\n", collectedAt = fixedInstant.minusSeconds(86400))
        val savedSlot = slot<BrawlStarsApiSpecEntity>()
        every { brawlStarsApiSpecJpaRepository.save(capture(savedSlot)) } answers { savedSlot.captured }
        justRun { alertManager.alert(any()) }

        // when
        sut.track()

        // then
        assertThat(savedSlot.captured.content).isEqualTo("a: 1\nb: 2\nnewField: true\n")
        assertThat(savedSlot.captured.collectedAt).isEqualTo(fixedInstant)
        verify(exactly = 1) { alertManager.alert(any()) }
    }
}
```

- [ ] **Step 2: 테스트가 실패하는지 확인**

Run (in `imstargg-backend/`):

```bash
./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.job.brawlstars.task.BrawlStarsApiSpecTrackTaskTest"
```

Expected: **FAILED** — 최소 4개 실패. 예: `맵 키 순서만 다른 응답은 변경으로 판정하지 않는다` 는 현재 구현이 원문 비교로 save 를 호출하므로 MockK `no answer found for BrawlStarsApiSpecJpaRepository.save(...)` 로 실패, `최초 수집이면 정규화본 스냅샷을...` 은 content 가 원문 그대로라 assertion 불일치로 실패.

- [ ] **Step 3: 구현 수정**

`imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/task/BrawlStarsApiSpecTrackTask.kt` 전체를 아래로 교체:

```kotlin
package com.imstargg.core.batch.job.brawlstars.task

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.core.batch.support.alert.AlertCommand
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.infrastructure.db.core.brawlstars.system.BrawlStarsApiSpecEntity
import com.imstargg.infrastructure.db.core.brawlstars.system.repository.BrawlStarsApiSpecJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Clock

/**
 * 브롤스타즈 API 스펙(`GET /v1`)을 수집해 정규화본 기준으로 직전 스냅샷과 비교한다.
 *
 * 응답 YAML 은 요청마다 맵 키 순서가 달라지므로 원문이 아닌 [YamlCanonicalizer] 정규화본을
 * 비교·저장한다 (원문 문자열 비교 시절에는 순서 차이만으로 매 실행 오탐 알림이 발생했다).
 *
 * - 의미가 같으면 저장/알림 없이 로그만 남긴다. 단 직전 스냅샷이 정규화 도입 전
 *   원문 형태면 알림 없이 정규화본으로 1회 이관 저장한다 (이후 어드민 diff 가
 *   항상 정규화본끼리 비교되도록).
 * - 다르면 새 스냅샷을 저장하고, 최초 수집이 아닌 경우 변경 알림을 보낸다.
 */
@Component
class BrawlStarsApiSpecTrackTask(
    private val clock: Clock,
    private val brawlStarsClient: BrawlStarsClient,
    private val brawlStarsApiSpecJpaRepository: BrawlStarsApiSpecJpaRepository,
    private val alertManager: AlertManager,
) {
    fun track() {
        log.info("[{}] 시작", JOB_NAME)
        val content = brawlStarsClient.getApiSpec()
        check(content.isNotBlank()) { "브롤스타즈 API 스펙 응답이 비어있습니다." }
        val canonical = YamlCanonicalizer.canonicalize(content)
        val previous = brawlStarsApiSpecJpaRepository.findTopByOrderByCollectedAtDesc()

        if (previous == null) {
            saveSnapshot(canonical)
            log.info("[{}] 완료 - 최초 스냅샷 저장", JOB_NAME)
            return
        }

        if (YamlCanonicalizer.canonicalize(previous.content) == canonical) {
            if (previous.content != canonical) {
                saveSnapshot(canonical)
                log.info("[{}] 완료 - 스펙 변경 없음, 레거시 원문 스냅샷을 정규화본으로 이관", JOB_NAME)
                return
            }
            log.info("[{}] 완료 - 스펙 변경 없음", JOB_NAME)
            return
        }

        saveSnapshot(canonical)
        log.info("[{}] 완료 - 스펙 변경 감지, 새 스냅샷 저장 및 알림 발송", JOB_NAME)
        alertManager.alert(
            AlertCommand
                .builder()
                .warn()
                .title("[BrawlStarsApiSpecTrackJob] 브롤스타즈 API 스펙 변경 감지")
                .content("새 스냅샷을 저장했습니다. 어드민 API 스펙 추적 페이지에서 diff 를 확인하세요.")
                .build(),
        )
    }

    private fun saveSnapshot(content: String) {
        brawlStarsApiSpecJpaRepository.save(
            BrawlStarsApiSpecEntity(content = content, collectedAt = clock.instant()),
        )
    }

    companion object {
        private const val JOB_NAME = "brawlStarsApiSpecTrackJob"
        private val log = LoggerFactory.getLogger(BrawlStarsApiSpecTrackTask::class.java)
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

Run (in `imstargg-backend/`):

```bash
./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.job.brawlstars.task.BrawlStarsApiSpecTrackTaskTest"
```

Expected: **BUILD SUCCESSFUL**, 6 tests passed.

- [ ] **Step 5: 엔티티 KDoc 을 새 저장 규칙으로 갱신**

`imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/brawlstars/system/BrawlStarsApiSpecEntity.kt` 의 클래스 KDoc 만 교체 — 기존:

```kotlin
/**
 * 브롤스타즈 API 스펙(`GET /v1`, swagger 2.0 YAML) 스냅샷.
 *
 * 직전 스냅샷과 내용이 다를 때만(또는 최초 수집 시) 저장하는 append-only 테이블.
 * 행이 연 수 건 수준으로만 늘어나므로 인덱스를 두지 않는다 (PK 만 사용).
 */
```

를 아래로 교체 (db-core 는 core-batch 를 참조하지 않으므로 KDoc 링크 대신 평문 표기):

```kotlin
/**
 * 브롤스타즈 API 스펙(`GET /v1`, swagger 2.0 YAML) 스냅샷.
 *
 * content 는 core-batch 의 YamlCanonicalizer 로 맵 키를 정렬한 정규화본이다
 * (2026-07 정규화 도입 이전 행은 원문 — 도입 후 첫 실행에서 정규화본으로 이관 저장됨).
 * 직전 스냅샷과 정규화본이 다를 때만(또는 최초 수집·레거시 이관 시) 저장하는 append-only 테이블.
 * 행이 연 수 건 수준으로만 늘어나므로 인덱스를 두지 않는다 (PK 만 사용).
 */
```

- [ ] **Step 6: 모듈 전체 테스트 + ktlint 확인**

Run (in `imstargg-backend/`):

```bash
./gradlew :core:core-batch:test :core:core-batch:ktlintCheck :infrastructure:db-core:ktlintCheck
```

Expected: **BUILD SUCCESSFUL** — core-batch 모듈의 다른 잡 테스트 포함 전체 통과 (실패 시 `./gradlew :core:core-batch:ktlintFormat` 후 재확인).

- [ ] **Step 7: 커밋**

레포 루트에서:

```bash
git add imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/brawlstars/task/BrawlStarsApiSpecTrackTask.kt \
  imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/brawlstars/task/BrawlStarsApiSpecTrackTaskTest.kt \
  imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/brawlstars/system/BrawlStarsApiSpecEntity.kt
git commit -m "fix(core-batch): API 스펙 추적을 정규화본 비교로 전환해 순서 재배열 오탐 제거

응답 YAML 의 맵 키 순서가 매 요청 달라져 원문 비교가 매일 오탐 알림을
발생시킴. YamlCanonicalizer 정규화본으로 비교·저장하고, 정규화 이전
레거시 스냅샷은 알림 없이 1회 이관한다.

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## 완료 기준

- `./gradlew :core:core-batch:test` 전체 통과, ktlint 통과.
- 배포 후: 오탐 알림 소멸(첫 실행은 알림 없이 레거시 이관 저장 1건), 이후 실제 스펙 변경 시에만 알림 + 어드민 diff 에 변경분만 표시.
- 배포는 기존 흐름(develop PR → main PR → Deploy 워크플로 core-batch) — 이 플랜 범위 외, `/release` 스킬이 버전 bump 담당.
