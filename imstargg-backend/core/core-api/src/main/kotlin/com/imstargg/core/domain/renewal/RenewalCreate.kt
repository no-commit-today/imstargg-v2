package com.imstargg.core.domain.renewal

import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.enums.RenewalType
import java.time.OffsetDateTime

data class RenewalCreate(
    val tag: BrawlStarsTag,
    val type: RenewalType,
    val requestedAt: OffsetDateTime,
)
