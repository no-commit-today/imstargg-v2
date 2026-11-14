package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsTierRange
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BrawlerPairBattleResultStatsBackupEntityTest {
    @Test
    fun `brawler1 이 brawler2 와 같으면 IllegalArgumentException 이 발생한다`() {
        // when & then
        assertThatThrownBy { aPair(brawler1BrawlStarsId = 10L, brawler2BrawlStarsId = 10L) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `brawler1 이 brawler2 보다 크면 IllegalArgumentException 이 발생한다`() {
        // when & then
        assertThatThrownBy { aPair(brawler1BrawlStarsId = 20L, brawler2BrawlStarsId = 10L) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `brawler1 이 brawler2 보다 작으면 생성된다`() {
        // when
        val sut = aPair(brawler1BrawlStarsId = 10L, brawler2BrawlStarsId = 20L)

        // then
        assertThat(sut.brawler1BrawlStarsId).isEqualTo(10L)
        assertThat(sut.brawler2BrawlStarsId).isEqualTo(20L)
    }

    private fun aPair(
        brawler1BrawlStarsId: Long,
        brawler2BrawlStarsId: Long,
    ) = BrawlerPairBattleResultStatsBackupEntity(
        tierRange = StatsTierRange.TROPHY_1000_PLUS,
        startBattleDate = LocalDate.of(2026, 10, 26),
        endBattleDate = LocalDate.of(2026, 10, 31),
        brawler1BrawlStarsId = brawler1BrawlStarsId,
        brawler2BrawlStarsId = brawler2BrawlStarsId,
        resultCount = ResultCountEmbedded.create(1L, 0L, 0L),
    )
}
