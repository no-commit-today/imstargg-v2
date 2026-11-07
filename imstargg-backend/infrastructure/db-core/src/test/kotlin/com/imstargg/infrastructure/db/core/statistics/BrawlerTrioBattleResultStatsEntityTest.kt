package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsTierRange
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BrawlerTrioBattleResultStatsEntityTest {
    @Test
    fun `brawler2 와 brawler3 이 같으면 IllegalArgumentException 이 발생한다`() {
        // when & then
        assertThatThrownBy { aTrio(1L, 2L, 2L) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `brawler1 이 brawler2 보다 크면 IllegalArgumentException 이 발생한다`() {
        // when & then
        assertThatThrownBy { aTrio(2L, 1L, 3L) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `세 브롤러가 오름차순이면 생성된다`() {
        // when
        val sut = aTrio(1L, 2L, 3L)

        // then
        assertThat(sut.brawler1BrawlStarsId).isEqualTo(1L)
        assertThat(sut.brawler2BrawlStarsId).isEqualTo(2L)
        assertThat(sut.brawler3BrawlStarsId).isEqualTo(3L)
    }

    private fun aTrio(
        brawler1BrawlStarsId: Long,
        brawler2BrawlStarsId: Long,
        brawler3BrawlStarsId: Long,
    ) = BrawlerTrioBattleResultStatsEntity(
        tierRange = StatsTierRange.TROPHY_1000_PLUS,
        startBattleDate = LocalDate.of(2026, 10, 26),
        endBattleDate = LocalDate.of(2026, 10, 31),
        brawler1BrawlStarsId = brawler1BrawlStarsId,
        brawler2BrawlStarsId = brawler2BrawlStarsId,
        brawler3BrawlStarsId = brawler3BrawlStarsId,
        resultCount = ResultCountEmbedded.create(1L, 0L, 0L),
    )
}
