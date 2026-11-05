package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.core.enums.StatsTierRange
import java.time.LocalDate

interface BrawlerPairBattleResultStatsJpaRepositoryCustom {
    /**
     * 브롤러가 brawler1 이든 brawler2 이든 그 브롤러가 낀 모든 조합을, 기간 안 버킷을 합산해 돌려준다.
     */
    fun findAllByBrawler(
        brawlerBrawlStarsId: Long,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BrawlerPairResultProjection>

    /**
     * 기간 안 버킷을 합산한 판 수가 [minTotalCount] 이상인 조합 전체를 돌려준다(전체 조합 순위용).
     */
    fun findAllHavingTotalCountAtLeast(
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
        minTotalCount: Long,
    ): List<BrawlerPairResultProjection>

    /**
     * 기간 안 모든 조합의 total_count 합. 행이 없으면 0.
     */
    fun sumTotalCount(
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Long
}
