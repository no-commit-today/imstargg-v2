package com.imstargg.storage.db.core.brawlstars.player

import com.imstargg.storage.db.core.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "player",
    indexes = [
        Index(name = "uk_player__brawlstarstag", columnList = "brawlstars_tag", unique = true),
    ],
)
class PlayerEntity(
    @Column(name = "brawlstars_tag", length = 45, updatable = false, nullable = false)
    val brawlStarsTag: String,
    name: String,
    nameColor: String?,
    iconBrawlStarsId: Long,
    trophies: Int,
    highestTrophies: Int,
    expLevel: Int,
    expPoints: Int,
    qualifiedFromChampionshipChallenge: Boolean,
    victories3vs3: Int,
    victoriesSolo: Int,
    victoriesDuo: Int,
    bestRoboRumbleTime: Int,
    bestTimeAsBigBrawler: Int,
    club: PlayerEntityClub?,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "player_id")
    val id: Long? = null

    @Column(name = "name", length = 105, nullable = false)
    var name: String = name
        protected set

    @Column(name = "name_color", length = 45)
    var nameColor: String? = nameColor
        protected set

    @Column(name = "icon_brawlstars_id", nullable = false)
    var iconBrawlStarsId: Long = iconBrawlStarsId
        protected set

    @Column(name = "trophies", nullable = false)
    var trophies: Int = trophies
        protected set

    @Column(name = "highest_trophies", nullable = false)
    var highestTrophies: Int = highestTrophies
        protected set

    @Column(name = "exp_level", nullable = false)
    var expLevel: Int = expLevel
        protected set

    @Column(name = "exp_points", nullable = false)
    var expPoints: Int = expPoints
        protected set

    @Column(name = "qualified_from_championship_challenge", nullable = false)
    var qualifiedFromChampionshipChallenge: Boolean = qualifiedFromChampionshipChallenge
        protected set

    @Column(name = "victories_3vs3", nullable = false)
    var victories3vs3: Int = victories3vs3
        protected set

    @Column(name = "victories_solo", nullable = false)
    var victoriesSolo: Int = victoriesSolo
        protected set

    @Column(name = "victories_duo", nullable = false)
    var victoriesDuo: Int = victoriesDuo
        protected set

    @Column(name = "best_robo_rumble_time", nullable = false)
    var bestRoboRumbleTime: Int = bestRoboRumbleTime
        protected set

    @Column(name = "best_time_as_big_brawler", nullable = false)
    var bestTimeAsBigBrawler: Int = bestTimeAsBigBrawler
        protected set

    @Embedded
    var club: PlayerEntityClub? = club
        protected set

    fun update(
        name: String,
        nameColor: String?,
        iconBrawlStarsId: Long,
        trophies: Int,
        highestTrophies: Int,
        expLevel: Int,
        expPoints: Int,
        qualifiedFromChampionshipChallenge: Boolean,
        victories3vs3: Int,
        victoriesSolo: Int,
        victoriesDuo: Int,
        bestRoboRumbleTime: Int,
        bestTimeAsBigBrawler: Int,
        club: PlayerEntityClub?,
    ) {
        this.name = name
        this.nameColor = nameColor
        this.iconBrawlStarsId = iconBrawlStarsId
        this.trophies = trophies
        this.highestTrophies = highestTrophies
        this.expLevel = expLevel
        this.expPoints = expPoints
        this.qualifiedFromChampionshipChallenge = qualifiedFromChampionshipChallenge
        this.victories3vs3 = victories3vs3
        this.victoriesSolo = victoriesSolo
        this.victoriesDuo = victoriesDuo
        this.bestRoboRumbleTime = bestRoboRumbleTime
        this.bestTimeAsBigBrawler = bestTimeAsBigBrawler
        this.club = club
    }
}
