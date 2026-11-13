package com.imstargg.core.batch.domain.statistics.patch

import com.imstargg.core.enums.StatsTierRange

/** 1인 통계(승률·순위)의 집계 키. 날짜가 없다. */
data class BrawlerEventBattleStatsKey(
    val tierRange: StatsTierRange,
    val eventBrawlStarsId: Long,
    val brawlerBrawlStarsId: Long,
)
