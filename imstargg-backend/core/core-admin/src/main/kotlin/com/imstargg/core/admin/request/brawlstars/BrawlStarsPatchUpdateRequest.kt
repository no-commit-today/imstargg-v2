package com.imstargg.core.admin.request.brawlstars

import java.time.LocalDate

data class BrawlStarsPatchUpdateRequest(
    val patchDate: LocalDate,
    val memo: String? = null,
)
