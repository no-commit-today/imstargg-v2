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
import java.time.LocalDate
import java.time.OffsetDateTime

@Entity
@Table(
    name = "brawler_duo_battle_rank_stats",
    indexes = [
        Index(
            name = "ix_brawler_duo_battle_rank_stats_1",
            columnList = "event_brawlstars_id, period_type, tier_range, date",
        ),
        Index(
            name = "ix_brawler_duo_battle_rank_stats_2",
            columnList = "event_brawlstars_id, period_type, tier_range, date, brawler_brawlstars_id",
        ),
    ],
)
class BrawlerDuoBattleRankStatsEntity(
    eventBrawlStarsId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", columnDefinition = "varchar(25)", updatable = false, nullable = false)
    val periodType: StatsPeriodType,
    tierRange: StatsTierRange,
    date: LocalDate,
    startAt: OffsetDateTime,
    endAt: OffsetDateTime,
    @Column(name = "brawler_brawlstars_id", updatable = false, nullable = false)
    val brawlerBrawlStarsId: Long,
    @Column(name = "teammate_brawlstars_id", updatable = false, nullable = false)
    val teammateBrawlStarsId: Long,
    rankCount1: Long,
    rankCount2: Long,
    rankCount3: Long,
    rankCount4: Long,
    rankCount5: Long,
) : BaseBattleRankStatsEntity(
        eventBrawlStarsId = eventBrawlStarsId,
        tierRange = tierRange,
        date = date,
        startAt = startAt,
        endAt = endAt,
    ) {
    init {
        require(brawlerBrawlStarsId != teammateBrawlStarsId) {
            "brawlerBrawlStarsId($brawlerBrawlStarsId)와 teammateBrawlStarsId($teammateBrawlStarsId)는 달라야 합니다."
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawler_duo_battle_rank_stats_id", nullable = false)
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

    val duoBrawlerBrawlStarsIds: List<Long>
        get() = listOf(brawlerBrawlStarsId, teammateBrawlStarsId).sorted()
}
