package com.imstargg.core.admin.response

import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageEntity
import com.imstargg.infrastructure.db.core.brawlstars.brawler.StarPowerEntity

data class StarPowerResponse(
    val starPower: StarPowerEntity,
    val names: Map<Language, String>,
    val image: BrawlStarsImageEntity?,
)
