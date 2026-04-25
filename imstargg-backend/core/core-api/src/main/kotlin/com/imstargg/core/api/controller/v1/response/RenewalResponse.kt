package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.renewal.Renewal
import com.imstargg.core.enums.RenewalStatus
import java.time.OffsetDateTime

data class RenewalResponse(
    val tag: String,
    val status: RenewalStatus,
    val progressing: Boolean,
    val canRenew: Boolean,
    val requestedAt: OffsetDateTime,
    val executedAt: OffsetDateTime?,
    val completedAt: OffsetDateTime?,
    val renewedAt: OffsetDateTime?,
) {
    companion object {
        fun of(
            renewal: Renewal,
            now: OffsetDateTime,
        ) = RenewalResponse(
            tag = renewal.tag.value,
            status = renewal.status,
            progressing = renewal.status.isProgressing(),
            canRenew = renewal.canRenew(now),
            requestedAt = renewal.requestedAt,
            executedAt = renewal.executedAt,
            completedAt = renewal.completedAt,
            renewedAt = renewal.completedAt,
        )
    }
}
