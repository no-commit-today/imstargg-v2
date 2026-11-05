package com.imstargg.core.batch.domain.statistics

import com.imstargg.core.enums.StatsTierRange

/**
 * 3인 조합 집계 키. 조합은 순서가 없으므로 `brawler1BrawlStarsId < brawler2BrawlStarsId < brawler3BrawlStarsId` 로 정렬해 담는다.
 */
data class BrawlerTrioBattleStatsKey(
    val brawler1BrawlStarsId: Long,
    val brawler2BrawlStarsId: Long,
    val brawler3BrawlStarsId: Long,
    val tierRange: StatsTierRange,
)
