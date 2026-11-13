package com.imstargg.core.admin.response.brawlstars

import java.time.LocalDate

data class BrawlStarsPatchListResponse(
    val today: LocalDate,
    val earliestRegistrableDate: LocalDate,
    val patches: List<BrawlStarsPatchResponse>,
)
