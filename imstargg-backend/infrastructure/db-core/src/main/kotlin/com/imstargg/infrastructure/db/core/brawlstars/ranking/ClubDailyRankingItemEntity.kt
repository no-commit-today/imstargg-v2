package com.imstargg.infrastructure.db.core.brawlstars.ranking

import com.imstargg.infrastructure.db.core.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "club_daily_ranking_item",
    indexes = [
        Index(
            name = "uk_club_daily_ranking_item__ranking_id_tag",
            columnList = "club_daily_ranking_id, brawlstars_tag",
            unique = true,
        ),
    ],
)
class ClubDailyRankingItemEntity(
    @Column(name = "club_daily_ranking_id", nullable = false, updatable = false)
    val clubDailyRankingId: Long,
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
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_daily_ranking_item_id")
    val id: Long? = null
}
