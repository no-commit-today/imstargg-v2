package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventRotationEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventRotationItemEntity
import com.imstargg.infrastructure.db.core.test.fixture.BattleEventRotationEntityFixture.Companion.aBattleEventRotationEntity
import java.time.OffsetDateTime

class BattleEventRotationItemEntityFixture {
    private var battleEventRotation: BattleEventRotationEntity = aBattleEventRotationEntity().build()
    private var eventBrawlStarsId: Long = 15000001L
    private var modifiers: List<String> = emptyList()
    private var slotId: Int = 1
    private var startTime: OffsetDateTime = OffsetDateTime.now()
    private var endTime: OffsetDateTime = OffsetDateTime.now().plusHours(2)

    companion object {
        fun aBattleEventRotationItemEntity() = BattleEventRotationItemEntityFixture()
    }

    fun withBattleEventRotation(value: BattleEventRotationEntity) = apply { battleEventRotation = value }

    fun withEventBrawlStarsId(value: Long) = apply { eventBrawlStarsId = value }

    fun withModifiers(value: List<String>) = apply { modifiers = value }

    fun withSlotId(value: Int) = apply { slotId = value }

    fun withStartTime(value: OffsetDateTime) = apply { startTime = value }

    fun withEndTime(value: OffsetDateTime) = apply { endTime = value }

    fun build() =
        BattleEventRotationItemEntity(
            battleEventRotation = battleEventRotation,
            eventBrawlStarsId = eventBrawlStarsId,
            modifiers = modifiers,
            slotId = slotId,
            startTime = startTime,
            endTime = endTime,
        )
}
