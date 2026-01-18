package com.imstargg.core.domain.renewal

import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.enums.RenewalStatus
import com.imstargg.core.enums.RenewalType
import java.time.OffsetDateTime

class RenewalFixture {
    private var tag: BrawlStarsTag = BrawlStarsTag("#TEST12345")
    private var type: RenewalType = RenewalType.PLAYER
    private var status: RenewalStatus = RenewalStatus.REQUESTED
    private var requestedAt: OffsetDateTime = OffsetDateTime.now()
    private var executedAt: OffsetDateTime? = null
    private var completedAt: OffsetDateTime? = null

    companion object {
        fun aRenewal() = RenewalFixture()
    }

    fun withTag(value: BrawlStarsTag) = apply { tag = value }

    fun withType(value: RenewalType) = apply { type = value }

    fun withStatus(value: RenewalStatus) = apply { status = value }

    fun withRequestedAt(value: OffsetDateTime) = apply { requestedAt = value }

    fun withExecutedAt(value: OffsetDateTime?) = apply { executedAt = value }

    fun withCompletedAt(value: OffsetDateTime?) = apply { completedAt = value }

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
