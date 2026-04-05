package com.imstargg.core.batch.domain

import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayerBrawler
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PlayerCombinatorTest {
    @Nested
    inner class Combine {
        @Test
        fun `2명에서 2명 조합을 구하면 1개가 반환된다`() {
            // given
            val players = createPlayers(2)

            // when
            val result = PlayerCombinator.combine(players, 2)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0]).containsExactlyElementsOf(players)
        }

        @Test
        fun `3명에서 2명 조합을 구하면 3개가 반환된다`() {
            // given
            val players = createPlayers(3)

            // when
            val result = PlayerCombinator.combine(players, 2)

            // then
            assertThat(result).hasSize(3)
            assertThat(result.all { it.size == 2 }).isTrue()
        }

        @Test
        fun `3명에서 3명 조합을 구하면 1개가 반환된다`() {
            // given
            val players = createPlayers(3)

            // when
            val result = PlayerCombinator.combine(players, 3)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0]).containsExactlyElementsOf(players)
        }

        @Test
        fun `5명에서 2명 조합을 구하면 10개가 반환된다`() {
            // given
            val players = createPlayers(5)

            // when
            val result = PlayerCombinator.combine(players, 2)

            // then
            assertThat(result).hasSize(10)
            assertThat(result.all { it.size == 2 }).isTrue()
        }

        @Test
        fun `5명에서 3명 조합을 구하면 10개가 반환된다`() {
            // given
            val players = createPlayers(5)

            // when
            val result = PlayerCombinator.combine(players, 3)

            // then
            assertThat(result).hasSize(10)
            assertThat(result.all { it.size == 3 }).isTrue()
        }

        @Test
        fun `조합에 중복이 없다`() {
            // given
            val players = createPlayers(4)

            // when
            val result = PlayerCombinator.combine(players, 2)

            // then
            val sorted = result.map { combo -> combo.map { it.brawlStarsTag }.sorted() }
            assertThat(sorted).doesNotHaveDuplicates()
        }

        @Test
        fun `size가 0이면 예외가 발생한다`() {
            // given
            val players = createPlayers(3)

            // when
            val ex =
                catchThrowableOfType(IllegalArgumentException::class.java) {
                    PlayerCombinator.combine(players, 0)
                }

            // then
            assertThat(ex).isNotNull()
        }

        @Test
        fun `size가 팀 크기보다 크면 예외가 발생한다`() {
            // given
            val players = createPlayers(2)

            // when
            val ex =
                catchThrowableOfType(IllegalArgumentException::class.java) {
                    PlayerCombinator.combine(players, 3)
                }

            // then
            assertThat(ex).isNotNull()
        }
    }

    private fun createPlayers(count: Int): List<StatsBattleEntityTeamPlayer> =
        (1..count).map { i ->
            StatsBattleEntityTeamPlayer(
                brawlStarsTag = "#TAG$i",
                name = "Player$i",
                brawler =
                    StatsBattleEntityTeamPlayerBrawler(
                        brawlStarsId = i.toLong(),
                        name = "Brawler$i",
                        power = 11,
                        trophies = 1000,
                    ),
            )
        }
}
