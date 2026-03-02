package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsTierRange
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(
    name = "stats_weekly_brawler_duo_battle_rank",
    indexes = [
        Index(
            name = "ix_stats_weekly_brawler_duo_battle_rank",
            columnList = "event_brawlstars_id, tier_range, start_date",
        ),
    ],
)
class StatsWeeklyBrawlerDuoBattleRankEntity(
    eventBrawlStarsId: Long,
    tierRange: StatsTierRange,
    startDate: LocalDate,
    endDate: LocalDate,
    @Column(name = "brawler_brawlstars_id_1", updatable = false, nullable = false)
    val brawlerBrawlStarsId1: Long,
    @Column(name = "brawler_brawlstars_id_2", updatable = false, nullable = false)
    val brawlerBrawlStarsId2: Long,
    rankCount1: Long,
    rankCount2: Long,
    rankCount3: Long,
    rankCount4: Long,
    rankCount5: Long,
) : BaseWeeklyBattleRankEntity(
        eventBrawlStarsId = eventBrawlStarsId,
        tierRange = tierRange,
        startDate = startDate,
        endDate = endDate,
    ) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stats_weekly_brawler_duo_battle_rank_id", nullable = false)
    val id: Long? = null

    @Column(name = "rank_1_count", nullable = false)
    var rankCount1: Long = rankCount1
        protected set

    @Column(name = "rank_2_count", nullable = false)
    var rankCount2: Long = rankCount2
        protected set

    @Column(name = "rank_3_count", nullable = false)
    var rankCount3: Long = rankCount3
        protected set

    @Column(name = "rank_4_count", nullable = false)
    var rankCount4: Long = rankCount4
        protected set

    @Column(name = "rank_5_count", nullable = false)
    var rankCount5: Long = rankCount5
        protected set
}
