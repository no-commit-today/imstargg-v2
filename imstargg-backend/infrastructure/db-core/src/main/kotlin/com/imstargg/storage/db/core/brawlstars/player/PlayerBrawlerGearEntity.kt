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
    name = "player_brawler_gear",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_playerbrawlergear__playerbrawlerid_gearbrawlstarsid",
            columnNames = ["player_brawler_id", "gear_brawlstars_id"],
        ),
    ],
)
class PlayerBrawlerGearEntity(
    @Column(name = "player_brawler_id", updatable = false, nullable = false)
    val playerBrawlerId: Long,
    @Column(name = "gear_brawlstars_id", updatable = false, nullable = false)
    val gearBrawlStarsId: Long,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "player_brawler_gear_id")
    val id: Long? = null
}
