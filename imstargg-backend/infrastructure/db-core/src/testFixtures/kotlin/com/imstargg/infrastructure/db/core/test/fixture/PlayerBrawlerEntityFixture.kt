package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerBrawlerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerEntity
import com.imstargg.infrastructure.db.core.test.fixture.PlayerEntityFixture.Companion.aPlayerEntity

class PlayerBrawlerEntityFixture {
    private var player: PlayerEntity = aPlayerEntity().build()
    private var brawlerBrawlStarsId: Long = 16000000L
    private var power: Int = 11
    private var rank: Int = 25
    private var trophies: Int = 750
    private var highestTrophies: Int = 800
    private var prestigeLevel: Int = 0

    companion object {
        fun aPlayerBrawlerEntity() = PlayerBrawlerEntityFixture()
    }

    fun withPlayer(value: PlayerEntity) = apply { player = value }

    fun withBrawlerBrawlStarsId(value: Long) = apply { brawlerBrawlStarsId = value }

    fun withPower(value: Int) = apply { power = value }

    fun withRank(value: Int) = apply { rank = value }

    fun withTrophies(value: Int) = apply { trophies = value }

    fun withHighestTrophies(value: Int) = apply { highestTrophies = value }

    fun withPrestigeLevel(value: Int) = apply { prestigeLevel = value }

    fun build() =
        PlayerBrawlerEntity(
            player = player,
            brawlerBrawlStarsId = brawlerBrawlStarsId,
            power = power,
            rank = rank,
            trophies = trophies,
            highestTrophies = highestTrophies,
            prestigeLevel = prestigeLevel,
        )
}
