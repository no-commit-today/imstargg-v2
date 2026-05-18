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
            clubJpaRepository.save(
                aClubEntity()
                    .withBrawlStarsTag("#A")
                    .withName("스타포스")
                    .withTrophies(100)
                    .build(),
            )
            clubJpaRepository.save(
                aClubEntity()
                    .withBrawlStarsTag("#B")
                    .withName("스타라이트")
                    .withTrophies(500)
                    .build(),
            )
            clubJpaRepository.save(
                aClubEntity()
                    .withBrawlStarsTag("#C")
                    .withName("문라이트")
                    .withTrophies(300)
                    .build(),
            )

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
            clubJpaRepository.save(
                aClubEntity()
                    .withBrawlStarsTag("#A")
                    .withName("스타1")
                    .withTrophies(100)
                    .build(),
            )
            clubJpaRepository.save(
                aClubEntity()
                    .withBrawlStarsTag("#B")
                    .withName("스타2")
                    .withTrophies(200)
                    .build(),
            )
            clubJpaRepository.save(
                aClubEntity()
                    .withBrawlStarsTag("#C")
                    .withName("스타3")
                    .withTrophies(300)
                    .build(),
            )

            // when
            val result = sut.searchByName("스타", OffsetLimit(offset = 0, limit = 2))

            // then
            assertThat(result.content).hasSize(2)
            assertThat(result.hasNext).isTrue
        }
    }
}
