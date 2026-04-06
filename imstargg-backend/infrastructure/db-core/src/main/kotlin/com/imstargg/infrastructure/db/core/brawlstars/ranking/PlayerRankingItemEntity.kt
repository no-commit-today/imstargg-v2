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
    name = "player_ranking_item",
    indexes = [
        Index(
            name = "uk_player_ranking_item__player_ranking_id_player_brawlstars_tag",
            columnList = "player_ranking_id, player_brawlstars_tag",
            unique = true,
        ),
    ],
)
class PlayerRankingItemEntity(
    @Column(name = "player_ranking_id", nullable = false, updatable = false)
    val playerRankingId: Long,
    @Column(name = "rank_value", nullable = false, updatable = false)
    val rank: Int,
    @Embedded
    val player: RankingEntityPlayer,
    @Column(name = "trophies", nullable = false, updatable = false)
    val trophies: Int,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "player_ranking_item_id")
    val id: Long? = null
}
