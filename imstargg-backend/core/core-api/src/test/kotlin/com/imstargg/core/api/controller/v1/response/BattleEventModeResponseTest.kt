package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.Messages
import com.imstargg.core.domain.brawlstars.event.BattleEventMode
import com.imstargg.core.domain.statistics.StatsEventModeTypes
import com.imstargg.core.enums.BattleEventModeResultType
import com.imstargg.core.enums.StatsEventModeType.RESULT_3VS3
import com.imstargg.core.enums.StatsEventModeType.SOLO_SHOWDOWN
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BattleEventModeResponseTest {
    private val types =
        StatsEventModeTypes(
            mapOf(
                BrawlStarsId(20) to RESULT_3VS3,
                BrawlStarsId(6) to SOLO_SHOWDOWN,
            ),
        )

    @Test
    fun `수집 모드는 그 유형의 resultType 을 낸다`() {
        val result = BattleEventModeResponse.of(mode(20), types)
        val rank = BattleEventModeResponse.of(mode(6), types)

        assertThat(result.resultType).isEqualTo(BattleEventModeResultType.RESULT)
        assertThat(rank.resultType).isEqualTo(BattleEventModeResultType.RANK)
    }

    @Test
    fun `수집하지 않는 모드는 resultType 이 null 이다`() {
        val response = BattleEventModeResponse.of(mode(25), types)

        assertThat(response.resultType).isNull()
    }

    private fun mode(id: Long) =
        BattleEventMode(
            id = BrawlStarsId(id),
            name = Messages.newDefault("mode"),
            iconImageUrl = null,
        )
}
