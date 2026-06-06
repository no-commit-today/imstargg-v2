package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.brawlstars.ranking.PlayerRankingEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.PlayerRankingItemEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.RankingEntityPlayer
import java.time.Instant

class PlayerRankingEntityFixture {
    private var country: Country = Country.GLOBAL
    private var collectedAt: Instant = Instant.now()

    companion object {
        fun aPlayerRankingEntity() = PlayerRankingEntityFixture()
    }

    fun withCountry(value: Country) = apply { country = value }

    fun withCollectedAt(value: Instant) = apply { collectedAt = value }

    fun build() =
        PlayerRankingEntity(
            country = country,
            collectedAt = collectedAt,
        )
}

class PlayerRankingItemEntityFixture {
    private var playerRankingId: Long? = null
    private var rank: Int = 1
    private var player: RankingEntityPlayer = defaultRankingEntityPlayer()
    private var trophies: Int = 50000

    companion object {
        fun aPlayerRankingItemEntity() = PlayerRankingItemEntityFixture()

        fun defaultRankingEntityPlayer() =
            RankingEntityPlayer(
                brawlStarsTag = "#TEST12345",
                name = "테스트플레이어",
                nameColor = "0xffffffff",
                iconBrawlStarsId = 28000000L,
                clubName = "테스트클럽",
            )
    }

    fun withPlayerRankingId(value: Long) = apply { playerRankingId = value }

    fun withRank(value: Int) = apply { rank = value }

    fun withPlayer(value: RankingEntityPlayer) = apply { player = value }

    fun withTrophies(value: Int) = apply { trophies = value }

    fun build(): PlayerRankingItemEntity {
        requireNotNull(playerRankingId) { "playerRankingId must be set" }
        return PlayerRankingItemEntity(
            playerRankingId = playerRankingId!!,
            rank = rank,
            player = player,
            trophies = trophies,
        )
    }
}
