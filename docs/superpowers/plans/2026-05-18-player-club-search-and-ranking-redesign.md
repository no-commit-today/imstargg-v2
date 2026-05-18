# 플레이어/클럽 검색 & 랭킹 재배치 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 이름/태그 통합 검색 API + 라이브 검색 UX + 랭킹을 각 도메인 탭(플레이어/클럽/브롤러 상세)으로 재배치.

**Architecture:** Backend 는 PostgreSQL `LOWER(name) varchar_pattern_ops` B-tree 인덱스 + `LIKE q || '%'` prefix 매칭, `Slice<T>` 도메인을 활용한 hasNext 기반 페이징. Frontend 는 공용 `SearchableInput<T>` 컴포넌트(디바운스, 키보드 네비) + 검색 결과 페이지에 갱신 카드 + 검색 내역 v2 스키마(결과 클릭 시에만 저장).

**Tech Stack:** Spring Boot 3 + Kotlin (JPA), PostgreSQL, Flyway / Next.js 15 App Router + RSC + TanStack Query + Vitest

**스펙**: [docs/superpowers/specs/2026-05-18-player-club-search-and-ranking-redesign-design.md](../specs/2026-05-18-player-club-search-and-ranking-redesign-design.md)

---

## 파일 구조

### Backend (`imstargg-backend`)

| 파일 | 책임 |
|---|---|
| `infrastructure/db-core/src/main/resources/db/migration/V260518__add_player_club_name_search_index.sql` | LOWER 표현식 prefix 인덱스 추가 |
| `infrastructure/db-core/src/main/resources/schema.sql` | 인덱스 스냅샷 동기화 |
| `infrastructure/db-core/.../player/repository/PlayerJpaRepository.kt` | nativeQuery 로 prefix 검색 메서드 추가 |
| `infrastructure/db-core/.../player/repository/ClubJpaRepository.kt` | 동일 |
| `core/core-api/.../application/brawlstars/PlayerRepository.kt` | `searchByName`, `searchByTag` |
| `core/core-api/.../application/brawlstars/ClubRepository.kt` | 동일 |
| `core/core-api/.../application/brawlstars/PlayerService.kt` | `search(q, offsetLimit): Slice<Player>` |
| `core/core-api/.../application/brawlstars/ClubService.kt` | `search(q, offsetLimit): Slice<Club>` |
| `core/core-api/.../api/controller/v1/response/PlayerSearchResponse.kt` | 신규 DTO (Slice → SliceResponse<PlayerResponse>) |
| `core/core-api/.../api/controller/v1/response/ClubSearchResponse.kt` | 신규 DTO |
| `core/core-api/.../api/controller/v1/PlayerController.kt` | `GET /v1/player/search` 핸들러 추가 |
| `core/core-api/.../api/controller/v1/ClubController.kt` | `GET /v1/club/search` 핸들러 추가 |
| `core/core-api/src/test/.../application/brawlstars/PlayerRepositorySearchTest.kt` | Repository 통합 테스트 |
| `core/core-api/src/test/.../application/brawlstars/ClubRepositorySearchTest.kt` | Repository 통합 테스트 |

### Frontend (`imstargg-frontend`)

| 파일 | 책임 |
|---|---|
| `src/api/types.ts` | `PlayerSearchResponse`, `ClubSearchResponse` 타입 추가 |
| `src/api/endpoints/player.ts` | `playerApi.search` 추가 |
| `src/api/endpoints/club.ts` | `clubApi.search` 추가 |
| `src/lib/searchHistory.ts` | v2 키 + `name`/`trophies` 필드 추가 |
| `src/components/shared/SearchableInput.tsx` | 신규 제네릭 공용 컴포넌트 |
| `src/components/shared/PlayerSearchInput.tsx` | SearchableInput wrapper 로 리팩토링 |
| `app/clubs/_components/ClubSearchInput.tsx` | SearchableInput wrapper 로 리팩토링 |
| `src/components/shared/SearchHistoryDropdown.tsx` | 항목에 name/trophies 표시 |
| `src/components/shared/RenewalCard.tsx` | 신규 — 검색 결과 페이지용 갱신 카드 |
| `app/players/page.tsx` | 신규 — 검색창 + 플레이어 트로피 랭킹 |
| `app/players/search/page.tsx` | 신규 — 플레이어 검색 결과 |
| `app/players/search/_components/PlayerSearchResults.tsx` | 클라이언트 결과 리스트 + 더보기 |
| `app/clubs/page.tsx` | 검색창 아래에 클럽 랭킹 섹션 추가 |
| `app/clubs/search/page.tsx` | 신규 — 클럽 검색 결과 |
| `app/clubs/search/_components/ClubSearchResults.tsx` | 클라이언트 결과 리스트 |
| `app/brawlers/[id]/_components/BrawlerContent.tsx` | shadcn Tabs 도입 — 상세/랭킹 탭 |
| `app/brawlers/[id]/_components/BrawlerRankingTab.tsx` | 신규 — 국가 셀렉터 + 브롤러 랭킹 리스트 |
| `app/ranking/**` | 전부 삭제 |
| `src/components/layout/TopNav.tsx` | NAV_ITEMS 에서 "랭킹" 제거, "플레이어" 추가 |

---

## Task 1: Backend — Flyway 마이그레이션 (인덱스 추가)

**Files:**
- Create: `imstargg-backend/infrastructure/db-core/src/main/resources/db/migration/V260518__add_player_club_name_search_index.sql`
- Modify: `imstargg-backend/infrastructure/db-core/src/main/resources/schema.sql` (인덱스 두 줄 추가)

- [ ] **Step 1: 마이그레이션 파일 작성**

```sql
-- player.name / club.name 대소문자 무관 prefix 검색 인덱스
-- LIKE 'xxx%' 매칭에 최적화된 varchar_pattern_ops B-tree.
-- 멱등 작성: create index if not exists.

create index if not exists ix_player__lower_name
    on player (lower(name) varchar_pattern_ops);

create index if not exists ix_club__lower_name
    on club (lower(name) varchar_pattern_ops);
```

- [ ] **Step 2: schema.sql 동기화**

`schema.sql` 의 `create unique index ... uk_player__brawlstarstag on player ...` 줄(라인 26) 바로 다음 줄에 추가:

```sql
create index if not exists ix_player__lower_name on player (lower(name) varchar_pattern_ops);
```

`create unique index ... uk_club__brawlstarstag on club ...` 줄(라인 114) 바로 다음 줄에 추가:

```sql
create index if not exists ix_club__lower_name on club (lower(name) varchar_pattern_ops);
```

- [ ] **Step 3: Flyway가 적용되는지 build 로 확인**

Run: `cd imstargg-backend && ./gradlew :core:core-api:contextTest -x test --tests "com.imstargg.CoreApiApplicationTest"`
Expected: PASS (스키마 적용 + 마이그레이션 적용)

- [ ] **Step 4: 커밋**

```bash
cd imstargg-backend
git add infrastructure/db-core/src/main/resources/db/migration/V260518__add_player_club_name_search_index.sql \
        infrastructure/db-core/src/main/resources/schema.sql
git commit -m "feat(db-core): add LOWER name prefix index for player/club search"
```

---

## Task 2: Backend — JpaRepository 검색 메서드 (PlayerJpaRepository)

**Files:**
- Modify: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/brawlstars/player/repository/PlayerJpaRepository.kt`

> 쿼리 메서드(`findAllByLowerNameStartingWith`) 는 SpEL/PrimaryKey 변환이 까다롭고, `LOWER(name) LIKE :pattern` 을 인덱스에 정확히 매칭시키려면 nativeQuery 가 안전.

- [ ] **Step 1: nativeQuery 메서드 추가**

`PlayerJpaRepository.kt` 의 마지막 `}` 직전에 추가:

```kotlin
    @org.springframework.data.jpa.repository.Query(
        nativeQuery = true,
        value = """
            select * from player
            where lower(name) like :pattern
            order by trophies desc, brawlstars_tag asc
            offset :offset limit :limit
        """,
    )
    fun findAllByLowerNamePrefix(
        @org.springframework.data.repository.query.Param("pattern") pattern: String,
        @org.springframework.data.repository.query.Param("offset") offset: Int,
        @org.springframework.data.repository.query.Param("limit") limit: Int,
    ): List<PlayerEntity>
```

- [ ] **Step 2: import 정리 (linter 가 자동 처리하지 못할 경우 수동)**

상단 import 블록에 `import org.springframework.data.jpa.repository.Query` 와 `import org.springframework.data.repository.query.Param` 추가하고, 위 메서드의 fully-qualified 이름은 짧은 형태로 교체:

```kotlin
    @Query(
        nativeQuery = true,
        value = """
            select * from player
            where lower(name) like :pattern
            order by trophies desc, brawlstars_tag asc
            offset :offset limit :limit
        """,
    )
    fun findAllByLowerNamePrefix(
        @Param("pattern") pattern: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int,
    ): List<PlayerEntity>
```

- [ ] **Step 3: ktlint 통과 확인**

Run: `cd imstargg-backend && ./gradlew :infrastructure:db-core:ktlintCheck`
Expected: PASS

- [ ] **Step 4: 커밋**

```bash
cd imstargg-backend
git add infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/brawlstars/player/repository/PlayerJpaRepository.kt
git commit -m "feat(db-core): add player name prefix search query"
```

---

## Task 3: Backend — JpaRepository 검색 메서드 (ClubJpaRepository)

**Files:**
- Modify: `imstargg-backend/infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/brawlstars/player/repository/ClubJpaRepository.kt`

- [ ] **Step 1: nativeQuery 메서드 추가**

`ClubJpaRepository.kt` 를 다음과 같이 교체:

```kotlin
package com.imstargg.infrastructure.db.core.brawlstars.player.repository

import com.imstargg.infrastructure.db.core.brawlstars.player.ClubEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ClubJpaRepository : JpaRepository<ClubEntity, Long> {
    fun findByBrawlStarsTag(brawlStarsTag: String): ClubEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findWithLockByBrawlStarsTag(brawlStarsTag: String): ClubEntity?

    @Query(
        nativeQuery = true,
        value = """
            select * from club
            where lower(name) like :pattern
            order by trophies desc, brawlstars_tag asc
            offset :offset limit :limit
        """,
    )
    fun findAllByLowerNamePrefix(
        @Param("pattern") pattern: String,
        @Param("offset") offset: Int,
        @Param("limit") limit: Int,
    ): List<ClubEntity>
}
```

- [ ] **Step 2: ktlint 통과 확인**

Run: `cd imstargg-backend && ./gradlew :infrastructure:db-core:ktlintCheck`
Expected: PASS

- [ ] **Step 3: 커밋**

```bash
cd imstargg-backend
git add infrastructure/db-core/src/main/kotlin/com/imstargg/infrastructure/db/core/brawlstars/player/repository/ClubJpaRepository.kt
git commit -m "feat(db-core): add club name prefix search query"
```

---

## Task 4: Backend — PlayerRepository 검색 통합 테스트 (TDD)

**Files:**
- Create: `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/application/brawlstars/PlayerRepositorySearchTest.kt`

- [ ] **Step 1: 실패하는 테스트 작성**

```kotlin
package com.imstargg.core.application.brawlstars

import com.imstargg.CoreApiContextTest
import com.imstargg.core.domain.OffsetLimit
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.PlayerEntityFixture.Companion.aPlayerEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PlayerRepositorySearchTest(
    private val sut: PlayerRepository,
    private val cleanUp: CleanUp,
    private val playerJpaRepository: PlayerJpaRepository,
) : CoreApiContextTest() {
    @BeforeEach
    fun setUp() {
        cleanUp.all()
    }

    @Nested
    inner class SearchByTag {
        @Test
        fun `태그 정확 일치 - 단건 반환`() {
            // given
            playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#ABC").withName("야옹이").build())
            playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#XYZ").withName("멍멍이").build())

            // when
            val result = sut.searchByTag(BrawlStarsTag("#ABC"))

            // then
            assertThat(result).isNotNull
            assertThat(result!!.tag.value).isEqualTo("#ABC")
        }

        @Test
        fun `존재하지 않는 태그 - null 반환`() {
            // when
            val result = sut.searchByTag(BrawlStarsTag("#NONE"))

            // then
            assertThat(result).isNull()
        }
    }

    @Nested
    inner class SearchByName {
        @Test
        fun `이름 prefix 정확 매칭 - 트로피 desc 정렬`() {
            // given
            playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#A").withName("야옹이").withTrophies(100).build())
            playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#B").withName("야옹이짱").withTrophies(500).build())
            playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#C").withName("멍멍이").withTrophies(300).build())

            // when
            val result = sut.searchByName("야옹", OffsetLimit(offset = 0, limit = 10))

            // then
            assertThat(result.content).hasSize(2)
            assertThat(result.content[0].name).isEqualTo("야옹이짱")
            assertThat(result.content[1].name).isEqualTo("야옹이")
            assertThat(result.hasNext).isFalse
        }

        @Test
        fun `이름 prefix 대소문자 무시`() {
            // given
            playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#A").withName("Alpha").withTrophies(100).build())

            // when
            val result = sut.searchByName("alp", OffsetLimit(offset = 0, limit = 10))

            // then
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].name).isEqualTo("Alpha")
        }

        @Test
        fun `limit 경계 - hasNext true`() {
            // given
            playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#A").withName("야옹이1").withTrophies(100).build())
            playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#B").withName("야옹이2").withTrophies(200).build())
            playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#C").withName("야옹이3").withTrophies(300).build())

            // when
            val result = sut.searchByName("야옹", OffsetLimit(offset = 0, limit = 2))

            // then
            assertThat(result.content).hasSize(2)
            assertThat(result.hasNext).isTrue
        }

        @Test
        fun `결과 없음 - 빈 slice`() {
            // when
            val result = sut.searchByName("없는이름", OffsetLimit(offset = 0, limit = 10))

            // then
            assertThat(result.content).isEmpty()
            assertThat(result.hasNext).isFalse
        }
    }
}
```

- [ ] **Step 2: 테스트 실행하여 컴파일 실패 확인**

Run: `cd imstargg-backend && ./gradlew :core:core-api:contextTest --tests "com.imstargg.core.application.brawlstars.PlayerRepositorySearchTest"`
Expected: FAIL — `searchByTag`, `searchByName` 메서드 정의 없음 (컴파일 에러)

- [ ] **Step 3: PlayerRepository 에 검색 메서드 구현**

`imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/brawlstars/PlayerRepository.kt` 의 `findAllPlayerBrawler` 메서드 바로 위에 추가:

```kotlin
    fun searchByTag(tag: BrawlStarsTag): Player? {
        val entity = playerJpaRepository.findByBrawlStarsTag(tag.value) ?: return null
        return toPlayer(entity)
    }

    fun searchByName(
        namePrefix: String,
        offsetLimit: com.imstargg.core.domain.OffsetLimit,
    ): com.imstargg.core.domain.Slice<Player> {
        val pattern = namePrefix.lowercase() + "%"
        val entities =
            playerJpaRepository.findAllByLowerNamePrefix(
                pattern = pattern,
                offset = offsetLimit.offset,
                limit = offsetLimit.limit + 1,
            )
        val hasNext = entities.size > offsetLimit.limit
        val content = entities.take(offsetLimit.limit).map { toPlayer(it) }
        return com.imstargg.core.domain.Slice(content = content, hasNext = hasNext)
    }
```

상단 import 정리:
```kotlin
import com.imstargg.core.domain.OffsetLimit
import com.imstargg.core.domain.Slice
```
그리고 위 메서드 본문의 fully-qualified 이름은 짧은 형태로 교체.

- [ ] **Step 4: 테스트 실행 → PASS 확인**

Run: `cd imstargg-backend && ./gradlew :core:core-api:contextTest --tests "com.imstargg.core.application.brawlstars.PlayerRepositorySearchTest"`
Expected: PASS

- [ ] **Step 5: ktlint 통과 확인**

Run: `cd imstargg-backend && ./gradlew :core:core-api:ktlintCheck`
Expected: PASS

- [ ] **Step 6: 커밋**

```bash
cd imstargg-backend
git add core/core-api/src/main/kotlin/com/imstargg/core/application/brawlstars/PlayerRepository.kt \
        core/core-api/src/test/kotlin/com/imstargg/core/application/brawlstars/PlayerRepositorySearchTest.kt
git commit -m "feat(core-api): add player search by tag/name to PlayerRepository"
```

---

## Task 5: Backend — ClubRepository 검색 통합 테스트 (TDD)

**Files:**
- Create: `imstargg-backend/core/core-api/src/test/kotlin/com/imstargg/core/application/brawlstars/ClubRepositorySearchTest.kt`

- [ ] **Step 1: 실패하는 테스트 작성**

```kotlin
package com.imstargg.core.application.brawlstars

import com.imstargg.CoreApiContextTest
import com.imstargg.core.domain.OffsetLimit
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.ClubJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.ClubEntityFixture.Companion.aClubEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ClubRepositorySearchTest(
    private val sut: ClubRepository,
    private val cleanUp: CleanUp,
    private val clubJpaRepository: ClubJpaRepository,
) : CoreApiContextTest() {
    @BeforeEach
    fun setUp() {
        cleanUp.all()
    }

    @Nested
    inner class SearchByTag {
        @Test
        fun `태그 정확 일치 - 단건 반환`() {
            // given
            clubJpaRepository.save(aClubEntity().withBrawlStarsTag("#ABC").withName("스타포스").build())
            clubJpaRepository.save(aClubEntity().withBrawlStarsTag("#XYZ").withName("문라이트").build())

            // when
            val result = sut.searchByTag(BrawlStarsTag("#ABC"))

            // then
            assertThat(result).isNotNull
            assertThat(result!!.tag.value).isEqualTo("#ABC")
        }

        @Test
        fun `존재하지 않는 태그 - null 반환`() {
            // when
            val result = sut.searchByTag(BrawlStarsTag("#NONE"))

            // then
            assertThat(result).isNull()
        }
    }

    @Nested
    inner class SearchByName {
        @Test
        fun `이름 prefix 정확 매칭 - 트로피 desc 정렬`() {
            // given
            clubJpaRepository.save(aClubEntity().withBrawlStarsTag("#A").withName("스타포스").withTrophies(100).build())
            clubJpaRepository.save(aClubEntity().withBrawlStarsTag("#B").withName("스타라이트").withTrophies(500).build())
            clubJpaRepository.save(aClubEntity().withBrawlStarsTag("#C").withName("문라이트").withTrophies(300).build())

            // when
            val result = sut.searchByName("스타", OffsetLimit(offset = 0, limit = 10))

            // then
            assertThat(result.content).hasSize(2)
            assertThat(result.content[0].name).isEqualTo("스타라이트")
            assertThat(result.content[1].name).isEqualTo("스타포스")
            assertThat(result.hasNext).isFalse
        }

        @Test
        fun `limit 경계 - hasNext true`() {
            // given
            clubJpaRepository.save(aClubEntity().withBrawlStarsTag("#A").withName("스타1").withTrophies(100).build())
            clubJpaRepository.save(aClubEntity().withBrawlStarsTag("#B").withName("스타2").withTrophies(200).build())
            clubJpaRepository.save(aClubEntity().withBrawlStarsTag("#C").withName("스타3").withTrophies(300).build())

            // when
            val result = sut.searchByName("스타", OffsetLimit(offset = 0, limit = 2))

            // then
            assertThat(result.content).hasSize(2)
            assertThat(result.hasNext).isTrue
        }
    }
}
```

- [ ] **Step 2: 컴파일 실패 확인**

Run: `cd imstargg-backend && ./gradlew :core:core-api:contextTest --tests "com.imstargg.core.application.brawlstars.ClubRepositorySearchTest"`
Expected: FAIL — `searchByTag`, `searchByName` 메서드 정의 없음

- [ ] **Step 3: ClubRepository 에 검색 메서드 구현**

`imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/brawlstars/ClubRepository.kt` 의 `find` 메서드 바로 아래에 추가:

```kotlin
    fun searchByTag(tag: BrawlStarsTag): Club? {
        val entity = clubJpaRepository.findByBrawlStarsTag(tag.value) ?: return null
        val memberEntities = clubMemberJpaRepository.findAllByClub(entity)
        return toClub(entity, memberEntities)
    }

    fun searchByName(
        namePrefix: String,
        offsetLimit: OffsetLimit,
    ): Slice<Club> {
        val pattern = namePrefix.lowercase() + "%"
        val entities =
            clubJpaRepository.findAllByLowerNamePrefix(
                pattern = pattern,
                offset = offsetLimit.offset,
                limit = offsetLimit.limit + 1,
            )
        val hasNext = entities.size > offsetLimit.limit
        val pageEntities = entities.take(offsetLimit.limit)
        val membersByClub =
            pageEntities.associateWith { clubMemberJpaRepository.findAllByClub(it) }
        val content = pageEntities.map { toClub(it, membersByClub.getValue(it)) }
        return Slice(content = content, hasNext = hasNext)
    }
```

상단 import 에 추가:
```kotlin
import com.imstargg.core.domain.OffsetLimit
import com.imstargg.core.domain.Slice
```

- [ ] **Step 4: 테스트 실행 → PASS 확인**

Run: `cd imstargg-backend && ./gradlew :core:core-api:contextTest --tests "com.imstargg.core.application.brawlstars.ClubRepositorySearchTest"`
Expected: PASS

- [ ] **Step 5: ktlint 통과 확인**

Run: `cd imstargg-backend && ./gradlew :core:core-api:ktlintCheck`
Expected: PASS

- [ ] **Step 6: 커밋**

```bash
cd imstargg-backend
git add core/core-api/src/main/kotlin/com/imstargg/core/application/brawlstars/ClubRepository.kt \
        core/core-api/src/test/kotlin/com/imstargg/core/application/brawlstars/ClubRepositorySearchTest.kt
git commit -m "feat(core-api): add club search by tag/name to ClubRepository"
```

---

## Task 6: Backend — PlayerService.search 추가

**Files:**
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/brawlstars/PlayerService.kt`

- [ ] **Step 1: PlayerService 에 search 메서드 추가**

파일 전체 교체:

```kotlin
package com.imstargg.core.application.brawlstars

import com.imstargg.core.domain.OffsetLimit
import com.imstargg.core.domain.Slice
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.brawlstars.player.Player
import com.imstargg.core.domain.brawlstars.player.PlayerBrawler
import com.imstargg.core.error.CoreException
import com.imstargg.core.error.ErrorType
import org.springframework.stereotype.Service

@Service
class PlayerService(
    private val playerRepository: PlayerRepository,
) {
    fun get(tag: BrawlStarsTag): Player =
        playerRepository.find(tag)
            ?: throw CoreException(ErrorType.NOT_FOUND, "tag=$tag")

    fun getPlayerBrawlers(tag: BrawlStarsTag): List<PlayerBrawler> = playerRepository.findAllPlayerBrawler(tag)

    fun search(
        query: String,
        offsetLimit: OffsetLimit,
    ): Slice<Player> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            throw CoreException(ErrorType.INVALID_REQUEST, "query is blank")
        }
        return if (trimmed.startsWith("#")) {
            val player = playerRepository.searchByTag(BrawlStarsTag(trimmed))
            Slice(content = listOfNotNull(player), hasNext = false)
        } else {
            playerRepository.searchByName(trimmed, offsetLimit)
        }
    }
}
```

> `ErrorType.INVALID_REQUEST` 가 없으면 `BAD_REQUEST` 같은 가까운 enum 으로 교체. 코드에서 `ErrorType` 정의 확인 후 일치하는 이름 사용.

- [ ] **Step 2: ErrorType enum 확인 + 필요 시 교체**

Run: `cd imstargg-backend && grep -rn "enum class ErrorType" core/core-api/src/main/kotlin`
Expected: ErrorType 위치 노출 → 그 파일의 enum 값을 보고 INVALID_REQUEST 같은 적절한 값으로 교체

- [ ] **Step 3: ktlint + 빌드**

Run: `cd imstargg-backend && ./gradlew :core:core-api:ktlintCheck && ./gradlew :core:core-api:compileKotlin`
Expected: PASS

- [ ] **Step 4: 커밋**

```bash
cd imstargg-backend
git add core/core-api/src/main/kotlin/com/imstargg/core/application/brawlstars/PlayerService.kt
git commit -m "feat(core-api): add PlayerService.search dispatching tag/name"
```

---

## Task 7: Backend — ClubService.search 추가

**Files:**
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/application/brawlstars/ClubService.kt`

- [ ] **Step 1: ClubService 에 search 메서드 추가**

파일 전체 교체:

```kotlin
package com.imstargg.core.application.brawlstars

import com.imstargg.core.domain.OffsetLimit
import com.imstargg.core.domain.Slice
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.brawlstars.player.Club
import com.imstargg.core.error.CoreException
import com.imstargg.core.error.ErrorType
import org.springframework.stereotype.Service

@Service
class ClubService(
    private val clubRepository: ClubRepository,
) {
    fun get(tag: BrawlStarsTag): Club =
        clubRepository.find(tag)
            ?: throw CoreException(ErrorType.NOT_FOUND, "tag=$tag")

    fun search(
        query: String,
        offsetLimit: OffsetLimit,
    ): Slice<Club> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            throw CoreException(ErrorType.INVALID_REQUEST, "query is blank")
        }
        return if (trimmed.startsWith("#")) {
            val club = clubRepository.searchByTag(BrawlStarsTag(trimmed))
            Slice(content = listOfNotNull(club), hasNext = false)
        } else {
            clubRepository.searchByName(trimmed, offsetLimit)
        }
    }
}
```

> Task 6 와 동일하게 `ErrorType` enum 값 확인 후 필요 시 교체.

- [ ] **Step 2: ktlint + 빌드**

Run: `cd imstargg-backend && ./gradlew :core:core-api:ktlintCheck && ./gradlew :core:core-api:compileKotlin`
Expected: PASS

- [ ] **Step 3: 커밋**

```bash
cd imstargg-backend
git add core/core-api/src/main/kotlin/com/imstargg/core/application/brawlstars/ClubService.kt
git commit -m "feat(core-api): add ClubService.search dispatching tag/name"
```

---

## Task 8: Backend — Controller 엔드포인트 추가 (Player + Club)

**Files:**
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/PlayerController.kt`
- Modify: `imstargg-backend/core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/ClubController.kt`

- [ ] **Step 1: PlayerController 에 검색 엔드포인트 추가**

전체 교체:

```kotlin
package com.imstargg.core.api.controller.v1

import com.imstargg.core.api.controller.ListResponse
import com.imstargg.core.api.controller.OffsetLimitRequest
import com.imstargg.core.api.controller.SliceResponse
import com.imstargg.core.api.controller.v1.response.PlayerBrawlerResponse
import com.imstargg.core.api.controller.v1.response.PlayerResponse
import com.imstargg.core.application.brawlstars.PlayerService
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class PlayerController(
    private val playerService: PlayerService,
) {
    @GetMapping("/v1/players/{tag}")
    fun getPlayer(
        @PathVariable tag: String,
    ): ResponseEntity<PlayerResponse> {
        val player = playerService.get(BrawlStarsTag(tag))
        return ResponseEntity.ok(PlayerResponse.of(player))
    }

    @GetMapping("/v1/players/{tag}/brawlers")
    fun getPlayerBrawlers(
        @PathVariable tag: String,
    ): ResponseEntity<ListResponse<PlayerBrawlerResponse>> {
        val playerBrawlers = playerService.getPlayerBrawlers(BrawlStarsTag(tag))
        return ResponseEntity.ok(
            ListResponse(playerBrawlers.map { PlayerBrawlerResponse.of(it) }),
        )
    }

    @GetMapping("/v1/player/search")
    fun searchPlayers(
        @RequestParam q: String,
        @ModelAttribute offsetLimit: OffsetLimitRequest,
    ): ResponseEntity<SliceResponse<PlayerResponse>> {
        val slice = playerService.search(q, offsetLimit.toOffsetLimit())
        return ResponseEntity.ok(SliceResponse.from(slice.map { PlayerResponse.of(it) }))
    }
}
```

- [ ] **Step 2: ClubController 에 검색 엔드포인트 추가**

전체 교체:

```kotlin
package com.imstargg.core.api.controller.v1

import com.imstargg.core.api.controller.OffsetLimitRequest
import com.imstargg.core.api.controller.SliceResponse
import com.imstargg.core.api.controller.v1.response.ClubResponse
import com.imstargg.core.application.brawlstars.ClubService
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ClubController(
    private val clubService: ClubService,
) {
    @GetMapping("/v1/clubs/{tag}")
    fun getClub(
        @PathVariable tag: String,
    ): ResponseEntity<ClubResponse> {
        val club = clubService.get(BrawlStarsTag(tag))
        return ResponseEntity.ok(ClubResponse.of(club))
    }

    @GetMapping("/v1/club/search")
    fun searchClubs(
        @RequestParam q: String,
        @ModelAttribute offsetLimit: OffsetLimitRequest,
    ): ResponseEntity<SliceResponse<ClubResponse>> {
        val slice = clubService.search(q, offsetLimit.toOffsetLimit())
        return ResponseEntity.ok(SliceResponse.from(slice.map { ClubResponse.of(it) }))
    }
}
```

- [ ] **Step 3: ktlint + 컴파일**

Run: `cd imstargg-backend && ./gradlew :core:core-api:ktlintCheck && ./gradlew :core:core-api:compileKotlin`
Expected: PASS

- [ ] **Step 4: 커밋**

```bash
cd imstargg-backend
git add core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/PlayerController.kt \
        core/core-api/src/main/kotlin/com/imstargg/core/api/controller/v1/ClubController.kt
git commit -m "feat(core-api): add GET /v1/{player|club}/search endpoints"
```

---

## Task 9: Backend — 전체 contextTest 회귀 확인

**Files:** (없음 — 검증 단계)

- [ ] **Step 1: contextTest 전체 실행**

Run: `cd imstargg-backend && ./gradlew :core:core-api:contextTest`
Expected: PASS — 신규 두 테스트 클래스 포함, 모든 기존 테스트 통과

- [ ] **Step 2: unitTest 도 실행**

Run: `cd imstargg-backend && ./gradlew :core:core-api:unitTest`
Expected: PASS

- [ ] **Step 3: release-management 스킬로 core-api SNAPSHOT bump (minor)**

> 사용자에게 "core-api 변경 끝났어 — bump 해도 돼?" 질문 후 진행. 자동 트리거 가능하므로 단순 안내만.

---

## Task 10: Frontend — searchHistory.ts v2 스키마

**Files:**
- Modify: `imstargg-frontend/src/lib/searchHistory.ts`
- Modify: `imstargg-frontend/src/lib/favorites.ts` (favorites 도 같은 스키마 사용 시 확인 필요)

- [ ] **Step 1: 기존 favorites.ts 확인**

Run: `cat imstargg-frontend/src/lib/favorites.ts`
Expected: SearchEntry 또는 유사 타입 사용 — 스키마 변경 영향 파악

- [ ] **Step 2: searchHistory.ts 교체**

`imstargg-frontend/src/lib/searchHistory.ts` 전체 교체:

```typescript
import { normalizeTag } from "@/lib/tag";

export type SearchEntryKind = "player" | "club";

export interface SearchEntry {
  kind: SearchEntryKind;
  tag: string;
  name: string;
  trophies: number;
  addedAt: number;
}

const RECENTS_KEY = "imstargg.search.recents.v2";
export const RECENTS_LIMIT = 10;

function isBrowser(): boolean {
  return typeof window !== "undefined" && typeof window.localStorage !== "undefined";
}

function readArray(key: string): SearchEntry[] {
  if (!isBrowser()) return [];
  const raw = window.localStorage.getItem(key);
  if (raw == null) return [];
  try {
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) {
      window.localStorage.removeItem(key);
      return [];
    }
    return parsed.filter(
      (e): e is SearchEntry =>
        e != null &&
        (e.kind === "player" || e.kind === "club") &&
        typeof e.tag === "string" &&
        typeof e.name === "string" &&
        typeof e.trophies === "number" &&
        typeof e.addedAt === "number",
    );
  } catch {
    window.localStorage.removeItem(key);
    return [];
  }
}

function writeArray(key: string, entries: SearchEntry[]): void {
  if (!isBrowser()) return;
  try {
    window.localStorage.setItem(key, JSON.stringify(entries));
  } catch {
    // QuotaExceededError or SecurityError — silently drop the write
  }
}

export function getRecents(): SearchEntry[] {
  return readArray(RECENTS_KEY);
}

export function addRecent(input: Omit<SearchEntry, "addedAt">): void {
  if (!isBrowser()) return;
  const tag = normalizeTag(input.tag);
  const next: SearchEntry = { ...input, tag, addedAt: Date.now() };
  const filtered = getRecents().filter((e) => !(e.kind === next.kind && e.tag === next.tag));
  const merged = [next, ...filtered].slice(0, RECENTS_LIMIT);
  writeArray(RECENTS_KEY, merged);
}

export function clearRecents(): void {
  if (!isBrowser()) return;
  window.localStorage.removeItem(RECENTS_KEY);
}
```

- [ ] **Step 3: favorites.ts 가 SearchEntry 를 import 한다면 동일 스키마 적용**

`imstargg-frontend/src/lib/favorites.ts` 가 SearchEntry 를 사용한다면 키도 `imstargg.search.favorites.v2` 로 변경하고, `name`/`trophies` 필드 강제. 호출자(예: `toggleFavorite({ kind, tag })` → `{ kind, tag, name, trophies }`) 시그니처 확장.

> 만약 favorites 가 단순히 `Pick<SearchEntry, "kind" | "tag">` 만 저장한다면 굳이 강제하지 않아도 됨. 호환 가능한 최소한의 변경만 수행.

- [ ] **Step 4: 단위 테스트 작성**

`imstargg-frontend/src/lib/__tests__/searchHistory.test.ts` 신규:

```typescript
import { beforeEach, describe, expect, it } from "vitest";
import { addRecent, clearRecents, getRecents } from "@/lib/searchHistory";

beforeEach(() => {
  window.localStorage.clear();
});

describe("searchHistory v2", () => {
  it("addRecent 후 getRecents 로 조회 시 동일 항목 반환", () => {
    addRecent({ kind: "player", tag: "#ABC", name: "야옹이", trophies: 50000 });
    const recents = getRecents();
    expect(recents).toHaveLength(1);
    expect(recents[0].name).toEqual("야옹이");
    expect(recents[0].trophies).toEqual(50000);
  });

  it("같은 kind/tag 는 중복 저장되지 않고 최신으로 갱신", () => {
    addRecent({ kind: "player", tag: "#ABC", name: "야옹이", trophies: 100 });
    addRecent({ kind: "player", tag: "#ABC", name: "야옹이2", trophies: 200 });
    const recents = getRecents();
    expect(recents).toHaveLength(1);
    expect(recents[0].name).toEqual("야옹이2");
    expect(recents[0].trophies).toEqual(200);
  });

  it("최대 10개까지 보관", () => {
    for (let i = 0; i < 15; i++) {
      addRecent({ kind: "player", tag: `#TAG${i}`, name: `n${i}`, trophies: i });
    }
    expect(getRecents()).toHaveLength(10);
  });

  it("legacy 항목(name/trophies 없음)은 읽을 때 필터링", () => {
    window.localStorage.setItem(
      "imstargg.search.recents.v2",
      JSON.stringify([{ kind: "player", tag: "#OLD", addedAt: Date.now() }]),
    );
    expect(getRecents()).toEqual([]);
  });

  it("clearRecents 후 빈 배열", () => {
    addRecent({ kind: "player", tag: "#A", name: "n", trophies: 1 });
    clearRecents();
    expect(getRecents()).toEqual([]);
  });
});
```

- [ ] **Step 5: 테스트 실행 PASS 확인**

Run: `cd imstargg-frontend && pnpm test:run -- src/lib/__tests__/searchHistory.test.ts`
Expected: PASS

- [ ] **Step 6: typecheck + lint**

Run: `cd imstargg-frontend && pnpm typecheck && pnpm lint`
Expected: PASS (favorites.ts 호환 깨졌다면 여기서 빨간불 → 해결)

- [ ] **Step 7: 커밋**

```bash
cd imstargg-frontend
git add src/lib/searchHistory.ts src/lib/__tests__/searchHistory.test.ts
# favorites.ts 도 수정한 경우 같이 add
git commit -m "feat(frontend): bump search history to v2 with name/trophies"
```

---

## Task 11: Frontend — API 타입 + endpoints 검색 메서드 추가

**Files:**
- Modify: `imstargg-frontend/src/api/types.ts`
- Modify: `imstargg-frontend/src/api/endpoints/player.ts`
- Modify: `imstargg-frontend/src/api/endpoints/club.ts`

- [ ] **Step 1: types.ts 에 SliceResponse + 검색 응답 타입 추가**

`imstargg-frontend/src/api/types.ts` 의 `ListResponse` 정의 바로 아래(라인 6 직후)에 추가:

```typescript
export interface SliceResponse<T> {
  content: T[];
  hasNext: boolean;
}
```

> `PlayerSearchResponse`/`ClubSearchResponse` 는 별도 alias 가 필요하지 않다. Backend `SliceResponse<PlayerResponse>` 와 직접 매칭됨.

- [ ] **Step 2: player.ts 검색 메서드 추가**

`imstargg-frontend/src/api/endpoints/player.ts` 전체 교체:

```typescript
import { apiFetch, type FetchOpts } from "@/api/client";
import { toUrlPath } from "@/lib/tag";
import type {
  ListResponse,
  PlayerBrawlerResponse,
  PlayerResponse,
  SliceResponse,
} from "@/api/types";

export const playerApi = {
  get: (tag: string, opts?: FetchOpts) =>
    apiFetch<PlayerResponse>(`/v1/players/${toUrlPath(tag)}`, opts),

  getBrawlers: (tag: string, opts?: FetchOpts) =>
    apiFetch<ListResponse<PlayerBrawlerResponse>>(
      `/v1/players/${toUrlPath(tag)}/brawlers`,
      opts,
    ),

  search: (
    q: string,
    offset: number,
    limit: number,
    opts?: FetchOpts,
  ) =>
    apiFetch<SliceResponse<PlayerResponse>>(
      `/v1/player/search?q=${encodeURIComponent(q)}&offset=${offset}&limit=${limit}`,
      opts,
    ),
};
```

- [ ] **Step 3: club.ts 검색 메서드 추가**

`imstargg-frontend/src/api/endpoints/club.ts` 전체 교체:

```typescript
import { apiFetch, type FetchOpts } from "@/api/client";
import { toUrlPath } from "@/lib/tag";
import type { ClubResponse, SliceResponse } from "@/api/types";

export const clubApi = {
  get: (tag: string, opts?: FetchOpts) =>
    apiFetch<ClubResponse>(`/v1/clubs/${toUrlPath(tag)}`, opts),

  search: (
    q: string,
    offset: number,
    limit: number,
    opts?: FetchOpts,
  ) =>
    apiFetch<SliceResponse<ClubResponse>>(
      `/v1/club/search?q=${encodeURIComponent(q)}&offset=${offset}&limit=${limit}`,
      opts,
    ),
};
```

- [ ] **Step 4: typecheck**

Run: `cd imstargg-frontend && pnpm typecheck`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
cd imstargg-frontend
git add src/api/types.ts src/api/endpoints/player.ts src/api/endpoints/club.ts
git commit -m "feat(frontend): add player/club search API clients"
```

---

## Task 12: Frontend — SearchableInput 공용 컴포넌트

**Files:**
- Create: `imstargg-frontend/src/components/shared/SearchableInput.tsx`

- [ ] **Step 1: 컴포넌트 작성**

```tsx
"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Search } from "lucide-react";
import {
  addRecent,
  clearRecents,
  getRecents,
  type SearchEntry,
  type SearchEntryKind,
} from "@/lib/searchHistory";
import { getFavorites, isFavorite as readIsFavorite, toggleFavorite } from "@/lib/favorites";
import { SearchHistoryDropdown } from "@/components/shared/SearchHistoryDropdown";

const DEBOUNCE_MS = 250;
const MIN_QUERY_LEN = 2;
const LIVE_LIMIT = 5;

export interface SearchableInputResult {
  kind: SearchEntryKind;
  tag: string;
  name: string;
  trophies: number;
}

interface Props {
  kind: SearchEntryKind;
  placeholder: string;
  inputAriaLabel: string;
  buttonAriaLabel: string;
  defaultValue?: string;
  fetchLiveResults: (q: string) => Promise<SearchableInputResult[]>;
  renderResult: (item: SearchableInputResult, active: boolean) => React.ReactNode;
  toDetailHref: (item: SearchableInputResult) => string;
  toSearchHref: (q: string) => string;
}

export function SearchableInput({
  kind,
  placeholder,
  inputAriaLabel,
  buttonAriaLabel,
  defaultValue = "",
  fetchLiveResults,
  renderResult,
  toDetailHref,
  toSearchHref,
}: Props) {
  const router = useRouter();
  const [value, setValue] = useState(defaultValue);
  const [open, setOpen] = useState(false);
  const [recents, setRecents] = useState<SearchEntry[]>([]);
  const [favorites, setFavorites] = useState<SearchEntry[]>([]);
  const [liveResults, setLiveResults] = useState<SearchableInputResult[]>([]);
  const [activeIndex, setActiveIndex] = useState(-1);
  const wrapperRef = useRef<HTMLDivElement | null>(null);
  const abortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    setRecents(getRecents());
    setFavorites(getFavorites());
  }, []);

  useEffect(() => {
    function onMouseDown(e: MouseEvent) {
      if (!wrapperRef.current) return;
      if (!wrapperRef.current.contains(e.target as Node)) setOpen(false);
    }
    document.addEventListener("mousedown", onMouseDown);
    return () => document.removeEventListener("mousedown", onMouseDown);
  }, []);

  // 디바운스된 라이브 검색
  useEffect(() => {
    const trimmed = value.trim();
    if (trimmed.length < MIN_QUERY_LEN) {
      setLiveResults([]);
      return;
    }
    const handle = setTimeout(async () => {
      abortRef.current?.abort();
      const ctrl = new AbortController();
      abortRef.current = ctrl;
      try {
        const results = await fetchLiveResults(trimmed);
        if (!ctrl.signal.aborted) setLiveResults(results);
      } catch (err) {
        if (!(err instanceof DOMException && err.name === "AbortError")) {
          // 라이브 검색 실패는 조용히 무시 — 사용자는 Enter 로 결과 페이지 진입 가능
        }
      }
    }, DEBOUNCE_MS);
    return () => clearTimeout(handle);
  }, [value, fetchLiveResults]);

  // 활성 항목 초기화
  useEffect(() => {
    setActiveIndex(-1);
  }, [open, liveResults, recents, favorites]);

  function refresh() {
    setRecents(getRecents());
    setFavorites(getFavorites());
  }

  // 표출되는 항목: 입력값이 있으면 liveResults, 없으면 favorites + recents
  const showingLive = value.trim().length >= MIN_QUERY_LEN;
  const navItems: Array<{ entry: SearchableInputResult; href: string }> = showingLive
    ? liveResults.map((r) => ({ entry: r, href: toDetailHref(r) }))
    : [];

  function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const trimmed = value.trim();
    if (!trimmed) return;
    setOpen(false);
    router.push(toSearchHref(trimmed));
  }

  function handleSelectLive(item: SearchableInputResult) {
    addRecent({ kind: item.kind, tag: item.tag, name: item.name, trophies: item.trophies });
    setOpen(false);
    router.push(toDetailHref(item));
  }

  function handleToggleFavorite(entry: { kind: SearchEntryKind; tag: string }) {
    toggleFavorite(entry);
    refresh();
  }

  function handleClearRecents() {
    clearRecents();
    refresh();
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (!open) return;
    if (showingLive) {
      if (navItems.length === 0) return;
      if (e.key === "ArrowDown") {
        e.preventDefault();
        setActiveIndex((i) => (i + 1) % navItems.length);
      } else if (e.key === "ArrowUp") {
        e.preventDefault();
        setActiveIndex((i) => (i <= 0 ? navItems.length - 1 : i - 1));
      } else if (e.key === "Enter" && activeIndex >= 0) {
        e.preventDefault();
        handleSelectLive(navItems[activeIndex].entry);
      }
      return;
    }
    // history 모드 (즐겨찾기/최근검색) — 기존 키 동작 유지
    const flat = [...favorites, ...recents];
    if (flat.length === 0) return;
    if (e.key === "ArrowDown") {
      e.preventDefault();
      setActiveIndex((i) => (i + 1) % flat.length);
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setActiveIndex((i) => (i <= 0 ? flat.length - 1 : i - 1));
    } else if (e.key === "Enter" && activeIndex >= 0) {
      e.preventDefault();
      const entry = flat[activeIndex];
      const href =
        entry.kind === "player" ? `/players/${entry.tag.replace(/^#/, "")}` : `/clubs/${entry.tag.replace(/^#/, "")}`;
      setOpen(false);
      router.push(href);
    }
  }

  return (
    <div ref={wrapperRef} style={{ position: "relative", width: "100%", maxWidth: 448 }}>
      <form onSubmit={handleSubmit} className="flex w-full items-center gap-2">
        <Input
          type="text"
          inputMode="text"
          spellCheck={false}
          placeholder={placeholder}
          value={value}
          onChange={(e) => setValue(e.target.value)}
          onFocus={() => setOpen(true)}
          onKeyDown={handleKeyDown}
          aria-label={inputAriaLabel}
          role="combobox"
          aria-expanded={open}
          aria-haspopup="menu"
          aria-controls="search-dropdown"
        />
        <Button type="submit" size="default" aria-label={buttonAriaLabel}>
          <Search className="size-4" />
        </Button>
      </form>
      {open && (
        <div id="search-dropdown">
          {showingLive ? (
            <LiveResultsDropdown
              items={navItems}
              activeIndex={activeIndex}
              renderResult={renderResult}
              onSelect={handleSelectLive}
            />
          ) : (
            <SearchHistoryDropdown
              recents={recents}
              favorites={favorites}
              activeIndex={activeIndex}
              onSelect={() => setOpen(false)}
              onToggleFavorite={handleToggleFavorite}
              onClearRecents={handleClearRecents}
              isFavorite={readIsFavorite}
            />
          )}
        </div>
      )}
    </div>
  );
}

function LiveResultsDropdown({
  items,
  activeIndex,
  renderResult,
  onSelect,
}: {
  items: Array<{ entry: SearchableInputResult; href: string }>;
  activeIndex: number;
  renderResult: (item: SearchableInputResult, active: boolean) => React.ReactNode;
  onSelect: (item: SearchableInputResult) => void;
}) {
  if (items.length === 0) {
    return (
      <div
        role="menu"
        style={{
          position: "absolute",
          top: "100%",
          left: 0,
          right: 0,
          marginTop: 4,
          background: "#fff",
          border: "1px solid #D8DCE5",
          borderRadius: 12,
          boxShadow: "0 12px 32px rgba(20,22,27,0.12)",
          padding: 12,
          color: "#8A90A0",
          fontSize: 13,
          zIndex: 50,
        }}
      >
        검색 결과가 없습니다
      </div>
    );
  }
  return (
    <ul
      role="menu"
      style={{
        position: "absolute",
        top: "100%",
        left: 0,
        right: 0,
        marginTop: 4,
        background: "#fff",
        border: "1px solid #D8DCE5",
        borderRadius: 12,
        boxShadow: "0 12px 32px rgba(20,22,27,0.12)",
        padding: 8,
        zIndex: 50,
        maxHeight: 360,
        overflowY: "auto",
        listStyle: "none",
        margin: 0,
      }}
    >
      {items.map((it, idx) => {
        const active = idx === activeIndex;
        return (
          <li key={`${it.entry.kind}:${it.entry.tag}`}>
            <button
              type="button"
              role="menuitem"
              onClick={() => onSelect(it.entry)}
              style={{
                display: "block",
                width: "100%",
                textAlign: "left",
                background: active ? "rgba(255,198,10,0.15)" : "transparent",
                border: "none",
                borderRadius: 6,
                padding: "6px 8px",
                cursor: "pointer",
              }}
            >
              {renderResult(it.entry, active)}
            </button>
          </li>
        );
      })}
    </ul>
  );
}
```

- [ ] **Step 2: typecheck**

Run: `cd imstargg-frontend && pnpm typecheck`
Expected: PASS

- [ ] **Step 3: 커밋**

```bash
cd imstargg-frontend
git add src/components/shared/SearchableInput.tsx
git commit -m "feat(frontend): add SearchableInput shared component with live search"
```

---

## Task 13: Frontend — PlayerSearchInput 리팩토링

**Files:**
- Modify: `imstargg-frontend/src/components/shared/PlayerSearchInput.tsx`

- [ ] **Step 1: SearchableInput wrapper 로 단순화**

전체 교체:

```tsx
"use client";

import { playerApi } from "@/api/endpoints/player";
import { Trophy } from "@/components/primitives/Trophy";
import { SearchableInput } from "@/components/shared/SearchableInput";
import type { SearchableInputResult } from "@/components/shared/SearchableInput";

const LIVE_LIMIT = 5;

async function fetchPlayers(q: string): Promise<SearchableInputResult[]> {
  const res = await playerApi.search(q, 0, LIVE_LIMIT);
  return res.content.map((p) => ({
    kind: "player",
    tag: p.tag,
    name: p.name,
    trophies: p.trophies,
  }));
}

function renderPlayerResult(item: SearchableInputResult, active: boolean) {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 13 }}>
      <span style={{ fontWeight: 700, color: "#14161B" }}>{item.name}</span>
      <span style={{ fontFamily: "var(--font-mono)", color: "#5C6271" }}>{item.tag}</span>
      <span style={{ marginLeft: "auto", display: "inline-flex", alignItems: "center", gap: 4 }}>
        <Trophy value={item.trophies} />
      </span>
    </div>
  );
}

export function PlayerSearchInput({ defaultValue = "" }: { defaultValue?: string }) {
  return (
    <SearchableInput
      kind="player"
      placeholder="플레이어 이름 또는 #태그"
      inputAriaLabel="플레이어 이름 또는 태그 입력"
      buttonAriaLabel="플레이어 검색"
      defaultValue={defaultValue}
      fetchLiveResults={fetchPlayers}
      renderResult={renderPlayerResult}
      toDetailHref={(item) => `/players/${item.tag.replace(/^#/, "")}`}
      toSearchHref={(q) => `/players/search?q=${encodeURIComponent(q)}`}
    />
  );
}
```

> `Trophy` 컴포넌트 import 경로/시그니처는 `src/components/primitives/Trophy` 의 실제 export 에 맞게 조정. 다른 export 명이면 grep 으로 확인.

- [ ] **Step 2: Trophy 컴포넌트 import 확인**

Run: `cd imstargg-frontend && grep -n "export.*Trophy" src/components/primitives/Trophy*.tsx`
Expected: 실제 export 명 확인 → 위 코드 수정

- [ ] **Step 3: typecheck**

Run: `cd imstargg-frontend && pnpm typecheck`
Expected: PASS

- [ ] **Step 4: 커밋**

```bash
cd imstargg-frontend
git add src/components/shared/PlayerSearchInput.tsx
git commit -m "refactor(frontend): rewire PlayerSearchInput on SearchableInput"
```

---

## Task 14: Frontend — ClubSearchInput 리팩토링

**Files:**
- Modify: `imstargg-frontend/app/clubs/_components/ClubSearchInput.tsx`

- [ ] **Step 1: SearchableInput wrapper 로 단순화**

전체 교체:

```tsx
"use client";

import { clubApi } from "@/api/endpoints/club";
import { Trophy } from "@/components/primitives/Trophy";
import { SearchableInput } from "@/components/shared/SearchableInput";
import type { SearchableInputResult } from "@/components/shared/SearchableInput";

const LIVE_LIMIT = 5;

async function fetchClubs(q: string): Promise<SearchableInputResult[]> {
  const res = await clubApi.search(q, 0, LIVE_LIMIT);
  return res.content.map((c) => ({
    kind: "club",
    tag: c.tag,
    name: c.name,
    trophies: c.trophies,
  }));
}

function renderClubResult(item: SearchableInputResult, active: boolean) {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 13 }}>
      <span style={{ fontWeight: 700, color: "#14161B" }}>{item.name}</span>
      <span style={{ fontFamily: "var(--font-mono)", color: "#5C6271" }}>{item.tag}</span>
      <span style={{ marginLeft: "auto", display: "inline-flex", alignItems: "center", gap: 4 }}>
        <Trophy value={item.trophies} />
      </span>
    </div>
  );
}

export function ClubSearchInput({ defaultValue = "" }: { defaultValue?: string }) {
  return (
    <SearchableInput
      kind="club"
      placeholder="클럽 이름 또는 #태그"
      inputAriaLabel="클럽 이름 또는 태그 입력"
      buttonAriaLabel="클럽 검색"
      defaultValue={defaultValue}
      fetchLiveResults={fetchClubs}
      renderResult={renderClubResult}
      toDetailHref={(item) => `/clubs/${item.tag.replace(/^#/, "")}`}
      toSearchHref={(q) => `/clubs/search?q=${encodeURIComponent(q)}`}
    />
  );
}
```

- [ ] **Step 2: typecheck**

Run: `cd imstargg-frontend && pnpm typecheck`
Expected: PASS

- [ ] **Step 3: 커밋**

```bash
cd imstargg-frontend
git add app/clubs/_components/ClubSearchInput.tsx
git commit -m "refactor(frontend): rewire ClubSearchInput on SearchableInput"
```

---

## Task 15: Frontend — SearchHistoryDropdown 항목 강화

**Files:**
- Modify: `imstargg-frontend/src/components/shared/SearchHistoryDropdown.tsx`

- [ ] **Step 1: 항목 렌더에 name/trophies 추가**

`SearchHistoryDropdown.tsx` 의 `<Link ...>` 안 내용을 다음으로 교체 (라인 125–145 부근):

```tsx
                <Link
                  href={entryHref(e)}
                  role="menuitem"
                  onClick={onSelect}
                  style={{
                    flex: 1,
                    display: "flex",
                    alignItems: "center",
                    gap: 8,
                    padding: "6px 8px",
                    borderRadius: 6,
                    color: "#14161B",
                    textDecoration: "none",
                    fontSize: 13,
                  }}
                >
                  <span
                    style={{
                      fontSize: 10,
                      fontWeight: 700,
                      color: "#5C6271",
                      textTransform: "uppercase",
                      minWidth: 36,
                    }}
                  >
                    {e.kind === "player" ? "PLAYER" : "CLUB"}
                  </span>
                  <span style={{ fontWeight: 700 }}>{e.name}</span>
                  <span style={{ fontFamily: "var(--font-mono)", color: "#5C6271" }}>
                    {e.tag}
                  </span>
                  <span
                    style={{
                      marginLeft: "auto",
                      fontFamily: "var(--font-num)",
                      fontWeight: 700,
                      color: "#14161B",
                      fontVariantNumeric: "tabular-nums",
                    }}
                  >
                    {e.trophies.toLocaleString()} 🏆
                  </span>
                </Link>
```

> 기존 `e.label` 분기는 v2 스키마에 없으므로 삭제 가능.

- [ ] **Step 2: typecheck**

Run: `cd imstargg-frontend && pnpm typecheck`
Expected: PASS

- [ ] **Step 3: 커밋**

```bash
cd imstargg-frontend
git add src/components/shared/SearchHistoryDropdown.tsx
git commit -m "feat(frontend): enrich SearchHistoryDropdown items with name and trophies"
```

---

## Task 16: Frontend — RenewalCard 컴포넌트 (신규)

**Files:**
- Create: `imstargg-frontend/src/components/shared/RenewalCard.tsx`

> 기존 `usePlayerRenewal` 훅 패턴을 그대로 차용. 검색 결과 페이지 하단에 노출되어 사용자가 직접 태그를 입력해 갱신을 트리거.

- [ ] **Step 1: 갱신 훅/엔드포인트 위치 확인**

Run: `cd imstargg-frontend && grep -rn "usePlayerRenewal\|useClubRenewal\|renewal" src/hooks src/api/endpoints | head -20`
Expected: 갱신 훅 위치와 시그니처 노출. 클럽 갱신 훅이 없다면 단순 POST 호출로 처리.

- [ ] **Step 2: RenewalCard 작성**

```tsx
"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { isValidTag } from "@/lib/tag";
import { toast } from "sonner";
import { apiFetch } from "@/api/client";

interface Props {
  kind: "player" | "club";
  defaultTag?: string;
}

export function RenewalCard({ kind, defaultTag = "" }: Props) {
  const router = useRouter();
  const [tag, setTag] = useState(defaultTag);
  const [submitting, setSubmitting] = useState(false);

  const segment = kind === "player" ? "players" : "clubs";
  const detailBase = kind === "player" ? "/players" : "/clubs";

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const trimmed = tag.trim();
    if (!isValidTag(trimmed)) {
      toast.error(`올바른 ${kind === "player" ? "플레이어" : "클럽"} 태그를 입력해 주세요. 예: #88J8Q9RPV`);
      return;
    }
    setSubmitting(true);
    try {
      await apiFetch(`/v1/${segment}/${encodeURIComponent(trimmed.replace(/^#/, ""))}/renewal`, {
        method: "POST",
      });
      toast.success("갱신 요청 접수 — 잠시 후 결과 페이지에서 확인하세요.");
      router.push(`${detailBase}/${trimmed.replace(/^#/, "")}`);
    } catch (err) {
      toast.error("갱신 요청 실패 — 잠시 후 다시 시도해 주세요.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section
      style={{
        background: "#FFFFFF",
        border: "1px solid #D8DCE5",
        borderRadius: 12,
        padding: 18,
        marginTop: 24,
      }}
    >
      <h2 style={{ margin: 0, fontSize: 15, fontWeight: 700 }}>원하는 결과가 없나요?</h2>
      <p style={{ margin: "6px 0 12px", fontSize: 13, color: "#5C6271" }}>
        태그를 입력해 {kind === "player" ? "플레이어" : "클럽"} 정보를 직접 갱신할 수 있습니다.
      </p>
      <form onSubmit={handleSubmit} className="flex w-full max-w-md items-center gap-2">
        <Input
          type="text"
          inputMode="text"
          spellCheck={false}
          placeholder={kind === "player" ? "#PLAYER_TAG" : "#CLUB_TAG"}
          value={tag}
          onChange={(e) => setTag(e.target.value)}
          aria-label={kind === "player" ? "플레이어 태그 입력" : "클럽 태그 입력"}
          disabled={submitting}
        />
        <Button type="submit" size="default" disabled={submitting}>
          {submitting ? "요청 중…" : "갱신"}
        </Button>
      </form>
    </section>
  );
}
```

- [ ] **Step 3: typecheck**

Run: `cd imstargg-frontend && pnpm typecheck`
Expected: PASS

- [ ] **Step 4: 커밋**

```bash
cd imstargg-frontend
git add src/components/shared/RenewalCard.tsx
git commit -m "feat(frontend): add RenewalCard for search result pages"
```

---

## Task 17: Frontend — /players/search 페이지

**Files:**
- Create: `imstargg-frontend/app/players/search/page.tsx`
- Create: `imstargg-frontend/app/players/search/_components/PlayerSearchResults.tsx`

- [ ] **Step 1: 클라이언트 결과 컴포넌트 작성**

`imstargg-frontend/app/players/search/_components/PlayerSearchResults.tsx`:

```tsx
"use client";

import { useState } from "react";
import Link from "next/link";
import { playerApi } from "@/api/endpoints/player";
import type { PlayerResponse, SliceResponse } from "@/api/types";
import { Button } from "@/components/ui/button";
import { addRecent } from "@/lib/searchHistory";
import { Trophy } from "@/components/primitives/Trophy";

const PAGE_SIZE = 20;

interface Props {
  query: string;
  initial: SliceResponse<PlayerResponse>;
}

export function PlayerSearchResults({ query, initial }: Props) {
  const [items, setItems] = useState<PlayerResponse[]>(initial.content);
  const [hasNext, setHasNext] = useState(initial.hasNext);
  const [offset, setOffset] = useState(initial.content.length);
  const [loading, setLoading] = useState(false);

  async function loadMore() {
    setLoading(true);
    try {
      const next = await playerApi.search(query, offset, PAGE_SIZE);
      setItems((prev) => [...prev, ...next.content]);
      setHasNext(next.hasNext);
      setOffset(offset + next.content.length);
    } finally {
      setLoading(false);
    }
  }

  if (items.length === 0) {
    return (
      <p style={{ color: "#5C6271", padding: "12px 0" }}>
        “{query}” 검색 결과가 없습니다.
      </p>
    );
  }

  return (
    <div>
      <ul style={{ listStyle: "none", padding: 0, margin: 0, display: "grid", gap: 8 }}>
        {items.map((p) => (
          <li
            key={p.tag}
            style={{
              background: "#FFFFFF",
              border: "1px solid #D8DCE5",
              borderRadius: 12,
            }}
          >
            <Link
              href={`/players/${p.tag.replace(/^#/, "")}`}
              onClick={() =>
                addRecent({ kind: "player", tag: p.tag, name: p.name, trophies: p.trophies })
              }
              style={{
                display: "flex",
                alignItems: "center",
                gap: 12,
                padding: "12px 16px",
                color: "#14161B",
                textDecoration: "none",
              }}
            >
              <span style={{ fontWeight: 700 }}>{p.name}</span>
              <span style={{ fontFamily: "var(--font-mono)", color: "#5C6271" }}>{p.tag}</span>
              {p.club && (
                <span style={{ color: "#8A90A0", fontSize: 12 }}>· {p.club.name}</span>
              )}
              <span style={{ marginLeft: "auto" }}>
                <Trophy value={p.trophies} />
              </span>
            </Link>
          </li>
        ))}
      </ul>
      {hasNext && (
        <div style={{ display: "flex", justifyContent: "center", marginTop: 16 }}>
          <Button type="button" onClick={loadMore} disabled={loading}>
            {loading ? "불러오는 중…" : "더보기"}
          </Button>
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 2: 서버 페이지 작성**

`imstargg-frontend/app/players/search/page.tsx`:

```tsx
import type { Metadata } from "next";
import { playerApi } from "@/api/endpoints/player";
import { PlayerSearchInput } from "@/components/shared/PlayerSearchInput";
import { RenewalCard } from "@/components/shared/RenewalCard";
import { buildMetadata } from "@/lib/seo";
import { PlayerSearchResults } from "./_components/PlayerSearchResults";

type SearchParams = Promise<{ q?: string }>;

export async function generateMetadata({
  searchParams,
}: {
  searchParams: SearchParams;
}): Promise<Metadata> {
  const q = (await searchParams).q ?? "";
  return buildMetadata({
    title: q ? `“${q}” 플레이어 검색 결과 | ImStarGG` : "플레이어 검색 | ImStarGG",
    description: "브롤스타즈 플레이어 이름 또는 태그로 검색합니다.",
    path: `/players/search`,
    noindex: true,
  });
}

export default async function Page({
  searchParams,
}: {
  searchParams: SearchParams;
}) {
  const q = ((await searchParams).q ?? "").trim();
  const initial = q
    ? await playerApi.search(q, 0, 20, { cache: "no-store" })
    : { content: [], hasNext: false };
  const isTagQuery = q.startsWith("#");

  return (
    <main className="mx-auto w-full max-w-2xl px-4 py-10">
      <div className="mb-6">
        <h1 className="m-0 font-display text-3xl tracking-[0.02em]">플레이어 검색</h1>
        <p className="mt-1.5 text-sm text-muted-foreground">
          이름 또는 #태그로 검색하세요.
        </p>
      </div>
      <PlayerSearchInput defaultValue={q} />
      <div style={{ marginTop: 20 }}>
        {q && <PlayerSearchResults query={q} initial={initial} />}
      </div>
      <RenewalCard kind="player" defaultTag={isTagQuery ? q : ""} />
    </main>
  );
}
```

- [ ] **Step 3: typecheck + lint**

Run: `cd imstargg-frontend && pnpm typecheck && pnpm lint`
Expected: PASS

- [ ] **Step 4: 커밋**

```bash
cd imstargg-frontend
git add app/players/search/page.tsx app/players/search/_components/PlayerSearchResults.tsx
git commit -m "feat(frontend): add /players/search results page"
```

---

## Task 18: Frontend — /clubs/search 페이지

**Files:**
- Create: `imstargg-frontend/app/clubs/search/page.tsx`
- Create: `imstargg-frontend/app/clubs/search/_components/ClubSearchResults.tsx`

- [ ] **Step 1: 클라이언트 결과 컴포넌트 작성**

`imstargg-frontend/app/clubs/search/_components/ClubSearchResults.tsx`:

```tsx
"use client";

import { useState } from "react";
import Link from "next/link";
import { clubApi } from "@/api/endpoints/club";
import type { ClubResponse, SliceResponse } from "@/api/types";
import { Button } from "@/components/ui/button";
import { addRecent } from "@/lib/searchHistory";
import { Trophy } from "@/components/primitives/Trophy";

const PAGE_SIZE = 20;

interface Props {
  query: string;
  initial: SliceResponse<ClubResponse>;
}

export function ClubSearchResults({ query, initial }: Props) {
  const [items, setItems] = useState<ClubResponse[]>(initial.content);
  const [hasNext, setHasNext] = useState(initial.hasNext);
  const [offset, setOffset] = useState(initial.content.length);
  const [loading, setLoading] = useState(false);

  async function loadMore() {
    setLoading(true);
    try {
      const next = await clubApi.search(query, offset, PAGE_SIZE);
      setItems((prev) => [...prev, ...next.content]);
      setHasNext(next.hasNext);
      setOffset(offset + next.content.length);
    } finally {
      setLoading(false);
    }
  }

  if (items.length === 0) {
    return (
      <p style={{ color: "#5C6271", padding: "12px 0" }}>
        “{query}” 검색 결과가 없습니다.
      </p>
    );
  }

  return (
    <div>
      <ul style={{ listStyle: "none", padding: 0, margin: 0, display: "grid", gap: 8 }}>
        {items.map((c) => (
          <li
            key={c.tag}
            style={{
              background: "#FFFFFF",
              border: "1px solid #D8DCE5",
              borderRadius: 12,
            }}
          >
            <Link
              href={`/clubs/${c.tag.replace(/^#/, "")}`}
              onClick={() =>
                addRecent({ kind: "club", tag: c.tag, name: c.name, trophies: c.trophies })
              }
              style={{
                display: "flex",
                alignItems: "center",
                gap: 12,
                padding: "12px 16px",
                color: "#14161B",
                textDecoration: "none",
              }}
            >
              <span style={{ fontWeight: 700 }}>{c.name}</span>
              <span style={{ fontFamily: "var(--font-mono)", color: "#5C6271" }}>{c.tag}</span>
              <span style={{ color: "#8A90A0", fontSize: 12 }}>· 멤버 {c.members.length}명</span>
              <span style={{ marginLeft: "auto" }}>
                <Trophy value={c.trophies} />
              </span>
            </Link>
          </li>
        ))}
      </ul>
      {hasNext && (
        <div style={{ display: "flex", justifyContent: "center", marginTop: 16 }}>
          <Button type="button" onClick={loadMore} disabled={loading}>
            {loading ? "불러오는 중…" : "더보기"}
          </Button>
        </div>
      )}
    </div>
  );
}
```

- [ ] **Step 2: 서버 페이지 작성**

`imstargg-frontend/app/clubs/search/page.tsx`:

```tsx
import type { Metadata } from "next";
import { clubApi } from "@/api/endpoints/club";
import { ClubSearchInput } from "@/app/clubs/_components/ClubSearchInput";
import { RenewalCard } from "@/components/shared/RenewalCard";
import { buildMetadata } from "@/lib/seo";
import { ClubSearchResults } from "./_components/ClubSearchResults";

type SearchParams = Promise<{ q?: string }>;

export async function generateMetadata({
  searchParams,
}: {
  searchParams: SearchParams;
}): Promise<Metadata> {
  const q = (await searchParams).q ?? "";
  return buildMetadata({
    title: q ? `“${q}” 클럽 검색 결과 | ImStarGG` : "클럽 검색 | ImStarGG",
    description: "브롤스타즈 클럽 이름 또는 태그로 검색합니다.",
    path: `/clubs/search`,
    noindex: true,
  });
}

export default async function Page({
  searchParams,
}: {
  searchParams: SearchParams;
}) {
  const q = ((await searchParams).q ?? "").trim();
  const initial = q
    ? await clubApi.search(q, 0, 20, { cache: "no-store" })
    : { content: [], hasNext: false };
  const isTagQuery = q.startsWith("#");

  return (
    <main className="mx-auto w-full max-w-2xl px-4 py-10">
      <div className="mb-6">
        <h1 className="m-0 font-display text-3xl tracking-[0.02em]">클럽 검색</h1>
        <p className="mt-1.5 text-sm text-muted-foreground">
          이름 또는 #태그로 검색하세요.
        </p>
      </div>
      <ClubSearchInput defaultValue={q} />
      <div style={{ marginTop: 20 }}>
        {q && <ClubSearchResults query={q} initial={initial} />}
      </div>
      <RenewalCard kind="club" defaultTag={isTagQuery ? q : ""} />
    </main>
  );
}
```

> `@/app/clubs/_components/ClubSearchInput` import 경로가 tsconfig 의 alias 설정과 일치하는지 확인. 기존 코드에서 사용하던 import 경로 (예: `@app/clubs/...`) 와 일치시킬 것.

- [ ] **Step 3: import alias 확인 및 보정**

Run: `cd imstargg-frontend && grep -n '"paths"' tsconfig.json`
Expected: `@app/*` 또는 `@/app/*` 등 alias 확인 → 코드 수정

- [ ] **Step 4: typecheck + lint**

Run: `cd imstargg-frontend && pnpm typecheck && pnpm lint`
Expected: PASS

- [ ] **Step 5: 커밋**

```bash
cd imstargg-frontend
git add app/clubs/search/page.tsx app/clubs/search/_components/ClubSearchResults.tsx
git commit -m "feat(frontend): add /clubs/search results page"
```

---

## Task 19: Frontend — /players 신설 (검색창 + 랭킹)

**Files:**
- Create: `imstargg-frontend/app/players/page.tsx`

- [ ] **Step 1: 페이지 작성**

```tsx
import type { Metadata } from "next";
import { rankingApi } from "@/api/endpoints/ranking";
import { PlayerSearchInput } from "@/components/shared/PlayerSearchInput";
import { COUNTRY_LABELS, parseCountry } from "@/domain/ranking/country";
import { toPlayerRanking } from "@/domain/ranking/mappers";
import { buildMetadata } from "@/lib/seo";
import { CountrySelector } from "@/app/ranking/_components/CountrySelector";
import { PlayerRankingList } from "@/app/ranking/_components/PlayerRankingList";

type SearchParams = Promise<{ country?: string }>;

export async function generateMetadata({
  searchParams,
}: {
  searchParams: SearchParams;
}): Promise<Metadata> {
  const country = parseCountry((await searchParams).country);
  const label = COUNTRY_LABELS[country];
  return buildMetadata({
    title: `${label} 플레이어 랭킹 | ImStarGG`,
    description: `브롤스타즈 ${label} 플레이어 트로피 랭킹을 확인하세요.`,
    path: "/players",
  });
}

export default async function Page({
  searchParams,
}: {
  searchParams: SearchParams;
}) {
  const country = parseCountry((await searchParams).country);
  const dto = await rankingApi.players(country, { cache: "no-store" });
  const rankings = dto.content.map(toPlayerRanking);

  return (
    <main className="mx-auto w-full max-w-5xl px-4 py-10">
      <div className="mb-6">
        <h1 className="m-0 font-display text-3xl tracking-[0.02em]">플레이어</h1>
        <p className="mt-1.5 text-sm text-muted-foreground">
          이름 또는 #태그로 검색하세요.
        </p>
      </div>
      <PlayerSearchInput />
      <section className="mt-10 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-2xl font-bold">
            {COUNTRY_LABELS[country]} 플레이어 랭킹
          </h2>
          <CountrySelector current={country} />
        </div>
        <PlayerRankingList rankings={rankings} />
      </section>
    </main>
  );
}
```

> `CountrySelector` / `PlayerRankingList` import 경로는 task 22 에서 `/app/ranking/_components/*` 가 `/src/components/ranking/*` 로 이동될 예정. 지금은 기존 경로 사용 후 task 22 에서 한꺼번에 보정.

- [ ] **Step 2: typecheck + lint**

Run: `cd imstargg-frontend && pnpm typecheck && pnpm lint`
Expected: PASS

- [ ] **Step 3: 커밋**

```bash
cd imstargg-frontend
git add app/players/page.tsx
git commit -m "feat(frontend): add /players page with search and ranking"
```

---

## Task 20: Frontend — /clubs 페이지에 랭킹 섹션 추가

**Files:**
- Modify: `imstargg-frontend/app/clubs/page.tsx`
- Modify: `imstargg-frontend/app/clubs/_components/ClubSearchPage.tsx`

- [ ] **Step 1: ClubSearchPage 를 서버 컴포넌트로 옮기거나 props 로 랭킹 받기**

ClubSearchPage 가 클라이언트일 필요는 없음 (검색창만 client). 단순화 위해 `app/clubs/page.tsx` 에 랭킹 섹션을 직접 추가하고, `ClubSearchPage` 는 폐기하거나 단순 ClubSearchInput wrapper 만 남김.

`imstargg-frontend/app/clubs/page.tsx` 전체 교체:

```tsx
import type { Metadata } from "next";
import { rankingApi } from "@/api/endpoints/ranking";
import { ClubSearchInput } from "@/app/clubs/_components/ClubSearchInput";
import { COUNTRY_LABELS, parseCountry } from "@/domain/ranking/country";
import { toClubRanking } from "@/domain/ranking/mappers";
import { buildMetadata } from "@/lib/seo";
import { CountrySelector } from "@/app/ranking/_components/CountrySelector";
import { ClubRankingList } from "@/app/ranking/_components/ClubRankingList";

type SearchParams = Promise<{ country?: string }>;

export async function generateMetadata({
  searchParams,
}: {
  searchParams: SearchParams;
}): Promise<Metadata> {
  const country = parseCountry((await searchParams).country);
  const label = COUNTRY_LABELS[country];
  return buildMetadata({
    title: `${label} 클럽 랭킹 | ImStarGG`,
    description: `브롤스타즈 ${label} 클럽 트로피 랭킹과 클럽 검색.`,
    path: "/clubs",
  });
}

export default async function Page({
  searchParams,
}: {
  searchParams: SearchParams;
}) {
  const country = parseCountry((await searchParams).country);
  const dto = await rankingApi.clubs(country, { cache: "no-store" });
  const rankings = dto.content.map(toClubRanking);

  return (
    <main className="mx-auto w-full max-w-5xl px-4 py-10">
      <div className="mb-6">
        <h1 className="m-0 font-display text-3xl tracking-[0.02em]">클럽</h1>
        <p className="mt-1.5 text-sm text-muted-foreground">
          이름 또는 #태그로 검색하세요.
        </p>
      </div>
      <ClubSearchInput />
      <section className="mt-10 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-2xl font-bold">
            {COUNTRY_LABELS[country]} 클럽 랭킹
          </h2>
          <CountrySelector current={country} />
        </div>
        <ClubRankingList rankings={rankings} />
      </section>
    </main>
  );
}
```

- [ ] **Step 2: 더 이상 사용되지 않는 ClubSearchPage 삭제**

Run: `cd imstargg-frontend && rm app/clubs/_components/ClubSearchPage.tsx`
Expected: OK

- [ ] **Step 3: typecheck + lint**

Run: `cd imstargg-frontend && pnpm typecheck && pnpm lint`
Expected: PASS

- [ ] **Step 4: 커밋**

```bash
cd imstargg-frontend
git add app/clubs/page.tsx app/clubs/_components/ClubSearchPage.tsx
git commit -m "feat(frontend): integrate club ranking into /clubs page"
```

---

## Task 21: Frontend — 브롤러 상세 페이지에 랭킹 탭 추가

**Files:**
- Modify: `imstargg-frontend/app/brawlers/[id]/_components/BrawlerContent.tsx`
- Create: `imstargg-frontend/app/brawlers/[id]/_components/BrawlerRankingTab.tsx`

- [ ] **Step 1: 랭킹 탭 컴포넌트 작성**

`BrawlerRankingTab.tsx`:

```tsx
"use client";

import { useQuery } from "@tanstack/react-query";
import { rankingApi } from "@/api/endpoints/ranking";
import { CountrySelector } from "@/app/ranking/_components/CountrySelector";
import { BrawlerRankingList } from "@/app/ranking/_components/BrawlerRankingList";
import { parseCountry } from "@/domain/ranking/country";
import { toBrawlerRanking } from "@/domain/ranking/mappers";
import { useSearchParams } from "next/navigation";

interface Props {
  brawlerId: number;
}

export function BrawlerRankingTab({ brawlerId }: Props) {
  const searchParams = useSearchParams();
  const country = parseCountry(searchParams.get("country") ?? undefined);

  const { data, isLoading } = useQuery({
    queryKey: ["brawler-ranking", brawlerId, country],
    queryFn: async () => {
      const res = await rankingApi.brawlers(brawlerId, country, { cache: "no-store" });
      return res.content.map(toBrawlerRanking);
    },
  });

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-end">
        <CountrySelector current={country} />
      </div>
      {isLoading ? (
        <p style={{ color: "#5C6271" }}>로딩 중…</p>
      ) : (
        <BrawlerRankingList rankings={data ?? []} />
      )}
    </div>
  );
}
```

- [ ] **Step 2: BrawlerContent 에 Tabs 도입**

`BrawlerContent.tsx` 전체 교체:

```tsx
"use client";

import type { BrawlerWithItems } from "@/domain/brawler/types";
import type { BrawlerTrophyRange } from "@/domain/statistics/enums";
import type { ItemOwnership } from "@/domain/statistics/types";
import { useBrawler } from "@/hooks/queries/useBrawler";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { BrawlerHero } from "./BrawlerHero";
import { BrawlerItems } from "./BrawlerItems";
import { BrawlerRankingTab } from "./BrawlerRankingTab";

type Props = {
  initialDetail: BrawlerWithItems;
  ownership: ItemOwnership;
  trophy: BrawlerTrophyRange;
};

export function BrawlerContent({ initialDetail, ownership, trophy }: Props) {
  const { data: detail = initialDetail } = useBrawler(initialDetail.brawler.id, {
    initialData: initialDetail,
  });
  return (
    <section className="py-6 space-y-5">
      <BrawlerHero brawler={detail.brawler} />
      <Tabs defaultValue="overview">
        <TabsList>
          <TabsTrigger value="overview">상세</TabsTrigger>
          <TabsTrigger value="ranking">랭킹</TabsTrigger>
        </TabsList>
        <TabsContent value="overview">
          <BrawlerItems items={detail.items} ownership={ownership} trophy={trophy} />
        </TabsContent>
        <TabsContent value="ranking">
          <BrawlerRankingTab brawlerId={detail.brawler.id} />
        </TabsContent>
      </Tabs>
    </section>
  );
}
```

- [ ] **Step 3: typecheck + lint**

Run: `cd imstargg-frontend && pnpm typecheck && pnpm lint`
Expected: PASS

- [ ] **Step 4: dev server 로 시각 확인 (사용자가 직접)**

Run: `cd imstargg-frontend && pnpm dev`
브라우저 `/brawlers/{any-id}` 접속 → "상세"/"랭킹" 탭 동작 확인.

- [ ] **Step 5: 커밋**

```bash
cd imstargg-frontend
git add app/brawlers/[id]/_components/BrawlerContent.tsx \
        app/brawlers/[id]/_components/BrawlerRankingTab.tsx
git commit -m "feat(frontend): add ranking tab to brawler detail page"
```

---

## Task 22: Frontend — /ranking/* 삭제 + 랭킹 컴포넌트 이전 + 네비게이션 수정

**Files:**
- Move: `imstargg-frontend/app/ranking/_components/*` → `imstargg-frontend/src/components/ranking/*`
- Delete: `imstargg-frontend/app/ranking/` 전체
- Modify: `imstargg-frontend/src/components/layout/TopNav.tsx`
- Modify: 위에서 만든 `app/players/page.tsx`, `app/clubs/page.tsx`, `app/brawlers/[id]/_components/BrawlerRankingTab.tsx` 의 import 경로

- [ ] **Step 1: 랭킹 컴포넌트를 공용 위치로 이동**

```bash
cd imstargg-frontend
mkdir -p src/components/ranking
git mv app/ranking/_components/CountrySelector.tsx src/components/ranking/CountrySelector.tsx
git mv app/ranking/_components/PlayerRankingList.tsx src/components/ranking/PlayerRankingList.tsx
git mv app/ranking/_components/ClubRankingList.tsx src/components/ranking/ClubRankingList.tsx
git mv app/ranking/_components/BrawlerRankingList.tsx src/components/ranking/BrawlerRankingList.tsx
git mv app/ranking/_components/RankBadge.tsx src/components/ranking/RankBadge.tsx
```

> `RankingTabs.tsx` 는 더 이상 필요 없으므로 이동하지 않고 다음 스텝에서 삭제.

- [ ] **Step 2: 이동한 컴포넌트들의 내부 import 경로 보정**

Run: `cd imstargg-frontend && grep -l "@app/ranking/_components\|@/app/ranking/_components" src app -r`
이동된 컴포넌트의 모든 참조를 새 경로 `@/components/ranking/*` 로 일괄 교체.

대상 파일 (이번 plan 으로 생성/수정된 것들):
- `app/players/page.tsx`
- `app/clubs/page.tsx`
- `app/brawlers/[id]/_components/BrawlerRankingTab.tsx`

각 파일에서 `@/app/ranking/_components/...` → `@/components/ranking/...` 교체.

이동된 컴포넌트 내부에서도 서로 import 가 있다면 동일하게 보정.

- [ ] **Step 3: /ranking 디렉토리 전체 삭제**

```bash
cd imstargg-frontend
git rm -r app/ranking
```

- [ ] **Step 4: TopNav.tsx NAV_ITEMS 수정**

`imstargg-frontend/src/components/layout/TopNav.tsx` 의 라인 36 영역:

```tsx
const NAV_ITEMS: NavItem[] = [
  { href: "/brawlers", label: "브롤러" },
  { href: "/events", label: "이벤트" },
  { href: "/players", label: "플레이어" },
  { href: "/clubs", label: "클럽" },
];
```

- [ ] **Step 5: 사이트맵 / robots / 기타 ranking 참조 확인**

Run: `cd imstargg-frontend && grep -rn "/ranking" app src --include="*.ts" --include="*.tsx"`
Expected: 잔존 참조 노출. `app/sitemap.ts`, `app/_components/*` 등에 `/ranking/...` 링크가 있다면 새 경로로 교체:
- `/ranking/players` → `/players`
- `/ranking/clubs` → `/clubs`
- `/ranking/brawlers` → `/brawlers` (브롤러 일람), `/ranking/brawlers/${id}` → `/brawlers/${id}` (랭킹 탭은 fragment 또는 default tab — UX 결정 필요. 단순화 위해 `/brawlers/${id}` 로 보내고 사용자가 탭 클릭).

- [ ] **Step 6: typecheck + lint**

Run: `cd imstargg-frontend && pnpm typecheck && pnpm lint`
Expected: PASS

- [ ] **Step 7: 전체 vitest 실행**

Run: `cd imstargg-frontend && pnpm test:run`
Expected: PASS

- [ ] **Step 8: 커밋**

```bash
cd imstargg-frontend
git add -A
git commit -m "feat(frontend): retire /ranking routes, relocate ranking components, update nav"
```

---

## Task 23: 시각 검증 (사용자 직접 수행)

**Files:** (없음)

- [ ] **Step 1: dev 서버 기동**

Run: `cd imstargg-frontend && pnpm dev`

- [ ] **Step 2: 시나리오 점검**

브라우저에서 확인:
1. `/players` — 플레이어 검색창 + 트로피 랭킹 표시
2. `/clubs` — 클럽 검색창 + 클럽 랭킹 표시
3. `/players` 검색창에 `야` 입력 → 250ms 후 라이브 드롭다운 노출, ↑↓로 항목 이동, Enter 시 상세 페이지 이동
4. `/players` 검색창에 `Enter` (선택 항목 없이) → `/players/search?q=야` 이동, 결과 + 갱신 카드 노출
5. `/players/search?q=%23ABC` — `#`으로 시작하는 검색 → 갱신 카드의 태그 입력에 `#ABC` 자동 채움 확인
6. 결과 항목 클릭 → 검색 내역에 이름/태그/트로피 표시 (검색창 재포커스로 확인)
7. 검색 결과 0건 → "검색 결과가 없습니다" + 갱신 카드 노출
8. `/brawlers/{id}` — "상세"/"랭킹" 탭 전환 확인
9. 네비게이션 메뉴 — "랭킹" 삭제 / "플레이어" 추가 반영
10. `/ranking/players` 등 직접 접속 → 404 (의도된 동작)

> 비주얼 회귀나 컴포넌트 깨짐 발견 시 해당 파일로 돌아가 inline 수정 후 커밋.

---

## Task 24: 릴리즈 노트 / 버전 bump

**Files:** (release-management 스킬이 자동 처리)

- [ ] **Step 1: 사용자에게 변경 요약 전달 + bump 의사 확인**

> "core-api / imstargg-frontend 둘 다 변경됐어. minor bump 해도 돼?" 라고 묻고 사용자 승인 후 release-management 스킬 자동 트리거.

- [ ] **Step 2: bump 결과 확인 (스킬이 처리)**

Run: `git log --oneline -5`
Expected: 두 앱의 버전 bump 커밋이 생성

---

## 자체 점검 (구현자가 시작 전에 한 번 읽기)

1. **태그 검색 정규화**: `q.trim()` 후 `#` 시작 여부만 본다. BrawlStarsTag 생성자가 정규화를 책임지므로 추가 보정 불필요.
2. **인덱스 사용 검증**: 운영 적용 후 `EXPLAIN ANALYZE select * from player where lower(name) like '야%' order by trophies desc, brawlstars_tag asc limit 21;` 로 인덱스 hit 확인. plan 외 운영 작업.
3. **ClubResponse 페이로드 부담**: `ClubRepository.searchByName` 은 클럽마다 멤버 쿼리를 별도로 발사. limit=20 이면 추가 20쿼리 — 운영 모니터링 필요. 디자인 doc 의 위험 섹션 참조. 이번 plan 에서 N+1 해소는 안 한다 (스펙 결정사항).
4. **frontend 컨테이너 max-width**: `/players`, `/clubs` 모두 `max-w-5xl` (랭킹 테이블이 있어 검색 페이지보다 넓음). 검색 결과 페이지는 `max-w-2xl`.
5. **CountrySelector / BrawlerRankingList 의 client/server 경계**: 기존 `/ranking/*` 페이지에서 서버 컴포넌트 안에서 사용되었으므로 이동 후에도 동일하게 동작. `BrawlerRankingTab` 은 client (`useQuery` 사용) — country 변경 시 URL 의 `?country=` 파라미터로 리렌더.
