package com.imstargg.core.admin.response

import com.imstargg.core.enums.Language

data class HyperChargeResponse(
    val brawlStarsId: Long,
    val names: Map<Language, String>,
    val imageUrl: String?,
)
