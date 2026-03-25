package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.core.enums.StatsTierRange
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(
    name = "brawler_enemy_battle_result_stats",
    indexes = [
        Index(
            name = "ix_brawler_enemy_battle_result_stats_1",
            columnList = "event_brawlstars_id, tier_range, period_type, date",
        ),
        Index(
            name = "ix_brawler_enemy_battle_result_stats_2",
            columnList = "brawler_brawlstars_id, tier_range, period_type, date",
        ),
    ],
)
class BrawlerEnemyBattleResultStatsEntity internal constructor(
    eventBrawlStarsId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", columnDefinition = "varchar(25)", updatable = false, nullable = false)
    val periodType: StatsPeriodType,
    tierRange: StatsTierRange,
    date: java.time.LocalDate,
    startAt: OffsetDateTime,
    endAt: OffsetDateTime,
    @Column(name = "brawler_brawlstars_id", updatable = false, nullable = false)
    val brawlerBrawlStarsId: Long,
    @Column(name = "enemy_brawler_brawlstars_id", updatable = false, nullable = false)
    val enemyBrawlerBrawlStarsId: Long,
    resultCount: ResultCountEmbedded,
) : BaseBattleResultStatsEntity(
        eventBrawlStarsId = eventBrawlStarsId,
        tierRange = tierRange,
        date = date,
        startAt = startAt,
        endAt = endAt,
        resultCount = resultCount,
    ) {
    init {
        require(brawlerBrawlStarsId != enemyBrawlerBrawlStarsId) {
            "brawlerBrawlStarsId($brawlerBrawlStarsId)와 enemyBrawlerBrawlStarsId($enemyBrawlerBrawlStarsId)는 달라야 합니다."
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawler_enemy_battle_result_stats_id", nullable = false)
    val id: Long? = null

    companion object {
        fun create(
            eventBrawlStarsId: Long,
            periodType: StatsPeriodType,
            tierRange: StatsTierRange,
            startAt: OffsetDateTime,
            brawlerBrawlStarsId: Long,
            enemyBrawlerBrawlStarsId: Long,
            resultCount: ResultCountEmbedded,
        ) = BrawlerEnemyBattleResultStatsEntity(
            eventBrawlStarsId = eventBrawlStarsId,
            periodType = periodType,
            tierRange = tierRange,
            date = periodType.dateOf(startAt),
            startAt = startAt,
            endAt = periodType.endAt(startAt),
            brawlerBrawlStarsId = brawlerBrawlStarsId,
            enemyBrawlerBrawlStarsId = enemyBrawlerBrawlStarsId,
            resultCount = resultCount,
        )
    }
}
