package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntity
import com.imstargg.infrastructure.db.core.brawlstars.battle.BattleEntityEvent
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerEntity
import com.imstargg.infrastructure.db.core.test.fixture.PlayerEntityFixture.Companion.aPlayerEntity
import java.time.Instant

class BattleEntityFixture {
    private var battleKey: String = "battle-key-12345"
    private var battleAt: Instant = Instant.now()
    private var event: BattleEntityEvent? = defaultBattleEntityEvent()
    private var mode: String? = "gemGrab"
    private var type: String? = "ranked"
    private var result: String? = "victory"
    private var duration: Int? = 120
    private var starPlayerBrawlStarsTag: String? = "#TEST12345"
    private var player: PlayerEntity = aPlayerEntity().build()
    private var battleRank: Int? = 1
    private var trophyChange: Int? = 8

    companion object {
        fun aBattleEntity() = BattleEntityFixture()

        fun defaultBattleEntityEvent() =
            BattleEntityEvent(
                brawlStarsId = 15000001L,
                modeBrawlStarsName = "gemGrab",
                modeBrawlStarsId = 1L,
                map = "Hard Rock Mine",
            )
    }

    fun withBattleKey(value: String) = apply { battleKey = value }

    fun withBattleAt(value: Instant) = apply { battleAt = value }

    fun withEvent(value: BattleEntityEvent?) = apply { event = value }

    fun withMode(value: String?) = apply { mode = value }

    fun withType(value: String?) = apply { type = value }

    fun withResult(value: String?) = apply { result = value }

    fun withDuration(value: Int?) = apply { duration = value }

    fun withStarPlayerBrawlStarsTag(value: String?) = apply { starPlayerBrawlStarsTag = value }

    fun withPlayer(value: PlayerEntity) = apply { player = value }

    fun withBattleRank(value: Int?) = apply { battleRank = value }

    fun withTrophyChange(value: Int?) = apply { trophyChange = value }

    fun build() =
        BattleEntity(
            battleKey = battleKey,
            battleAt = battleAt,
            event = event,
            mode = mode,
            type = type,
            result = result,
            duration = duration,
            starPlayerBrawlStarsTag = starPlayerBrawlStarsTag,
            player = player,
            battleRank = battleRank,
            trophyChange = trophyChange,
        )
}
