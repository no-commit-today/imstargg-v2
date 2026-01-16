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
    name = "player_brawler_star_power",
    indexes = [
        Index(
            name = "uk_playerbrawlerstarpower__playerbrawlerid_starpowerbrawlstarsid",
            columnList = "player_brawler_id, star_power_brawlstars_id",
            unique = true,
        ),
    ],
)
class PlayerBrawlerStarPowerEntity(
    @Column(name = "player_brawler_id", updatable = false, nullable = false)
    val playerBrawlerId: Long,
    @Column(name = "star_power_brawlstars_id", updatable = false, nullable = false)
    val starPowerBrawlStarsId: Long,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "player_brawler_star_power_id")
    val id: Long? = null
}
