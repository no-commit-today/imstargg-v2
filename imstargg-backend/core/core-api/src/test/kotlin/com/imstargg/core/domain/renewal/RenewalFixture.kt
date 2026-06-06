package com.imstargg.core.domain.renewal

import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.enums.RenewalStatus
import com.imstargg.core.enums.RenewalType
import java.time.Instant

class RenewalFixture {
    private var tag: BrawlStarsTag = BrawlStarsTag("#TEST12345")
    private var type: RenewalType = RenewalType.PLAYER
    private var status: RenewalStatus = RenewalStatus.REQUESTED
    private var requestedAt: Instant = Instant.now()
    private var executedAt: Instant? = null
    private var completedAt: Instant? = null

    companion object {
        fun aRenewal() = RenewalFixture()
    }

    fun withTag(value: BrawlStarsTag) = apply { tag = value }

    fun withType(value: RenewalType) = apply { type = value }

    fun withStatus(value: RenewalStatus) = apply { status = value }

    fun withRequestedAt(value: Instant) = apply { requestedAt = value }

    fun withExecutedAt(value: Instant?) = apply { executedAt = value }

    fun withCompletedAt(value: Instant?) = apply { completedAt = value }

    fun build() =
        Renewal(
            tag = tag,
            type = type,
            status = status,
            requestedAt = requestedAt,
            executedAt = executedAt,
            completedAt = completedAt,
        )
}
