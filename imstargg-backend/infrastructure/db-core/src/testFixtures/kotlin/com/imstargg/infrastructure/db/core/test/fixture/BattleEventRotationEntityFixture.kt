package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventRotationEntity
import java.time.OffsetDateTime

class BattleEventRotationEntityFixture {
    private var collectedAt: OffsetDateTime = OffsetDateTime.now()

    companion object {
        fun aBattleEventRotationEntity() = BattleEventRotationEntityFixture()
    }

    fun withCollectedAt(value: OffsetDateTime) = apply { collectedAt = value }

    fun build() = BattleEventRotationEntity(collectedAt = collectedAt)
}
