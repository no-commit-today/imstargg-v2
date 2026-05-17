# User Feedback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Allow public users to submit feedback (bug reports, feature requests, etc.) from the imstargg-frontend, let admins review/check submissions in imstargg-admin-frontend, and send a daily Discord digest of the last 24h via core-batch.

**Architecture:** Single `feedback` table under a new `operation` package (umbrella for future operational features). Public POST endpoint in `core-api` hashes the client IP (SHA-256 + salt) before persistence. Admin list/check endpoints in `core-admin`. A cron-scheduled tasklet in `core-batch` aggregates 24h counts and posts to Discord via the existing `AlertManager`.

**Tech Stack:** Kotlin 21, Spring Boot, JPA (PostgreSQL) + QueryDSL, Flyway, Spring Batch + `@Scheduled`, Next.js (App Router) + react-hook-form + zod + shadcn/ui + sonner.

**Spec reference:** `docs/superpowers/specs/2026-05-17-user-feedback-design.md`

**Conventions reminder (from imstargg-backend/CLAUDE.md):**
- File-per-class
- BaseEntity → use `BaseTimeEntity` (createdAt/updatedAt only; no `deleted` for this feature)
- No `@OneToMany`; domain ID references only
- Application Layer Repository wrapper in `core-api`
- API path convention: no `/api` prefix; singular path for single-resource POST (`/v1/feedback`)
- Test bases: `CoreApiContextTest`, `CoreAdminContextTest`, `CoreBatchContextTest`, `DbCoreContextTest`
- AssertJ; `sut` naming; `// given`/`// when`/`// then`; Korean nested class/method names
- Migration: add `V260517__add_feedback_table.sql` + update `schema.sql`

---

## File Structure (full plan)

```
imstargg-backend/

core/core-enum/src/main/kotlin/com/imstargg/core/enums/operation/
  FeedbackCategory.kt                                  (new)

infrastructure/db-core/src/main/resources/db/migration/
  V260517__add_feedback_table.sql                      (new)
infrastructure/db-core/src/main/resources/
  schema.sql                                           (modify – append feedback DDL)
infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/operation/
  FeedbackEntity.kt                                    (new)
  FeedbackJpaRepository.kt                             (new)

core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/operation/
  FeedbackController.kt                                (new)
  FeedbackCreateRequest.kt                             (new)
core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/response/operation/
  FeedbackCreateResponse.kt                            (new)
core/core-api/src/main/kotlin/com/imstargg/core/api/support/
  ClientIpResolver.kt                                  (new)
core/core-api/src/main/kotlin/com/imstargg/core/application/operation/
  FeedbackService.kt                                   (new)
  FeedbackRepository.kt                                (new — application layer wrapper)
  IpHasher.kt                                          (new)
  IpHasherProperties.kt                                (new)
core/core-api/src/main/kotlin/com/imstargg/core/domain/operation/
  Feedback.kt                                          (new — domain model)
  FeedbackCreate.kt                                    (new — domain create command)
core/core-api/src/main/resources/application.yml      (modify – add ip-salt property)

core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/operation/
  AdminFeedbackController.kt                           (new)
core/core-admin/src/main/kotlin/com/imstargg/core/admin/request/operation/
  FeedbackSearchRequest.kt                             (new)
  FeedbackCheckRequest.kt                              (new)
core/core-admin/src/main/kotlin/com/imstargg/core/admin/response/operation/
  FeedbackListResponse.kt                              (new)
  FeedbackResponse.kt                                  (new)
core/core-admin/src/main/kotlin/com/imstargg/core/admin/application/operation/
  AdminFeedbackService.kt                              (new)
  AdminFeedbackRepository.kt                           (new — QueryDSL paging)

core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/operation/
  FeedbackDailyDigestJobConfig.kt                      (new)
  FeedbackDailyDigestTasklet.kt                        (new)
  FeedbackDailyDigestScheduler.kt                      (new — @Scheduled cron)
  FeedbackDigestMessage.kt                             (new — pure message builder)
core/core-batch/src/main/resources/application.yml    (modify – add schedule.feedback-digest property)

imstargg-frontend/

src/api/operation/
  feedback.ts                                          (new)
src/components/feedback/
  FeedbackDialog.tsx                                   (new)
  FeedbackForm.tsx                                     (new)
  feedback-category.ts                                 (new)
src/hooks/
  use-feedback-dialog.ts                               (new)
src/stores/
  feedback-dialog-store.ts                             (new — zustand)
src/components/layout/
  TopNav.tsx                                           (modify – add feedback trigger)
  site-footer.tsx                                      (modify – add feedback trigger)
src/components/layout/
  FeedbackDialogMount.tsx                              (new — root-mounted dialog)
app/layout.tsx                                         (modify – mount FeedbackDialogMount once)

imstargg-admin-frontend/

lib/api/operation/
  feedback.ts                                          (new)
lib/types.ts                                           (modify – add Feedback types)
app/feedbacks/
  page.tsx                                             (new)
app/feedbacks/_components/
  FeedbackTable.tsx                                    (new)
  FeedbackFilters.tsx                                  (new)
  FeedbackCheckToggle.tsx                              (new)
  FeedbackContentCell.tsx                              (new)
components/admin-sidebar.tsx                           (modify – add /feedbacks nav link, IF such a sidebar exists)
```

---

## Task 1: FeedbackCategory enum

**Files:**
- Create: `imstargg-backend/core/core-enum/src/main/kotlin/com/imstargg/core/enums/operation/FeedbackCategory.kt`

- [ ] **Step 1: Create enum**

```kotlin
package com.imstargg.core.enums.operation

enum class FeedbackCategory {
    BUG,
    FEATURE_REQUEST,
    ETC,
    ;
}
```

- [ ] **Step 2: Commit**

```bash
git add imstargg-backend/core/core-enum/src/main/kotlin/com/imstargg/core/enums/operation/FeedbackCategory.kt
git commit -m "feat(core-enum): FeedbackCategory enum 추가"
```

---

## Task 2: DB migration + schema.sql

**Files:**
- Create: `imstargg-backend/infrastructure/db-core/src/main/resources/db/migration/V260517__add_feedback_table.sql`
- Modify: `imstargg-backend/infrastructure/db-core/src/main/resources/schema.sql` (append)

- [ ] **Step 1: Add migration file**

```sql
-- V260517__add_feedback_table.sql
create table if not exists feedback (
    id         bigserial   primary key,
    category   varchar(32) not null,
    content    text        not null,
    email      varchar(254),
    ip_hash    varchar(64) not null,
    checked    boolean     not null default false,
    created_at timestamp   not null,
    updated_at timestamp   not null
);

create index if not exists idx_feedback_created_at on feedback (created_at desc);
create index if not exists idx_feedback_checked    on feedback (checked);
create index if not exists idx_feedback_ip_hash    on feedback (ip_hash);
```

- [ ] **Step 2: Append the same DDL to `schema.sql`**

Open `imstargg-backend/infrastructure/db-core/src/main/resources/schema.sql` and append the identical DDL block from Step 1 at the end of the file.

- [ ] **Step 3: Commit**

```bash
git add imstargg-backend/infrastructure/db-core/src/main/resources/db/migration/V260517__add_feedback_table.sql \
        imstargg-backend/infrastructure/db-core/src/main/resources/schema.sql
git commit -m "feat(db-core): feedback 테이블 추가 (V260517)"
```

---

## Task 3: FeedbackEntity + JpaRepository

**Files:**
- Create: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/operation/FeedbackEntity.kt`
- Create: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/operation/FeedbackJpaRepository.kt`
- Create: `imstargg-backend/infrastructure/db-core/src/test/kotlin/com/imstargg/infrastructure/db/core/operation/FeedbackEntityTest.kt`

- [ ] **Step 1: Write failing entity behavior test**

```kotlin
package com.imstargg.infrastructure.db.core.operation

import com.imstargg.core.enums.operation.FeedbackCategory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FeedbackEntityTest {

    @Test
    fun `check()를 호출하면 checked가 true가 된다`() {
        // given
        val sut = FeedbackEntity(
            category = FeedbackCategory.BUG,
            content = "버그 발생",
            email = null,
            ipHash = "abc",
        )

        // when
        sut.check()

        // then
        assertThat(sut.checked).isTrue()
    }

    @Test
    fun `uncheck()를 호출하면 checked가 false가 된다`() {
        // given
        val sut = FeedbackEntity(
            category = FeedbackCategory.BUG,
            content = "버그 발생",
            email = null,
            ipHash = "abc",
        )
        sut.check()

        // when
        sut.uncheck()

        // then
        assertThat(sut.checked).isFalse()
    }
}
```

- [ ] **Step 2: Run test — expect FAIL (entity does not exist)**

```bash
./gradlew :infrastructure:db-core:unitTest --tests "com.imstargg.infrastructure.db.core.operation.FeedbackEntityTest"
```

Expected: compile failure / class not found.

- [ ] **Step 3: Create entity**

```kotlin
package com.imstargg.infrastructure.db.core.operation

import com.imstargg.core.enums.operation.FeedbackCategory
import com.imstargg.infrastructure.db.core.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "feedback")
class FeedbackEntity(
    @Enumerated(EnumType.STRING)
    @Column(name = "category", updatable = false, nullable = false, length = 32)
    val category: FeedbackCategory,

    @Column(name = "content", updatable = false, nullable = false, columnDefinition = "text")
    val content: String,

    @Column(name = "email", updatable = false, length = 254)
    val email: String?,

    @Column(name = "ip_hash", updatable = false, nullable = false, length = 64)
    val ipHash: String,
) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null

    @Column(name = "checked", nullable = false)
    var checked: Boolean = false
        protected set

    fun check() {
        this.checked = true
    }

    fun uncheck() {
        this.checked = false
    }
}
```

- [ ] **Step 4: Run test — expect PASS**

```bash
./gradlew :infrastructure:db-core:unitTest --tests "com.imstargg.infrastructure.db.core.operation.FeedbackEntityTest"
```

- [ ] **Step 5: Create JpaRepository**

```kotlin
package com.imstargg.infrastructure.db.core.operation

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface FeedbackJpaRepository : JpaRepository<FeedbackEntity, Long> {

    fun findAllByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
        from: Instant,
        to: Instant,
    ): List<FeedbackEntity>
}
```

- [ ] **Step 6: Build to confirm wiring**

```bash
./gradlew :infrastructure:db-core:build -x test
```

- [ ] **Step 7: Commit**

```bash
git add imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/operation \
        imstargg-backend/infrastructure/db-core/src/test/kotlin/com/imstargg/infrastructure/db/core/operation
git commit -m "feat(db-core): FeedbackEntity 와 JpaRepository 추가"
```

---

## Task 4: IpHasher (core-api)

**Files:**
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/operation/IpHasherProperties.kt`
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/operation/IpHasher.kt`
- Create: `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/application/operation/IpHasherTest.kt`
- Modify: `imstargg-backend/core/core-api/src/main/resources/application.yml` (add `app.operation.feedback.ip-salt`)
- Modify: any `@ConfigurationPropertiesScan` location if needed (verify before adding `@EnableConfigurationProperties`).

- [ ] **Step 1: Write failing unit test**

```kotlin
package com.imstargg.core.application.operation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class IpHasherTest {

    private val sut = IpHasher(IpHasherProperties(salt = "test-salt"))

    @Test
    fun `같은 IP는 같은 해시를 만든다`() {
        // when
        val first = sut.hash("1.2.3.4")
        val second = sut.hash("1.2.3.4")

        // then
        assertThat(first).isEqualTo(second)
    }

    @Test
    fun `다른 IP는 다른 해시를 만든다`() {
        // when
        val a = sut.hash("1.2.3.4")
        val b = sut.hash("1.2.3.5")

        // then
        assertThat(a).isNotEqualTo(b)
    }

    @Test
    fun `해시는 64자리 hex 문자열이다`() {
        // when
        val hash = sut.hash("1.2.3.4")

        // then
        assertThat(hash).hasSize(64)
        assertThat(hash).matches("^[0-9a-f]{64}$")
    }

    @Test
    fun `salt가 다르면 해시가 달라진다`() {
        // given
        val other = IpHasher(IpHasherProperties(salt = "different-salt"))

        // when
        val a = sut.hash("1.2.3.4")
        val b = other.hash("1.2.3.4")

        // then
        assertThat(a).isNotEqualTo(b)
    }
}
```

- [ ] **Step 2: Run — expect FAIL (class missing)**

```bash
./gradlew :core:core-api:unitTest --tests "com.imstargg.core.application.operation.IpHasherTest"
```

- [ ] **Step 3: Implement IpHasherProperties**

```kotlin
package com.imstargg.core.application.operation

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.operation.feedback")
data class IpHasherProperties(
    val ipSalt: String,
) {
    val salt: String get() = ipSalt
}
```

(Use `ipSalt` as the binding name; the `salt` alias avoids a verbose `ipSalt` callsite. If your project prefers a single name, drop the alias and use `ipSalt` everywhere.)

- [ ] **Step 4: Implement IpHasher**

```kotlin
package com.imstargg.core.application.operation

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component
import java.security.MessageDigest

@Component
@EnableConfigurationProperties(IpHasherProperties::class)
class IpHasher(
    private val properties: IpHasherProperties,
) {
    fun hash(ip: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((properties.salt + ip).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
```

- [ ] **Step 5: Add property to application.yml**

Append to `imstargg-backend/core/core-api/src/main/resources/application.yml`:

```yaml
app:
  operation:
    feedback:
      ip-salt: ${APP_OPERATION_FEEDBACK_IP_SALT:local-dev-salt-change-me}
```

(Adapt indentation to merge with any existing `app:` root key; do not duplicate the root.)

- [ ] **Step 6: Run — expect PASS**

```bash
./gradlew :core:core-api:unitTest --tests "com.imstargg.core.application.operation.IpHasherTest"
```

- [ ] **Step 7: Commit**

```bash
git add imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/operation \
        imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/application/operation \
        imstargg-backend/core/core-api/src/main/resources/application.yml
git commit -m "feat(core-api): IpHasher + properties 추가"
```

---

## Task 5: ClientIpResolver (core-api)

**Files:**
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/support/ClientIpResolver.kt`
- Create: `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/api/support/ClientIpResolverTest.kt`

- [ ] **Step 1: Write failing test**

```kotlin
package com.imstargg.core.api.support

import jakarta.servlet.http.HttpServletRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ClientIpResolverTest {

    private val sut = ClientIpResolver()

    @Test
    fun `X-Forwarded-For 헤더가 있으면 첫 토큰을 반환한다`() {
        // given
        val request = mock<HttpServletRequest> {
            on { getHeader("X-Forwarded-For") } doReturn "1.2.3.4, 10.0.0.1"
        }

        // when
        val ip = sut.resolve(request)

        // then
        assertThat(ip).isEqualTo("1.2.3.4")
    }

    @Test
    fun `X-Forwarded-For가 없으면 RemoteAddr를 반환한다`() {
        // given
        val request = mock<HttpServletRequest> {
            on { getHeader("X-Forwarded-For") } doReturn null
            on { remoteAddr } doReturn "9.9.9.9"
        }

        // when
        val ip = sut.resolve(request)

        // then
        assertThat(ip).isEqualTo("9.9.9.9")
    }
}
```

(Add Mockito-Kotlin imports `import org.mockito.kotlin.doReturn` if your codebase uses that style; if it uses `whenever(...).thenReturn(...)`, convert accordingly.)

- [ ] **Step 2: Run — expect FAIL**

```bash
./gradlew :core:core-api:unitTest --tests "com.imstargg.core.api.support.ClientIpResolverTest"
```

- [ ] **Step 3: Implement**

```kotlin
package com.imstargg.core.api.support

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

@Component
class ClientIpResolver {

    fun resolve(request: HttpServletRequest): String {
        val forwarded = request.getHeader(HEADER_FORWARDED_FOR)
        if (!forwarded.isNullOrBlank()) {
            return forwarded.substringBefore(',').trim()
        }
        return request.remoteAddr
    }

    companion object {
        private const val HEADER_FORWARDED_FOR = "X-Forwarded-For"
    }
}
```

- [ ] **Step 4: Run — expect PASS**

```bash
./gradlew :core:core-api:unitTest --tests "com.imstargg.core.api.support.ClientIpResolverTest"
```

- [ ] **Step 5: Commit**

```bash
git add imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/support/ClientIpResolver.kt \
        imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/api/support/ClientIpResolverTest.kt
git commit -m "feat(core-api): ClientIpResolver 추가"
```

---

## Task 6: Feedback domain + FeedbackCreate command + FeedbackRepository (application wrapper)

**Files:**
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/operation/FeedbackCreate.kt`
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/operation/Feedback.kt`
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/operation/FeedbackRepository.kt`

- [ ] **Step 1: Create FeedbackCreate command**

```kotlin
package com.imstargg.core.domain.operation

import com.imstargg.core.enums.operation.FeedbackCategory

data class FeedbackCreate(
    val category: FeedbackCategory,
    val content: String,
    val email: String?,
    val ipHash: String,
)
```

- [ ] **Step 2: Create Feedback domain model**

```kotlin
package com.imstargg.core.domain.operation

import com.imstargg.core.enums.operation.FeedbackCategory

data class Feedback(
    val id: Long,
    val category: FeedbackCategory,
    val content: String,
    val email: String?,
    val ipHash: String,
    val checked: Boolean,
)
```

- [ ] **Step 3: Create FeedbackRepository (application-layer wrapper)**

```kotlin
package com.imstargg.core.application.operation

import com.imstargg.core.domain.operation.Feedback
import com.imstargg.core.domain.operation.FeedbackCreate
import com.imstargg.infrastructure.db.core.operation.FeedbackEntity
import com.imstargg.infrastructure.db.core.operation.FeedbackJpaRepository
import org.springframework.stereotype.Repository

@Repository
class FeedbackRepository(
    private val jpaRepository: FeedbackJpaRepository,
) {
    fun save(command: FeedbackCreate): Feedback {
        val entity = FeedbackEntity(
            category = command.category,
            content = command.content,
            email = command.email,
            ipHash = command.ipHash,
        )
        val saved = jpaRepository.save(entity)
        return saved.toDomain()
    }

    private fun FeedbackEntity.toDomain(): Feedback = Feedback(
        id = requireNotNull(this.id) { "Feedback id must not be null after save" },
        category = this.category,
        content = this.content,
        email = this.email,
        ipHash = this.ipHash,
        checked = this.checked,
    )
}
```

- [ ] **Step 4: Build**

```bash
./gradlew :core:core-api:compileKotlin
```

- [ ] **Step 5: Commit**

```bash
git add imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/domain/operation \
        imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/operation/FeedbackRepository.kt
git commit -m "feat(core-api): Feedback 도메인 + Repository 래퍼 추가"
```

---

## Task 7: FeedbackService (core-api)

**Files:**
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/operation/FeedbackService.kt`
- Create: `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/application/operation/FeedbackServiceTest.kt`

- [ ] **Step 1: Write failing service test**

```kotlin
package com.imstargg.core.application.operation

import com.imstargg.core.domain.operation.FeedbackCreate
import com.imstargg.core.enums.operation.FeedbackCategory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class FeedbackServiceTest {

    private val ipHasher = IpHasher(IpHasherProperties(ipSalt = "salt"))
    private val repository = mock<FeedbackRepository>()
    private val sut = FeedbackService(ipHasher, repository)

    @Test
    fun `IP를 해시한 뒤 저장한다`() {
        // given
        val expectedHash = ipHasher.hash("1.2.3.4")
        whenever(repository.save(any())).thenAnswer { invocation ->
            val cmd = invocation.arguments[0] as FeedbackCreate
            com.imstargg.core.domain.operation.Feedback(
                id = 1L,
                category = cmd.category,
                content = cmd.content,
                email = cmd.email,
                ipHash = cmd.ipHash,
                checked = false,
            )
        }

        // when
        val result = sut.submit(
            category = FeedbackCategory.BUG,
            content = "버그",
            email = "a@b.com",
            ip = "1.2.3.4",
        )

        // then
        assertThat(result.id).isEqualTo(1L)
        assertThat(result.ipHash).isEqualTo(expectedHash)
        verify(repository).save(
            FeedbackCreate(
                category = FeedbackCategory.BUG,
                content = "버그",
                email = "a@b.com",
                ipHash = expectedHash,
            ),
        )
    }
}
```

- [ ] **Step 2: Run — expect FAIL**

```bash
./gradlew :core:core-api:unitTest --tests "com.imstargg.core.application.operation.FeedbackServiceTest"
```

- [ ] **Step 3: Implement service**

```kotlin
package com.imstargg.core.application.operation

import com.imstargg.core.domain.operation.Feedback
import com.imstargg.core.domain.operation.FeedbackCreate
import com.imstargg.core.enums.operation.FeedbackCategory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FeedbackService(
    private val ipHasher: IpHasher,
    private val repository: FeedbackRepository,
) {
    @Transactional
    fun submit(
        category: FeedbackCategory,
        content: String,
        email: String?,
        ip: String,
    ): Feedback {
        val command = FeedbackCreate(
            category = category,
            content = content.trim(),
            email = email?.takeIf { it.isNotBlank() },
            ipHash = ipHasher.hash(ip),
        )
        return repository.save(command)
    }
}
```

- [ ] **Step 4: Run — expect PASS**

```bash
./gradlew :core:core-api:unitTest --tests "com.imstargg.core.application.operation.FeedbackServiceTest"
```

- [ ] **Step 5: Commit**

```bash
git add imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/operation/FeedbackService.kt \
        imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/application/operation/FeedbackServiceTest.kt
git commit -m "feat(core-api): FeedbackService 추가"
```

---

## Task 8: FeedbackController + DTOs + context test

**Files:**
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/operation/FeedbackCreateRequest.kt`
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/response/operation/FeedbackCreateResponse.kt`
- Create: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/operation/FeedbackController.kt`
- Create: `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/api/controller/v1/operation/FeedbackControllerTest.kt`

- [ ] **Step 1: Request DTO with Bean Validation**

```kotlin
package com.imstargg.core.api.controller.v1.operation

import com.imstargg.core.enums.operation.FeedbackCategory
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class FeedbackCreateRequest(
    @field:NotNull
    val category: FeedbackCategory?,

    @field:NotNull
    @field:Size(min = 1, max = 2000)
    val content: String?,

    @field:Email
    @field:Size(max = 254)
    val email: String?,
)
```

- [ ] **Step 2: Response DTO**

```kotlin
package com.imstargg.core.api.controller.v1.response.operation

import com.imstargg.core.domain.operation.Feedback

data class FeedbackCreateResponse(
    val id: Long,
) {
    companion object {
        fun from(feedback: Feedback): FeedbackCreateResponse =
            FeedbackCreateResponse(id = feedback.id)
    }
}
```

- [ ] **Step 3: Controller**

```kotlin
package com.imstargg.core.api.controller.v1.operation

import com.imstargg.core.api.controller.v1.response.operation.FeedbackCreateResponse
import com.imstargg.core.api.support.ClientIpResolver
import com.imstargg.core.application.operation.FeedbackService
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class FeedbackController(
    private val feedbackService: FeedbackService,
    private val clientIpResolver: ClientIpResolver,
) {
    @PostMapping("/v1/feedback")
    fun create(
        @Valid @RequestBody request: FeedbackCreateRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<FeedbackCreateResponse> {
        val ip = clientIpResolver.resolve(httpRequest)
        val feedback = feedbackService.submit(
            category = requireNotNull(request.category),
            content = requireNotNull(request.content),
            email = request.email,
            ip = ip,
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(FeedbackCreateResponse.from(feedback))
    }
}
```

- [ ] **Step 4: Context test for controller (find an existing controller context test in core-api to copy the test base — use the same base class, e.g. `CoreApiContextTest`)**

```kotlin
package com.imstargg.core.api.controller.v1.operation

import com.fasterxml.jackson.databind.ObjectMapper
import com.imstargg.CoreApiContextTest
import com.imstargg.core.enums.operation.FeedbackCategory
import com.imstargg.infrastructure.db.core.operation.FeedbackJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

class FeedbackControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val feedbackJpaRepository: FeedbackJpaRepository,
) : CoreApiContextTest() {

    @AfterEach
    fun tearDown() {
        feedbackJpaRepository.deleteAll()
    }

    @Test
    fun `정상 제출시 201과 id를 반환한다`() {
        // given
        val body = mapOf(
            "category" to "BUG",
            "content" to "버그가 발생했어요",
            "email" to "user@example.com",
        )

        // when & then
        mockMvc.post("/v1/feedback") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(body)
            header("X-Forwarded-For", "1.2.3.4")
        }.andExpect {
            status { isCreated() }
            jsonPath("$.id") { exists() }
        }

        val saved = feedbackJpaRepository.findAll()
        assertThat(saved).hasSize(1)
        assertThat(saved[0].category).isEqualTo(FeedbackCategory.BUG)
        assertThat(saved[0].content).isEqualTo("버그가 발생했어요")
        assertThat(saved[0].email).isEqualTo("user@example.com")
        assertThat(saved[0].ipHash).hasSize(64)
    }

    @Test
    fun `content가 비어있으면 400을 반환한다`() {
        // given
        val body = mapOf(
            "category" to "BUG",
            "content" to "",
        )

        // when & then
        mockMvc.post("/v1/feedback") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(body)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `잘못된 이메일 형식이면 400을 반환한다`() {
        // given
        val body = mapOf(
            "category" to "BUG",
            "content" to "내용",
            "email" to "not-an-email",
        )

        // when & then
        mockMvc.post("/v1/feedback") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(body)
        }.andExpect {
            status { isBadRequest() }
        }
    }
}
```

If the existing `CoreApiContextTest` does not inject `MockMvc`, mirror the pattern used in another controller test in the same module (e.g. `RenewalControllerTest` — look it up and copy the imports/setup).

- [ ] **Step 5: Run context test — expect PASS**

```bash
./gradlew :core:core-api:contextTest --tests "com.imstargg.core.api.controller.v1.operation.FeedbackControllerTest"
```

- [ ] **Step 6: Run lint**

```bash
./gradlew :core:core-api:ktlintCheck
```

- [ ] **Step 7: Commit**

```bash
git add imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/operation \
        imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/response/operation \
        imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/api/controller/v1/operation
git commit -m "feat(core-api): POST /v1/feedback 엔드포인트 추가"
```

---

## Task 9: AdminFeedbackRepository (QueryDSL paging)

**Files:**
- Create: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/application/operation/AdminFeedbackRepository.kt`

- [ ] **Step 1: Implement QueryDSL repository**

```kotlin
package com.imstargg.core.admin.application.operation

import com.imstargg.core.enums.operation.FeedbackCategory
import com.imstargg.infrastructure.db.core.operation.FeedbackEntity
import com.imstargg.infrastructure.db.core.operation.QFeedbackEntity.feedbackEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class AdminFeedbackRepository(
    private val queryFactory: JPAQueryFactory,
) {
    fun search(
        category: FeedbackCategory?,
        checked: Boolean?,
        pageable: Pageable,
    ): Page<FeedbackEntity> {
        val predicates = listOfNotNull(
            category?.let { feedbackEntity.category.eq(it) },
            checked?.let { feedbackEntity.checked.eq(it) },
        ).toTypedArray()

        val content = queryFactory
            .selectFrom(feedbackEntity)
            .where(*predicates)
            .orderBy(feedbackEntity.createdAt.desc())
            .offset(pageable.offset)
            .limit(pageable.pageSize.toLong())
            .fetch()

        val total = queryFactory
            .select(feedbackEntity.count())
            .from(feedbackEntity)
            .where(*predicates)
            .fetchOne() ?: 0L

        return PageImpl(content, pageable, total)
    }

    fun findById(id: Long): FeedbackEntity? =
        queryFactory.selectFrom(feedbackEntity)
            .where(feedbackEntity.id.eq(id))
            .fetchOne()
}
```

- [ ] **Step 2: Build (will trigger KSP generating QFeedbackEntity)**

```bash
./gradlew :core:core-admin:compileKotlin
```

If `QFeedbackEntity` is not generated, ensure `infrastructure/db-core` is on classpath for KSP — `core-admin` already depends on `db-core` and uses KSP per project config.

- [ ] **Step 3: Commit**

```bash
git add imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/application/operation/AdminFeedbackRepository.kt
git commit -m "feat(core-admin): AdminFeedbackRepository (QueryDSL) 추가"
```

---

## Task 10: AdminFeedbackService

**Files:**
- Create: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/application/operation/AdminFeedbackService.kt`

- [ ] **Step 1: Implement service**

```kotlin
package com.imstargg.core.admin.application.operation

import com.imstargg.core.enums.operation.FeedbackCategory
import com.imstargg.infrastructure.db.core.operation.FeedbackEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminFeedbackService(
    private val repository: AdminFeedbackRepository,
) {
    @Transactional(readOnly = true)
    fun search(
        category: FeedbackCategory?,
        checked: Boolean?,
        page: Int,
        size: Int,
    ): Page<FeedbackEntity> =
        repository.search(category, checked, PageRequest.of(page, size))

    @Transactional
    fun updateCheck(id: Long, checked: Boolean) {
        val entity = repository.findById(id)
            ?: throw NoSuchElementException("Feedback not found: id=$id")
        if (checked) entity.check() else entity.uncheck()
    }
}
```

- [ ] **Step 2: Build**

```bash
./gradlew :core:core-admin:compileKotlin
```

- [ ] **Step 3: Commit**

```bash
git add imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/application/operation/AdminFeedbackService.kt
git commit -m "feat(core-admin): AdminFeedbackService 추가"
```

---

## Task 11: AdminFeedbackController + DTOs + context test

**Files:**
- Create: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/request/operation/FeedbackSearchRequest.kt`
- Create: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/request/operation/FeedbackCheckRequest.kt`
- Create: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/response/operation/FeedbackResponse.kt`
- Create: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/response/operation/FeedbackListResponse.kt`
- Create: `imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/operation/AdminFeedbackController.kt`
- Create: `imstargg-backend/core/core-admin/src/test/kotlin/com/imstargg/core/admin/controller/operation/AdminFeedbackControllerTest.kt`

- [ ] **Step 1: Search request**

```kotlin
package com.imstargg.core.admin.request.operation

import com.imstargg.core.enums.operation.FeedbackCategory

data class FeedbackSearchRequest(
    val category: FeedbackCategory? = null,
    val checked: Boolean? = null,
    val page: Int = 0,
    val size: Int = 20,
) {
    fun normalizedSize(): Int = size.coerceIn(1, 100)
}
```

- [ ] **Step 2: Check request**

```kotlin
package com.imstargg.core.admin.request.operation

data class FeedbackCheckRequest(
    val checked: Boolean,
)
```

- [ ] **Step 3: Item response**

```kotlin
package com.imstargg.core.admin.response.operation

import com.imstargg.core.enums.operation.FeedbackCategory
import com.imstargg.infrastructure.db.core.operation.FeedbackEntity
import java.time.Instant

data class FeedbackResponse(
    val id: Long,
    val category: FeedbackCategory,
    val content: String,
    val email: String?,
    val ipHash: String,
    val checked: Boolean,
    val createdAt: Instant,
) {
    companion object {
        fun from(entity: FeedbackEntity): FeedbackResponse = FeedbackResponse(
            id = requireNotNull(entity.id),
            category = entity.category,
            content = entity.content,
            email = entity.email,
            ipHash = entity.ipHash,
            checked = entity.checked,
            createdAt = requireNotNull(entity.createdAt),
        )
    }
}
```

- [ ] **Step 4: List response**

```kotlin
package com.imstargg.core.admin.response.operation

import com.imstargg.infrastructure.db.core.operation.FeedbackEntity
import org.springframework.data.domain.Page

data class FeedbackListResponse(
    val content: List<FeedbackResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        fun from(page: Page<FeedbackEntity>): FeedbackListResponse = FeedbackListResponse(
            content = page.content.map(FeedbackResponse::from),
            page = page.number,
            size = page.size,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
        )
    }
}
```

- [ ] **Step 5: Controller**

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
        val page = service.search(
            category = request.category,
            checked = request.checked,
            page = request.page,
            size = request.normalizedSize(),
        )
        return ResponseEntity.ok(FeedbackListResponse.from(page))
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

- [ ] **Step 6: Context test**

```kotlin
package com.imstargg.core.admin.controller.operation

import com.fasterxml.jackson.databind.ObjectMapper
import com.imstargg.CoreAdminContextTest
import com.imstargg.core.enums.operation.FeedbackCategory
import com.imstargg.infrastructure.db.core.operation.FeedbackEntity
import com.imstargg.infrastructure.db.core.operation.FeedbackJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put

class AdminFeedbackControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
    private val feedbackJpaRepository: FeedbackJpaRepository,
) : CoreAdminContextTest() {

    @AfterEach
    fun tearDown() {
        feedbackJpaRepository.deleteAll()
    }

    @Test
    fun `목록을 카테고리로 필터링할 수 있다`() {
        // given
        feedbackJpaRepository.save(FeedbackEntity(FeedbackCategory.BUG, "버그", null, "h1"))
        feedbackJpaRepository.save(FeedbackEntity(FeedbackCategory.FEATURE_REQUEST, "기능", null, "h2"))

        // when & then
        mockMvc.get("/feedbacks?category=BUG").andExpect {
            status { isOk() }
            jsonPath("$.content.length()") { value(1) }
            jsonPath("$.content[0].category") { value("BUG") }
        }
    }

    @Test
    fun `PUT _feedbacks_{id}_check가 checked를 true로 만든다`() {
        // given
        val saved = feedbackJpaRepository.save(FeedbackEntity(FeedbackCategory.BUG, "버그", null, "h1"))

        // when
        mockMvc.put("/feedbacks/${saved.id}/check") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("checked" to true))
        }.andExpect {
            status { isNoContent() }
        }

        // then
        val reloaded = feedbackJpaRepository.findById(saved.id!!).orElseThrow()
        assertThat(reloaded.checked).isTrue()
    }

    @Test
    fun `존재하지 않는 id에 PUT하면 404를 반환한다`() {
        // when & then
        mockMvc.put("/feedbacks/999999/check") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("checked" to true))
        }.andExpect {
            status { isNotFound() }
        }
    }
}
```

If `CoreAdminContextTest` is not exactly named or located here, find the actual admin context test base in `core-admin` test sources and use that. If the global exception handler does not map `NoSuchElementException` to 404, add a handler or change the thrown type to one that already maps to 404 in `core-admin` error handling — verify by reading existing controllers' error mappings first.

- [ ] **Step 7: Run context test**

```bash
./gradlew :core:core-admin:contextTest --tests "com.imstargg.core.admin.controller.operation.AdminFeedbackControllerTest"
```

- [ ] **Step 8: Lint**

```bash
./gradlew :core:core-admin:ktlintCheck
```

- [ ] **Step 9: Commit**

```bash
git add imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/request/operation \
        imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/response/operation \
        imstargg-backend/core/core-admin/src/main/kotlin/com/imstargg/core/admin/controller/operation \
        imstargg-backend/core/core-admin/src/test/kotlin/com/imstargg/core/admin/controller/operation
git commit -m "feat(core-admin): /feedbacks 목록/체크 엔드포인트 추가"
```

---

## Task 12: FeedbackDigestMessage builder

**Files:**
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/operation/FeedbackDigestMessage.kt`
- Create: `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/operation/FeedbackDigestMessageTest.kt`

This is a pure function so the Tasklet stays thin. The builder takes counts and returns a `Pair<title, content>` (or null if 0 total).

- [ ] **Step 1: Write failing tests**

```kotlin
package com.imstargg.core.batch.job.operation

import com.imstargg.core.enums.operation.FeedbackCategory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FeedbackDigestMessageTest {

    @Test
    fun `0건이면 null을 반환한다`() {
        // when
        val result = FeedbackDigestMessage.build(emptyMap(), emailCount = 0)

        // then
        assertThat(result).isNull()
    }

    @Test
    fun `카테고리별 건수와 이메일 응답 요청 라인을 포함한다`() {
        // given
        val counts = mapOf(
            FeedbackCategory.BUG to 2L,
            FeedbackCategory.FEATURE_REQUEST to 1L,
            FeedbackCategory.ETC to 2L,
        )

        // when
        val result = FeedbackDigestMessage.build(counts, emailCount = 1)!!

        // then
        assertThat(result.title).isEqualTo("📨 지난 24시간 피드백 5건")
        assertThat(result.content).contains("• BUG 2건")
        assertThat(result.content).contains("• FEATURE_REQUEST 1건")
        assertThat(result.content).contains("• ETC 2건")
        assertThat(result.content).contains("✉️ 이메일 응답 요청 1건")
    }

    @Test
    fun `0인 카테고리 라인은 생략한다`() {
        // given
        val counts = mapOf(
            FeedbackCategory.BUG to 0L,
            FeedbackCategory.FEATURE_REQUEST to 3L,
            FeedbackCategory.ETC to 0L,
        )

        // when
        val result = FeedbackDigestMessage.build(counts, emailCount = 0)!!

        // then
        assertThat(result.title).isEqualTo("📨 지난 24시간 피드백 3건")
        assertThat(result.content).doesNotContain("BUG")
        assertThat(result.content).doesNotContain("ETC")
        assertThat(result.content).doesNotContain("이메일")
        assertThat(result.content).contains("• FEATURE_REQUEST 3건")
    }
}
```

- [ ] **Step 2: Run — expect FAIL**

```bash
./gradlew :core:core-batch:unitTest --tests "com.imstargg.core.batch.job.operation.FeedbackDigestMessageTest"
```

- [ ] **Step 3: Implement**

```kotlin
package com.imstargg.core.batch.job.operation

import com.imstargg.core.enums.operation.FeedbackCategory

data class FeedbackDigest(
    val title: String,
    val content: String,
)

object FeedbackDigestMessage {

    fun build(
        countByCategory: Map<FeedbackCategory, Long>,
        emailCount: Long,
    ): FeedbackDigest? {
        val total = countByCategory.values.sum()
        if (total == 0L) return null

        val lines = mutableListOf<String>()
        FeedbackCategory.entries.forEach { category ->
            val count = countByCategory[category] ?: 0L
            if (count > 0L) {
                lines += "• ${category.name} ${count}건"
            }
        }
        if (emailCount > 0L) {
            lines += "✉️ 이메일 응답 요청 ${emailCount}건"
        }

        return FeedbackDigest(
            title = "📨 지난 24시간 피드백 ${total}건",
            content = lines.joinToString("\n"),
        )
    }
}
```

(If the project prefers one-class-per-file: split `FeedbackDigest` into its own file `FeedbackDigest.kt` and keep `FeedbackDigestMessage.kt` for the object only. Do split — CLAUDE.md mandates one class per file.)

After splitting:

`FeedbackDigest.kt`
```kotlin
package com.imstargg.core.batch.job.operation

data class FeedbackDigest(
    val title: String,
    val content: String,
)
```

`FeedbackDigestMessage.kt`
```kotlin
package com.imstargg.core.batch.job.operation

import com.imstargg.core.enums.operation.FeedbackCategory

object FeedbackDigestMessage {

    fun build(
        countByCategory: Map<FeedbackCategory, Long>,
        emailCount: Long,
    ): FeedbackDigest? {
        val total = countByCategory.values.sum()
        if (total == 0L) return null

        val lines = mutableListOf<String>()
        FeedbackCategory.entries.forEach { category ->
            val count = countByCategory[category] ?: 0L
            if (count > 0L) {
                lines += "• ${category.name} ${count}건"
            }
        }
        if (emailCount > 0L) {
            lines += "✉️ 이메일 응답 요청 ${emailCount}건"
        }

        return FeedbackDigest(
            title = "📨 지난 24시간 피드백 ${total}건",
            content = lines.joinToString("\n"),
        )
    }
}
```

- [ ] **Step 4: Run — expect PASS**

```bash
./gradlew :core:core-batch:unitTest --tests "com.imstargg.core.batch.job.operation.FeedbackDigestMessageTest"
```

- [ ] **Step 5: Commit**

```bash
git add imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/operation/FeedbackDigest.kt \
        imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/operation/FeedbackDigestMessage.kt \
        imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/operation/FeedbackDigestMessageTest.kt
git commit -m "feat(core-batch): 피드백 일일 다이제스트 메시지 빌더 추가"
```

---

## Task 13: FeedbackDailyDigestTasklet

**Files:**
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/operation/FeedbackDailyDigestTasklet.kt`
- Create: `imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/operation/FeedbackDailyDigestTaskletTest.kt`

- [ ] **Step 1: Write failing unit test (Tasklet logic, no Spring context)**

```kotlin
package com.imstargg.core.batch.job.operation

import com.imstargg.core.batch.support.alert.AlertCommand
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.core.enums.operation.FeedbackCategory
import com.imstargg.infrastructure.db.core.operation.FeedbackEntity
import com.imstargg.infrastructure.db.core.operation.FeedbackJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class FeedbackDailyDigestTaskletTest {

    private val fixedNow = Instant.parse("2026-05-17T12:00:00Z")
    private val clock = Clock.fixed(fixedNow, ZoneId.of("UTC"))

    @Test
    fun `0건이면 알림을 보내지 않는다`() {
        // given
        val repository = mock<FeedbackJpaRepository>()
        whenever(repository.findAllByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any()))
            .thenReturn(emptyList())
        val alertManager = mock<AlertManager>()
        val sut = FeedbackDailyDigestTasklet(repository, alertManager, clock)

        // when
        sut.run()

        // then
        verify(alertManager, never()).alert(any())
    }

    @Test
    fun `24시간 윈도우 카운트와 이메일 카운트를 알림으로 보낸다`() {
        // given
        val repository = mock<FeedbackJpaRepository>()
        whenever(repository.findAllByCreatedAtGreaterThanEqualAndCreatedAtLessThan(any(), any()))
            .thenReturn(
                listOf(
                    feedback(FeedbackCategory.BUG, email = "a@b.com"),
                    feedback(FeedbackCategory.BUG, email = null),
                    feedback(FeedbackCategory.FEATURE_REQUEST, email = null),
                    feedback(FeedbackCategory.ETC, email = null),
                    feedback(FeedbackCategory.ETC, email = null),
                ),
            )
        val alertManager = mock<AlertManager>()
        val sut = FeedbackDailyDigestTasklet(repository, alertManager, clock)

        // when
        sut.run()

        // then
        val captor = argumentCaptor<AlertCommand>()
        verify(alertManager).alert(captor.capture())
        val cmd = captor.firstValue
        assertThat(cmd.title).isEqualTo("📨 지난 24시간 피드백 5건")
        assertThat(cmd.content).contains("• BUG 2건")
        assertThat(cmd.content).contains("• FEATURE_REQUEST 1건")
        assertThat(cmd.content).contains("• ETC 2건")
        assertThat(cmd.content).contains("✉️ 이메일 응답 요청 1건")
    }

    private fun feedback(category: FeedbackCategory, email: String?): FeedbackEntity =
        FeedbackEntity(category = category, content = "x", email = email, ipHash = "h")
}
```

- [ ] **Step 2: Run — expect FAIL**

```bash
./gradlew :core:core-batch:unitTest --tests "com.imstargg.core.batch.job.operation.FeedbackDailyDigestTaskletTest"
```

- [ ] **Step 3: Implement Tasklet**

```kotlin
package com.imstargg.core.batch.job.operation

import com.imstargg.core.batch.support.alert.AlertCommand
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.core.enums.operation.FeedbackCategory
import com.imstargg.infrastructure.db.core.operation.FeedbackJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration

@Component
class FeedbackDailyDigestTasklet(
    private val feedbackJpaRepository: FeedbackJpaRepository,
    private val alertManager: AlertManager,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun run() {
        val now = clock.instant()
        val from = now.minus(Duration.ofHours(24))
        val recent = feedbackJpaRepository
            .findAllByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from, now)

        if (recent.isEmpty()) {
            log.info("피드백 24시간 다이제스트: 0건 — 알림 미발송")
            return
        }

        val counts: Map<FeedbackCategory, Long> = FeedbackCategory.entries.associateWith { cat ->
            recent.count { it.category == cat }.toLong()
        }
        val emailCount = recent.count { !it.email.isNullOrBlank() }.toLong()

        val digest = FeedbackDigestMessage.build(counts, emailCount) ?: return

        alertManager.alert(AlertCommand(title = digest.title, content = digest.content, ex = null))
        log.info("피드백 24시간 다이제스트: 총 {}건 알림 발송", recent.size)
    }
}
```

(If `AlertCommand` constructor signature differs from `(title, content, ex)`, adjust — verify by reading `AlertCommand.kt`.)

- [ ] **Step 4: Run — expect PASS**

```bash
./gradlew :core:core-batch:unitTest --tests "com.imstargg.core.batch.job.operation.FeedbackDailyDigestTaskletTest"
```

- [ ] **Step 5: Commit**

```bash
git add imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/operation/FeedbackDailyDigestTasklet.kt \
        imstargg-backend/core/core-batch/src/test/kotlin/com/imstargg/core/batch/job/operation/FeedbackDailyDigestTaskletTest.kt
git commit -m "feat(core-batch): 피드백 24시간 다이제스트 Tasklet 추가"
```

---

## Task 14: FeedbackDailyDigestJobConfig + Scheduler

**Files:**
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/operation/FeedbackDailyDigestJobConfig.kt`
- Create: `imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/operation/FeedbackDailyDigestScheduler.kt`
- Modify: `imstargg-backend/core/core-batch/src/main/resources/application.yml` (add `app.schedule.feedback-digest.*`)

- [ ] **Step 1: Job config (single Step + Tasklet)**

```kotlin
package com.imstargg.core.batch.job.operation

import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class FeedbackDailyDigestJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val tasklet: FeedbackDailyDigestTasklet,
) {
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
                tasklet.run()
                RepeatStatus.FINISHED
            }, txManager)
            .build()
}
```

- [ ] **Step 2: Scheduler**

```kotlin
package com.imstargg.core.batch.job.operation

import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.OffsetDateTime

@Component
@ConditionalOnProperty(prefix = "app.schedule.feedback-digest", name = ["enabled"], havingValue = "true")
class FeedbackDailyDigestScheduler(
    private val jobLauncher: JobLauncher,
    private val job: org.springframework.batch.core.job.Job,
    private val clock: Clock,
) {

    @Scheduled(cron = "\${app.schedule.feedback-digest.cron}", zone = "Asia/Seoul")
    fun runDailyDigest() {
        val params = JobParametersBuilder()
            .addString("runAt", OffsetDateTime.now(clock).toString())
            .toJobParameters()
        jobLauncher.run(job, params)
    }
}
```

(If the project already wires a single `JobLauncher` and resolves the specific job via name lookup, adapt accordingly — refer to existing schedulers in `core-batch`/`core-worker`. This snippet assumes Spring auto-injects the job bean by `@Bean(JOB_NAME)` name; if multiple jobs exist, qualify with `@Qualifier(FeedbackDailyDigestJobConfig.JOB_NAME)`.)

- [ ] **Step 3: Wire properties in `application.yml`**

Append to `imstargg-backend/core/core-batch/src/main/resources/application.yml`:

```yaml
app:
  schedule:
    feedback-digest:
      enabled: ${APP_SCHEDULE_FEEDBACK_DIGEST_ENABLED:false}
      cron: ${APP_SCHEDULE_FEEDBACK_DIGEST_CRON:0 0 21 * * *}
```

Per-environment override: enable `true` only in `prod` (and optionally `dev`) via env vars or profile-specific config.

- [ ] **Step 4: Build to confirm wiring**

```bash
./gradlew :core:core-batch:compileKotlin
```

- [ ] **Step 5: Lint**

```bash
./gradlew :core:core-batch:ktlintCheck
```

- [ ] **Step 6: Commit**

```bash
git add imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/operation/FeedbackDailyDigestJobConfig.kt \
        imstargg-backend/core/core-batch/src/main/kotlin/com/imstargg/core/batch/job/operation/FeedbackDailyDigestScheduler.kt \
        imstargg-backend/core/core-batch/src/main/resources/application.yml
git commit -m "feat(core-batch): 피드백 24시간 다이제스트 잡 + 21시 KST 스케줄"
```

---

## Task 15: Frontend API client (imstargg-frontend)

**Files:**
- Create: `imstargg-frontend/src/api/operation/feedback.ts`

- [ ] **Step 1: Implement client**

```ts
import { apiClient } from "@/api/client"; // Adjust import to match existing api client (axios/fetch wrapper used by other src/api/*.ts files)

export type FeedbackCategory = "BUG" | "FEATURE_REQUEST" | "ETC";

export interface FeedbackCreateRequest {
  category: FeedbackCategory;
  content: string;
  email?: string;
}

export interface FeedbackCreateResponse {
  id: number;
}

export async function createFeedback(
  body: FeedbackCreateRequest,
): Promise<FeedbackCreateResponse> {
  return apiClient.post<FeedbackCreateResponse>("/v1/feedback", body);
}
```

Inspect an existing file in `imstargg-frontend/src/api/*` first (e.g. brawler/event API) to mirror the exact import path and call shape. Replace the apiClient call style with whatever wrapper the project uses.

- [ ] **Step 2: Commit**

```bash
git add imstargg-frontend/src/api/operation/feedback.ts
git commit -m "feat(frontend): feedback API 클라이언트 추가"
```

---

## Task 16: Frontend zustand store + hook

**Files:**
- Create: `imstargg-frontend/src/stores/feedback-dialog-store.ts`
- Create: `imstargg-frontend/src/hooks/use-feedback-dialog.ts`

- [ ] **Step 1: Store**

```ts
import { create } from "zustand";

interface FeedbackDialogState {
  open: boolean;
  setOpen: (open: boolean) => void;
}

export const useFeedbackDialogStore = create<FeedbackDialogState>((set) => ({
  open: false,
  setOpen: (open) => set({ open }),
}));
```

- [ ] **Step 2: Hook**

```ts
import { useFeedbackDialogStore } from "@/stores/feedback-dialog-store";

export function useFeedbackDialog() {
  const open = useFeedbackDialogStore((s) => s.open);
  const setOpen = useFeedbackDialogStore((s) => s.setOpen);
  return { open, setOpen };
}
```

- [ ] **Step 3: Commit**

```bash
git add imstargg-frontend/src/stores/feedback-dialog-store.ts \
        imstargg-frontend/src/hooks/use-feedback-dialog.ts
git commit -m "feat(frontend): 피드백 다이얼로그 zustand 스토어 + 훅 추가"
```

---

## Task 17: Frontend FeedbackForm + category labels

**Files:**
- Create: `imstargg-frontend/src/components/feedback/feedback-category.ts`
- Create: `imstargg-frontend/src/components/feedback/FeedbackForm.tsx`

- [ ] **Step 1: Category labels**

```ts
import type { FeedbackCategory } from "@/api/operation/feedback";

export const FEEDBACK_CATEGORY_LABELS: Record<FeedbackCategory, string> = {
  BUG: "버그 신고",
  FEATURE_REQUEST: "기능 제안",
  ETC: "기타",
};

export const FEEDBACK_CATEGORIES: FeedbackCategory[] = [
  "BUG",
  "FEATURE_REQUEST",
  "ETC",
];
```

- [ ] **Step 2: Form component**

```tsx
"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { z } from "zod";

import { createFeedback, type FeedbackCategory } from "@/api/operation/feedback";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  FEEDBACK_CATEGORIES,
  FEEDBACK_CATEGORY_LABELS,
} from "@/components/feedback/feedback-category";

const schema = z.object({
  category: z.enum(["BUG", "FEATURE_REQUEST", "ETC"]),
  content: z
    .string()
    .trim()
    .min(1, "내용을 입력해주세요")
    .max(2000, "최대 2000자까지 입력 가능합니다"),
  email: z
    .union([z.literal(""), z.string().email("이메일 형식이 아닙니다").max(254)])
    .optional(),
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
    defaultValues: { category: "BUG", content: "", email: "" },
  });

  const content = watch("content") ?? "";

  const onSubmit = async (values: FormValues) => {
    try {
      await createFeedback({
        category: values.category as FeedbackCategory,
        content: values.content,
        email: values.email && values.email !== "" ? values.email : undefined,
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
        <Label className="mb-2 block">유형</Label>
        <div className="flex flex-col gap-1">
          {FEEDBACK_CATEGORIES.map((cat) => (
            <label key={cat} className="flex items-center gap-2 text-sm">
              <input type="radio" value={cat} {...register("category")} />
              {FEEDBACK_CATEGORY_LABELS[cat]}
            </label>
          ))}
        </div>
      </div>

      <div>
        <Label htmlFor="feedback-content">
          내용 <span className="text-red-500">*</span>
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

      <div>
        <Label htmlFor="feedback-email">이메일 (선택)</Label>
        <Input
          id="feedback-email"
          type="email"
          placeholder="답변이 필요한 경우 입력"
          aria-invalid={Boolean(errors.email)}
          {...register("email")}
        />
        {errors.email && (
          <p className="text-xs text-red-500">{errors.email.message}</p>
        )}
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

If `Textarea` is not yet present in `@/components/ui/textarea`, either add a thin shadcn-style component or replace with a plain `<textarea>` styled with Tailwind classes matching other inputs.

- [ ] **Step 3: Commit**

```bash
git add imstargg-frontend/src/components/feedback/feedback-category.ts \
        imstargg-frontend/src/components/feedback/FeedbackForm.tsx
git commit -m "feat(frontend): 피드백 폼 컴포넌트 추가"
```

---

## Task 18: Frontend FeedbackDialog + global mount

**Files:**
- Create: `imstargg-frontend/src/components/feedback/FeedbackDialog.tsx`
- Create: `imstargg-frontend/src/components/layout/FeedbackDialogMount.tsx`
- Modify: `imstargg-frontend/app/layout.tsx` (mount once)

- [ ] **Step 1: Dialog**

```tsx
"use client";

import {
  Dialog,
  DialogContent,
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
          <DialogTitle>의견 보내기</DialogTitle>
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

- [ ] **Step 2: Global mount component**

```tsx
"use client";

import { FeedbackDialog } from "@/components/feedback/FeedbackDialog";

export function FeedbackDialogMount() {
  return <FeedbackDialog />;
}
```

- [ ] **Step 3: Mount in root layout**

Open `imstargg-frontend/app/layout.tsx`. Inside the `<body>` (after existing providers, before `</body>`), add:

```tsx
import { FeedbackDialogMount } from "@/components/layout/FeedbackDialogMount";

// ...
<FeedbackDialogMount />
```

- [ ] **Step 4: Commit**

```bash
git add imstargg-frontend/src/components/feedback/FeedbackDialog.tsx \
        imstargg-frontend/src/components/layout/FeedbackDialogMount.tsx \
        imstargg-frontend/app/layout.tsx
git commit -m "feat(frontend): FeedbackDialog 전역 마운트 추가"
```

---

## Task 19: Frontend triggers in header & footer

**Files:**
- Modify: `imstargg-frontend/src/components/layout/TopNav.tsx`
- Modify: `imstargg-frontend/src/components/layout/site-footer.tsx`

- [ ] **Step 1: TopNav button (desktop label + mobile icon)**

In `TopNav.tsx`, add a button that calls `useFeedbackDialog().setOpen(true)`. Place it in the right-side nav area. Use the `MessageSquare` icon from `lucide-react`.

```tsx
"use client";
// ... existing imports
import { MessageSquare } from "lucide-react";
import { useFeedbackDialog } from "@/hooks/use-feedback-dialog";

// inside the component:
const { setOpen: setFeedbackOpen } = useFeedbackDialog();

// inside the right-side nav area (replace placement to match existing layout):
<button
  type="button"
  onClick={() => setFeedbackOpen(true)}
  className="inline-flex items-center gap-1 text-sm text-neutral-700 hover:text-neutral-900"
  aria-label="의견 보내기"
>
  <MessageSquare className="h-4 w-4" />
  <span className="hidden sm:inline">피드백</span>
</button>
```

Confirm `TopNav.tsx` is a client component (`"use client"` at top). If it isn't, this trigger must live in a small client child.

- [ ] **Step 2: SiteFooter link**

In `imstargg-frontend/src/components/layout/site-footer.tsx`, the current component is a Server Component. Extract a tiny client component for the link:

Create `imstargg-frontend/src/components/layout/FeedbackFooterLink.tsx`:

```tsx
"use client";

import { useFeedbackDialog } from "@/hooks/use-feedback-dialog";

export function FeedbackFooterLink() {
  const { setOpen } = useFeedbackDialog();
  return (
    <button
      type="button"
      onClick={() => setOpen(true)}
      className="text-neutral-500 hover:text-neutral-700 underline-offset-2 hover:underline"
    >
      의견 보내기
    </button>
  );
}
```

Modify `site-footer.tsx` to render it:

```tsx
import Image from "next/image";
import Link from "next/link";
import { siteConfig } from "@/config/site";
import { FeedbackFooterLink } from "@/components/layout/FeedbackFooterLink";

export default function SiteFooter() {
  return (
    <footer className="bg-neutral-100 py-6 sm:py-10 mt-6 sm:mt-10">
      <div className="max-w-7xl mx-auto px-4 space-y-4">
        <div className="flex items-center">
          <Image
            src={siteConfig.logo}
            alt={siteConfig.name}
            width={102}
            height={24}
            priority={false}
          />
        </div>
        <div className="text-neutral-500 text-xs">
          <p>이메일: {siteConfig.email}</p>
          <p className="mt-1"><FeedbackFooterLink /></p>
        </div>
        <div className="text-neutral-500 text-xs space-y-1">
          <p>ImStarGG는 Supercell과 무관한 비공식 팬 사이트입니다.</p>
          <p>
            This content is not affiliated with, endorsed, sponsored, or
            specifically approved by Supercell and Supercell is not responsible
            for it.
          </p>
        </div>
      </div>
    </footer>
  );
}
```

- [ ] **Step 3: Manual verify in dev browser**

```bash
cd imstargg-frontend
pnpm dev
```

Open http://localhost:3000 → click header "피드백" → dialog opens → fill → submit → toast appears + dialog closes. Submit with invalid email → inline error. Repeat from footer "의견 보내기" link.

- [ ] **Step 4: Commit**

```bash
git add imstargg-frontend/src/components/layout/TopNav.tsx \
        imstargg-frontend/src/components/layout/site-footer.tsx \
        imstargg-frontend/src/components/layout/FeedbackFooterLink.tsx
git commit -m "feat(frontend): 헤더/푸터에 피드백 진입점 추가"
```

---

## Task 20: Admin frontend API client + types

**Files:**
- Modify: `imstargg-admin-frontend/lib/types.ts` (add types)
- Create: `imstargg-admin-frontend/lib/api/operation/feedback.ts`

- [ ] **Step 1: Add types to `lib/types.ts`**

Append:

```ts
export type FeedbackCategory = "BUG" | "FEATURE_REQUEST" | "ETC";

export interface FeedbackItem {
  id: number;
  category: FeedbackCategory;
  content: string;
  email: string | null;
  ipHash: string;
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

- [ ] **Step 2: API client**

```ts
import type {
  FeedbackListResponse,
  FeedbackSearchParams,
} from "@/lib/types";

const BASE = process.env.NEXT_PUBLIC_ADMIN_API_BASE_URL ?? "";
// If the admin frontend uses a different base url variable, match it (look in lib/api.ts).

export async function getFeedbacks(
  params: FeedbackSearchParams,
): Promise<FeedbackListResponse> {
  const search = new URLSearchParams();
  if (params.category) search.set("category", params.category);
  if (params.checked !== undefined) search.set("checked", String(params.checked));
  if (params.page !== undefined) search.set("page", String(params.page));
  if (params.size !== undefined) search.set("size", String(params.size));

  const res = await fetch(`${BASE}/feedbacks?${search.toString()}`, {
    credentials: "include",
  });
  if (!res.ok) throw new Error(`failed to fetch feedbacks: ${res.status}`);
  return res.json();
}

export async function updateFeedbackCheck(
  id: number,
  checked: boolean,
): Promise<void> {
  const res = await fetch(`${BASE}/feedbacks/${id}/check`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify({ checked }),
  });
  if (!res.ok) throw new Error(`failed to update check: ${res.status}`);
}
```

Inspect existing `imstargg-admin-frontend/lib/api.ts` first and match its fetch wrapper / base url / auth handling. Replace `BASE` and headers accordingly.

- [ ] **Step 3: Commit**

```bash
git add imstargg-admin-frontend/lib/api/operation/feedback.ts \
        imstargg-admin-frontend/lib/types.ts
git commit -m "feat(admin-frontend): feedback API 클라이언트 + 타입 추가"
```

---

## Task 21: Admin frontend table components

**Files:**
- Create: `imstargg-admin-frontend/app/feedbacks/_components/FeedbackCheckToggle.tsx`
- Create: `imstargg-admin-frontend/app/feedbacks/_components/FeedbackContentCell.tsx`
- Create: `imstargg-admin-frontend/app/feedbacks/_components/FeedbackFilters.tsx`
- Create: `imstargg-admin-frontend/app/feedbacks/_components/FeedbackTable.tsx`

- [ ] **Step 1: FeedbackCheckToggle**

```tsx
"use client";

import { useState } from "react";
import { toast } from "sonner";
import { Checkbox } from "@/components/ui/checkbox";
import { updateFeedbackCheck } from "@/lib/api/operation/feedback";

interface Props {
  id: number;
  initialChecked: boolean;
  onChanged?: (checked: boolean) => void;
}

export function FeedbackCheckToggle({ id, initialChecked, onChanged }: Props) {
  const [checked, setChecked] = useState(initialChecked);
  const [busy, setBusy] = useState(false);

  const handle = async (next: boolean) => {
    setBusy(true);
    const prev = checked;
    setChecked(next); // optimistic
    try {
      await updateFeedbackCheck(id, next);
      onChanged?.(next);
    } catch {
      setChecked(prev);
      toast.error("체크 상태 변경에 실패했습니다");
    } finally {
      setBusy(false);
    }
  };

  return (
    <Checkbox
      checked={checked}
      disabled={busy}
      onCheckedChange={(v) => handle(Boolean(v))}
    />
  );
}
```

If `@/components/ui/checkbox` is absent, mirror an existing shadcn install pattern in this repo (likely there is one because `Dialog`, `Table` etc. exist) or fall back to a styled `<input type="checkbox">`.

- [ ] **Step 2: FeedbackContentCell (expandable)**

```tsx
"use client";

import { useState } from "react";

export function FeedbackContentCell({ content }: { content: string }) {
  const [open, setOpen] = useState(false);
  if (!open && content.length > 60) {
    return (
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="text-left text-sm hover:underline"
        title={content}
      >
        {content.slice(0, 60)}…
      </button>
    );
  }
  return (
    <span
      className="whitespace-pre-wrap text-sm"
      onClick={() => setOpen(false)}
    >
      {content}
    </span>
  );
}
```

- [ ] **Step 3: FeedbackFilters**

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

const CATEGORIES = ["", "BUG", "FEATURE_REQUEST", "ETC"] as const;
const CHECKED = ["", "false", "true"] as const;

export function FeedbackFilters() {
  const router = useRouter();
  const params = useSearchParams();

  const update = (key: string, value: string) => {
    const next = new URLSearchParams(params.toString());
    if (value === "") next.delete(key);
    else next.set(key, value);
    next.delete("page"); // reset to first page when filter changes
    router.replace(`/feedbacks?${next.toString()}`);
  };

  const category = params.get("category") ?? "";
  const checked = params.get("checked") ?? "";

  return (
    <div className="flex gap-2">
      <Select
        value={category}
        onValueChange={(v) => update("category", v ?? "")}
      >
        <SelectTrigger className="w-40">
          <SelectValue placeholder="카테고리 (전체)" />
        </SelectTrigger>
        <SelectContent>
          {CATEGORIES.map((c) => (
            <SelectItem key={c} value={c}>
              {c === "" ? "전체" : c}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      <Select value={checked} onValueChange={(v) => update("checked", v ?? "")}>
        <SelectTrigger className="w-40">
          <SelectValue placeholder="상태 (전체)" />
        </SelectTrigger>
        <SelectContent>
          {CHECKED.map((c) => (
            <SelectItem key={c} value={c}>
              {c === "" ? "전체" : c === "true" ? "체크됨" : "미체크"}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  );
}
```

- [ ] **Step 4: FeedbackTable (with ipHash grouping color)**

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
import type { FeedbackItem, FeedbackListResponse } from "@/lib/types";
import { FeedbackCheckToggle } from "./FeedbackCheckToggle";
import { FeedbackContentCell } from "./FeedbackContentCell";

const GROUP_COLORS = [
  "bg-amber-50",
  "bg-rose-50",
  "bg-sky-50",
  "bg-emerald-50",
  "bg-violet-50",
];

function groupColorsByIpHash(items: FeedbackItem[]): Map<string, string> {
  const counts = new Map<string, number>();
  items.forEach((it) => counts.set(it.ipHash, (counts.get(it.ipHash) ?? 0) + 1));

  const result = new Map<string, string>();
  let idx = 0;
  counts.forEach((count, hash) => {
    if (count > 1) {
      result.set(hash, GROUP_COLORS[idx % GROUP_COLORS.length]);
      idx++;
    }
  });
  return result;
}

const CATEGORY_BADGE: Record<FeedbackItem["category"], string> = {
  BUG: "bg-red-100 text-red-800",
  FEATURE_REQUEST: "bg-blue-100 text-blue-800",
  ETC: "bg-neutral-100 text-neutral-700",
};

interface Props {
  data: FeedbackListResponse;
}

export function FeedbackTable({ data }: Props) {
  const groupBg = groupColorsByIpHash(data.content);

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead className="w-12">체크</TableHead>
          <TableHead className="w-32">유형</TableHead>
          <TableHead className="w-32">작성일시</TableHead>
          <TableHead>내용</TableHead>
          <TableHead className="w-48">이메일</TableHead>
          <TableHead className="w-32">ipHash</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {data.content.map((row) => (
          <TableRow key={row.id} className={groupBg.get(row.ipHash) ?? ""}>
            <TableCell>
              <FeedbackCheckToggle id={row.id} initialChecked={row.checked} />
            </TableCell>
            <TableCell>
              <Badge className={CATEGORY_BADGE[row.category]}>
                {row.category}
              </Badge>
            </TableCell>
            <TableCell title={row.createdAt}>
              {formatDate(row.createdAt)}
            </TableCell>
            <TableCell>
              <FeedbackContentCell content={row.content} />
            </TableCell>
            <TableCell>
              {row.email ? (
                <a className="underline" href={`mailto:${row.email}`}>
                  {row.email}
                </a>
              ) : (
                "-"
              )}
            </TableCell>
            <TableCell title={row.ipHash}>
              {row.ipHash.slice(0, 8)}
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}

function formatDate(iso: string): string {
  const d = new Date(iso);
  const pad = (n: number) => n.toString().padStart(2, "0");
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
```

If `Badge` is absent in `@/components/ui/badge`, swap for an inline `<span>` with the same Tailwind classes.

- [ ] **Step 5: Commit**

```bash
git add imstargg-admin-frontend/app/feedbacks/_components
git commit -m "feat(admin-frontend): 피드백 목록 테이블/필터/체크 토글 컴포넌트 추가"
```

---

## Task 22: Admin frontend /feedbacks page

**Files:**
- Create: `imstargg-admin-frontend/app/feedbacks/page.tsx`

- [ ] **Step 1: Page**

```tsx
"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { AdminLayout } from "../admin-layout";
import { Button } from "@/components/ui/button";
import { FeedbackFilters } from "./_components/FeedbackFilters";
import { FeedbackTable } from "./_components/FeedbackTable";
import { getFeedbacks } from "@/lib/api/operation/feedback";
import type {
  FeedbackCategory,
  FeedbackListResponse,
} from "@/lib/types";
import { toast } from "sonner";

const PAGE_SIZE = 20;

export default function FeedbackListPage() {
  const router = useRouter();
  const params = useSearchParams();
  const [data, setData] = useState<FeedbackListResponse | null>(null);
  const [loading, setLoading] = useState(false);

  const category = (params.get("category") as FeedbackCategory | null) ?? undefined;
  const checkedParam = params.get("checked");
  const checked =
    checkedParam === "true" ? true : checkedParam === "false" ? false : undefined;
  const page = Number(params.get("page") ?? "0");

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getFeedbacks({
        category,
        checked,
        page,
        size: PAGE_SIZE,
      });
      setData(res);
    } catch {
      toast.error("목록을 불러오지 못했습니다");
    } finally {
      setLoading(false);
    }
  }, [category, checked, page]);

  useEffect(() => {
    load();
  }, [load]);

  const goPage = (next: number) => {
    const sp = new URLSearchParams(params.toString());
    sp.set("page", String(next));
    router.replace(`/feedbacks?${sp.toString()}`);
  };

  return (
    <AdminLayout>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-2xl font-bold">피드백</h2>
        <FeedbackFilters />
      </div>

      {loading && <p className="text-sm text-neutral-500">로딩 중...</p>}
      {data && <FeedbackTable data={data} />}

      {data && data.totalPages > 1 && (
        <div className="mt-4 flex items-center justify-center gap-2">
          <Button
            variant="outline"
            size="sm"
            disabled={page <= 0}
            onClick={() => goPage(page - 1)}
          >
            이전
          </Button>
          <span className="text-sm">
            {page + 1} / {data.totalPages}
          </span>
          <Button
            variant="outline"
            size="sm"
            disabled={page + 1 >= data.totalPages}
            onClick={() => goPage(page + 1)}
          >
            다음
          </Button>
        </div>
      )}
    </AdminLayout>
  );
}
```

If `AdminLayout` lives at a different path (e.g. `@/app/admin-layout`), correct the import based on how other admin pages import it (look at `app/brawlers/page.tsx` which uses `../admin-layout`).

- [ ] **Step 2: Add nav link**

If there is a global admin sidebar/nav component (typically under `components/`), add a `/feedbacks` link to it next to existing entries (brawlers, events, gears, ...). If you cannot find such a component, skip this step — pages are still reachable by URL.

- [ ] **Step 3: Manual verify**

```bash
cd imstargg-admin-frontend
pnpm dev
```

Open http://localhost:3001/feedbacks (or correct admin port). After submitting feedback from the user frontend, refresh and verify rows appear. Toggle a checkbox → reload page → state persists. Apply category filter → URL updates → list narrows. Submit two feedbacks from the same browser → both rows should share the same group color background (proves ipHash grouping).

- [ ] **Step 4: Commit**

```bash
git add imstargg-admin-frontend/app/feedbacks/page.tsx
git commit -m "feat(admin-frontend): /feedbacks 페이지 추가"
```

---

## Task 23: End-to-end smoke check & docs

- [ ] **Step 1: Run full backend build + lint + tests**

```bash
cd imstargg-backend
./gradlew ktlintCheck unitTest contextTest
```

Expected: all pass.

- [ ] **Step 2: Manual E2E** (covered piecemeal above — rerun the spec's section 8.2 checklist end to end)

1. `docker-compose up -d`
2. Start backend: `./gradlew :core:core-api:bootRun :core:core-admin:bootRun` (in parallel terminals)
3. Start frontend: `cd imstargg-frontend && pnpm dev`
4. Start admin frontend: `cd imstargg-admin-frontend && pnpm dev`
5. Submit feedback (no email, then with email) from header and footer
6. Open admin `/feedbacks` → verify rows, filters, check toggle, ipHash grouping color
7. (Optional) Run the batch job manually:
   ```bash
   ./gradlew :core:core-batch:bootRun --args='--spring.batch.job.name=feedbackDailyDigestJob'
   ```
   Verify Discord receives the message (in dev set webhook to a personal channel). Then truncate `feedback` and rerun → no message sent.

- [ ] **Step 3: Final commit (no-op if everything already committed)**

```bash
git status
```

If clean, nothing to do. If any incidental files (e.g. updated schema.sql comments), `git add -p` and commit as `chore: 피드백 기능 마무리`.

---

## Self-Review (recorded)

Performed inline after writing all tasks. Findings:

- **Spec coverage:** Sections 2–8 of the spec each map to at least one task above. Section 9 (future work) is explicitly out of scope.
- **Placeholder scan:** No "TBD"/"TODO"/"implement later" in tasks. Adjustments noted (e.g. `AlertCommand` constructor verification, base test class names, `Textarea`/`Checkbox`/`Badge` availability) are stated as concrete checks against existing files, not deferred work.
- **Type consistency:** `FeedbackCategory` enum names (`BUG`, `FEATURE_REQUEST`, `ETC`) are identical across enum, request, response, and frontend types. Property names match (`ipHash`, `checked`, `createdAt`, `email`). The Tasklet method is consistently named `run()`. The application repository wrapper is `FeedbackRepository`; the JPA interface is `FeedbackJpaRepository`. The QueryDSL admin repository is `AdminFeedbackRepository`.
- **Spec adjustments locked in:** `BaseTimeEntity` (not `BaseEntity`) → no `deleted` column; db-core package is `com.imstargg.infrastructure.db.core.operation`; `AlertManager.alert(AlertCommand)` (not `send`); admin frontend uses `useEffect`+`fetch` per existing repo pattern (not React Query).
