package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId

object StatisticsCacheKeys {
    @JvmStatic
    fun joinIds(ids: List<BrawlStarsId>): String = ids.map { it.value }.sorted().joinToString("_")
}
