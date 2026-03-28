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
    name = "brawler_battle_result_stats",
    indexes = [
        Index(
            name = "ix_brawler_battle_result_stats_1",
            columnList = "event_brawlstars_id, tier_range, battle_date",
        ),
        Index(
            name = "ix_brawler_battle_result_stats_2",
            columnList = "tier_range, battle_date, brawler_brawlstars_id",
        ),
    ],
)
class BrawlerBattleResultStatsEntity(
    eventBrawlStarsId: Long,
    tierRange: StatsTierRange,
    battleDate: LocalDate,
    @Column(name = "brawler_brawlstars_id", updatable = false, nullable = false)
    val brawlerBrawlStarsId: Long,
    resultCount: ResultCountEmbedded,
    @Column(name = "star_player_count", updatable = false, nullable = false)
    val starPlayerCount: Long,
) : BaseBattleResultStatsEntity(
        eventBrawlStarsId = eventBrawlStarsId,
        tierRange = tierRange,
        battleDate = battleDate,
        resultCount = resultCount,
    ) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawler_battle_result_stats_id", nullable = false)
    val id: Long? = null
}
