package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerBrawlerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerBrawlerGearEntity
import com.imstargg.infrastructure.db.core.test.fixture.PlayerBrawlerEntityFixture.Companion.aPlayerBrawlerEntity

class PlayerBrawlerGearEntityFixture {
    private var playerBrawler: PlayerBrawlerEntity = aPlayerBrawlerEntity().build()
    private var gearBrawlStarsId: Long = 62000000L

    companion object {
        fun aPlayerBrawlerGearEntity() = PlayerBrawlerGearEntityFixture()
    }

    fun withPlayerBrawler(value: PlayerBrawlerEntity) = apply { playerBrawler = value }

    fun withGearBrawlStarsId(value: Long) = apply { gearBrawlStarsId = value }

    fun build() =
        PlayerBrawlerGearEntity(
            playerBrawler = playerBrawler,
            gearBrawlStarsId = gearBrawlStarsId,
        )
}
