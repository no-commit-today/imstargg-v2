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
    name = "stats_weekly_brawler_battle_result",
    indexes = [
        Index(
            name = "ix_stats_weekly_brawler_battle_result",
            columnList = "event_brawlstars_id, tier_range, start_date",
        ),
    ],
)
class StatsWeeklyBrawlerBattleResultEntity(
    eventBrawlStarsId: Long,
    tierRange: StatsTierRange,
    startDate: LocalDate,
    endDate: LocalDate,
    @Column(name = "brawler_brawlstars_id", updatable = false, nullable = false)
    val brawlerBrawlStarsId: Long,
    victoryCount: Long,
    defeatCount: Long,
    drawCount: Long,
    starPlayerCount: Long,
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
    @Column(name = "stats_weekly_brawler_battle_result_id", nullable = false)
    val id: Long? = null

    @Column(name = "star_player_count", nullable = false)
    var starPlayerCount: Long = starPlayerCount
        protected set
}
