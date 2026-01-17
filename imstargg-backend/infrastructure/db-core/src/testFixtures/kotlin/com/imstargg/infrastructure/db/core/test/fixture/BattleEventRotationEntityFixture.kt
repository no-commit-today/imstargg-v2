package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventRotationEntity

class BattleEventRotationEntityFixture {
    companion object {
        fun aBattleEventRotationEntity() = BattleEventRotationEntityFixture()
    }

    fun build() = BattleEventRotationEntity()
}
