package com.imstargg.core.application.statistics

import com.imstargg.core.domain.statistics.BrawlerResultDailyStatistics
import com.imstargg.core.domain.statistics.BrawlerResultStatistics
import com.imstargg.core.domain.statistics.BrawlerStatisticsParam
import com.imstargg.core.domain.statistics.BrawlersResultStatistics
import com.imstargg.core.domain.statistics.BrawlersStatisticsParam
import org.springframework.stereotype.Service

@Service
class BrawlerStatisticsService(
    private val resultRepository: BrawlerResultStatisticsRepositoryWithCache,
) {
    fun getBrawlerResultStatistics(param: BrawlerStatisticsParam): List<BrawlerResultStatistics> =
        resultRepository
            .findBrawlerResultCounts(param.tierRange, param.startDate, param.endDate)
            .map { it.toStatistics() }

    fun getBrawlerDailyResultStatistics(param: BrawlersStatisticsParam): List<BrawlerResultDailyStatistics> =
        resultRepository
            .findBrawlerDailyResultCounts(param.brawlerId, param.tierRange, param.startDate, param.endDate)
            .map { it.toStatistics(param.brawlerId) }

    fun getBrawlerDuoResultStatistics(param: BrawlersStatisticsParam): List<BrawlersResultStatistics> =
        resultRepository
            .findBrawlerDuoResultCounts(param.brawlerId, param.tierRange, param.startDate)
            .map { it.toStatistics() }

    fun getBrawlerTrioResultStatistics(param: BrawlersStatisticsParam): List<BrawlersResultStatistics> =
        resultRepository
            .findBrawlerTrioResultCounts(param.brawlerId, param.tierRange, param.startDate)
            .map { it.toStatistics() }
}
