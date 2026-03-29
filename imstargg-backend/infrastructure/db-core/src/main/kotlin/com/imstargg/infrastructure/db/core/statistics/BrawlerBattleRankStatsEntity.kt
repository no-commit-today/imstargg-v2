package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsTierRange
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(
    name = "brawler_battle_rank_stats",
    indexes = [
        Index(
            name = "ix_brawler_battle_rank_stats",
            columnList = "event_brawlstars_id, tier_range, battle_date",
        ),
    ],
)
class BrawlerBattleRankStatsEntity(
    eventBrawlStarsId: Long,
    tierRange: StatsTierRange,
    battleDate: LocalDate,
    @Column(name = "brawler_brawlstars_id", updatable = false, nullable = false)
    val brawlerBrawlStarsId: Long,
    rankCounts: RankCountsEmbedded,
) : BaseBattleRankStatsEntity(
        eventBrawlStarsId = eventBrawlStarsId,
        tierRange = tierRange,
        battleDate = battleDate,
    ) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawler_battle_rank_stats_id", nullable = false)
    val id: Long? = null

    @Embedded
    var rankCounts: RankCountsEmbedded = rankCounts
        protected set
}
