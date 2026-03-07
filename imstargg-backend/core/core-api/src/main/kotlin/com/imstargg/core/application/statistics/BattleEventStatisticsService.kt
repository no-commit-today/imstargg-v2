package com.imstargg.core.application.statistics

import com.imstargg.core.domain.statistics.BattleEventBrawlerStatisticsParam
import com.imstargg.core.domain.statistics.BattleEventBrawlersStatisticsParam
import com.imstargg.core.domain.statistics.BrawlerEnemyResultStatistics
import com.imstargg.core.domain.statistics.BrawlerRankStatistics
import com.imstargg.core.domain.statistics.BrawlerResultStatistics
import com.imstargg.core.domain.statistics.BrawlersRankStatistics
import com.imstargg.core.domain.statistics.BrawlersResultStatistics
import org.springframework.stereotype.Service

@Service
class BattleEventStatisticsService(
    private val resultRepository: BattleEventResultStatisticsRepositoryWithCache,
    private val rankRepository: BattleEventRankStatisticsRepositoryWithCache,
) {
    fun getBattleEventBrawlerResultStatistics(param: BattleEventBrawlerStatisticsParam): List<BrawlerResultStatistics> =
        resultRepository
            .findBrawlerResultCounts(param.eventId, param.tierRange, param.date)
            .map { it.toStatistics() }

    fun getBattleEventBrawlerDuoResultStatistics(param: BattleEventBrawlersStatisticsParam): List<BrawlersResultStatistics> =
        resultRepository
            .findBrawlerDuoResultCounts(param.eventId, param.brawlerId, param.tierRange, param.date)
            .map { it.toStatistics() }

    fun getBattleEventBrawlerTrioResultStatistics(param: BattleEventBrawlersStatisticsParam): List<BrawlersResultStatistics> =
        resultRepository
            .findBrawlerTrioResultCounts(param.eventId, param.brawlerId, param.tierRange, param.date)
            .map { it.toStatistics() }

    fun getBattleEventBrawlerEnemyResultStatistics(param: BattleEventBrawlersStatisticsParam): List<BrawlerEnemyResultStatistics> =
        resultRepository
            .findBrawlerEnemyResultCounts(param.eventId, param.brawlerId, param.tierRange, param.date)
            .map { it.toStatistics() }

    fun getBattleEventBrawlerRankStatistics(param: BattleEventBrawlerStatisticsParam): List<BrawlerRankStatistics> =
        rankRepository
            .findBrawlerRankCounts(param.eventId, param.tierRange, param.date)
            .map { it.toStatistics() }

    fun getBattleEventBrawlerDuoRankStatistics(param: BattleEventBrawlersStatisticsParam): List<BrawlersRankStatistics> =
        rankRepository
            .findBrawlerDuoRankCounts(param.eventId, param.brawlerId, param.tierRange, param.date)
            .map { it.toStatistics() }

    fun getBattleEventBrawlerTrioRankStatistics(param: BattleEventBrawlersStatisticsParam): List<BrawlersRankStatistics> =
        rankRepository
            .findBrawlerTrioRankCounts(param.eventId, param.brawlerId, param.tierRange, param.date)
            .map { it.toStatistics() }
}
