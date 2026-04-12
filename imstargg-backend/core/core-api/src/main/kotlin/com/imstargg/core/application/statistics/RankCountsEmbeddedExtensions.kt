package com.imstargg.core.application.statistics

import com.imstargg.core.domain.statistics.RankCount
import com.imstargg.infrastructure.db.core.statistics.RankCountsEmbedded

fun RankCountsEmbedded.toRankCount() = RankCount(counts = toMap())
