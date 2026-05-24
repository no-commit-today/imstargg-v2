# Feedback 재설계 (만족도 + 의견) 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 피드백 도메인을 BUG/FEATURE_REQUEST/ETC 카테고리 + 선택 이메일 구조에서 5단계 만족도(`FeedbackRating`) + 자유 의견 구조로 재설계하고, 작성일시를 `submitted_at` 전용 컬럼으로 분리해 `Clock` 으로 명시 세팅한다.

**Architecture:** core-enum 에 `FeedbackRating` 추가 → db-core 마이그레이션 + Entity 교체 (rating, submittedAt) → core-api/admin/batch 가 새 도메인을 따라 갱신 → core-enum 의 구 `FeedbackCategory` 폐기. 프론트 2개 (사용자/관리자) 가 새 API 계약에 맞춰 폼/리스트 변경. DDL 한 번에 drop & 재생성 (운영 데이터 폐기 결정).

**Tech Stack:** Spring Boot Kotlin, JPA + QueryDSL (KSP), Flyway-style 마이그레이션, Spring Batch, Next.js (App Router) + TanStack Query, zod, react-hook-form, sonner.

**Spec:** [docs/superpowers/specs/2026-05-22-feedback-rating-redesign-design.md](../specs/2026-05-22-feedback-rating-redesign-design.md)

**작업 흐름 (의존성):**
```
Phase 1 core-enum (additive) → Phase 2 db-core → Phase 3 core-api
                                                   ↓
                              Phase 4 core-admin / Phase 5 core-batch (병렬 가능)
                                                   ↓
Phase 6 cleanup (FeedbackCategory 삭제) → Phase 7 backend build verify
                                                   ↓
Phase 8 imstargg-frontend / Phase 9 imstargg-admin-frontend (병렬 가능)
                                                   ↓
                                          Phase 10 release bump
```

---

## Phase 1 — core-enum: FeedbackRating 추가

### Task 1: `FeedbackRating` enum 신설

**Files:**
- Create: `imstargg-backend/core/core-enum/src/main/kotlin/com/imstargg/core/enums/operation/FeedbackRating.kt`

이 단계에서는 **`FeedbackCategory` 를 삭제하지 않는다** — 다른 모듈에서 아직 참조 중이라 컴파일이 깨진다. Phase 6 에서 일괄 삭제.

- [ ] **Step 1: enum 파일 작성**

```kotlin
package com.imstargg.core.enums.operation

enum class FeedbackRating {
    VERY_SATISFIED,
    SATISFIED,
    NEUTRAL,
    DISSATISFIED,
    VERY_DISSATISFIED,
}
```

- [ ] **Step 2: 모듈 컴파일 확인**

Run: `./gradlew :core:core-enum:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 커밋**

```bash
git add imstargg-backend/core/core-enum/src/main/kotlin/com/imstargg/core/enums/operation/FeedbackRating.kt
git commit -m "feat(core-enum): FeedbackRating 5단계 만족도 enum 추가"
```

---

## Phase 2 — db-core: 테이블 / Entity / JpaRepository 교체

### Task 2: 마이그레이션 SQL + schema.sql

**Files:**
- Create: `imstargg-backend/infrastructure/db-core/src/main/resources/db/migration/V260522__redesign_feedback_to_rating.sql`
- Modify: `imstargg-backend/infrastructure/db-core/src/main/resources/schema.sql`

운영 데이터 폐기 결정에 따라 `drop & 재생성`. 단일 복합 인덱스 `(checked, submitted_at desc)` 만 둔다.

- [ ] **Step 1: 마이그레이션 파일 작성**

```sql
-- V260522__redesign_feedback_to_rating.sql
-- 피드백 도메인 재설계: 카테고리/이메일 제거, 5단계 rating + submittedAt 도입
drop table if exists feedback;

create table feedback (
    id           bigserial   primary key,
    rating       varchar(32) not null,
    content      text        not null,
    checked      boolean     not null default false,
    submitted_at timestamptz not null,
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now()
);

create index ix_feedback__checked__submittedat
    on feedback (checked, submitted_at desc);
```

- [ ] **Step 2: schema.sql 의 feedback 정의 교체**

`schema.sql` 에서 기존 `feedback` 테이블 / `ix_feedback__createdat` / `ix_feedback__checked` 인덱스 블록을 찾아 위 V260522 와 동일한 정의로 교체한다 (단, `create table if not exists` / `create index if not exists` 멱등 형태로):

```sql
create table if not exists feedback (
    id           bigserial   primary key,
    rating       varchar(32) not null,
    content      text        not null,
    checked      boolean     not null default false,
    submitted_at timestamptz not null,
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now()
);

create index if not exists ix_feedback__checked__submittedat
    on feedback (checked, submitted_at desc);
```

기존 `idx_feedback_*` / `ix_feedback__createdat` / `ix_feedback__checked` 라인이 남아 있다면 모두 제거한다 (정확한 라인 번호는 schema.sql 의 현재 상태에 따라 다름).

- [ ] **Step 3: 로컬 DB 재마이그레이션 검증 (선택)**

Run: `docker-compose up -d && ./gradlew :infrastructure:db-core:flywayMigrate` (또는 앱 부팅 시 Flyway 자동 실행)
Expected: `V260522` 가 적용되고 새 스키마가 보여야 한다. 검증은 다음 Task 의 `FeedbackEntityTest` 가 통과하는 것으로 갈음 가능.

- [ ] **Step 4: 커밋**

```bash
git add imstargg-backend/infrastructure/db-core/src/main/resources/db/migration/V260522__redesign_feedback_to_rating.sql \
        imstargg-backend/infrastructure/db-core/src/main/resources/schema.sql
git commit -m "feat(db-core): feedback 테이블을 rating + submitted_at 구조로 재생성"
```

---

### Task 3: `FeedbackEntity` 갱신

**Files:**
- Modify: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/operation/FeedbackEntity.kt`
- Modify: `imstargg-backend/infrastructure/db-core/src/test/kotlin/com/imstargg/infrastructure/db/core/operation/FeedbackEntityTest.kt`

`category` / `email` 필드를 제거하고 `rating: FeedbackRating`, `submittedAt: Instant` 를 도입한다. `BaseTimeEntity` 상속과 `checked` 의 mutable 패턴은 유지.

- [ ] **Step 1: 테스트를 새 시그니처로 교체 (Red)**

`FeedbackEntityTest.kt` 전체를 아래로 교체:

```kotlin
package com.imstargg.infrastructure.db.core.operation

import com.imstargg.core.enums.operation.FeedbackRating
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class FeedbackEntityTest {
    private val fixedSubmittedAt = Instant.parse("2026-05-22T10:00:00Z")

    @Test
    fun `check()를 호출하면 checked가 true가 된다`() {
        // given
        val sut =
            FeedbackEntity(
                rating = FeedbackRating.VERY_SATISFIED,
                content = "만족합니다",
                submittedAt = fixedSubmittedAt,
            )

        // when
        sut.check()

        // then
        assertThat(sut.checked).isTrue()
    }

    @Test
    fun `uncheck()를 호출하면 checked가 false가 된다`() {
        // given
        val sut =
            FeedbackEntity(
                rating = FeedbackRating.VERY_SATISFIED,
                content = "만족합니다",
                submittedAt = fixedSubmittedAt,
            )
        sut.check()

        // when
        sut.uncheck()

        // then
        assertThat(sut.checked).isFalse()
    }

    @Test
    fun `submittedAt은 생성자 인자 그대로 보관된다`() {
        // given & when
        val sut =
            FeedbackEntity(
                rating = FeedbackRating.NEUTRAL,
                content = "보통",
                submittedAt = fixedSubmittedAt,
            )

        // then
        assertThat(sut.submittedAt).isEqualTo(fixedSubmittedAt)
    }
}
```

- [ ] **Step 2: 테스트 실행 — 컴파일 실패 확인 (Red)**

Run: `./gradlew :infrastructure:db-core:compileTestKotlin`
Expected: `FeedbackEntity` 의 `category`/`email` 가 사라진 시그니처가 아직 없어 컴파일 에러 — 다음 단계에서 production 코드를 맞춘다.

- [ ] **Step 3: `FeedbackEntity` 교체**

`FeedbackEntity.kt` 전체를 아래로 교체:

```kotlin
package com.imstargg.infrastructure.db.core.operation

import com.imstargg.core.enums.operation.FeedbackRating
import com.imstargg.infrastructure.db.core.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null

    @Column(name = "checked", nullable = false)
    var checked: Boolean = checked
        protected set

    fun check() {
        this.checked = true
    }

    fun uncheck() {
        this.checked = false
    }
}
```

- [ ] **Step 4: 테스트 통과 확인 (Green)**

Run: `./gradlew :infrastructure:db-core:test --tests "com.imstargg.infrastructure.db.core.operation.FeedbackEntityTest"`
Expected: 3개 테스트 PASS

- [ ] **Step 5: QueryDSL Q-Class 재생성 검증**

Run: `./gradlew :infrastructure:db-core:kspKotlin`
Expected: `QFeedbackEntity` 가 `rating`, `submittedAt` 필드 포함하여 재생성 (생성된 파일은 build/generated 하위, 검증은 컴파일 통과로 갈음)

- [ ] **Step 6: 커밋**

```bash
git add imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/operation/FeedbackEntity.kt \
        imstargg-backend/infrastructure/db-core/src/test/kotlin/com/imstargg/infrastructure/db/core/operation/FeedbackEntityTest.kt
git commit -m "feat(db-core): FeedbackEntity를 rating + submittedAt로 교체"
```

---

### Task 4: `FeedbackJpaRepository` 쿼리 메서드 이름 변경

**Files:**
- Modify: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/operation/FeedbackJpaRepository.kt`

`createdAt` 기반 24시간 윈도우 조회를 `submittedAt` 기반으로 교체.

- [ ] **Step 1: 메서드 시그니처 교체**

`FeedbackJpaRepository.kt` 전체를 아래로 교체:

```kotlin
package com.imstargg.infrastructure.db.core.operation

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface FeedbackJpaRepository : JpaRepository<FeedbackEntity, Long> {
    fun findAllBySubmittedAtGreaterThanEqualAndSubmittedAtLessThan(
        from: Instant,
        to: Instant,
    ): List<FeedbackEntity>
}
```

- [ ] **Step 2: db-core 컴파일 통과 확인**

Run: `./gradlew :infrastructure:db-core:compileKotlin`
Expected: BUILD SUCCESSFUL (테스트 컴파일은 아직 다른 모듈을 안 봄)

- [ ] **Step 3: 커밋**

```bash
git add imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/operation/FeedbackJpaRepository.kt
git commit -m "feat(db-core): FeedbackJpaRepository를 submittedAt 범위 조회로 변경"
```

---

## Phase 3 — core-api: 도메인 / 서비스 / 컨트롤러

### Task 5: 도메인 모델 `Feedback` / `FeedbackCreate` 교체

**Files:**
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/operation/Feedback.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/operation/FeedbackCreate.kt`

- [ ] **Step 1: `Feedback.kt` 교체**

```kotlin
package com.imstargg.core.domain.operation

import com.imstargg.core.enums.operation.FeedbackRating
import java.time.Instant

data class Feedback(
    val id: Long,
    val rating: FeedbackRating,
    val content: String,
    val submittedAt: Instant,
    val checked: Boolean,
)
```

- [ ] **Step 2: `FeedbackCreate.kt` 교체**

```kotlin
package com.imstargg.core.domain.operation

import com.imstargg.core.enums.operation.FeedbackRating
import java.time.Instant

data class FeedbackCreate(
    val rating: FeedbackRating,
    val content: String,
    val submittedAt: Instant,
)
```

- [ ] **Step 3: 커밋**

```bash
git add imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/operation/Feedback.kt \
        imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/operation/FeedbackCreate.kt
git commit -m "feat(core-api): Feedback/FeedbackCreate 도메인에 rating + submittedAt 도입"
```

---

### Task 6: `FeedbackRepository` (application 계층) 매퍼 갱신

**Files:**
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/operation/FeedbackRepository.kt`

엔티티 생성자 / `toDomain()` 매퍼를 새 필드에 맞춰 교체.

- [ ] **Step 1: 파일 전체 교체**

```kotlin
package com.imstargg.core.application.operation

import com.imstargg.core.domain.operation.Feedback
import com.imstargg.core.domain.operation.FeedbackCreate
import com.imstargg.infrastructure.db.core.operation.FeedbackEntity
import com.imstargg.infrastructure.db.core.operation.FeedbackJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
class FeedbackRepository(
    private val jpaRepository: FeedbackJpaRepository,
) {
    @Transactional
    fun save(command: FeedbackCreate): Feedback {
        val entity =
            FeedbackEntity(
                rating = command.rating,
                content = command.content,
                submittedAt = command.submittedAt,
            )
        val saved = jpaRepository.save(entity)
        return saved.toDomain()
    }

    private fun FeedbackEntity.toDomain(): Feedback =
        Feedback(
            id = requireNotNull(id) { "Feedback id must not be null after save" },
            rating = rating,
            content = content,
            submittedAt = submittedAt,
            checked = checked,
        )
}
```

- [ ] **Step 2: 커밋**

```bash
git add imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/operation/FeedbackRepository.kt
git commit -m "feat(core-api): FeedbackRepository를 새 도메인 시그니처로 갱신"
```

---

### Task 7: `FeedbackService` 에 `Clock` 주입

**Files:**
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/operation/FeedbackService.kt`

서비스가 `Clock` 을 주입받아 `submittedAt = clock.instant()` 로 명시 세팅. `email` 정규화 로직 삭제.

- [ ] **Step 1: 파일 전체 교체**

```kotlin
package com.imstargg.core.application.operation

import com.imstargg.core.domain.operation.Feedback
import com.imstargg.core.domain.operation.FeedbackCreate
import com.imstargg.core.enums.operation.FeedbackRating
import org.springframework.stereotype.Service
import java.time.Clock

@Service
class FeedbackService(
    private val repository: FeedbackRepository,
    private val clock: Clock,
) {
    fun submit(
        rating: FeedbackRating,
        content: String,
    ): Feedback {
        val command =
            FeedbackCreate(
                rating = rating,
                content = content.trim(),
                submittedAt = clock.instant(),
            )
        return repository.save(command)
    }
}
```

- [ ] **Step 2: 커밋**

```bash
git add imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/operation/FeedbackService.kt
git commit -m "feat(core-api): FeedbackService에 Clock 주입하여 submittedAt 명시 세팅"
```

---

### Task 8: `FeedbackCreateRequest` / `FeedbackController` 교체

**Files:**
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/operation/FeedbackCreateRequest.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/operation/FeedbackController.kt`

`FeedbackCreateResponse.kt` 는 `id` 만 반환하므로 변경 없음.

- [ ] **Step 1: `FeedbackCreateRequest.kt` 교체**

```kotlin
package com.imstargg.core.api.controller.v1.operation

import com.imstargg.core.enums.operation.FeedbackRating
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class FeedbackCreateRequest(
    @field:NotNull
    val rating: FeedbackRating?,
    @field:NotBlank
    @field:Size(max = 2000)
    val content: String,
)
```

> 메모: `rating: FeedbackRating?` + `@NotNull` 조합은 JSON 에서 `rating` 키 누락 시 400 을 받기 위함. 컨트롤러는 `requireNotNull` 또는 `!!` 로 풀어서 서비스로 전달.

- [ ] **Step 2: `FeedbackController.kt` 교체**

```kotlin
package com.imstargg.core.api.controller.v1.operation

import com.imstargg.core.api.controller.v1.response.operation.FeedbackCreateResponse
import com.imstargg.core.application.operation.FeedbackService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class FeedbackController(
    private val feedbackService: FeedbackService,
) {
    @PostMapping("/v1/feedback")
    fun create(
        @Valid @RequestBody request: FeedbackCreateRequest,
    ): ResponseEntity<FeedbackCreateResponse> {
        val feedback =
            feedbackService.submit(
                rating = requireNotNull(request.rating),
                content = request.content,
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(FeedbackCreateResponse.of(feedback))
    }
}
```

- [ ] **Step 3: core-api 컴파일 확인**

Run: `./gradlew :core:core-api:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/operation/FeedbackCreateRequest.kt \
        imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/operation/FeedbackController.kt
git commit -m "feat(core-api): /v1/feedback 요청 본문을 {rating, content}로 교체"
```

---

### Task 9: `FeedbackServiceTest` 를 Clock.fixed 기반으로 재작성

**Files:**
- Modify: `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/application/operation/FeedbackServiceTest.kt`

`@TestConfiguration` 으로 `Clock.fixed(...)` 빈을 등록하고 `@Import` 로 가져오는 패턴 (CLAUDE.md: "시간: `Clock.fixed(...)` 주입"). 이메일 관련 테스트는 모두 제거.

- [ ] **Step 1: 파일 전체 교체**

```kotlin
package com.imstargg.core.application.operation

import com.imstargg.CoreApiContextTest
import com.imstargg.core.enums.operation.FeedbackRating
import com.imstargg.infrastructure.db.core.operation.FeedbackJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@Import(FeedbackServiceTest.FixedClockConfig::class)
class FeedbackServiceTest(
    private val sut: FeedbackService,
    private val cleanUp: CleanUp,
    private val feedbackJpaRepository: FeedbackJpaRepository,
) : CoreApiContextTest() {
    @TestConfiguration
    class FixedClockConfig {
        @Bean
        @Primary
        fun fixedClock(): Clock = Clock.fixed(NOW, ZoneId.of("Asia/Seoul"))
    }

    companion object {
        val NOW: Instant = Instant.parse("2026-05-22T10:00:00Z")
    }

    @BeforeEach
    fun setUp() {
        cleanUp.all()
    }

    @Test
    fun `요청한 값이 그대로 저장되고 식별자가 부여된다`() {
        // when
        val result =
            sut.submit(
                rating = FeedbackRating.VERY_SATISFIED,
                content = "만족합니다",
            )

        // then
        assertThat(result.id).isNotNull
        assertThat(result.rating).isEqualTo(FeedbackRating.VERY_SATISFIED)
        assertThat(result.content).isEqualTo("만족합니다")
        assertThat(result.submittedAt).isEqualTo(NOW)
        assertThat(result.checked).isFalse

        val saved = feedbackJpaRepository.findById(result.id).orElseThrow()
        assertThat(saved.rating).isEqualTo(FeedbackRating.VERY_SATISFIED)
        assertThat(saved.content).isEqualTo("만족합니다")
        assertThat(saved.submittedAt).isEqualTo(NOW)
    }

    @Test
    fun `content는 trim 되어 저장된다`() {
        // when
        val result =
            sut.submit(
                rating = FeedbackRating.SATISFIED,
                content = "  의견 내용  ",
            )

        // then
        assertThat(result.content).isEqualTo("의견 내용")
        val saved = feedbackJpaRepository.findById(result.id).orElseThrow()
        assertThat(saved.content).isEqualTo("의견 내용")
    }

    @Test
    fun `submittedAt은 주입된 Clock 시각으로 세팅된다`() {
        // when
        val result =
            sut.submit(
                rating = FeedbackRating.NEUTRAL,
                content = "보통",
            )

        // then
        assertThat(result.submittedAt).isEqualTo(NOW)
        val saved = feedbackJpaRepository.findById(result.id).orElseThrow()
        assertThat(saved.submittedAt).isEqualTo(NOW)
    }
}
```

- [ ] **Step 2: 테스트 실행 (Green)**

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.application.operation.FeedbackServiceTest"`
Expected: 3개 테스트 PASS

- [ ] **Step 3: 커밋**

```bash
git add imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/application/operation/FeedbackServiceTest.kt
git commit -m "test(core-api): FeedbackServiceTest를 Clock.fixed 기반 rating 시나리오로 교체"
```

---

### Task 10: `FeedbackControllerTest` 갱신

**Files:**
- Modify: `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/api/controller/v1/operation/FeedbackControllerTest.kt`

이메일 케이스 / category 케이스 제거, rating 누락 / content 검증 케이스 유지.

- [ ] **Step 1: 파일 전체 교체**

```kotlin
package com.imstargg.core.api.controller.v1.operation

import com.imstargg.CoreApiContextTest
import com.imstargg.core.enums.operation.FeedbackRating
import com.imstargg.infrastructure.db.core.operation.FeedbackJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@AutoConfigureMockMvc
class FeedbackControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val feedbackJpaRepository: FeedbackJpaRepository,
        private val cleanUp: CleanUp,
    ) : CoreApiContextTest() {
        @AfterEach
        fun tearDown() {
            cleanUp.all()
        }

        @Test
        fun `정상 제출시 201과 id를 반환한다`() {
            // given
            val body = """{"rating":"VERY_SATISFIED","content":"만족합니다"}"""

            // when & then
            mockMvc
                .post("/v1/feedback") {
                    contentType = MediaType.APPLICATION_JSON
                    content = body
                }.andExpect {
                    status { isCreated() }
                    jsonPath("$.id") { exists() }
                }

            val saved = feedbackJpaRepository.findAll()
            assertThat(saved).hasSize(1)
            assertThat(saved[0].rating).isEqualTo(FeedbackRating.VERY_SATISFIED)
            assertThat(saved[0].content).isEqualTo("만족합니다")
        }

        @Test
        fun `content가 빈문자열이면 400을 반환한다`() {
            // given
            val body = """{"rating":"VERY_SATISFIED","content":"   "}"""

            // when & then
            mockMvc
                .post("/v1/feedback") {
                    contentType = MediaType.APPLICATION_JSON
                    content = body
                }.andExpect {
                    status { isBadRequest() }
                }
        }

        @Test
        fun `content가 2000자를 초과하면 400을 반환한다`() {
            // given
            val longContent = "a".repeat(2001)
            val body = """{"rating":"VERY_SATISFIED","content":"$longContent"}"""

            // when & then
            mockMvc
                .post("/v1/feedback") {
                    contentType = MediaType.APPLICATION_JSON
                    content = body
                }.andExpect {
                    status { isBadRequest() }
                }
        }

        @Test
        fun `rating이 없으면 400을 반환한다`() {
            // given
            val body = """{"content":"내용"}"""

            // when & then
            mockMvc
                .post("/v1/feedback") {
                    contentType = MediaType.APPLICATION_JSON
                    content = body
                }.andExpect {
                    status { isBadRequest() }
                }
        }

        @Test
        fun `rating이 enum에 없는 값이면 400을 반환한다`() {
            // given
            val body = """{"rating":"BUG","content":"내용"}"""

            // when & then
            mockMvc
                .post("/v1/feedback") {
                    contentType = MediaType.APPLICATION_JSON
                    content = body
                }.andExpect {
                    status { isBadRequest() }
                }
        }
    }
```

- [ ] **Step 2: 테스트 실행 (Green)**

Run: `./gradlew :core:core-api:test --tests "com.imstargg.core.api.controller.v1.operation.FeedbackControllerTest"`
Expected: 5개 테스트 PASS

- [ ] **Step 3: 커밋**

```bash
git add imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/api/controller/v1/operation/FeedbackControllerTest.kt
git commit -m "test(core-api): FeedbackControllerTest를 rating 기반 케이스로 교체"
```

---

## Phase 4 — core-admin: 검색 / 응답 / 컨트롤러 갱신

### Task 11: `FeedbackSearchRequest` / `FeedbackResponse` 교체

**Files:**
- Modify: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/request/operation/FeedbackSearchRequest.kt`
- Modify: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/response/operation/FeedbackResponse.kt`

`FeedbackListResponse.kt` 는 `FeedbackResponse` 만 참조하므로 추가 변경 없음.

- [ ] **Step 1: `FeedbackSearchRequest.kt` 교체**

```kotlin
package com.imstargg.core.admin.request.operation

import com.imstargg.core.enums.operation.FeedbackRating

data class FeedbackSearchRequest(
    val rating: FeedbackRating? = null,
    val checked: Boolean? = null,
    val page: Int = 0,
    val size: Int = 20,
) {
    fun normalizedPage(): Int = page.coerceAtLeast(0)

    fun normalizedSize(): Int = size.coerceIn(1, 100)
}
```

- [ ] **Step 2: `FeedbackResponse.kt` 교체**

```kotlin
package com.imstargg.core.admin.response.operation

import com.imstargg.core.enums.operation.FeedbackRating
import com.imstargg.infrastructure.db.core.operation.FeedbackEntity
import java.time.Instant

data class FeedbackResponse(
    val id: Long,
    val rating: FeedbackRating,
    val content: String,
    val checked: Boolean,
    val submittedAt: Instant,
) {
    companion object {
        fun of(entity: FeedbackEntity): FeedbackResponse =
            FeedbackResponse(
                id = requireNotNull(entity.id),
                rating = entity.rating,
                content = entity.content,
                checked = entity.checked,
                submittedAt = entity.submittedAt,
            )
    }
}
```

- [ ] **Step 3: 커밋**

```bash
git add imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/request/operation/FeedbackSearchRequest.kt \
        imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/response/operation/FeedbackResponse.kt
git commit -m "feat(core-admin): admin 피드백 요청/응답 DTO를 rating + submittedAt로 교체"
```

---

### Task 12: `AdminFeedbackRepository` / `AdminFeedbackService` / `AdminFeedbackController` 교체

**Files:**
- Modify: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/application/operation/AdminFeedbackRepository.kt`
- Modify: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/application/operation/AdminFeedbackService.kt`
- Modify: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/operation/AdminFeedbackController.kt`

QueryDSL predicate / orderBy / 시그니처를 새 도메인에 맞춤.

- [ ] **Step 1: `AdminFeedbackRepository.kt` 교체**

```kotlin
package com.imstargg.core.admin.application.operation

import com.imstargg.core.enums.operation.FeedbackRating
import com.imstargg.infrastructure.db.core.operation.FeedbackEntity
import com.imstargg.infrastructure.db.core.operation.FeedbackJpaRepository
import com.imstargg.infrastructure.db.core.operation.QFeedbackEntity.Companion.feedbackEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
class AdminFeedbackRepository(
    entityManager: EntityManager,
    private val jpaRepository: FeedbackJpaRepository,
) {
    private val queryFactory = JPAQueryFactory(entityManager)

    fun search(
        rating: FeedbackRating?,
        checked: Boolean?,
        pageable: Pageable,
    ): Page<FeedbackEntity> {
        val predicates =
            listOfNotNull(
                rating?.let { feedbackEntity.rating.eq(it) },
                checked?.let { feedbackEntity.checked.eq(it) },
            ).toTypedArray()

        val content =
            queryFactory
                .selectFrom(feedbackEntity)
                .where(*predicates)
                .orderBy(feedbackEntity.submittedAt.desc())
                .offset(pageable.offset)
                .limit(pageable.pageSize.toLong())
                .fetch()

        val total =
            queryFactory
                .select(feedbackEntity.count())
                .from(feedbackEntity)
                .where(*predicates)
                .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
    }

    fun findById(id: Long): FeedbackEntity? = jpaRepository.findById(id).orElse(null)
}
```

- [ ] **Step 2: `AdminFeedbackService.kt` 교체**

```kotlin
package com.imstargg.core.admin.application.operation

import com.imstargg.core.enums.operation.FeedbackRating
import com.imstargg.infrastructure.db.core.operation.FeedbackEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class AdminFeedbackService(
    private val repository: AdminFeedbackRepository,
) {
    @Transactional(readOnly = true)
    fun search(
        rating: FeedbackRating?,
        checked: Boolean?,
        page: Int,
        size: Int,
    ): Page<FeedbackEntity> = repository.search(rating, checked, PageRequest.of(page, size))

    @Transactional
    fun updateCheck(
        id: Long,
        checked: Boolean,
    ) {
        val entity =
            repository.findById(id)
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Feedback not found: id=$id")
        if (checked) entity.check() else entity.uncheck()
    }
}
```

- [ ] **Step 3: `AdminFeedbackController.kt` 교체**

```kotlin
package com.imstargg.core.admin.controller.operation

import com.imstargg.core.admin.application.operation.AdminFeedbackService
import com.imstargg.core.admin.request.operation.FeedbackCheckRequest
import com.imstargg.core.admin.request.operation.FeedbackSearchRequest
import com.imstargg.core.admin.response.operation.FeedbackListResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AdminFeedbackController(
    private val service: AdminFeedbackService,
) {
    @GetMapping("/feedbacks")
    fun list(
        @ModelAttribute request: FeedbackSearchRequest,
    ): ResponseEntity<FeedbackListResponse> {
        val page =
            service.search(
                rating = request.rating,
                checked = request.checked,
                page = request.normalizedPage(),
                size = request.normalizedSize(),
            )
        return ResponseEntity.ok(FeedbackListResponse.of(page))
    }

    @PutMapping("/feedbacks/{id}/check")
    fun updateCheck(
        @PathVariable id: Long,
        @RequestBody request: FeedbackCheckRequest,
    ): ResponseEntity<Unit> {
        service.updateCheck(id, request.checked)
        return ResponseEntity.noContent().build()
    }
}
```

- [ ] **Step 4: core-admin 컴파일 확인**

Run: `./gradlew :core:core-admin:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 커밋**

```bash
git add imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/application/operation/AdminFeedbackRepository.kt \
        imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/application/operation/AdminFeedbackService.kt \
        imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/operation/AdminFeedbackController.kt
git commit -m "feat(core-admin): rating 필터 + submittedAt 정렬로 조회 경로 교체"
```

---

### Task 13: `AdminFeedbackControllerTest` 갱신

**Files:**
- Modify: `imstargg-backend/core/core-admin/src/test/kotlin/com/imstargg/core/admin/controller/operation/AdminFeedbackControllerTest.kt`

기존 BUG/FEATURE_REQUEST 픽스처를 rating 값으로 교체. 응답 필드명도 `submittedAt` 으로 변경된 것을 검증.

- [ ] **Step 1: 파일 전체 교체**

```kotlin
package com.imstargg.core.admin.controller.operation

import com.imstargg.CoreAdminContextTest
import com.imstargg.core.enums.operation.FeedbackRating
import com.imstargg.infrastructure.db.core.operation.FeedbackEntity
import com.imstargg.infrastructure.db.core.operation.FeedbackJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import java.time.Instant

@AutoConfigureMockMvc
class AdminFeedbackControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val feedbackJpaRepository: FeedbackJpaRepository,
        private val cleanUp: CleanUp,
    ) : CoreAdminContextTest() {
        private val baseSubmittedAt: Instant = Instant.parse("2026-05-22T10:00:00Z")

        @AfterEach
        fun tearDown() {
            cleanUp.all()
        }

        @Test
        fun `목록을 rating으로 필터링할 수 있다`() {
            // given
            feedbackJpaRepository.save(
                FeedbackEntity(
                    rating = FeedbackRating.VERY_SATISFIED,
                    content = "매우 만족",
                    submittedAt = baseSubmittedAt,
                ),
            )
            feedbackJpaRepository.save(
                FeedbackEntity(
                    rating = FeedbackRating.DISSATISFIED,
                    content = "불만족",
                    submittedAt = baseSubmittedAt,
                ),
            )

            // when & then
            mockMvc
                .get("/feedbacks") {
                    param("rating", "VERY_SATISFIED")
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.content.length()") { value(1) }
                    jsonPath("$.content[0].rating") { value("VERY_SATISFIED") }
                    jsonPath("$.content[0].submittedAt") { exists() }
                }
        }

        @Test
        fun `목록은 submittedAt 내림차순으로 정렬된다`() {
            // given
            val older =
                feedbackJpaRepository.save(
                    FeedbackEntity(
                        rating = FeedbackRating.NEUTRAL,
                        content = "오래된 의견",
                        submittedAt = baseSubmittedAt.minusSeconds(3600),
                    ),
                )
            val newer =
                feedbackJpaRepository.save(
                    FeedbackEntity(
                        rating = FeedbackRating.NEUTRAL,
                        content = "최근 의견",
                        submittedAt = baseSubmittedAt,
                    ),
                )

            // when & then
            mockMvc
                .get("/feedbacks")
                .andExpect {
                    status { isOk() }
                    jsonPath("$.content[0].id") { value(newer.id!!.toInt()) }
                    jsonPath("$.content[1].id") { value(older.id!!.toInt()) }
                }
        }

        @Test
        fun `PUT _feedbacks_{id}_check가 checked를 true로 만든다`() {
            // given
            val saved =
                feedbackJpaRepository.save(
                    FeedbackEntity(
                        rating = FeedbackRating.SATISFIED,
                        content = "체크 테스트",
                        submittedAt = baseSubmittedAt,
                        checked = false,
                    ),
                )

            // when
            mockMvc
                .put("/feedbacks/${saved.id}/check") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"checked":true}"""
                }.andExpect {
                    // then
                    status { isNoContent() }
                }

            val updated = feedbackJpaRepository.findById(requireNotNull(saved.id)).get()
            assertThat(updated.checked).isTrue()
        }

        @Test
        fun `존재하지 않는 id에 PUT하면 404를 반환한다`() {
            // when & then
            mockMvc
                .put("/feedbacks/999999/check") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"checked":true}"""
                }.andExpect {
                    status { isNotFound() }
                }
        }
    }
```

- [ ] **Step 2: 테스트 실행 (Green)**

Run: `./gradlew :core:core-admin:test --tests "com.imstargg.core.admin.controller.operation.AdminFeedbackControllerTest"`
Expected: 4개 테스트 PASS

- [ ] **Step 3: 커밋**

```bash
git add imstargg-backend/core/core-admin/src/test/kotlin/com/imstargg/core/admin/controller/operation/AdminFeedbackControllerTest.kt
git commit -m "test(core-admin): rating 필터 + submittedAt 정렬 시나리오로 교체"
```

---

## Phase 5 — core-batch: 24h 다이제스트 갱신

### Task 14: `FeedbackDigestMessage` 재작성 (rating 분포 기반)

**Files:**
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/operation/FeedbackDigestMessage.kt`
- Modify: `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/operation/FeedbackDigestMessageTest.kt`

`countByRating` 으로 시그니처 교체 + 이메일 카운트 라인 제거. 한국어 라벨은 인라인 매핑.

- [ ] **Step 1: 테스트 파일 교체 (Red)**

```kotlin
package com.imstargg.core.batch.job.operation

import com.imstargg.core.enums.operation.FeedbackRating
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FeedbackDigestMessageTest {
    @Test
    fun `0건이면 null을 반환한다`() {
        // when
        val result = FeedbackDigestMessage.build(emptyMap())

        // then
        assertThat(result).isNull()
    }

    @Test
    fun `rating별 건수를 한국어 라벨로 포함한다`() {
        // given
        val counts =
            mapOf(
                FeedbackRating.VERY_SATISFIED to 4L,
                FeedbackRating.SATISFIED to 5L,
                FeedbackRating.NEUTRAL to 2L,
                FeedbackRating.DISSATISFIED to 1L,
                FeedbackRating.VERY_DISSATISFIED to 0L,
            )

        // when
        val result = FeedbackDigestMessage.build(counts)!!

        // then
        assertThat(result.title).isEqualTo("📨 지난 24시간 피드백 12건")
        assertThat(result.content).contains("• 매우 만족 4건")
        assertThat(result.content).contains("• 만족 5건")
        assertThat(result.content).contains("• 보통 2건")
        assertThat(result.content).contains("• 불만족 1건")
        assertThat(result.content).doesNotContain("매우 불만족")
    }

    @Test
    fun `0인 rating 라인은 생략한다`() {
        // given
        val counts =
            mapOf(
                FeedbackRating.VERY_SATISFIED to 0L,
                FeedbackRating.SATISFIED to 3L,
                FeedbackRating.NEUTRAL to 0L,
            )

        // when
        val result = FeedbackDigestMessage.build(counts)!!

        // then
        assertThat(result.title).isEqualTo("📨 지난 24시간 피드백 3건")
        assertThat(result.content).doesNotContain("매우 만족")
        assertThat(result.content).doesNotContain("보통")
        assertThat(result.content).contains("• 만족 3건")
    }
}
```

- [ ] **Step 2: 테스트 실행 — 컴파일/실행 실패 확인 (Red)**

Run: `./gradlew :core:core-batch:compileTestKotlin`
Expected: `FeedbackDigestMessage.build(emptyMap())` 호출이 기존 `(emailCount: Long)` 시그니처와 안 맞아 컴파일 에러.

- [ ] **Step 3: `FeedbackDigestMessage.kt` 재작성**

```kotlin
package com.imstargg.core.batch.job.operation

import com.imstargg.core.enums.operation.FeedbackRating

object FeedbackDigestMessage {
    private val RATING_LABELS: Map<FeedbackRating, String> =
        mapOf(
            FeedbackRating.VERY_SATISFIED to "매우 만족",
            FeedbackRating.SATISFIED to "만족",
            FeedbackRating.NEUTRAL to "보통",
            FeedbackRating.DISSATISFIED to "불만족",
            FeedbackRating.VERY_DISSATISFIED to "매우 불만족",
        )

    fun build(countByRating: Map<FeedbackRating, Long>): FeedbackDigest? {
        val total = countByRating.values.sum()
        if (total == 0L) return null

        val lines = mutableListOf<String>()
        FeedbackRating.entries.forEach { rating ->
            val count = countByRating[rating] ?: 0L
            if (count > 0L) {
                val label = RATING_LABELS[rating] ?: rating.name
                lines += "• $label ${count}건"
            }
        }

        return FeedbackDigest(
            title = "📨 지난 24시간 피드백 ${total}건",
            content = lines.joinToString("\n"),
        )
    }
}
```

- [ ] **Step 4: 테스트 실행 (Green)**

Run: `./gradlew :core:core-batch:test --tests "com.imstargg.core.batch.job.operation.FeedbackDigestMessageTest"`
Expected: 3개 테스트 PASS

- [ ] **Step 5: 커밋**

```bash
git add imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/operation/FeedbackDigestMessage.kt \
        imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/operation/FeedbackDigestMessageTest.kt
git commit -m "feat(core-batch): FeedbackDigestMessage를 rating 분포 기반으로 재작성"
```

---

### Task 15: `FeedbackDailyDigestJobConfig` 갱신

**Files:**
- Modify: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/operation/FeedbackDailyDigestJobConfig.kt`

`submittedAt` 범위 조회 + rating 분포 카운트로 교체.

- [ ] **Step 1: 파일 전체 교체**

```kotlin
package com.imstargg.core.batch.job.operation

import com.imstargg.core.batch.support.alert.AlertCommand
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.core.enums.operation.FeedbackRating
import com.imstargg.infrastructure.db.core.operation.FeedbackJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import java.time.Clock
import java.time.Duration

@Configuration
class FeedbackDailyDigestJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val feedbackJpaRepository: FeedbackJpaRepository,
    private val alertManager: AlertManager,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val JOB_NAME = "feedbackDailyDigestJob"
        const val STEP_NAME = "feedbackDailyDigestStep"
    }

    @Bean(JOB_NAME)
    fun job(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .start(step())
            .build()

    @Bean(STEP_NAME)
    fun step(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet({ _, _ ->
                val now = clock.instant()
                val from = now.minus(Duration.ofHours(24))
                val recent =
                    feedbackJpaRepository.findAllBySubmittedAtGreaterThanEqualAndSubmittedAtLessThan(from = from, to = now)

                if (recent.isEmpty()) {
                    log.info("피드백 24시간 다이제스트: 0건 — 알림 미발송")
                    return@tasklet RepeatStatus.FINISHED
                }

                val counts: Map<FeedbackRating, Long> =
                    FeedbackRating.entries.associateWith { r -> recent.count { it.rating == r }.toLong() }

                val digest = FeedbackDigestMessage.build(countByRating = counts)
                if (digest != null) {
                    alertManager.alert(AlertCommand(title = digest.title, content = digest.content, ex = null))
                    log.info("피드백 24시간 다이제스트: 총 {}건 알림 발송", recent.size)
                }

                RepeatStatus.FINISHED
            }, txManager)
            .build()
}
```

- [ ] **Step 2: core-batch 컴파일/테스트 확인**

Run: `./gradlew :core:core-batch:test`
Expected: BUILD SUCCESSFUL (다이제스트 메시지 테스트 + 기타 batch 테스트)

- [ ] **Step 3: 커밋**

```bash
git add imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/operation/FeedbackDailyDigestJobConfig.kt
git commit -m "feat(core-batch): 24h 다이제스트를 submittedAt 범위 + rating 카운트로 교체"
```

---

## Phase 6 — cleanup: 구 `FeedbackCategory` 삭제

### Task 16: `FeedbackCategory` enum 삭제

**Files:**
- Delete: `imstargg-backend/core/core-enum/src/main/kotlin/com/imstargg/core/enums/operation/FeedbackCategory.kt`

이 시점에 db-core / core-api / core-admin / core-batch 어디서도 `FeedbackCategory` 를 참조하지 않아야 한다.

- [ ] **Step 1: 잔존 참조 검색**

Run:
```bash
grep -rn "FeedbackCategory" imstargg-backend --include="*.kt"
```
Expected: 결과 0건. 잔존 참조가 있으면 해당 파일을 먼저 정리한다.

- [ ] **Step 2: 파일 삭제**

Run: `rm imstargg-backend/core/core-enum/src/main/kotlin/com/imstargg/core/enums/operation/FeedbackCategory.kt`

- [ ] **Step 3: 전체 빌드 (테스트 제외) 검증**

Run: `./gradlew build -x test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 커밋**

```bash
git add -A
git commit -m "chore(core-enum): 구 FeedbackCategory enum 삭제"
```

---

## Phase 7 — 백엔드 전체 빌드 검증

### Task 17: 전체 테스트 + ktlint 통과 확인

- [ ] **Step 1: ktlint 자동 수정**

Run: `./gradlew ktlintFormat`
Expected: BUILD SUCCESSFUL (변경된 파일이 있다면 후속 step 에서 커밋)

- [ ] **Step 2: ktlint 검증**

Run: `./gradlew ktlintCheck`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 전체 테스트 실행 (단위 + context)**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL — `FeedbackEntityTest`, `FeedbackServiceTest`, `FeedbackControllerTest`, `AdminFeedbackControllerTest`, `FeedbackDigestMessageTest` 포함 모든 테스트 PASS

- [ ] **Step 4: ktlint 변경분 있으면 커밋**

```bash
git status
# 변경된 파일이 있을 경우만 커밋
git add -A
git commit -m "style: ktlintFormat 자동 적용" 2>/dev/null || echo "no ktlint changes"
```

---

## Phase 8 — imstargg-frontend (사용자 프론트)

> **Phase 8 노트:** Task 18–22 는 서로 강하게 결합돼 있어 **개별 task 커밋 직후에는 typecheck 가 통과하지 않는다** (예: Task 18 에서 `FeedbackCategory` 타입을 export 에서 빼면, 아직 갱신되지 않은 `feedback-category.ts` 와 `FeedbackForm.tsx` 가 깨진다). 정상 typecheck 는 Task 22 (구 모듈 삭제) 까지 완료된 시점에만 보장된다. 최종 검증(Task 23) 까지 진행해야 Phase 8 가 완료된 것으로 본다.

### Task 18: `api/operation/feedback.ts` 타입 교체 (먼저 진행 — `FeedbackRating` export 가 다음 task 들의 import 기반)

**Files:**
- Modify: `imstargg-frontend/src/api/operation/feedback.ts`

- [ ] **Step 1: 파일 전체 교체**

```ts
import { apiFetch } from "@/api/client";

export type FeedbackRating =
  | "VERY_SATISFIED"
  | "SATISFIED"
  | "NEUTRAL"
  | "DISSATISFIED"
  | "VERY_DISSATISFIED";

export interface FeedbackCreateRequest {
  rating: FeedbackRating;
  content: string;
}

export interface FeedbackCreateResponse {
  id: number;
}

export async function createFeedback(
  body: FeedbackCreateRequest,
): Promise<FeedbackCreateResponse> {
  return apiFetch<FeedbackCreateResponse>("/v1/feedback", {
    method: "POST",
    body,
  });
}
```

- [ ] **Step 2: 커밋**

```bash
git add imstargg-frontend/src/api/operation/feedback.ts
git commit -m "feat(frontend): /v1/feedback 클라이언트 타입을 rating + content로 교체"
```

> 이 커밋 직후 typecheck 는 실패한다 — 기존 `feedback-category.ts` 와 `FeedbackForm.tsx` 가 사라진 `FeedbackCategory` 타입을 참조. 다음 task 들에서 일괄 정리.

---

### Task 19: `feedback-rating.ts` 신설

**Files:**
- Create: `imstargg-frontend/src/components/feedback/feedback-rating.ts`

- [ ] **Step 1: 파일 작성**

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

- [ ] **Step 2: 커밋**

```bash
git add imstargg-frontend/src/components/feedback/feedback-rating.ts
git commit -m "feat(frontend): feedback-rating 라벨/이모지 매핑 신설"
```

---

### Task 20: `FeedbackForm.tsx` 재작성

**Files:**
- Modify: `imstargg-frontend/src/components/feedback/FeedbackForm.tsx`

zod 스키마, 만족도 라디오 그룹, 이메일 제거, content 카운터 유지.

- [ ] **Step 1: 파일 전체 교체**

```tsx
"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { z } from "zod";

import { createFeedback, type FeedbackRating } from "@/api/operation/feedback";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  FEEDBACK_RATINGS,
  FEEDBACK_RATING_LABELS,
  FEEDBACK_RATING_EMOJIS,
} from "@/components/feedback/feedback-rating";

const schema = z.object({
  rating: z.enum([
    "VERY_SATISFIED",
    "SATISFIED",
    "NEUTRAL",
    "DISSATISFIED",
    "VERY_DISSATISFIED",
  ], { required_error: "만족도를 선택해주세요" }),
  content: z
    .string()
    .trim()
    .min(1, "의견을 입력해주세요")
    .max(2000, "최대 2000자까지 입력 가능합니다"),
});

type FormValues = z.infer<typeof schema>;

interface Props {
  onSuccess: () => void;
  onCancel: () => void;
}

export function FeedbackForm({ onSuccess, onCancel }: Props) {
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { content: "" },
  });

  const content = watch("content") ?? "";

  const onSubmit = async (values: FormValues) => {
    try {
      await createFeedback({
        rating: values.rating as FeedbackRating,
        content: values.content,
      });
      toast.success("의견이 접수되었습니다. 감사합니다!");
      onSuccess();
    } catch {
      toast.error("전송에 실패했습니다. 잠시 후 다시 시도해주세요");
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
      <div>
        <Label className="mb-2 block">만족도 <span className="text-red-500">*</span></Label>
        <div role="radiogroup" className="grid grid-cols-5 gap-2">
          {FEEDBACK_RATINGS.map((r) => (
            <label
              key={r}
              className="flex cursor-pointer flex-col items-center gap-1 rounded-md border border-neutral-200 bg-white p-2 text-center transition has-[:checked]:border-yellow-400 has-[:checked]:shadow-[0_3px_0_0_rgba(0,0,0,0.35)] has-[:checked]:ring-4 has-[:checked]:ring-yellow-200/50 hover:border-neutral-300"
            >
              <input
                type="radio"
                value={r}
                {...register("rating")}
                className="sr-only"
              />
              <span className="text-xl" aria-hidden>
                {FEEDBACK_RATING_EMOJIS[r]}
              </span>
              <span className="text-[12px] font-bold leading-none">
                {FEEDBACK_RATING_LABELS[r]}
              </span>
            </label>
          ))}
        </div>
        {errors.rating && (
          <p className="mt-1 text-xs text-red-500">{errors.rating.message}</p>
        )}
      </div>

      <div>
        <Label htmlFor="feedback-content">
          의견 <span className="text-red-500">*</span>
        </Label>
        <Textarea
          id="feedback-content"
          rows={6}
          maxLength={2000}
          aria-invalid={Boolean(errors.content)}
          {...register("content")}
        />
        <div className="flex justify-between text-xs text-muted-foreground">
          <span>{errors.content?.message}</span>
          <span>{content.length} / 2000</span>
        </div>
      </div>

      <div className="flex justify-end gap-2">
        <Button type="button" variant="outline" onClick={onCancel}>
          취소
        </Button>
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "보내는 중..." : "보내기"}
        </Button>
      </div>
    </form>
  );
}
```

- [ ] **Step 2: 커밋**

```bash
git add imstargg-frontend/src/components/feedback/FeedbackForm.tsx
git commit -m "feat(frontend): FeedbackForm을 5단계 만족도 + 의견 입력으로 재작성"
```

---

### Task 21: `FeedbackDialog.tsx` 카피 갱신

**Files:**
- Modify: `imstargg-frontend/src/components/feedback/FeedbackDialog.tsx`

타이틀/부제 갱신, `DialogDescription` 추가.

- [ ] **Step 1: 파일 전체 교체**

```tsx
"use client";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { FeedbackForm } from "@/components/feedback/FeedbackForm";
import { useFeedbackDialog } from "@/hooks/use-feedback-dialog";

export function FeedbackDialog() {
  const { open, setOpen } = useFeedbackDialog();

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>ImStarGG에 피드백을 전달해주세요.</DialogTitle>
          <DialogDescription>
            주신 의견은 빠짐없이 확인하고 감사히 받아들이겠습니다.
          </DialogDescription>
        </DialogHeader>
        <FeedbackForm
          onSuccess={() => setOpen(false)}
          onCancel={() => setOpen(false)}
        />
      </DialogContent>
    </Dialog>
  );
}
```

- [ ] **Step 2: `DialogDescription` 컴포넌트가 export 되는지 확인**

Run: `grep -n "DialogDescription" imstargg-frontend/src/components/ui/dialog.tsx`
Expected: export 되어 있어야 함. shadcn/ui 의 표준 dialog 모듈은 `DialogDescription` 을 포함한다. 만약 export 가 누락돼 있다면 export 라인을 추가하고 별도 커밋한다.

- [ ] **Step 3: 커밋**

```bash
git add imstargg-frontend/src/components/feedback/FeedbackDialog.tsx
git commit -m "feat(frontend): FeedbackDialog 타이틀/부제를 새 카피로 교체"
```

---

### Task 22: `feedback-category.ts` 삭제

**Files:**
- Delete: `imstargg-frontend/src/components/feedback/feedback-category.ts`

- [ ] **Step 1: 잔존 참조 검증**

Run:
```bash
grep -rn "feedback-category\|FEEDBACK_CATEGORIES\|FEEDBACK_CATEGORY_LABELS" imstargg-frontend/src
```
Expected: 결과 0건.

- [ ] **Step 2: 파일 삭제 + 커밋**

```bash
rm imstargg-frontend/src/components/feedback/feedback-category.ts
git add -A
git commit -m "chore(frontend): 구 feedback-category 모듈 삭제"
```

---

### Task 23: imstargg-frontend 검증

- [ ] **Step 1: typecheck**

Run: `cd imstargg-frontend && pnpm typecheck`
Expected: 에러 0건

- [ ] **Step 2: lint**

Run: `cd imstargg-frontend && pnpm lint`
Expected: 에러 0건

- [ ] **Step 3: 단위 테스트 (있다면)**

Run: `cd imstargg-frontend && pnpm test:run`
Expected: PASS

---

## Phase 9 — imstargg-admin-frontend (관리자 프론트)

### Task 24: `lib/types.ts` 갱신

**Files:**
- Modify: `imstargg-admin-frontend/lib/types.ts`

`FeedbackCategory` → `FeedbackRating`, `FeedbackItem.createdAt` → `submittedAt`, `email` 필드 제거. `FeedbackSearchParams.category` → `rating`. 다른 타입(`Brawler` 등) 은 그대로 둔다.

- [ ] **Step 1: feedback 관련 블록만 교체**

`imstargg-admin-frontend/lib/types.ts` 의 다음 라인 (현재 98–122):
```ts
export type FeedbackCategory = "BUG" | "FEATURE_REQUEST" | "ETC";

export interface FeedbackItem {
  id: number;
  category: FeedbackCategory;
  content: string;
  email: string | null;
  checked: boolean;
  createdAt: string;
}

export interface FeedbackListResponse {
  content: FeedbackItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface FeedbackSearchParams {
  category?: FeedbackCategory;
  checked?: boolean;
  page?: number;
  size?: number;
}
```
를 아래로 교체:
```ts
export type FeedbackRating =
  | "VERY_SATISFIED"
  | "SATISFIED"
  | "NEUTRAL"
  | "DISSATISFIED"
  | "VERY_DISSATISFIED";

export interface FeedbackItem {
  id: number;
  rating: FeedbackRating;
  content: string;
  checked: boolean;
  submittedAt: string;
}

export interface FeedbackListResponse {
  content: FeedbackItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface FeedbackSearchParams {
  rating?: FeedbackRating;
  checked?: boolean;
  page?: number;
  size?: number;
}
```

- [ ] **Step 2: 커밋**

```bash
git add imstargg-admin-frontend/lib/types.ts
git commit -m "feat(admin-frontend): 피드백 타입을 rating + submittedAt로 교체"
```

---

### Task 25: `lib/api/operation/feedback.ts` 쿼리 키 교체

**Files:**
- Modify: `imstargg-admin-frontend/lib/api/operation/feedback.ts`

- [ ] **Step 1: 파일 전체 교체**

```ts
import { request } from "@/lib/api";
import type { FeedbackListResponse, FeedbackSearchParams } from "@/lib/types";

export async function getFeedbacks(params: FeedbackSearchParams): Promise<FeedbackListResponse> {
  const search = new URLSearchParams();
  if (params.rating) search.set("rating", params.rating);
  if (params.checked !== undefined) search.set("checked", String(params.checked));
  if (params.page !== undefined) search.set("page", String(params.page));
  if (params.size !== undefined) search.set("size", String(params.size));

  const query = search.toString();
  return request<FeedbackListResponse>(`/feedbacks${query ? `?${query}` : ""}`);
}

export async function updateFeedbackCheck(id: number, checked: boolean): Promise<void> {
  return request<void>(`/feedbacks/${id}/check`, {
    method: "PUT",
    body: JSON.stringify({ checked }),
  });
}
```

- [ ] **Step 2: 커밋**

```bash
git add imstargg-admin-frontend/lib/api/operation/feedback.ts
git commit -m "feat(admin-frontend): /feedbacks 쿼리 키를 rating으로 교체"
```

---

### Task 26: `FeedbackFilters.tsx` 갱신

**Files:**
- Modify: `imstargg-admin-frontend/app/feedbacks/_components/FeedbackFilters.tsx`

카테고리 셀렉트를 rating 셀렉트로 교체.

- [ ] **Step 1: 파일 전체 교체**

```tsx
"use client";

import { useRouter, useSearchParams } from "next/navigation";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

const ALL = "ALL";

const RATINGS = [
  ALL,
  "VERY_SATISFIED",
  "SATISFIED",
  "NEUTRAL",
  "DISSATISFIED",
  "VERY_DISSATISFIED",
] as const;
const CHECKED_OPTIONS = [ALL, "false", "true"] as const;

const RATING_LABELS: Record<string, string> = {
  [ALL]: "만족도 (전체)",
  VERY_SATISFIED: "매우 만족",
  SATISFIED: "만족",
  NEUTRAL: "보통",
  DISSATISFIED: "불만족",
  VERY_DISSATISFIED: "매우 불만족",
};

const CHECKED_LABELS: Record<string, string> = {
  [ALL]: "상태 (전체)",
  true: "체크됨",
  false: "미체크",
};

export function FeedbackFilters() {
  const router = useRouter();
  const params = useSearchParams();

  const update = (key: string, value: string) => {
    const next = new URLSearchParams(params?.toString() ?? "");
    if (value === ALL) next.delete(key);
    else next.set(key, value);
    next.delete("page");
    router.replace(`/feedbacks?${next.toString()}`);
  };

  const rating = params?.get("rating") ?? ALL;
  const checked = params?.get("checked") ?? ALL;

  return (
    <div className="flex gap-2">
      <Select value={rating} onValueChange={(v) => update("rating", v ?? ALL)}>
        <SelectTrigger className="w-44">
          <SelectValue placeholder={RATING_LABELS[ALL]} />
        </SelectTrigger>
        <SelectContent>
          {RATINGS.map((r) => (
            <SelectItem key={r} value={r}>
              {RATING_LABELS[r]}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
      <Select value={checked} onValueChange={(v) => update("checked", v ?? ALL)}>
        <SelectTrigger className="w-36">
          <SelectValue placeholder={CHECKED_LABELS[ALL]} />
        </SelectTrigger>
        <SelectContent>
          {CHECKED_OPTIONS.map((c) => (
            <SelectItem key={c} value={c}>
              {CHECKED_LABELS[c]}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
}
```

- [ ] **Step 2: 커밋**

```bash
git add imstargg-admin-frontend/app/feedbacks/_components/FeedbackFilters.tsx
git commit -m "feat(admin-frontend): 필터를 rating 셀렉트로 교체"
```

---

### Task 27: `FeedbackTable.tsx` 갱신

**Files:**
- Modify: `imstargg-admin-frontend/app/feedbacks/_components/FeedbackTable.tsx`

이메일 컬럼 제거, 만족도 셀(이모지 + 라벨 + 색상 뱃지) 추가, 작성일시 셀이 `submittedAt` 참조.

- [ ] **Step 1: 파일 전체 교체**

```tsx
"use client";

import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import type { FeedbackItem, FeedbackListResponse, FeedbackRating } from "@/lib/types";
import { FeedbackCheckToggle } from "./FeedbackCheckToggle";
import { FeedbackContentCell } from "./FeedbackContentCell";

const RATING_LABEL: Record<FeedbackRating, string> = {
  VERY_SATISFIED: "매우 만족",
  SATISFIED: "만족",
  NEUTRAL: "보통",
  DISSATISFIED: "불만족",
  VERY_DISSATISFIED: "매우 불만족",
};

const RATING_EMOJI: Record<FeedbackRating, string> = {
  VERY_SATISFIED: "😄",
  SATISFIED: "🙂",
  NEUTRAL: "😐",
  DISSATISFIED: "🙁",
  VERY_DISSATISFIED: "😡",
};

const RATING_BADGE_CLASS: Record<FeedbackRating, string> = {
  VERY_SATISFIED: "bg-green-100 text-green-800 border-transparent",
  SATISFIED: "bg-lime-100 text-lime-800 border-transparent",
  NEUTRAL: "bg-neutral-100 text-neutral-700 border-transparent",
  DISSATISFIED: "bg-orange-100 text-orange-800 border-transparent",
  VERY_DISSATISFIED: "bg-red-100 text-red-800 border-transparent",
};

function formatDate(iso: string): string {
  const d = new Date(iso);
  const pad = (n: number) => n.toString().padStart(2, "0");
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

interface Props {
  data: FeedbackListResponse;
}

export function FeedbackTable({ data }: Props) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead className="w-12">체크</TableHead>
          <TableHead className="w-36">만족도</TableHead>
          <TableHead className="w-36">작성일시</TableHead>
          <TableHead>내용</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {data.content.map((row: FeedbackItem) => (
          <TableRow key={row.id}>
            <TableCell>
              <FeedbackCheckToggle id={row.id} initialChecked={row.checked} />
            </TableCell>
            <TableCell>
              <Badge className={RATING_BADGE_CLASS[row.rating]}>
                <span className="mr-1" aria-hidden>{RATING_EMOJI[row.rating]}</span>
                {RATING_LABEL[row.rating]}
              </Badge>
            </TableCell>
            <TableCell title={row.submittedAt}>{formatDate(row.submittedAt)}</TableCell>
            <TableCell>
              <FeedbackContentCell content={row.content} />
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
```

- [ ] **Step 2: 커밋**

```bash
git add imstargg-admin-frontend/app/feedbacks/_components/FeedbackTable.tsx
git commit -m "feat(admin-frontend): 테이블에 만족도 셀 추가, 이메일 컬럼 제거"
```

---

### Task 28: imstargg-admin-frontend 검증

- [ ] **Step 1: typecheck**

Run: `cd imstargg-admin-frontend && pnpm typecheck` (또는 `pnpm tsc --noEmit`)
Expected: 에러 0건

- [ ] **Step 2: lint**

Run: `cd imstargg-admin-frontend && pnpm lint`
Expected: 에러 0건

---

## Phase 10 — 릴리즈

### Task 29: release-management 스킬로 버전 bump

**영향 앱** (spec §12):
- `core-api`: minor — 요청 스키마 breaking
- `core-admin`: minor — 응답/필터 breaking
- `core-batch`: minor — digest 로직 변경
- `imstargg-frontend`: minor — API 클라이언트 + 폼 UI 변경
- `imstargg-admin-frontend`: 관리 대상 외 (스킬 무관)

- [ ] **Step 1: `bump` 모드 실행**

`release-management` 스킬을 `bump` 모드로 호출해 영향 앱의 SNAPSHOT 버전을 minor 단위로 bump.

> 메모: 사용자가 main PR / 배포 단계로 진입할 때 별도 트리거로 `prepare` 모드 호출. 본 plan 의 마지막 step 은 dev 단계의 `bump` 까지로 한정.

- [ ] **Step 2: 변경 확인**

Run: `git log --oneline -5`
Expected: 가장 최근 커밋이 release-management 스킬의 bump 커밋.

---

## Self-Review 메모

작성 후 점검:

1. **spec 커버리지** — spec 의 모든 섹션이 task 로 매핑됨:
   - §3.1 enum 신설 → Task 1
   - §3.2 DDL → Task 2
   - §3.3 Entity → Task 3
   - §3.4 검증 → Task 8 (request DTO) + Task 10 (controller test)
   - §4 core-api → Task 5–10
   - §5 core-admin → Task 11–13
   - §6 core-batch → Task 14–15
   - §7 사용자 프론트 → Task 18–23
   - §8 관리자 프론트 → Task 24–28
   - §9 API 계약 → Task 8 (요청) + Task 11/12 (응답)
   - §10 테스트 → Task 3 / 9 / 10 / 13 / 14
   - §11 마이그레이션 → Task 2
   - §12 릴리즈 → Task 29
2. **placeholder 없음** — TODO/TBD 없음, 코드 블록 모두 실제 코드.
3. **타입 정합성** — `submittedAt: Instant` (Kotlin) / `submittedAt: string` (TS), `FeedbackRating` 5개 값 전체 task 에서 일관, 컨트롤러 경로 `/v1/feedback` / `/feedbacks` / `/feedbacks/{id}/check` 일관.
