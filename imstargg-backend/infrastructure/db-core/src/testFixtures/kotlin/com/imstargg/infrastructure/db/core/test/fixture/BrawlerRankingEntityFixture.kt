package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.brawlstars.ranking.BrawlerRankingEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.RankingEntityPlayer

class BrawlerRankingEntityFixture {
    private var country: Country = Country.GLOBAL
    private var brawlerBrawlStarsId: Long = 16000000L
    private var rank: Int = 1
    private var player: RankingEntityPlayer = defaultRankingEntityPlayer()
    private var trophies: Int = 1500
    private var rankChange: Int? = null

    companion object {
        fun aBrawlerRankingEntity() = BrawlerRankingEntityFixture()

        fun defaultRankingEntityPlayer() =
            RankingEntityPlayer(
                brawlStarsTag = "#TEST12345",
                name = "테스트플레이어",
                nameColor = "0xffffffff",
                iconBrawlStarsId = 28000000L,
                clubName = "테스트클럽",
            )
    }

    fun withCountry(value: Country) = apply { country = value }

    fun withBrawlerBrawlStarsId(value: Long) = apply { brawlerBrawlStarsId = value }

    fun withRank(value: Int) = apply { rank = value }

    fun withPlayer(value: RankingEntityPlayer) = apply { player = value }

    fun withTrophies(value: Int) = apply { trophies = value }

    fun withRankChange(value: Int?) = apply { rankChange = value }

    fun build() =
        BrawlerRankingEntity(
            country = country,
            brawlerBrawlStarsId = brawlerBrawlStarsId,
            rank = rank,
            player = player,
            trophies = trophies,
            rankChange = rankChange,
        )
}
