package com.imstargg.core.infrastructure.jpa

import com.imstargg.core.domain.statistics.ResultCount
import com.imstargg.infrastructure.db.core.statistics.ResultCountEmbedded

fun ResultCountEmbedded.toResultCount() =
    ResultCount(
        victoryCount = victoryCount,
        defeatCount = defeatCount,
        drawCount = drawCount,
    )
