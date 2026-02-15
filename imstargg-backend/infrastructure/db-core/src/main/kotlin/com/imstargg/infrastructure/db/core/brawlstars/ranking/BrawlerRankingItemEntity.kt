package com.imstargg.infrastructure.db.core.brawlstars.ranking

import com.imstargg.infrastructure.db.core.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "brawler_ranking_item",
    indexes = [
        Index(name = "ix_brawler_ranking_item__brawler_ranking_id", columnList = "brawler_ranking_id"),
    ],
)
class BrawlerRankingItemEntity(
    @Column(name = "brawler_ranking_id", nullable = false, updatable = false)
    val brawlerRankingId: Long,
    @Column(name = "rank_value", nullable = false, updatable = false)
    val rank: Int,
    @Embedded
    val player: RankingEntityPlayer,
    @Column(name = "trophies", nullable = false, updatable = false)
    val trophies: Int,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawler_ranking_item_id")
    val id: Long? = null
}
