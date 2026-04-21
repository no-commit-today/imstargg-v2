package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(
    name = "brawler_team_battle_rank_stats",
    indexes = [
        Index(
            name = "ix_brawler_team_battle_rank_stats_1",
            columnList = "event_brawlstars_id, tier_range, start_battle_date, end_battle_date",
        ),
    ],
)
class BrawlerTeamBattleRankStatsEntity(
    @Column(name = "event_brawlstars_id", updatable = false, nullable = false)
    val eventBrawlStarsId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "tier_range", columnDefinition = "varchar(45)", updatable = false, nullable = false)
    val tierRange: StatsTierRange,
    @Column(name = "start_battle_date", updatable = false, nullable = false)
    val startBattleDate: LocalDate,
    @Column(name = "end_battle_date", updatable = false, nullable = false)
    val endBattleDate: LocalDate,
    rankCounts: RankCountsEmbedded,
    @Embedded
    val brawlerTeam: BrawlerTeamEmbedded,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawler_team_battle_rank_stats_id", nullable = false)
    val id: Long? = null

    @Embedded
    var rankCounts: RankCountsEmbedded = rankCounts
        protected set

    @Column(name = "weighted_rank_sum", updatable = false, nullable = false)
    val weightedRankSum: Long = rankCounts.weightedRankSum()
}
