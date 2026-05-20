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
        fun `tag prefix 매칭 - 트로피 desc 정렬`() {
            // given
            playerJpaRepository.save(
                aPlayerEntity()
                    .withBrawlStarsTag("#ABC")
                    .withName("야옹이")
                    .withTrophies(100)
                    .build(),
            )
            playerJpaRepository.save(
                aPlayerEntity()
                    .withBrawlStarsTag("#ABD")
                    .withName("야옹이짱")
                    .withTrophies(500)
                    .build(),
            )
            playerJpaRepository.save(
                aPlayerEntity()
                    .withBrawlStarsTag("#XYZ")
                    .withName("멍멍이")
                    .withTrophies(300)
                    .build(),
            )

            // when
            val result = sut.searchByTag(BrawlStarsTag("#AB"), OffsetLimit(offset = 0, limit = 10))

            // then
            assertThat(result.content).hasSize(2)
            assertThat(result.content[0].tag.value).isEqualTo("#ABD")
            assertThat(result.content[1].tag.value).isEqualTo("#ABC")
            assertThat(result.hasNext).isFalse
        }

        @Test
        fun `매칭되는 tag 없음 - 빈 Slice`() {
            // given
            playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#XYZ").withName("멍멍이").build())

            // when
            val result = sut.searchByTag(BrawlStarsTag("#NONE"), OffsetLimit(offset = 0, limit = 10))

            // then
            assertThat(result.content).isEmpty()
            assertThat(result.hasNext).isFalse
        }

        @Test
        fun `limit 경계 - hasNext true`() {
            // given
            playerJpaRepository.save(
                aPlayerEntity().withBrawlStarsTag("#AA").withTrophies(100).build(),
            )
            playerJpaRepository.save(
                aPlayerEntity().withBrawlStarsTag("#AB").withTrophies(200).build(),
            )
            playerJpaRepository.save(
                aPlayerEntity().withBrawlStarsTag("#AC").withTrophies(300).build(),
            )

            // when
            val result = sut.searchByTag(BrawlStarsTag("#A"), OffsetLimit(offset = 0, limit = 2))

            // then
            assertThat(result.content).hasSize(2)
            assertThat(result.hasNext).isTrue
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
        fun `이름 prefix 는 대소문자 구분`() {
            // given
            playerJpaRepository.save(
                aPlayerEntity()
                    .withBrawlStarsTag("#A")
                    .withName("Alpha")
                    .withTrophies(100)
                    .build(),
            )

            // when
            val lower = sut.searchByName("alp", OffsetLimit(offset = 0, limit = 10))
            val upper = sut.searchByName("Alp", OffsetLimit(offset = 0, limit = 10))

            // then
            assertThat(lower.content).isEmpty()
            assertThat(upper.content).hasSize(1)
            assertThat(upper.content[0].name).isEqualTo("Alpha")
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
