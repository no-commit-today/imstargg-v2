package com.imstargg.core.application.brawlstars

import com.imstargg.CoreApiContextTest
import com.imstargg.core.domain.OffsetLimit
import com.imstargg.core.domain.brawlstars.player.ClubSearch
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.ClubJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.ClubEntityFixture.Companion.aClubEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ClubServiceTest(
    private val sut: ClubService,
    private val cleanUp: CleanUp,
    private val clubJpaRepository: ClubJpaRepository,
) : CoreApiContextTest() {
    @BeforeEach
    fun setUp() {
        cleanUp.all()
    }

    @Nested
    inner class Search {
        @Test
        fun `# 으로 시작하는 정상 태그 query 는 해당 클럽 단건 Slice 를 반환한다`() {
            // given
            clubJpaRepository.save(aClubEntity().withBrawlStarsTag("#ABC").withName("스타포스").build())
            clubJpaRepository.save(aClubEntity().withBrawlStarsTag("#XYZ").withName("문라이트").build())

            // when
            val result = sut.search(ClubSearch.of("#ABC"), OffsetLimit(offset = 0, limit = 20))

            // then
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].tag.value).isEqualTo("#ABC")
            assertThat(result.hasNext).isFalse()
        }

        @Test
        fun `# 으로 시작하는 미존재 태그 query 는 빈 Slice 가 반환된다`() {
            // when
            val result = sut.search(ClubSearch.of("#NONE"), OffsetLimit(offset = 0, limit = 20))

            // then
            assertThat(result.content).isEmpty()
            assertThat(result.hasNext).isFalse()
        }

        @Test
        fun `이름 query 는 trim 된 prefix 로 검색되어 매칭되는 클럽들이 반환된다`() {
            // given
            clubJpaRepository.save(
                aClubEntity()
                    .withBrawlStarsTag("#A")
                    .withName("뉴비클럽")
                    .withTrophies(100)
                    .build(),
            )
            clubJpaRepository.save(
                aClubEntity()
                    .withBrawlStarsTag("#B")
                    .withName("뉴비룸")
                    .withTrophies(200)
                    .build(),
            )
            clubJpaRepository.save(
                aClubEntity()
                    .withBrawlStarsTag("#C")
                    .withName("고수클럽")
                    .withTrophies(300)
                    .build(),
            )

            // when
            val result = sut.search(ClubSearch.of("  뉴비  "), OffsetLimit(offset = 0, limit = 20))

            // then
            assertThat(result.content)
                .extracting<String> { it.tag.value }
                .containsExactlyInAnyOrder("#A", "#B")
            assertThat(result.hasNext).isFalse()
        }
    }
}
