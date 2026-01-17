package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.brawlstars.ranking.ClubRankingEntity

class ClubRankingEntityFixture {
    private var country: Country = Country.GLOBAL
    private var brawlStarsTag: String = "#CLUB12345"
    private var name: String = "테스트클럽"
    private var badgeBrawlStarsId: Long = 8000000L
    private var trophies: Int = 1000000
    private var rank: Int = 1
    private var memberCount: Int = 30

    companion object {
        fun aClubRankingEntity() = ClubRankingEntityFixture()
    }

    fun withCountry(value: Country) = apply { country = value }

    fun withBrawlStarsTag(value: String) = apply { brawlStarsTag = value }

    fun withName(value: String) = apply { name = value }

    fun withBadgeBrawlStarsId(value: Long) = apply { badgeBrawlStarsId = value }

    fun withTrophies(value: Int) = apply { trophies = value }

    fun withRank(value: Int) = apply { rank = value }

    fun withMemberCount(value: Int) = apply { memberCount = value }

    fun build() =
        ClubRankingEntity(
            country = country,
            brawlStarsTag = brawlStarsTag,
            name = name,
            badgeBrawlStarsId = badgeBrawlStarsId,
            trophies = trophies,
            rank = rank,
            memberCount = memberCount,
        )
}
