package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.brawlstars.ranking.BrawlerRankingEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.BrawlerRankingItemEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.RankingEntityPlayer
import java.time.Instant

class BrawlerRankingEntityFixture {
    private var country: Country = Country.GLOBAL
    private var brawlerBrawlStarsId: Long = 16000000L
    private var collectedAt: Instant = Instant.now()

    companion object {
        fun aBrawlerRankingEntity() = BrawlerRankingEntityFixture()
    }

    fun withCountry(value: Country) = apply { country = value }

    fun withBrawlerBrawlStarsId(value: Long) = apply { brawlerBrawlStarsId = value }

    fun withCollectedAt(value: Instant) = apply { collectedAt = value }

    fun build() =
        BrawlerRankingEntity(
            country = country,
            brawlerBrawlStarsId = brawlerBrawlStarsId,
            collectedAt = collectedAt,
        )
}

class BrawlerRankingItemEntityFixture {
    private var brawlerRankingId: Long? = null
    private var rank: Int = 1
    private var player: RankingEntityPlayer = defaultRankingEntityPlayer()
    private var trophies: Int = 1500

    companion object {
        fun aBrawlerRankingItemEntity() = BrawlerRankingItemEntityFixture()

        fun defaultRankingEntityPlayer() =
            RankingEntityPlayer(
                brawlStarsTag = "#TEST12345",
                name = "테스트플레이어",
                nameColor = "0xffffffff",
                iconBrawlStarsId = 28000000L,
                clubName = "테스트클럽",
            )
    }

    fun withBrawlerRankingId(value: Long) = apply { brawlerRankingId = value }

    fun withRank(value: Int) = apply { rank = value }

    fun withPlayer(value: RankingEntityPlayer) = apply { player = value }

    fun withTrophies(value: Int) = apply { trophies = value }

    fun build(): BrawlerRankingItemEntity {
        requireNotNull(brawlerRankingId) { "brawlerRankingId must be set" }
        return BrawlerRankingItemEntity(
            brawlerRankingId = brawlerRankingId!!,
            rank = rank,
            player = player,
            trophies = trophies,
        )
    }
}
