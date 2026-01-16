package com.imstargg.storage.db.core.brawlstars.player

import com.imstargg.storage.db.core.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "player_brawler",
    indexes = [
        Index(name = "uk_playerbrawler__playerid_brawlerbrawlstarsid", columnList = "player_id, brawler_brawlstars_id", unique = true),
    ],
)
class PlayerBrawlerEntity(
    @Column(name = "player_id", updatable = false, nullable = false)
    val playerId: Long,
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
}
