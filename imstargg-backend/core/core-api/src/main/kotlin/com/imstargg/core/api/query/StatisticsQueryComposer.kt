package com.imstargg.core.api.query

import com.imstargg.core.api.controller.v1.response.BrawlerEnemyResultStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerRankStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerResponse
import com.imstargg.core.api.controller.v1.response.BrawlerResultDailyStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlerResultStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlersRankStatisticsResponse
import com.imstargg.core.api.controller.v1.response.BrawlersResultStatisticsResponse
import com.imstargg.core.application.brawlstars.BrawlerService
import com.imstargg.core.application.statistics.BattleEventStatisticsService
import com.imstargg.core.application.statistics.BrawlerStatisticsService
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.brawler.Brawlers
import com.imstargg.core.domain.statistics.BattleEventBrawlerStatisticsParam
import com.imstargg.core.domain.statistics.BattleEventBrawlersStatisticsParam
import com.imstargg.core.domain.statistics.BrawlerStatisticsParam
import com.imstargg.core.domain.statistics.BrawlersStatisticsParam
import org.springframework.stereotype.Component

@Component
class StatisticsQueryComposer(
    private val brawlerService: BrawlerService,
    private val battleEventStatisticsService: BattleEventStatisticsService,
    private val brawlerStatisticsService: BrawlerStatisticsService,
) {
    fun getBattleEventBrawlerResultStatistics(param: BattleEventBrawlerStatisticsParam): BrawlerResultStatisticsResponse {
        val brawlers = brawlers()
        val statistics = battleEventStatisticsService.getBattleEventBrawlerResultStatistics(param)
        return BrawlerResultStatisticsResponse.of(statistics, brawlers)
    }

    fun getBattleEventBrawlerDuoResultStatistics(param: BattleEventBrawlersStatisticsParam): BrawlersResultStatisticsResponse {
        val brawlers = brawlers()
        val statistics = battleEventStatisticsService.getBattleEventBrawlerDuoResultStatistics(param)
        return BrawlersResultStatisticsResponse.of(statistics, brawlers)
    }

    fun getBattleEventBrawlerTrioResultStatistics(param: BattleEventBrawlersStatisticsParam): BrawlersResultStatisticsResponse {
        val brawlers = brawlers()
        val statistics = battleEventStatisticsService.getBattleEventBrawlerTrioResultStatistics(param)
        return BrawlersResultStatisticsResponse.of(statistics, brawlers)
    }

    fun getBattleEventBrawlerEnemyResultStatistics(param: BattleEventBrawlersStatisticsParam): BrawlerEnemyResultStatisticsResponse {
        val brawlers = brawlers()
        val statistics = battleEventStatisticsService.getBattleEventBrawlerEnemyResultStatistics(param)
        return BrawlerEnemyResultStatisticsResponse.of(statistics, brawlers)
    }

    fun getBattleEventBrawlerRankStatistics(param: BattleEventBrawlerStatisticsParam): List<BrawlerRankStatisticsResponse> {
        val brawlers = brawlers()
        return battleEventStatisticsService
            .getBattleEventBrawlerRankStatistics(param)
            .map { BrawlerRankStatisticsResponse.of(it, brawlers.toBrawlerResponse(it.brawlerId)) }
    }

    fun getBattleEventBrawlerDuoRankStatistics(param: BattleEventBrawlersStatisticsParam): List<BrawlersRankStatisticsResponse> {
        val brawlers = brawlers()
        return battleEventStatisticsService
            .getBattleEventBrawlerDuoRankStatistics(param)
            .map { BrawlersRankStatisticsResponse.of(it, it.brawlerIds.map { id -> brawlers.toBrawlerResponse(id) }) }
    }

    fun getBattleEventBrawlerTrioRankStatistics(param: BattleEventBrawlersStatisticsParam): List<BrawlersRankStatisticsResponse> {
        val brawlers = brawlers()
        return battleEventStatisticsService
            .getBattleEventBrawlerTrioRankStatistics(param)
            .map { BrawlersRankStatisticsResponse.of(it, it.brawlerIds.map { id -> brawlers.toBrawlerResponse(id) }) }
    }

    fun getBrawlerResultStatistics(param: BrawlerStatisticsParam): BrawlerResultStatisticsResponse {
        val brawlers = brawlers()
        val statistics = brawlerStatisticsService.getBrawlerResultStatistics(param)
        return BrawlerResultStatisticsResponse.of(statistics, brawlers)
    }

    fun getBrawlerDailyResultStatistics(param: BrawlersStatisticsParam): List<BrawlerResultDailyStatisticsResponse> {
        val brawlers = brawlers()
        return brawlerStatisticsService
            .getBrawlerDailyResultStatistics(param)
            .map { BrawlerResultDailyStatisticsResponse.of(it, brawlers.toBrawlerResponse(it.brawlerId)) }
    }

    fun getBrawlerDuoResultStatistics(param: BrawlersStatisticsParam): BrawlersResultStatisticsResponse {
        val brawlers = brawlers()
        val statistics = brawlerStatisticsService.getBrawlerDuoResultStatistics(param)
        return BrawlersResultStatisticsResponse.of(statistics, brawlers)
    }

    fun getBrawlerTrioResultStatistics(param: BrawlersStatisticsParam): BrawlersResultStatisticsResponse {
        val brawlers = brawlers()
        val statistics = brawlerStatisticsService.getBrawlerTrioResultStatistics(param)
        return BrawlersResultStatisticsResponse.of(statistics, brawlers)
    }

    private fun brawlers(): Brawlers = Brawlers(brawlerService.getAll())

    private fun Brawlers.toBrawlerResponse(id: BrawlStarsId): BrawlerResponse? = find(id)?.let { BrawlerResponse.of(it) }
}
