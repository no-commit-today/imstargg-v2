package com.imstargg.core.infrastructure.jpa

import com.imstargg.core.domain.statistics.RankCount
import com.imstargg.infrastructure.db.core.statistics.RankCountsEmbedded

fun RankCountsEmbedded.toRankCount() = RankCount(counts = toMap())
