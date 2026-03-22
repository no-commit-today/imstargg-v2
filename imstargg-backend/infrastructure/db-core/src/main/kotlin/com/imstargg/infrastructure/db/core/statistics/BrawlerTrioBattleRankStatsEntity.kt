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
    @Column(name = "teammate_brawlstars_id_1", updatable = false, nullable = false)
    val teammateBrawlStarsId1: Long,
    @Column(name = "teammate_brawlstars_id_2", updatable = false, nullable = false)
    val teammateBrawlStarsId2: Long,
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
        require(teammateBrawlStarsId1 < teammateBrawlStarsId2) {
            "teammateBrawlStarsId1($teammateBrawlStarsId1)은 teammateBrawlStarsId2($teammateBrawlStarsId2)보다 작아야 합니다."
        }
        require(
            brawlerBrawlStarsId != teammateBrawlStarsId1 && brawlerBrawlStarsId != teammateBrawlStarsId2,
        ) {
            "brawlerBrawlStarsId($brawlerBrawlStarsId)는 teammateBrawlStarsId1($teammateBrawlStarsId1), teammateBrawlStarsId2($teammateBrawlStarsId2)와 달라야 합니다."
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
        get() = listOf(brawlerBrawlStarsId, teammateBrawlStarsId1, teammateBrawlStarsId2).sorted()

    val teammateBrawlStarsIds: List<Long>
        get() = listOf(teammateBrawlStarsId1, teammateBrawlStarsId2)

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
        ): BrawlerTrioBattleRankStatsEntity {
            require(teammateBrawlStarsIds.size == 2) {
                "teammateBrawlStarsIds 크기는 2여야 합니다. actual=${teammateBrawlStarsIds.size}"
            }
            val sorted = teammateBrawlStarsIds.sorted()
            return BrawlerTrioBattleRankStatsEntity(
                eventBrawlStarsId = eventBrawlStarsId,
                periodType = periodType,
                tierRange = tierRange,
                date = periodType.dateOf(startAt),
                startAt = startAt,
                endAt = periodType.endAt(startAt),
                brawlerBrawlStarsId = brawlerBrawlStarsId,
                teammateBrawlStarsId1 = sorted[0],
                teammateBrawlStarsId2 = sorted[1],
                rankCount1 = rankCount1,
                rankCount2 = rankCount2,
                rankCount3 = rankCount3,
                rankCount4 = rankCount4,
            )
        }
    }
}
