package com.imstargg.core.application.brawlstars

import com.imstargg.core.domain.OffsetLimit
import com.imstargg.core.domain.Slice
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.brawlstars.player.Player
import com.imstargg.core.error.CoreException
import com.imstargg.core.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PlayerServiceTest {
    private val playerRepository = mockk<PlayerRepository>()

    private val sut = PlayerService(playerRepository = playerRepository)

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
            val player = aPlayer(tag = tag)
            every { playerRepository.searchByTag(tag) } returns player

            // when
            val result = sut.search("#ABC", OffsetLimit(offset = 0, limit = 20))

            // then
            assertThat(result.content).containsExactly(player)
            assertThat(result.hasNext).isFalse()
            verify { playerRepository.searchByTag(tag) }
        }

        @Test
        fun `# 으로 시작하는 query 가 미존재 태그면 빈 Slice 가 반환된다`() {
            // given
            val tag = BrawlStarsTag("#NONE")
            every { playerRepository.searchByTag(tag) } returns null

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
            val expected = Slice(content = listOf(aPlayer(tag = BrawlStarsTag("#A"))), hasNext = true)
            every { playerRepository.searchByName("야옹이", offsetLimit) } returns expected

            // when
            val result = sut.search("  야옹이  ", offsetLimit)

            // then
            assertThat(result).isEqualTo(expected)
            verify { playerRepository.searchByName("야옹이", offsetLimit) }
        }
    }

    private fun aPlayer(tag: BrawlStarsTag): Player =
        Player(
            tag = tag,
            name = "name",
            nameColor = null,
            iconId = BrawlStarsId(0),
            trophies = 0,
            highestTrophies = 0,
            expLevel = 0,
            expPoints = 0,
            qualifiedFromChampionshipChallenge = false,
            victories3vs3 = 0,
            victoriesSolo = 0,
            victoriesDuo = 0,
            bestRoboRumbleTime = 0,
            bestTimeAsBigBrawler = 0,
            totalPrestigeLevel = 0,
            club = null,
        )
}
