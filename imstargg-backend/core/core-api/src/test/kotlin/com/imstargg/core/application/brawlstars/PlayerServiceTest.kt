package com.imstargg.core.application.brawlstars

import com.imstargg.CoreApiContextTest
import com.imstargg.core.domain.OffsetLimit
import com.imstargg.core.domain.brawlstars.player.PlayerSearch
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.PlayerEntityFixture.Companion.aPlayerEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PlayerServiceTest(
    private val sut: PlayerService,
    private val cleanUp: CleanUp,
    private val playerJpaRepository: PlayerJpaRepository,
) : CoreApiContextTest() {
    @BeforeEach
    fun setUp() {
        cleanUp.all()
    }

    @Nested
    inner class Search {
        @Test
        fun `# 으로 시작하는 정상 태그 query 는 해당 플레이어 단건 Slice 를 반환한다`() {
            // given
            playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#ABC").withName("야옹이").build())
            playerJpaRepository.save(aPlayerEntity().withBrawlStarsTag("#XYZ").withName("멍멍이").build())

            // when
            val result = sut.search(PlayerSearch.of("#ABC"), OffsetLimit(offset = 0, limit = 20))

            // then
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].tag.value).isEqualTo("#ABC")
            assertThat(result.hasNext).isFalse()
        }

        @Test
        fun `# 으로 시작하는 미존재 태그 query 는 빈 Slice 가 반환된다`() {
            // when
            val result = sut.search(PlayerSearch.of("#NONE"), OffsetLimit(offset = 0, limit = 20))

            // then
            assertThat(result.content).isEmpty()
            assertThat(result.hasNext).isFalse()
        }

        @Test
        fun `이름 query 는 trim 된 prefix 로 검색되어 매칭되는 플레이어들이 반환된다`() {
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
                    .withName("야옹이친구")
                    .withTrophies(200)
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
            val result = sut.search(PlayerSearch.of("  야옹이  "), OffsetLimit(offset = 0, limit = 20))

            // then
            assertThat(result.content)
                .extracting<String> { it.tag.value }
                .containsExactlyInAnyOrder("#A", "#B")
            assertThat(result.hasNext).isFalse()
        }
    }
}
