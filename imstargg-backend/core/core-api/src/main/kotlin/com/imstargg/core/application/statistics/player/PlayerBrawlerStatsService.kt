package com.imstargg.core.application.statistics.player

import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.statistics.player.PlayerBrawlerStats
import com.imstargg.core.enums.BattleType
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PlayerBrawlerStatsService(
    private val playerBrawlerStatsRepository: PlayerBrawlerStatsRepository,
) {
    fun get(
        playerTag: BrawlStarsTag,
        battleType: BattleType,
        startBattleDate: LocalDate,
        endBattleDate: LocalDate,
    ): PlayerBrawlerStats =
        playerBrawlerStatsRepository.find(
            playerTag = playerTag,
            battleType = battleType,
            startBattleDate = startBattleDate,
        ) ?: PlayerBrawlerStats(
            playerTag = playerTag,
            battleType = battleType,
            startBattleDate = startBattleDate,
            endBattleDate = endBattleDate,
            items = emptyList(),
        )
}
