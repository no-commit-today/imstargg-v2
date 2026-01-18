package com.imstargg.core.domain.renewal

import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.enums.RenewalStatus
import com.imstargg.core.enums.RenewalType
import java.time.Duration
import java.time.OffsetDateTime

data class Renewal(
    val tag: BrawlStarsTag,
    val type: RenewalType,
    val status: RenewalStatus,
    val requestedAt: OffsetDateTime,
    val executedAt: OffsetDateTime?,
    val completedAt: OffsetDateTime?,
) {
    companion object {
        private val STALE_REQUESTED_THRESHOLD = Duration.ofMinutes(1)
    }

    fun canRenew(now: OffsetDateTime): Boolean =
        when (status) {
            RenewalStatus.PENDING, RenewalStatus.EXECUTING -> false
            RenewalStatus.REQUESTED -> {
                val elapsed = Duration.between(requestedAt, now)
                elapsed > STALE_REQUESTED_THRESHOLD
            }
            else -> true
        }

    fun toPending(): Renewal {
        check(status == RenewalStatus.REQUESTED) {
            "Cannot transition to PENDING from $status"
        }
        return copy(status = RenewalStatus.PENDING)
    }
}
