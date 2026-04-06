package com.imstargg.core.domain.brawlstars.brawler

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.Messages

data class HyperCharge(
    val id: BrawlStarsId,
    val name: Messages,
    val imagePath: String?,
)
