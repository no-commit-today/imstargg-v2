package com.imstargg.core.batch.domain.statistics

import com.imstargg.core.enums.StatsTierRange

/**
 * 2인 조합 집계 키. 조합은 순서가 없으므로 `brawler1BrawlStarsId < brawler2BrawlStarsId` 로 정렬해 담는다.
 */
data class BrawlerPairBattleStatsKey(
    val brawler1BrawlStarsId: Long,
    val brawler2BrawlStarsId: Long,
    val tierRange: StatsTierRange,
)
