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
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "brawler_gadget_ownership_stats",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_brawler_gadget_ownership_stats_1",
            columnNames = ["brawler_brawlstars_id", "trophy_range", "gadget_brawlstars_id"],
        ),
    ],
)
class BrawlerGadgetOwnershipStatsEntity(
    @Column(name = "brawler_brawlstars_id", updatable = false, nullable = false)
    val brawlerBrawlStarsId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "trophy_range", columnDefinition = "varchar(45)", updatable = false, nullable = false)
    val trophyRange: BrawlerTrophyRange,
    @Column(name = "gadget_brawlstars_id", updatable = false, nullable = false)
    val gadgetBrawlStarsId: Long,
    @Column(name = "owner_count", updatable = false, nullable = false)
    val ownerCount: Long,
    @Column(name = "total_count", updatable = false, nullable = false)
    val totalCount: Long,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "brawlerGadgetOwnershipStatsSeq")
    @SequenceGenerator(
        name = "brawlerGadgetOwnershipStatsSeq",
        sequenceName = "brawler_gadget_ownership_stats_seq",
        allocationSize = 100,
    )
    @Column(name = "brawler_gadget_ownership_stats_id", nullable = false)
    val id: Long? = null
}
