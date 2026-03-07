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
    name = "brawler_trio_battle_rank_weekly_stats",
    indexes = [
        Index(
            name = "ix_brawler_trio_battle_rank_weekly_stats",
            columnList = "event_brawlstars_id, tier_range, start_date",
        ),
    ],
)
class BrawlerTrioBattleRankWeeklyStatsEntity(
    eventBrawlStarsId: Long,
    tierRange: StatsTierRange,
    startDate: LocalDate,
    endDate: LocalDate,
    @Column(name = "brawler_brawlstars_id_1", updatable = false, nullable = false)
    val brawlerBrawlStarsId1: Long,
    @Column(name = "brawler_brawlstars_id_2", updatable = false, nullable = false)
    val brawlerBrawlStarsId2: Long,
    @Column(name = "brawler_brawlstars_id_3", updatable = false, nullable = false)
    val brawlerBrawlStarsId3: Long,
    rankCount1: Long,
    rankCount2: Long,
    rankCount3: Long,
    rankCount4: Long,
) : BaseWeeklyBattleRankEntity(
        eventBrawlStarsId = eventBrawlStarsId,
        tierRange = tierRange,
        startDate = startDate,
        endDate = endDate,
    ) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawler_trio_battle_rank_weekly_stats_id", nullable = false)
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
}
