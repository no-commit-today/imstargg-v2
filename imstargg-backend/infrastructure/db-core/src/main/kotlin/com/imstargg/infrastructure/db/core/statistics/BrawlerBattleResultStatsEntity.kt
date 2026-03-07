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
    name = "brawler_battle_result_stats",
    indexes = [
        Index(
            name = "ix_brawler_battle_result_stats_1",
            columnList = "event_brawlstars_id, tier_range, date",
        ),
        Index(
            name = "ix_brawler_battle_result_stats_2",
            columnList = "tier_range, date, brawler_brawlstars_id",
        ),
    ],
)
class BrawlerBattleResultStatsEntity(
    eventBrawlStarsId: Long,
    tierRange: StatsTierRange,
    date: LocalDate,
    startAt: OffsetDateTime,
    endAt: OffsetDateTime,
    @Column(name = "brawler_brawlstars_id", updatable = false, nullable = false)
    val brawlerBrawlStarsId: Long,
    victoryCount: Long,
    defeatCount: Long,
    drawCount: Long,
    starPlayerCount: Long,
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
    @Column(name = "brawler_battle_result_stats_id", nullable = false)
    val id: Long? = null

    @Column(name = "star_player_count", nullable = false)
    var starPlayerCount: Long = starPlayerCount
        protected set
}
