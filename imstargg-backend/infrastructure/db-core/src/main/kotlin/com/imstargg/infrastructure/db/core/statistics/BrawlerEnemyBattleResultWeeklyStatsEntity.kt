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
    name = "brawler_enemy_battle_result_weekly_stats",
    indexes = [
        Index(
            name = "ix_brawler_enemy_battle_result_weekly_stats",
            columnList = "event_brawlstars_id, tier_range, start_date",
        ),
    ],
)
class BrawlerEnemyBattleResultWeeklyStatsEntity(
    eventBrawlStarsId: Long,
    tierRange: StatsTierRange,
    startDate: LocalDate,
    endDate: LocalDate,
    @Column(name = "brawler_brawlstars_id", updatable = false, nullable = false)
    val brawlerBrawlStarsId: Long,
    @Column(name = "enemy_brawler_brawlstars_id", updatable = false, nullable = false)
    val enemyBrawlerBrawlStarsId: Long,
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
    @Column(name = "brawler_enemy_battle_result_weekly_stats_id", nullable = false)
    val id: Long? = null
}
