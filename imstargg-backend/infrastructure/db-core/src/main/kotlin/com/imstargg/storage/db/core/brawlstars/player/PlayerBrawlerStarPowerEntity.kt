package com.imstargg.storage.db.core.brawlstars.player

import com.imstargg.storage.db.core.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "player_brawler_star_power",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_playerbrawlerstarpower__playerbrawlerid_starpowerbrawlstarsid",
            columnNames = ["player_brawler_id", "star_power_brawlstars_id"],
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
