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
    fun getBattleEventBrawlerResultStatistics(param: BattleEventBrawlerStatisticsParam): BrawlerResultStatistics =
        BrawlerResultStatistics.create(
            resultRepository.findBrawlerResultCounts(param.eventId, param.tierRange, param.startDate, param.endDate),
        )

    fun getBattleEventBrawlerDuoResultStatistics(param: BattleEventBrawlersStatisticsParam): BrawlersResultStatistics =
        BrawlersResultStatistics.create(
            resultRepository.findBrawlerDuoResultCounts(param.eventId, param.brawlerId, param.tierRange, param.startDate),
        )

    fun getBattleEventBrawlerTrioResultStatistics(param: BattleEventBrawlersStatisticsParam): BrawlersResultStatistics =
        BrawlersResultStatistics.create(
            resultRepository.findBrawlerTrioResultCounts(param.eventId, param.brawlerId, param.tierRange, param.startDate),
        )

    fun getBattleEventBrawlerEnemyResultStatistics(param: BattleEventBrawlersStatisticsParam): BrawlerEnemyResultStatistics =
        BrawlerEnemyResultStatistics.create(
            resultRepository.findBrawlerEnemyResultCounts(param.eventId, param.brawlerId, param.tierRange, param.startDate),
        )

    fun getBattleEventBrawlerRankStatistics(param: BattleEventBrawlerStatisticsParam): List<BrawlerRankStatistics> =
        rankRepository
            .findBrawlerRankCounts(param.eventId, param.tierRange, param.startDate, param.endDate)
            .map { it.toStatistics() }

    fun getBattleEventBrawlerDuoRankStatistics(param: BattleEventBrawlersStatisticsParam): List<BrawlersRankStatistics> =
        rankRepository
            .findBrawlerDuoRankCounts(param.eventId, param.brawlerId, param.tierRange, param.startDate)
            .map { it.toStatistics() }

    fun getBattleEventBrawlerTrioRankStatistics(param: BattleEventBrawlersStatisticsParam): List<BrawlersRankStatistics> =
        rankRepository
            .findBrawlerTrioRankCounts(param.eventId, param.brawlerId, param.tierRange, param.startDate)
            .map { it.toStatistics() }
}
