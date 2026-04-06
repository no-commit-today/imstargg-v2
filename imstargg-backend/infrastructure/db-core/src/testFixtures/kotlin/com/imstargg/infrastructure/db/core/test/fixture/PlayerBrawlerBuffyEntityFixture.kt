package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.core.enums.BuffyType
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerBrawlerBuffyEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerBrawlerEntity
import com.imstargg.infrastructure.db.core.test.fixture.PlayerBrawlerEntityFixture.Companion.aPlayerBrawlerEntity

class PlayerBrawlerBuffyEntityFixture {
    private var playerBrawler: PlayerBrawlerEntity = aPlayerBrawlerEntity().build()
    private var buffyType: BuffyType = BuffyType.GADGET

    companion object {
        fun aPlayerBrawlerBuffyEntity() = PlayerBrawlerBuffyEntityFixture()
    }

    fun withPlayerBrawler(value: PlayerBrawlerEntity) = apply { playerBrawler = value }

    fun withBuffyType(value: BuffyType) = apply { buffyType = value }

    fun build() =
        PlayerBrawlerBuffyEntity(
            playerBrawler = playerBrawler,
            buffyType = buffyType,
        )
}
