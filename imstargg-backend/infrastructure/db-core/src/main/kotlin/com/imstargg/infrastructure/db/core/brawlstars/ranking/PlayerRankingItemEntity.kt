package com.imstargg.infrastructure.db.core.brawlstars.ranking

import com.imstargg.infrastructure.db.core.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.ConstraintMode
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(
    name = "player_ranking_item",
    indexes = [
        Index(
            name = "uk_player_ranking_item__1",
            columnList = "player_ranking_id, player_brawlstars_tag",
            unique = true,
        ),
    ],
)
class PlayerRankingItemEntity(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_ranking_id", updatable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    val playerRanking: PlayerRankingEntity,
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
