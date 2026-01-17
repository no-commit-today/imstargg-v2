package com.imstargg.storage.db.core.brawlstars.player

import com.imstargg.storage.db.core.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.ConstraintMode
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(
    name = "player_brawler",
    indexes = [
        Index(name = "uk_playerbrawler__playerid_brawlerbrawlstarsid", columnList = "player_id, brawler_brawlstars_id", unique = true),
    ],
)
class PlayerBrawlerEntity(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", updatable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val player: PlayerEntity,
    @Column(name = "brawler_brawlstars_id", updatable = false, nullable = false)
    val brawlerBrawlStarsId: Long,
    power: Int,
    rank: Int,
    trophies: Int,
    highestTrophies: Int,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "player_brawler_id")
    val id: Long? = null

    @Column(name = "power", nullable = false)
    var power: Int = power
        protected set

    @Column(name = "trophy_rank", nullable = false)
    var rank: Int = rank
        protected set

    @Column(name = "trophies", nullable = false)
    var trophies: Int = trophies
        protected set

    @Column(name = "highest_trophies", nullable = false)
    var highestTrophies: Int = highestTrophies
        protected set

    fun update(
        power: Int,
        rank: Int,
        trophies: Int,
        highestTrophies: Int,
    ) {
        this.power = power
        this.rank = rank
        this.trophies = trophies
        this.highestTrophies = highestTrophies
    }
}
