package com.imstargg.core.domain.brawlstars.brawler

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.Messages

data class Gadget(
    val id: BrawlStarsId,
    val names: Messages,
    val imagePath: String?,
)
