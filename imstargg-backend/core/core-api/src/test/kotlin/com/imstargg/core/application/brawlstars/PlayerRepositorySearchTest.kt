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
            playerJpaRepository.save(
                aPlayerEntity()
                    .withBrawlStarsTag("#A")
                    .withName("야옹이")
                    .withTrophies(100)
                    .build(),
            )
            playerJpaRepository.save(
                aPlayerEntity()
                    .withBrawlStarsTag("#B")
                    .withName("야옹이짱")
                    .withTrophies(500)
                    .build(),
            )
            playerJpaRepository.save(
                aPlayerEntity()
                    .withBrawlStarsTag("#C")
                    .withName("멍멍이")
                    .withTrophies(300)
                    .build(),
            )

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
            playerJpaRepository.save(
                aPlayerEntity()
                    .withBrawlStarsTag("#A")
                    .withName("Alpha")
                    .withTrophies(100)
                    .build(),
            )

            // when
            val result = sut.searchByName("alp", OffsetLimit(offset = 0, limit = 10))

            // then
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].name).isEqualTo("Alpha")
        }

        @Test
        fun `limit 경계 - hasNext true`() {
            // given
            playerJpaRepository.save(
                aPlayerEntity()
                    .withBrawlStarsTag("#A")
                    .withName("야옹이1")
                    .withTrophies(100)
                    .build(),
            )
            playerJpaRepository.save(
                aPlayerEntity()
                    .withBrawlStarsTag("#B")
                    .withName("야옹이2")
                    .withTrophies(200)
                    .build(),
            )
            playerJpaRepository.save(
                aPlayerEntity()
                    .withBrawlStarsTag("#C")
                    .withName("야옹이3")
                    .withTrophies(300)
                    .build(),
            )

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
