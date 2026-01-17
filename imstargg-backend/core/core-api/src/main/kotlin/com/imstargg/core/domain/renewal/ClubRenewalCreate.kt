package com.imstargg.core.domain.renewal

import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.enums.RenewalStatus
import java.time.OffsetDateTime

data class ClubRenewalCreate(
    val tag: BrawlStarsTag,
    val status: RenewalStatus,
    val requestedAt: OffsetDateTime,
)
