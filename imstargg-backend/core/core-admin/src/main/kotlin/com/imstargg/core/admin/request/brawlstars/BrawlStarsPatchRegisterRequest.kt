package com.imstargg.core.admin.request.brawlstars

import java.time.LocalDate

data class BrawlStarsPatchRegisterRequest(
    val patchDate: LocalDate,
    val memo: String? = null,
)
