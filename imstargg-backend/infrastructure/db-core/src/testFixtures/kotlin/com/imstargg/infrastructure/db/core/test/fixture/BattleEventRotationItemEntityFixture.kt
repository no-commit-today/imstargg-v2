package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventRotationItemEntity
import java.time.OffsetDateTime

class BattleEventRotationItemEntityFixture {
    private var eventBrawlStarsId: Long = 15000001L
    private var modifiers: List<String> = emptyList()
    private var slotId: Int = 1
    private var startTime: OffsetDateTime = OffsetDateTime.now()
    private var endTime: OffsetDateTime = OffsetDateTime.now().plusHours(2)
    private var eventName: String? = null
    private var eventModeName: String? = null
    private var eventModeBrawlStarsId: Long? = null
    private var eventMapName: String? = null

    companion object {
        fun aBattleEventRotationItemEntity() = BattleEventRotationItemEntityFixture()
    }

    fun withEventBrawlStarsId(value: Long) = apply { eventBrawlStarsId = value }

    fun withModifiers(value: List<String>) = apply { modifiers = value }

    fun withSlotId(value: Int) = apply { slotId = value }

    fun withStartTime(value: OffsetDateTime) = apply { startTime = value }

    fun withEndTime(value: OffsetDateTime) = apply { endTime = value }

    fun withEventName(value: String?) = apply { eventName = value }

    fun withEventModeName(value: String?) = apply { eventModeName = value }

    fun withEventModeBrawlStarsId(value: Long?) = apply { eventModeBrawlStarsId = value }

    fun withEventMapName(value: String?) = apply { eventMapName = value }

    fun build() =
        BattleEventRotationItemEntity(
            eventBrawlStarsId = eventBrawlStarsId,
            modifiers = modifiers,
            slotId = slotId,
            startTime = startTime,
            endTime = endTime,
            eventName = eventName,
            eventModeName = eventModeName,
            eventModeBrawlStarsId = eventModeBrawlStarsId,
            eventMapName = eventMapName,
        )
}
