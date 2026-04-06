package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerBrawlerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerBrawlerHyperChargeEntity
import com.imstargg.infrastructure.db.core.test.fixture.PlayerBrawlerEntityFixture.Companion.aPlayerBrawlerEntity

class PlayerBrawlerHyperChargeEntityFixture {
    private var playerBrawler: PlayerBrawlerEntity = aPlayerBrawlerEntity().build()
    private var hyperChargeBrawlStarsId: Long = 19000001L

    companion object {
        fun aPlayerBrawlerHyperChargeEntity() = PlayerBrawlerHyperChargeEntityFixture()
    }

    fun withPlayerBrawler(value: PlayerBrawlerEntity) = apply { playerBrawler = value }

    fun withHyperChargeBrawlStarsId(value: Long) = apply { hyperChargeBrawlStarsId = value }

    fun build() =
        PlayerBrawlerHyperChargeEntity(
            playerBrawler = playerBrawler,
            hyperChargeBrawlStarsId = hyperChargeBrawlStarsId,
        )
}
