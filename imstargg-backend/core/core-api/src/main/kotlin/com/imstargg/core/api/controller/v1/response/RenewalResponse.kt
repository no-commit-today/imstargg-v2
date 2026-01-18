package com.imstargg.core.api.controller.v1.response

import com.imstargg.core.domain.renewal.Renewal
import com.imstargg.core.enums.RenewalStatus
import java.time.OffsetDateTime

data class RenewalResponse(
    val tag: String,
    val status: RenewalStatus,
    val progressing: Boolean,
    val requestedAt: OffsetDateTime,
    val executedAt: OffsetDateTime?,
    val completedAt: OffsetDateTime?,
) {
    companion object {
        fun of(renewal: Renewal, progressing: Boolean) =
            RenewalResponse(
                tag = renewal.tag.value,
                status = renewal.status,
                progressing = progressing,
                requestedAt = renewal.requestedAt,
                executedAt = renewal.executedAt,
                completedAt = renewal.completedAt,
            )
    }
}
