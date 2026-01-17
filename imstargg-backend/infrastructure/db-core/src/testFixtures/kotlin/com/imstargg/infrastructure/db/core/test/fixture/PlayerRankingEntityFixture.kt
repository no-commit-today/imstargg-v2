package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.brawlstars.ranking.PlayerRankingEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.RankingEntityPlayer

class PlayerRankingEntityFixture {
    private var country: Country = Country.GLOBAL
    private var rank: Int = 1
    private var player: RankingEntityPlayer = defaultRankingEntityPlayer()
    private var trophies: Int = 50000
    private var rankChange: Int? = null

    companion object {
        fun aPlayerRankingEntity() = PlayerRankingEntityFixture()

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

    fun withRank(value: Int) = apply { rank = value }

    fun withPlayer(value: RankingEntityPlayer) = apply { player = value }

    fun withTrophies(value: Int) = apply { trophies = value }

    fun withRankChange(value: Int?) = apply { rankChange = value }

    fun build() =
        PlayerRankingEntity(
            country = country,
            rank = rank,
            player = player,
            trophies = trophies,
            rankChange = rankChange,
        )
}
