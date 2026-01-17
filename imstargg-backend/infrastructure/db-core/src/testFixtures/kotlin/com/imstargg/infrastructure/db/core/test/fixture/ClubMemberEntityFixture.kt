package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.infrastructure.db.core.brawlstars.player.ClubEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.ClubMemberEntity
import com.imstargg.infrastructure.db.core.test.fixture.ClubEntityFixture.Companion.aClubEntity

class ClubMemberEntityFixture {
    private var club: ClubEntity = aClubEntity().build()
    private var brawlStarsTag: String = "#TEST12345"
    private var name: String = "테스트플레이어"
    private var nameColor: String? = "0xffffffff"
    private var role: String = "member"
    private var trophies: Int = 10000
    private var iconBrawlStarsId: Long = 28000000L

    companion object {
        fun aClubMemberEntity() = ClubMemberEntityFixture()
    }

    fun withClub(value: ClubEntity) = apply { club = value }

    fun withBrawlStarsTag(value: String) = apply { brawlStarsTag = value }

    fun withName(value: String) = apply { name = value }

    fun withNameColor(value: String?) = apply { nameColor = value }

    fun withRole(value: String) = apply { role = value }

    fun withTrophies(value: Int) = apply { trophies = value }

    fun withIconBrawlStarsId(value: Long) = apply { iconBrawlStarsId = value }

    fun build() =
        ClubMemberEntity(
            club = club,
            brawlStarsTag = brawlStarsTag,
            name = name,
            nameColor = nameColor,
            role = role,
            trophies = trophies,
            iconBrawlStarsId = iconBrawlStarsId,
        )
}
