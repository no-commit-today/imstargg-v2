package com.imstargg.infrastructure.db.core.statistics.brawler

import com.imstargg.core.enums.BrawlerTrophyRange
import com.imstargg.core.enums.BuffyType
import com.imstargg.infrastructure.db.core.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Embedded
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
    name = "brawler_buffy_ownership_stats",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_brawler_buffy_ownership_stats_1",
            columnNames = ["brawler_brawlstars_id", "trophy_range", "buffy_type"],
        ),
    ],
)
class BrawlerBuffyOwnershipStatsEntity(
    @Column(name = "brawler_brawlstars_id", updatable = false, nullable = false)
    val brawlerBrawlStarsId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "trophy_range", columnDefinition = "varchar(45)", updatable = false, nullable = false)
    val trophyRange: BrawlerTrophyRange,
    @Enumerated(EnumType.STRING)
    @Column(name = "buffy_type", columnDefinition = "varchar(25)", updatable = false, nullable = false)
    val buffyType: BuffyType,
    @Embedded
    val ownershipCount: OwnershipCountEmbedded,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "brawlerBuffyOwnershipStatsSeq")
    @SequenceGenerator(
        name = "brawlerBuffyOwnershipStatsSeq",
        sequenceName = "brawler_buffy_ownership_stats_seq",
        allocationSize = 100,
    )
    @Column(name = "brawler_buffy_ownership_stats_id", nullable = false)
    val id: Long? = null
}
