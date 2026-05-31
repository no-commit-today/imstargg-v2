package com.imstargg.core.application.statistics.player

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.statistics.ResultCount
import com.imstargg.core.domain.statistics.player.PlayerBrawlerStats
import com.imstargg.core.domain.statistics.player.PlayerBrawlerStatsItem
import com.imstargg.core.enums.BattleResult
import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.infrastructure.db.core.brawlstars.battle.repository.BattleJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Repository
@Transactional(readOnly = true)
class PlayerBrawlerStatsRepository(
    private val playerJpaRepository: PlayerJpaRepository,
    private val battleJpaRepository: BattleJpaRepository,
) {
    fun compute(
        playerTag: BrawlStarsTag,
        battleType: BattleType,
        startAt: OffsetDateTime,
        endAt: OffsetDateTime,
    ): PlayerBrawlerStats {
        val playerId = playerJpaRepository.findByBrawlStarsTag(playerTag.value)?.id
        val items =
            if (playerId == null) {
                emptyList()
            } else {
                aggregate(playerId, playerTag.value, battleType, startAt, endAt)
            }
        return PlayerBrawlerStats(
            playerTag = playerTag,
            battleType = battleType,
            startBattleDate = startAt.toLocalDate(),
            endBattleDate = endAt.toLocalDate(),
            items = items,
        )
    }

    private fun aggregate(
        playerId: Long,
        playerTag: String,
        battleType: BattleType,
        startAt: OffsetDateTime,
        endAt: OffsetDateTime,
    ): List<PlayerBrawlerStatsItem> =
        battleJpaRepository
            .aggregatePlayerBrawlerResults(
                playerId = playerId,
                playerTag = playerTag,
                battleTypeCode = battleType.code,
                startAt = startAt,
                endAt = endAt,
            ).groupBy { it.brawlerBrawlStarsId }
            .map { (brawlerId, rows) ->
                var victory = 0L
                var defeat = 0L
                var draw = 0L
                rows.forEach { row ->
                    when (EnumCodeType.findByCode<BattleResult>(row.result)) {
                        BattleResult.VICTORY -> victory += row.count
                        BattleResult.DEFEAT -> defeat += row.count
                        BattleResult.DRAW -> draw += row.count
                        null -> {}
                    }
                }
                PlayerBrawlerStatsItem(
                    brawlerId = BrawlStarsId(brawlerId),
                    resultCount = ResultCount(victory, defeat, draw),
                )
            }
}
