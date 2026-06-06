package com.imstargg.infrastructure.db.core.brawlstars.ranking

import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "brawler_ranking",
    indexes = [
        Index(
            name = "ix_brawler_ranking__country_brawler_collected_at",
            columnList = "country, brawler_brawlstars_id, collected_at",
        ),
    ],
)
class BrawlerRankingEntity(
    @Enumerated(EnumType.STRING)
    @Column(name = "country", columnDefinition = "varchar(25)", nullable = false, updatable = false)
    val country: Country,
    @Column(name = "brawler_brawlstars_id", nullable = false, updatable = false)
    val brawlerBrawlStarsId: Long,
    @Column(name = "collected_at", nullable = false, updatable = false)
    val collectedAt: Instant,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawler_ranking_id")
    val id: Long? = null
}
