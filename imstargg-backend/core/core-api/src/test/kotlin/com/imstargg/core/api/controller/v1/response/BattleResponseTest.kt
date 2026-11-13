package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.Messages
import com.imstargg.core.domain.brawlstars.battle.Battle
import com.imstargg.core.domain.brawlstars.event.BattleEvent
import com.imstargg.core.domain.brawlstars.event.BattleEventMode
import com.imstargg.core.domain.statistics.StatsEventModeTypes
import com.imstargg.core.enums.BattleEventModeResultType
import com.imstargg.core.enums.BattleMode
import com.imstargg.core.enums.StatsEventModeType.RESULT_3VS3
import com.imstargg.core.enums.StatsEventModeType.SOLO_SHOWDOWN
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class BattleResponseTest {
    private val types =
        StatsEventModeTypes(
            mapOf(
                BrawlStarsId(20) to RESULT_3VS3,
                BrawlStarsId(6) to SOLO_SHOWDOWN,
            ),
        )

    @Test
    fun `전적의 이벤트 모드가 수집 모드면 그 유형의 resultType 을 낸다`() {
        val result = BattleResponse.of(battle(modeId = 20), types)
        val rank = BattleResponse.of(battle(modeId = 6), types)

        assertThat(result.event!!.mode!!.resultType).isEqualTo(BattleEventModeResultType.RESULT)
        assertThat(rank.event!!.mode!!.resultType).isEqualTo(BattleEventModeResultType.RANK)
    }

    @Test
    fun `전적의 이벤트 모드가 수집 모드가 아니면 resultType 이 null 이다`() {
        val response = BattleResponse.of(battle(modeId = 25), types)

        assertThat(response.event!!.mode!!.resultType).isNull()
    }

    private fun battle(modeId: Long) =
        Battle(
            battleTime = Instant.parse("2026-11-13T00:00:00Z"),
            event =
                BattleEvent(
                    id = BrawlStarsId(10000001),
                    mode =
                        BattleEventMode(
                            id = BrawlStarsId(modeId),
                            name = Messages.newDefault("mode"),
                            iconImageUrl = null,
                        ),
                    mapName = Messages.newDefault("map"),
                ),
            mode = BattleMode.KNOCKOUT,
            type = null,
            result = null,
            duration = null,
            starPlayerTag = null,
            rank = null,
            trophyChange = null,
            teams = emptyList(),
        )
}
