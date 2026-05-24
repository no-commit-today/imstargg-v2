package com.imstargg.core.application.statistics.player

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.statistics.player.PlayerBrawlerStats
import com.imstargg.core.domain.statistics.player.PlayerBrawlerStatsItem
import com.imstargg.core.enums.BattleType
import com.imstargg.core.infrastructure.jpa.toResultCount
import com.imstargg.infrastructure.db.core.statistics.player.PlayerBrawlerStatsItemJpaRepository
import com.imstargg.infrastructure.db.core.statistics.player.PlayerBrawlerStatsJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Repository
@Transactional(readOnly = true)
class PlayerBrawlerStatsRepository(
    private val playerBrawlerStatsJpaRepository: PlayerBrawlerStatsJpaRepository,
    private val playerBrawlerStatsItemJpaRepository: PlayerBrawlerStatsItemJpaRepository,
) {
    fun find(
        playerTag: BrawlStarsTag,
        battleType: BattleType,
        startBattleDate: LocalDate,
    ): PlayerBrawlerStats? {
        val header =
            playerBrawlerStatsJpaRepository.findByPlayerBrawlStarsTagAndBattleTypeAndStartBattleDate(
                playerBrawlStarsTag = playerTag.value,
                battleType = battleType,
                startBattleDate = startBattleDate,
            ) ?: return null
        val items =
            playerBrawlerStatsItemJpaRepository
                .findAllByPlayerBrawlerStatsId(checkNotNull(header.id))
                .map { item ->
                    PlayerBrawlerStatsItem(
                        brawlerId = BrawlStarsId(item.brawlerBrawlStarsId),
                        resultCount = item.resultCount.toResultCount(),
                    )
                }
        return PlayerBrawlerStats(
            playerTag = playerTag,
            battleType = header.battleType,
            startBattleDate = header.startBattleDate,
            endBattleDate = header.endBattleDate,
            items = items,
        )
    }
}
