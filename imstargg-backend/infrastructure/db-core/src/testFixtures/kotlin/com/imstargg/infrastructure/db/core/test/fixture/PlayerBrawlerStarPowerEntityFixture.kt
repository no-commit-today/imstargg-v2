package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerBrawlerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerBrawlerStarPowerEntity
import com.imstargg.infrastructure.db.core.test.fixture.PlayerBrawlerEntityFixture.Companion.aPlayerBrawlerEntity

class PlayerBrawlerStarPowerEntityFixture {
    private var playerBrawler: PlayerBrawlerEntity = aPlayerBrawlerEntity().build()
    private var starPowerBrawlStarsId: Long = 23000000L

    companion object {
        fun aPlayerBrawlerStarPowerEntity() = PlayerBrawlerStarPowerEntityFixture()
    }

    fun withPlayerBrawler(value: PlayerBrawlerEntity) = apply { playerBrawler = value }

    fun withStarPowerBrawlStarsId(value: Long) = apply { starPowerBrawlStarsId = value }

    fun build() =
        PlayerBrawlerStarPowerEntity(
            playerBrawler = playerBrawler,
            starPowerBrawlStarsId = starPowerBrawlStarsId,
        )
}
