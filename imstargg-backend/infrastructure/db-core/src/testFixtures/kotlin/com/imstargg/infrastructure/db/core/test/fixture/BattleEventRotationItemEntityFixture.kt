package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventRotationEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventRotationItemEntity
import java.time.Duration
import java.time.Instant

class BattleEventRotationItemEntityFixture {
    private var battleEventRotation: BattleEventRotationEntity? = null
    private var eventBrawlStarsId: Long = 15000001L
    private var modifiers: List<String> = emptyList()
    private var slotId: Int = 1
    private var startTime: Instant = Instant.now()
    private var endTime: Instant = Instant.now().plus(Duration.ofHours(2))
    private var eventModeName: String? = null
    private var eventModeBrawlStarsId: Long? = null
    private var eventMapName: String? = null

    companion object {
        fun aBattleEventRotationItemEntity() = BattleEventRotationItemEntityFixture()
    }

    fun withBattleEventRotation(value: BattleEventRotationEntity) = apply { battleEventRotation = value }

    fun withEventBrawlStarsId(value: Long) = apply { eventBrawlStarsId = value }

    fun withModifiers(value: List<String>) = apply { modifiers = value }

    fun withSlotId(value: Int) = apply { slotId = value }

    fun withStartTime(value: Instant) = apply { startTime = value }

    fun withEndTime(value: Instant) = apply { endTime = value }

    fun withEventModeName(value: String?) = apply { eventModeName = value }

    fun withEventModeBrawlStarsId(value: Long?) = apply { eventModeBrawlStarsId = value }

    fun withEventMapName(value: String?) = apply { eventMapName = value }

    fun build(): BattleEventRotationItemEntity {
        requireNotNull(battleEventRotation) { "battleEventRotation must be set" }
        return BattleEventRotationItemEntity(
            battleEventRotation = battleEventRotation!!,
            eventBrawlStarsId = eventBrawlStarsId,
            modifiers = modifiers,
            slotId = slotId,
            startTime = startTime,
            endTime = endTime,
            eventModeName = eventModeName,
            eventModeBrawlStarsId = eventModeBrawlStarsId,
            eventMapName = eventMapName,
        )
    }
}
