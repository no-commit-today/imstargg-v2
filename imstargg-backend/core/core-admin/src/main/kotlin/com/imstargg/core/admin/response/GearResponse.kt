package com.imstargg.core.admin.response

import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageEntity
import com.imstargg.infrastructure.db.core.brawlstars.brawler.GearEntity

data class GearResponse(
    val gear: GearEntity,
    val names: Map<Language, String>,
    val image: BrawlStarsImageEntity?,
)
