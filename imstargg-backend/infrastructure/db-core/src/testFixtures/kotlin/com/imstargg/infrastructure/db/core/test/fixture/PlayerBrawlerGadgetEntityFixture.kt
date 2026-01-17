package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerBrawlerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerBrawlerGadgetEntity
import com.imstargg.infrastructure.db.core.test.fixture.PlayerBrawlerEntityFixture.Companion.aPlayerBrawlerEntity

class PlayerBrawlerGadgetEntityFixture {
    private var playerBrawler: PlayerBrawlerEntity = aPlayerBrawlerEntity().build()
    private var gadgetBrawlStarsId: Long = 23000001L

    companion object {
        fun aPlayerBrawlerGadgetEntity() = PlayerBrawlerGadgetEntityFixture()
    }

    fun withPlayerBrawler(value: PlayerBrawlerEntity) = apply { playerBrawler = value }

    fun withGadgetBrawlStarsId(value: Long) = apply { gadgetBrawlStarsId = value }

    fun build() =
        PlayerBrawlerGadgetEntity(
            playerBrawler = playerBrawler,
            gadgetBrawlStarsId = gadgetBrawlStarsId,
        )
}
