package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.core.enums.ClubStatus
import com.imstargg.infrastructure.db.core.brawlstars.player.ClubEntity

class ClubEntityFixture {
    private var brawlStarsTag: String = "#CLUB12345"
    private var badgeBrawlStarsId: Long = 8000000L
    private var name: String = "테스트클럽"
    private var description: String = "테스트 클럽 설명"
    private var type: String = "open"
    private var requiredTrophies: Int = 0
    private var trophies: Int = 100000
    private var status: ClubStatus = ClubStatus.ACTIVE

    companion object {
        fun aClubEntity() = ClubEntityFixture()
    }

    fun withBrawlStarsTag(value: String) = apply { brawlStarsTag = value }

    fun withBadgeBrawlStarsId(value: Long) = apply { badgeBrawlStarsId = value }

    fun withName(value: String) = apply { name = value }

    fun withDescription(value: String) = apply { description = value }

    fun withType(value: String) = apply { type = value }

    fun withRequiredTrophies(value: Int) = apply { requiredTrophies = value }

    fun withTrophies(value: Int) = apply { trophies = value }

    fun withStatus(value: ClubStatus) = apply { status = value }

    fun build(): ClubEntity {
        val entity =
            ClubEntity(
                brawlStarsTag = brawlStarsTag,
                badgeBrawlStarsId = badgeBrawlStarsId,
                name = name,
                description = description,
                type = type,
                requiredTrophies = requiredTrophies,
                trophies = trophies,
            )
        if (status == ClubStatus.DELETED) {
            entity.toDeleted()
        }
        return entity
    }
}
