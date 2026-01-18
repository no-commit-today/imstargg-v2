package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.brawlstars.battle.Battle
import com.imstargg.core.enums.BattleMode
import com.imstargg.core.enums.BattleResult
import com.imstargg.core.enums.BattleType
import java.time.OffsetDateTime

data class BattleResponse(
    val battleTime: OffsetDateTime,
    val event: BattleEventResponse?,
    val mode: BattleMode?,
    val type: BattleType?,
    val result: BattleResult?,
    val duration: Int?,
    val starPlayerTag: String?,
    val rank: Int?,
    val trophyChange: Int?,
    val teams: List<List<BattlePlayerResponse>>,
) {
    companion object {
        fun of(battle: Battle) =
            BattleResponse(
                battleTime = battle.battleTime,
                event = battle.event?.let { BattleEventResponse.of(it) },
                mode = battle.mode,
                type = battle.type,
                result = battle.result,
                duration = battle.duration,
                starPlayerTag = battle.starPlayerTag?.value,
                rank = battle.rank,
                trophyChange = battle.trophyChange,
                teams =
                    battle.teams.map { team ->
                        team.map { BattlePlayerResponse.of(it) }
                    },
            )
    }
}
