package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventRotationEntity
import java.time.Instant

class BattleEventRotationEntityFixture {
    private var collectedAt: Instant = Instant.now()

    companion object {
        fun aBattleEventRotationEntity() = BattleEventRotationEntityFixture()
    }

    fun withCollectedAt(value: Instant) = apply { collectedAt = value }

    fun build() = BattleEventRotationEntity(collectedAt = collectedAt)
}
