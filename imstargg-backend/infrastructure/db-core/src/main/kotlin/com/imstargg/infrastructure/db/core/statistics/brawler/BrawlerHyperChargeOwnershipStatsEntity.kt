package com.imstargg.infrastructure.db.core.statistics.brawler

import com.imstargg.core.enums.BrawlerTrophyRange
import com.imstargg.infrastructure.db.core.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "brawler_hyper_charge_ownership_stats",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_brawler_hyper_charge_ownership_stats_1",
            columnNames = ["brawler_brawlstars_id", "trophy_range", "hyper_charge_brawlstars_id"],
        ),
    ],
)
class BrawlerHyperChargeOwnershipStatsEntity(
    @Column(name = "brawler_brawlstars_id", updatable = false, nullable = false)
    val brawlerBrawlStarsId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "trophy_range", columnDefinition = "varchar(45)", updatable = false, nullable = false)
    val trophyRange: BrawlerTrophyRange,
    @Column(name = "hyper_charge_brawlstars_id", updatable = false, nullable = false)
    val hyperChargeBrawlStarsId: Long,
    @Column(name = "owner_count", updatable = false, nullable = false)
    val ownerCount: Long,
    @Column(name = "total_count", updatable = false, nullable = false)
    val totalCount: Long,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawler_hyper_charge_ownership_stats_id", nullable = false)
    val id: Long? = null
}
