package com.imstargg.infrastructure.db.core.brawlstars.brawler

import com.imstargg.infrastructure.db.core.BaseSoftDeleteEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "brawler_gear",
    indexes = [
        Index(
            name = "uk_brawlergear__brawlerbrawlstarsid_gearbrawlstarsid",
            columnList = "brawler_brawlstars_id, gear_brawlstars_id",
            unique = true,
        ),
    ],
)
class BrawlerGearEntity(
    @Column(name = "brawler_brawlstars_id", updatable = false, nullable = false)
    val brawlerBrawlStarsId: Long,
    @Column(name = "gear_brawlstars_id", updatable = false, nullable = false)
    val gearBrawlStarsId: Long,
) : BaseSoftDeleteEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawler_gear_id")
    val id: Long? = null
}
