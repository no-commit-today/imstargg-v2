package com.imstargg.core.admin.request

import com.imstargg.core.enums.Language

data class BrawlerSubItemCreateRequest(
    val brawlStarsId: Long,
    val names: Map<Language, String> = emptyMap(),
)
