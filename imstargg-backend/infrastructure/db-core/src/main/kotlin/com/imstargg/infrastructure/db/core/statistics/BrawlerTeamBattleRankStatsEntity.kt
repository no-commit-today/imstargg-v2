package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.core.enums.StatsTierRange
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(
    name = "brawler_team_battle_rank_stats",
    indexes = [
        Index(
            name = "ix_brawler_team_battle_rank_stats_1",
            columnList = "event_brawlstars_id, tier_range, period_type, battle_date",
        ),
    ],
)
class BrawlerTeamBattleRankStatsEntity internal constructor(
    eventBrawlStarsId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", columnDefinition = "varchar(25)", updatable = false, nullable = false)
    val periodType: StatsPeriodType,
    tierRange: StatsTierRange,
    battleDate: LocalDate,
    @Column(name = "team_size", updatable = false, nullable = false)
    val teamSize: Int,
    rankCounts: RankCountsEmbedded,
) : BaseBattleRankStatsEntity(
        eventBrawlStarsId = eventBrawlStarsId,
        tierRange = tierRange,
        battleDate = battleDate,
    ) {
    init {
        require(teamSize in 2..5) {
            "teamSize($teamSize)는 2~5 사이여야 합니다."
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawler_team_battle_rank_stats_id", nullable = false)
    val id: Long? = null

    @Embedded
    var rankCounts: RankCountsEmbedded = rankCounts
        protected set

    @OneToMany(mappedBy = "stats", cascade = [CascadeType.ALL], orphanRemoval = true)
    val brawlers: MutableList<BrawlerTeamBattleRankStatsBrawlerEntity> = mutableListOf()

    val brawlerBrawlStarsIds: List<Long>
        get() = brawlers.map { it.brawlerBrawlStarsId }.sorted()

    companion object {
        fun create(
            eventBrawlStarsId: Long,
            periodType: StatsPeriodType,
            tierRange: StatsTierRange,
            battleDate: LocalDate,
            brawlerBrawlStarsIds: List<Long>,
            rankCounts: RankCountsEmbedded,
        ): BrawlerTeamBattleRankStatsEntity {
            val entity =
                BrawlerTeamBattleRankStatsEntity(
                    eventBrawlStarsId = eventBrawlStarsId,
                    periodType = periodType,
                    tierRange = tierRange,
                    battleDate = battleDate,
                    teamSize = brawlerBrawlStarsIds.size,
                    rankCounts = rankCounts,
                )
            brawlerBrawlStarsIds.forEach { brawlerId ->
                entity.brawlers.add(
                    BrawlerTeamBattleRankStatsBrawlerEntity(
                        stats = entity,
                        brawlerBrawlStarsId = brawlerId,
                    ),
                )
            }
            return entity
        }
    }
}
