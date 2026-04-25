package com.imstargg.core.worker.application

import com.imstargg.core.enums.BattleResult
import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.BrawlPassSeason
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.core.enums.SoloRankSeason
import com.imstargg.core.worker.domain.Battle
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerEntity
import com.imstargg.infrastructure.db.core.statistics.player.PlayerBrawlerStatsEntity
import com.imstargg.infrastructure.db.core.statistics.player.PlayerBrawlerStatsItemEntity
import com.imstargg.infrastructure.db.core.statistics.player.PlayerBrawlerStatsItemJpaRepository
import com.imstargg.infrastructure.db.core.statistics.player.PlayerBrawlerStatsJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
class PlayerBrawlerStatsAccumulator(
    private val statsRepository: PlayerBrawlerStatsJpaRepository,
    private val itemRepository: PlayerBrawlerStatsItemJpaRepository,
) {
    @Transactional
    fun accumulate(
        player: PlayerEntity,
        newBattles: List<Battle>,
    ) {
        newBattles.forEach { battle -> accumulate(player, battle) }
    }

    private fun accumulate(
        player: PlayerEntity,
        battle: Battle,
    ) {
        val battleType =
            battle.type
                ?.let { EnumCodeType.findByCode<BattleType>(it) }
                ?.takeIf { it in TARGET_BATTLE_TYPES }
                ?: return
        val result =
            battle.result?.let { EnumCodeType.findByCode<BattleResult>(it) } ?: return
        val brawlerId = battle.findOwnBrawlerBrawlStarsId(player.brawlStarsTag) ?: return
        val (startDate, endDate) = seasonRangeOf(battleType, battle) ?: return

        val header =
            statsRepository.findByPlayerBrawlStarsTagAndBattleTypeAndStartBattleDate(
                playerBrawlStarsTag = player.brawlStarsTag,
                battleType = battleType,
                startBattleDate = startDate,
            ) ?: statsRepository.save(
                PlayerBrawlerStatsEntity(
                    playerBrawlStarsTag = player.brawlStarsTag,
                    battleType = battleType,
                    startBattleDate = startDate,
                    endBattleDate = endDate,
                ),
            )
        val statsId = checkNotNull(header.id) { "PlayerBrawlerStatsEntity.id is null after save" }
        val item =
            itemRepository.findByPlayerBrawlerStatsIdAndBrawlerBrawlStarsId(
                playerBrawlerStatsId = statsId,
                brawlerBrawlStarsId = brawlerId,
            ) ?: PlayerBrawlerStatsItemEntity.zero(
                playerBrawlerStatsId = statsId,
                brawlerBrawlStarsId = brawlerId,
            )
        item.accumulate(result)
        itemRepository.save(item)
    }

    private fun seasonRangeOf(
        battleType: BattleType,
        battle: Battle,
    ): Pair<LocalDate, LocalDate>? =
        when (battleType) {
            BattleType.RANKED -> {
                val season = BrawlPassSeason.of(battle.battleAt)
                season.startAt.toLocalDate() to season.endAt.toLocalDate()
            }

            BattleType.SOLO_RANKED -> {
                val firstStart = SoloRankSeason.createFirst().startAt
                if (battle.battleAt.isBefore(firstStart)) {
                    null
                } else {
                    val season = SoloRankSeason.of(battle.battleAt)
                    season.startAt.toLocalDate() to season.endAt.toLocalDate()
                }
            }

            else -> null
        }

    private fun Battle.findOwnBrawlerBrawlStarsId(playerBrawlStarsTag: String): Long? =
        teams
            .asSequence()
            .flatten()
            .firstOrNull { it.brawlStarsTag == playerBrawlStarsTag }
            ?.brawler
            ?.brawlStarsId

    companion object {
        private val TARGET_BATTLE_TYPES = setOf(BattleType.RANKED, BattleType.SOLO_RANKED)
    }
}
