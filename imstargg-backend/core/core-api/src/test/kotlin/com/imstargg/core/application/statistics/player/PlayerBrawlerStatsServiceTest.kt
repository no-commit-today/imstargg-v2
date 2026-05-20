package com.imstargg.core.application.statistics.player

import com.imstargg.CoreApiContextTest
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.enums.BattleType
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PlayerBrawlerStatsServiceTest(
    private val sut: PlayerBrawlerStatsService,
    private val cleanUp: CleanUp,
) : CoreApiContextTest() {
    @BeforeEach
    fun setUp() {
        cleanUp.all()
    }

    @Test
    fun `저장된 통계가 없으면 입력값으로 채워진 빈 stats 가 반환된다`() {
        // given
        val tag = BrawlStarsTag("#PBS1")
        val start = LocalDate.of(2026, 4, 1)
        val end = LocalDate.of(2026, 4, 30)

        // when
        val result =
            sut.get(
                playerTag = tag,
                battleType = BattleType.RANKED,
                startBattleDate = start,
                endBattleDate = end,
            )

        // then
        assertThat(result.playerTag).isEqualTo(tag)
        assertThat(result.battleType).isEqualTo(BattleType.RANKED)
        assertThat(result.startBattleDate).isEqualTo(start)
        assertThat(result.endBattleDate).isEqualTo(end)
        assertThat(result.items).isEmpty()
    }
}
