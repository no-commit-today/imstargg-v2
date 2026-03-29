package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.BaseTimeEntity
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
    name = "brawler_team_battle_result_stats",
    indexes = [
        Index(
            name = "ix_brawler_team_battle_result_stats_1",
            columnList = "event_brawlstars_id, tier_range, period_type, start_battle_date",
        ),
    ],
)
class BrawlerTeamBattleResultStatsEntity(
    @Column(name = "event_brawlstars_id", updatable = false, nullable = false)
    val eventBrawlStarsId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", columnDefinition = "varchar(25)", updatable = false, nullable = false)
    val periodType: StatsPeriodType,
    @Enumerated(EnumType.STRING)
    @Column(name = "tier_range", columnDefinition = "varchar(45)", updatable = false, nullable = false)
    val tierRange: StatsTierRange,
    @Column(name = "start_battle_date", updatable = false, nullable = false)
    val startBattleDate: LocalDate,
    @Column(name = "end_battle_date", updatable = false, nullable = false)
    val endBattleDate: LocalDate,
    @Column(name = "team_size", updatable = false, nullable = false)
    val teamSize: Int,
    @Embedded
    val resultCount: ResultCountEmbedded,
    initialBrawlerBrawlStarsIds: List<Long>,
) : BaseTimeEntity() {
    init {
        require(teamSize in 2..3) {
            "teamSize($teamSize)는 2~3 사이여야 합니다."
        }
        require(startBattleDate <= endBattleDate) {
            "startBattleDate($startBattleDate)는 endBattleDate($endBattleDate) 이전이어야 합니다."
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawler_team_battle_result_stats_id", nullable = false)
    val id: Long? = null

    @OneToMany(mappedBy = "stats", cascade = [CascadeType.ALL], orphanRemoval = true)
    val brawlers: List<BrawlerTeamBattleResultStatsBrawlerEntity> =
        initialBrawlerBrawlStarsIds.map {
            BrawlerTeamBattleResultStatsBrawlerEntity(stats = this, brawlerBrawlStarsId = it)
        }

    val brawlerBrawlStarsIds: List<Long>
        get() = brawlers.map { it.brawlerBrawlStarsId }.sorted()
}
