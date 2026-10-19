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
    name = "hyper_charge",
    indexes = [
        Index(name = "uk_hypercharge__brawlstarsid", columnList = "brawlstars_id", unique = true),
    ],
)
class HyperChargeEntity(
    @Column(name = "brawlstars_id", updatable = false, nullable = false)
    val brawlStarsId: Long,
    @Column(name = "brawler_brawlstars_id", updatable = false, nullable = false)
    val brawlerBrawlStarsId: Long,
) : BaseSoftDeleteEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hyper_charge_id")
    val id: Long? = null
}
