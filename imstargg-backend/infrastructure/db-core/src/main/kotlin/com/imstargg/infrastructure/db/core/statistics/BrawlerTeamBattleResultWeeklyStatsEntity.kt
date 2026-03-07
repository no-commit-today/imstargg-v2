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
import java.time.OffsetDateTime

@Entity
@Table(
    name = "brawler_team_battle_result_weekly_stats",
    indexes = [
        Index(
            name = "ix_brawler_team_battle_result_weekly_stats_1",
            columnList = "event_brawlstars_id, tier_range, date",
        ),
        Index(
            name = "ix_brawler_team_battle_result_weekly_stats_2",
            columnList = "tier_range, date",
        ),
    ],
)
class BrawlerTeamBattleResultWeeklyStatsEntity(
    eventBrawlStarsId: Long,
    tierRange: StatsTierRange,
    date: LocalDate,
    startAt: OffsetDateTime,
    endAt: OffsetDateTime,
    @Column(name = "brawler_brawlstars_id_1", updatable = false, nullable = false)
    val brawlerBrawlStarsId1: Long,
    @Column(name = "brawler_brawlstars_id_2", updatable = false, nullable = false)
    val brawlerBrawlStarsId2: Long,
    @Column(name = "brawler_brawlstars_id_3", updatable = false, nullable = false)
    val brawlerBrawlStarsId3: Long,
    victoryCount: Long,
    defeatCount: Long,
    drawCount: Long,
) : BaseBattleResultStatsEntity(
        eventBrawlStarsId = eventBrawlStarsId,
        tierRange = tierRange,
        date = date,
        startAt = startAt,
        endAt = endAt,
        victoryCount = victoryCount,
        defeatCount = defeatCount,
        drawCount = drawCount,
    ) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawler_team_battle_result_weekly_stats_id", nullable = false)
    val id: Long? = null
}
