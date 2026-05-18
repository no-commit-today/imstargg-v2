package com.imstargg.core.application.brawlstars

import com.imstargg.core.domain.OffsetLimit
import com.imstargg.core.domain.Slice
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.brawlstars.player.Club
import com.imstargg.core.enums.ClubType
import com.imstargg.core.error.CoreException
import com.imstargg.core.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ClubServiceTest {
    private val clubRepository = mockk<ClubRepository>()

    private val sut = ClubService(clubRepository = clubRepository)

    @Nested
    inner class Search {
        @Test
        fun `공백 query 는 INVALID_REQUEST 예외가 발생한다`() {
            // given
            val query = "   "

            // when
            val ex =
                catchThrowableOfType(CoreException::class.java) {
                    sut.search(query, OffsetLimit(offset = 0, limit = 20))
                }

            // then
            assertThat(ex.errorType).isEqualTo(ErrorType.INVALID_REQUEST)
        }

        @Test
        fun `# 으로 시작하는 query 는 searchByTag 로 위임되고 단건 Slice 로 감싸진다`() {
            // given
            val tag = BrawlStarsTag("#ABC")
            val club = aClub(tag = tag)
            every { clubRepository.searchByTag(tag) } returns club

            // when
            val result = sut.search("#ABC", OffsetLimit(offset = 0, limit = 20))

            // then
            assertThat(result.content).containsExactly(club)
            assertThat(result.hasNext).isFalse()
            verify { clubRepository.searchByTag(tag) }
        }

        @Test
        fun `# 으로 시작하는 query 가 미존재 태그면 빈 Slice 가 반환된다`() {
            // given
            val tag = BrawlStarsTag("#NONE")
            every { clubRepository.searchByTag(tag) } returns null

            // when
            val result = sut.search("#NONE", OffsetLimit(offset = 0, limit = 20))

            // then
            assertThat(result.content).isEmpty()
            assertThat(result.hasNext).isFalse()
        }

        @Test
        fun `이름 query 는 searchByName 으로 위임되고 결과가 그대로 반환된다`() {
            // given
            val offsetLimit = OffsetLimit(offset = 0, limit = 20)
            val expected = Slice(content = listOf(aClub(tag = BrawlStarsTag("#A"))), hasNext = true)
            every { clubRepository.searchByName("뉴비클럽", offsetLimit) } returns expected

            // when
            val result = sut.search("  뉴비클럽  ", offsetLimit)

            // then
            assertThat(result).isEqualTo(expected)
            verify { clubRepository.searchByName("뉴비클럽", offsetLimit) }
        }
    }

    private fun aClub(tag: BrawlStarsTag): Club =
        Club(
            tag = tag,
            name = "name",
            description = "",
            type = ClubType.UNKNOWN,
            badgeId = BrawlStarsId(0),
            requiredTrophies = 0,
            trophies = 0,
            members = emptyList(),
        )
}
