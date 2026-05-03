package com.imstargg.core.api.query

import com.imstargg.core.api.controller.v1.response.BrawlerResponse
import com.imstargg.core.api.controller.v1.response.PlayerBrawlerRoleStatisticsItemResponse
import com.imstargg.core.api.controller.v1.response.PlayerBrawlerRoleStatisticsResponse
import com.imstargg.core.api.controller.v1.response.PlayerBrawlerStatisticsItemResponse
import com.imstargg.core.api.controller.v1.response.PlayerBrawlerStatisticsResponse
import com.imstargg.core.application.brawlstars.BrawlerService
import com.imstargg.core.application.statistics.player.PlayerBrawlerStatsService
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.brawlstars.brawler.Brawlers
import com.imstargg.core.domain.statistics.ResultCount
import com.imstargg.core.domain.statistics.player.PlayerBrawlerStatsItem
import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.BrawlPassSeason
import com.imstargg.core.enums.BrawlerRole
import com.imstargg.core.enums.SoloRankSeason
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class PlayerStatisticsQueryComposer(
    private val playerBrawlerStatsService: PlayerBrawlerStatsService,
    private val brawlerService: BrawlerService,
) {
    fun getRankedBrawlerStatistics(
        playerTag: BrawlStarsTag,
        season: BrawlPassSeason,
    ): PlayerBrawlerStatisticsResponse =
        buildBrawlerStatistics(
            playerTag = playerTag,
            battleType = BattleType.RANKED,
            startBattleDate = season.startAt.toLocalDate(),
            endBattleDate = season.endAt.toLocalDate(),
        )

    fun getSoloRankedBrawlerStatistics(
        playerTag: BrawlStarsTag,
        season: SoloRankSeason,
    ): PlayerBrawlerStatisticsResponse =
        buildBrawlerStatistics(
            playerTag = playerTag,
            battleType = BattleType.SOLO_RANKED,
            startBattleDate = season.startAt.toLocalDate(),
            endBattleDate = season.endAt.toLocalDate(),
        )

    fun getRankedBrawlerRoleStatistics(
        playerTag: BrawlStarsTag,
        season: BrawlPassSeason,
    ): PlayerBrawlerRoleStatisticsResponse =
        buildBrawlerRoleStatistics(
            playerTag = playerTag,
            battleType = BattleType.RANKED,
            startBattleDate = season.startAt.toLocalDate(),
            endBattleDate = season.endAt.toLocalDate(),
        )

    fun getSoloRankedBrawlerRoleStatistics(
        playerTag: BrawlStarsTag,
        season: SoloRankSeason,
    ): PlayerBrawlerRoleStatisticsResponse =
        buildBrawlerRoleStatistics(
            playerTag = playerTag,
            battleType = BattleType.SOLO_RANKED,
            startBattleDate = season.startAt.toLocalDate(),
            endBattleDate = season.endAt.toLocalDate(),
        )

    private fun buildBrawlerStatistics(
        playerTag: BrawlStarsTag,
        battleType: BattleType,
        startBattleDate: LocalDate,
        endBattleDate: LocalDate,
    ): PlayerBrawlerStatisticsResponse {
        val stats =
            playerBrawlerStatsService.get(
                playerTag = playerTag,
                battleType = battleType,
                startBattleDate = startBattleDate,
                endBattleDate = endBattleDate,
            )
        val items = stats.items
        val brawlers = Brawlers(brawlerService.getAll())
        val responseItems =
            items.map { item ->
                PlayerBrawlerStatisticsItemResponse(
                    brawler = brawlers.find(item.brawlerId)?.let { BrawlerResponse.of(it) },
                    totalBattleCount = item.resultCount.totalCount,
                    victoryCount = item.resultCount.victoryCount,
                    defeatCount = item.resultCount.defeatCount,
                    drawCount = item.resultCount.drawCount,
                    winRate = item.resultCount.winRate,
                )
            }
        return PlayerBrawlerStatisticsResponse(
            startBattleDate = startBattleDate,
            endBattleDate = endBattleDate,
            totalBattleCount = items.sumOf { it.resultCount.totalCount },
            items = responseItems,
        )
    }

    private fun buildBrawlerRoleStatistics(
        playerTag: BrawlStarsTag,
        battleType: BattleType,
        startBattleDate: LocalDate,
        endBattleDate: LocalDate,
    ): PlayerBrawlerRoleStatisticsResponse {
        val stats =
            playerBrawlerStatsService.get(
                playerTag = playerTag,
                battleType = battleType,
                startBattleDate = startBattleDate,
                endBattleDate = endBattleDate,
            )
        val items = stats.items
        val roleByBrawlerId = brawlerService.getAll().associate { it.id to it.role }
        val countsByRole: Map<BrawlerRole, ResultCount> =
            items
                .mapNotNull { item -> roleByBrawlerId[item.brawlerId]?.let { it to item } }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, sameRoleItems) -> sameRoleItems.sumResultCount() }
        val responseItems =
            BrawlerRole.entries.map { role ->
                val count = countsByRole[role] ?: ResultCount(0, 0, 0)
                PlayerBrawlerRoleStatisticsItemResponse(
                    role = role,
                    totalBattleCount = count.totalCount,
                    victoryCount = count.victoryCount,
                    defeatCount = count.defeatCount,
                    drawCount = count.drawCount,
                    winRate = count.winRate,
                )
            }
        return PlayerBrawlerRoleStatisticsResponse(
            startBattleDate = startBattleDate,
            endBattleDate = endBattleDate,
            totalBattleCount = items.sumOf { it.resultCount.totalCount },
            items = responseItems,
        )
    }

    private fun List<PlayerBrawlerStatsItem>.sumResultCount(): ResultCount =
        fold(ResultCount(0, 0, 0)) { acc, item -> acc.merge(item.resultCount) }
}
