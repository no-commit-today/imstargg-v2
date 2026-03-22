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
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime

@Entity
@Table(
    name = "brawler_trio_battle_rank_stats",
    indexes = [
        Index(
            name = "ix_brawler_trio_battle_rank_stats_1",
            columnList = "event_brawlstars_id, period_type, tier_range, date",
        ),
        Index(
            name = "ix_brawler_trio_battle_rank_stats_2",
            columnList = "event_brawlstars_id, period_type, tier_range, date, brawler_brawlstars_id",
        ),
    ],
)
class BrawlerTrioBattleRankStatsEntity internal constructor(
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
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "teammate_brawlstars_ids", columnDefinition = "json", updatable = false, nullable = false)
    val teammateBrawlStarsIds: List<Long>,
    rankCount1: Long,
    rankCount2: Long,
    rankCount3: Long,
    rankCount4: Long,
) : BaseBattleRankStatsEntity(
        eventBrawlStarsId = eventBrawlStarsId,
        tierRange = tierRange,
        date = date,
        startAt = startAt,
        endAt = endAt,
    ) {
    init {
        require(teammateBrawlStarsIds.isNotEmpty()) {
            "teammateBrawlStarsIds는 비어 있을 수 없습니다."
        }
        require(teammateBrawlStarsIds == teammateBrawlStarsIds.sorted()) {
            "teammateBrawlStarsIds($teammateBrawlStarsIds)는 정렬되어야 합니다."
        }
        require(teammateBrawlStarsIds.distinct().size == teammateBrawlStarsIds.size) {
            "teammateBrawlStarsIds($teammateBrawlStarsIds)에 중복이 있습니다."
        }
        require(brawlerBrawlStarsId !in teammateBrawlStarsIds) {
            "brawlerBrawlStarsId($brawlerBrawlStarsId)는 teammateBrawlStarsIds($teammateBrawlStarsIds)에 포함될 수 없습니다."
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawler_trio_battle_rank_stats_id", nullable = false)
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

    val teamBrawlerBrawlStarsIds: List<Long>
        get() = (teammateBrawlStarsIds + brawlerBrawlStarsId).sorted()

    companion object {
        fun create(
            eventBrawlStarsId: Long,
            periodType: StatsPeriodType,
            tierRange: StatsTierRange,
            startAt: OffsetDateTime,
            brawlerBrawlStarsId: Long,
            teammateBrawlStarsIds: List<Long>,
            rankCount1: Long,
            rankCount2: Long,
            rankCount3: Long,
            rankCount4: Long,
        ): BrawlerTrioBattleRankStatsEntity =
            BrawlerTrioBattleRankStatsEntity(
                eventBrawlStarsId = eventBrawlStarsId,
                periodType = periodType,
                tierRange = tierRange,
                date = periodType.dateOf(startAt),
                startAt = startAt,
                endAt = periodType.endAt(startAt),
                brawlerBrawlStarsId = brawlerBrawlStarsId,
                teammateBrawlStarsIds = teammateBrawlStarsIds.sorted(),
                rankCount1 = rankCount1,
                rankCount2 = rankCount2,
                rankCount3 = rankCount3,
                rankCount4 = rankCount4,
            )
    }
}
