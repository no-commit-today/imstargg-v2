package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.core.enums.PlayerStatus
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.PlayerEntityClub

class PlayerEntityFixture {
    private var brawlStarsTag: String = "#TEST12345"
    private var name: String = "테스트플레이어"
    private var nameColor: String? = "0xffffffff"
    private var iconBrawlStarsId: Long = 28000000L
    private var trophies: Int = 10000
    private var highestTrophies: Int = 15000
    private var expLevel: Int = 100
    private var expPoints: Int = 50000
    private var qualifiedFromChampionshipChallenge: Boolean = false
    private var victories3vs3: Int = 1000
    private var victoriesSolo: Int = 500
    private var victoriesDuo: Int = 300
    private var bestRoboRumbleTime: Int = 0
    private var bestTimeAsBigBrawler: Int = 0
    private var club: PlayerEntityClub? = defaultPlayerEntityClub()
    private var status: PlayerStatus = PlayerStatus.ACTIVE

    companion object {
        fun aPlayerEntity() = PlayerEntityFixture()

        fun defaultPlayerEntityClub() =
            PlayerEntityClub(
                brawlStarsTag = "#CLUB12345",
                name = "테스트클럽",
            )
    }

    fun withBrawlStarsTag(value: String) = apply { brawlStarsTag = value }

    fun withName(value: String) = apply { name = value }

    fun withNameColor(value: String?) = apply { nameColor = value }

    fun withIconBrawlStarsId(value: Long) = apply { iconBrawlStarsId = value }

    fun withTrophies(value: Int) = apply { trophies = value }

    fun withHighestTrophies(value: Int) = apply { highestTrophies = value }

    fun withExpLevel(value: Int) = apply { expLevel = value }

    fun withExpPoints(value: Int) = apply { expPoints = value }

    fun withQualifiedFromChampionshipChallenge(value: Boolean) = apply { qualifiedFromChampionshipChallenge = value }

    fun withVictories3vs3(value: Int) = apply { victories3vs3 = value }

    fun withVictoriesSolo(value: Int) = apply { victoriesSolo = value }

    fun withVictoriesDuo(value: Int) = apply { victoriesDuo = value }

    fun withBestRoboRumbleTime(value: Int) = apply { bestRoboRumbleTime = value }

    fun withBestTimeAsBigBrawler(value: Int) = apply { bestTimeAsBigBrawler = value }

    fun withClub(value: PlayerEntityClub?) = apply { club = value }

    fun withStatus(value: PlayerStatus) = apply { status = value }

    fun build(): PlayerEntity {
        val entity =
            PlayerEntity(
                brawlStarsTag = brawlStarsTag,
                name = name,
                nameColor = nameColor,
                iconBrawlStarsId = iconBrawlStarsId,
                trophies = trophies,
                highestTrophies = highestTrophies,
                expLevel = expLevel,
                expPoints = expPoints,
                qualifiedFromChampionshipChallenge = qualifiedFromChampionshipChallenge,
                victories3vs3 = victories3vs3,
                victoriesSolo = victoriesSolo,
                victoriesDuo = victoriesDuo,
                bestRoboRumbleTime = bestRoboRumbleTime,
                bestTimeAsBigBrawler = bestTimeAsBigBrawler,
                club = club,
            )
        if (status == PlayerStatus.DELETED) {
            entity.toDeleted()
        }
        return entity
    }
}
