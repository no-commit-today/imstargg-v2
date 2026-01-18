package com.imstargg.infrastructure.db.core.brawlstars.ranking

import com.imstargg.infrastructure.db.core.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "club_ranking_item",
    indexes = [
        Index(name = "ix_club_ranking_item__club_ranking_id", columnList = "club_ranking_id"),
    ],
)
class ClubRankingItemEntity(
    @Column(name = "club_ranking_id", nullable = false, updatable = false)
    val clubRankingId: Long,
    @Column(name = "rank_value", nullable = false, updatable = false)
    val rank: Int,
    @Column(name = "brawlstars_tag", length = 45, nullable = false, updatable = false)
    val brawlStarsTag: String,
    @Column(name = "name", length = 105, nullable = false, updatable = false)
    val name: String,
    @Column(name = "badge_brawlstars_id", nullable = false, updatable = false)
    val badgeBrawlStarsId: Long,
    @Column(name = "trophies", nullable = false, updatable = false)
    val trophies: Int,
    @Column(name = "member_count", nullable = false, updatable = false)
    val memberCount: Int,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_ranking_item_id")
    val id: Long? = null
}
