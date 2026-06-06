package com.imstargg.core.domain.renewal

import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.enums.RenewalStatus
import com.imstargg.core.enums.RenewalType
import java.time.Duration
import java.time.Instant

data class Renewal(
    val tag: BrawlStarsTag,
    val type: RenewalType,
    val status: RenewalStatus,
    val requestedAt: Instant,
    val executedAt: Instant?,
    val completedAt: Instant?,
) {
    companion object {
        private val STALE_THRESHOLD = Duration.ofSeconds(10)
    }

    fun canRenew(now: Instant): Boolean {
        if (status.isCompleted()) return true
        val elapsed = Duration.between(requestedAt, now)
        return elapsed > STALE_THRESHOLD
    }
}
