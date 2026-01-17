package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.core.enums.RenewalType
import com.imstargg.infrastructure.db.core.renewal.RenewalEntity
import java.time.OffsetDateTime

class RenewalEntityFixture {
    private var brawlStarsTag: String = "#TEST12345"
    private var type: RenewalType = RenewalType.PLAYER
    private var requestedAt: OffsetDateTime = OffsetDateTime.now()

    companion object {
        fun aRenewalEntity() = RenewalEntityFixture()
    }

    fun withBrawlStarsTag(value: String) = apply { brawlStarsTag = value }

    fun withType(value: RenewalType) = apply { type = value }

    fun withRequestedAt(value: OffsetDateTime) = apply { requestedAt = value }

    fun build() =
        RenewalEntity(
            brawlStarsTag = brawlStarsTag,
            type = type,
            requestedAt = requestedAt,
        )
}
