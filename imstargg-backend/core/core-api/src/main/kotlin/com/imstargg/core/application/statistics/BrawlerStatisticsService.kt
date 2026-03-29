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
    fun getBrawlerResultStatistics(param: BrawlerStatisticsParam): BrawlerResultStatistics =
        BrawlerResultStatistics.create(
            resultRepository.findBrawlerResultCounts(param.tierRange, param.startDate, param.endDate),
        )

    fun getBrawlerDailyResultStatistics(param: BrawlersStatisticsParam): List<BrawlerResultDailyStatistics> =
        resultRepository
            .findBrawlerDailyResultCounts(param.brawlerId, param.tierRange, param.startDate, param.endDate)
            .map { BrawlerResultDailyStatistics.create(it) }

    fun getBrawlerDuoResultStatistics(param: BrawlersStatisticsParam): BrawlersResultStatistics =
        BrawlersResultStatistics.create(
            resultRepository.findBrawlerDuoResultCounts(param.brawlerId, param.tierRange, param.startDate),
        )

    fun getBrawlerTrioResultStatistics(param: BrawlersStatisticsParam): BrawlersResultStatistics =
        BrawlersResultStatistics.create(
            resultRepository.findBrawlerTrioResultCounts(param.brawlerId, param.tierRange, param.startDate),
        )
}
