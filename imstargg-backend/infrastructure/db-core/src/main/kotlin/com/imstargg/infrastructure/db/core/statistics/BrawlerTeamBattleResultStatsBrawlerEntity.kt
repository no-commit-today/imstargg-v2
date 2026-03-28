package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.infrastructure.db.core.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.ConstraintMode
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
    name = "brawler_team_battle_result_stats_brawler",
    indexes = [
        Index(
            name = "ix_brawler_team_battle_result_stats_brawler_1",
            columnList = "brawler_brawlstars_id, brawler_team_battle_result_stats_id",
        ),
    ],
)
class BrawlerTeamBattleResultStatsBrawlerEntity(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "brawler_team_battle_result_stats_id",
        updatable = false,
        foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT),
    )
    val stats: BrawlerTeamBattleResultStatsEntity,
    @Column(name = "brawler_brawlstars_id", updatable = false, nullable = false)
    val brawlerBrawlStarsId: Long,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawler_team_battle_result_stats_brawler_id", nullable = false)
    val id: Long? = null
}
