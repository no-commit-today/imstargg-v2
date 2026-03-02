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
    name = "stats_weekly_brawler_team_battle_result",
    indexes = [
        Index(
            name = "ix_stats_weekly_brawler_team_battle_result",
            columnList = "event_brawlstars_id, tier_range, start_date",
        ),
    ],
)
class StatsWeeklyBrawlerTeamBattleResultEntity(
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
    victoryCount: Long,
    defeatCount: Long,
    drawCount: Long,
) : BaseWeeklyBattleResultEntity(
        eventBrawlStarsId = eventBrawlStarsId,
        tierRange = tierRange,
        startDate = startDate,
        endDate = endDate,
        victoryCount = victoryCount,
        defeatCount = defeatCount,
        drawCount = drawCount,
    ) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stats_weekly_brawler_team_battle_result_id", nullable = false)
    val id: Long? = null
}
