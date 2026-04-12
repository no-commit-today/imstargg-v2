package com.imstargg.core.admin.controller.response

import com.imstargg.core.enums.Language

data class GadgetResponse(
    val brawlStarsId: Long,
    val names: Map<Language, String>,
    val imageUrl: String?,
)
