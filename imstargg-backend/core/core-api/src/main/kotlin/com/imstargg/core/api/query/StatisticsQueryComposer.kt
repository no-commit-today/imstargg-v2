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

    fun getBattleEventBrawlerResultStatistics(
        param: BattleEventBrawlerStatisticsParam,
    ): List<BrawlerResultStatisticsResponse> {
        val brawlers = brawlers()
        return battleEventStatisticsService.getBattleEventBrawlerResultStatistics(param)
            .map { BrawlerResultStatisticsResponse.of(it, brawlers.toBrawlerResponse(it.brawlerId)) }
    }

    fun getBattleEventBrawlerDuoResultStatistics(
        param: BattleEventBrawlersStatisticsParam,
    ): List<BrawlersResultStatisticsResponse> {
        val brawlers = brawlers()
        return battleEventStatisticsService.getBattleEventBrawlerDuoResultStatistics(param)
            .map { BrawlersResultStatisticsResponse.of(it, it.brawlerIds.map { id -> brawlers.toBrawlerResponse(id) }) }
    }

    fun getBattleEventBrawlerTrioResultStatistics(
        param: BattleEventBrawlersStatisticsParam,
    ): List<BrawlersResultStatisticsResponse> {
        val brawlers = brawlers()
        return battleEventStatisticsService.getBattleEventBrawlerTrioResultStatistics(param)
            .map { BrawlersResultStatisticsResponse.of(it, it.brawlerIds.map { id -> brawlers.toBrawlerResponse(id) }) }
    }

    fun getBattleEventBrawlerEnemyResultStatistics(
        param: BattleEventBrawlersStatisticsParam,
    ): List<BrawlerEnemyResultStatisticsResponse> {
        val brawlers = brawlers()
        return battleEventStatisticsService.getBattleEventBrawlerEnemyResultStatistics(param)
            .map {
                BrawlerEnemyResultStatisticsResponse.of(
                    it,
                    brawlers.toBrawlerResponse(it.brawlerId),
                    brawlers.toBrawlerResponse(it.enemyBrawlerId),
                )
            }
    }

    fun getBattleEventBrawlerRankStatistics(
        param: BattleEventBrawlerStatisticsParam,
    ): List<BrawlerRankStatisticsResponse> {
        val brawlers = brawlers()
        return battleEventStatisticsService.getBattleEventBrawlerRankStatistics(param)
            .map { BrawlerRankStatisticsResponse.of(it, brawlers.toBrawlerResponse(it.brawlerId)) }
    }

    fun getBattleEventBrawlerDuoRankStatistics(
        param: BattleEventBrawlersStatisticsParam,
    ): List<BrawlersRankStatisticsResponse> {
        val brawlers = brawlers()
        return battleEventStatisticsService.getBattleEventBrawlerDuoRankStatistics(param)
            .map { BrawlersRankStatisticsResponse.of(it, it.brawlerIds.map { id -> brawlers.toBrawlerResponse(id) }) }
    }

    fun getBattleEventBrawlerTrioRankStatistics(
        param: BattleEventBrawlersStatisticsParam,
    ): List<BrawlersRankStatisticsResponse> {
        val brawlers = brawlers()
        return battleEventStatisticsService.getBattleEventBrawlerTrioRankStatistics(param)
            .map { BrawlersRankStatisticsResponse.of(it, it.brawlerIds.map { id -> brawlers.toBrawlerResponse(id) }) }
    }

    fun getBrawlerResultStatistics(
        param: BrawlerStatisticsParam,
    ): List<BrawlerResultStatisticsResponse> {
        val brawlers = brawlers()
        return brawlerStatisticsService.getBrawlerResultStatistics(param)
            .map { BrawlerResultStatisticsResponse.of(it, brawlers.toBrawlerResponse(it.brawlerId)) }
    }

    fun getBrawlerDailyResultStatistics(
        param: BrawlersStatisticsParam,
    ): List<BrawlerResultDailyStatisticsResponse> {
        val brawlers = brawlers()
        return brawlerStatisticsService.getBrawlerDailyResultStatistics(param)
            .map { BrawlerResultDailyStatisticsResponse.of(it, brawlers.toBrawlerResponse(it.brawlerId)) }
    }

    fun getBrawlerDuoResultStatistics(
        param: BrawlersStatisticsParam,
    ): List<BrawlersResultStatisticsResponse> {
        val brawlers = brawlers()
        return brawlerStatisticsService.getBrawlerDuoResultStatistics(param)
            .map { BrawlersResultStatisticsResponse.of(it, it.brawlerIds.map { id -> brawlers.toBrawlerResponse(id) }) }
    }

    fun getBrawlerTrioResultStatistics(
        param: BrawlersStatisticsParam,
    ): List<BrawlersResultStatisticsResponse> {
        val brawlers = brawlers()
        return brawlerStatisticsService.getBrawlerTrioResultStatistics(param)
            .map { BrawlersResultStatisticsResponse.of(it, it.brawlerIds.map { id -> brawlers.toBrawlerResponse(id) }) }
    }

    private fun brawlers(): Brawlers = Brawlers(brawlerService.getAll())

    private fun Brawlers.toBrawlerResponse(id: BrawlStarsId): BrawlerResponse? =
        find(id)?.let { BrawlerResponse.of(it) }
}
