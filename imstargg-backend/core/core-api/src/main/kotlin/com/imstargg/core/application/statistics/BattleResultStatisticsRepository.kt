package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerEnemyResultCount
import com.imstargg.core.domain.statistics.BrawlerEnemyResultStatistics
import com.imstargg.core.domain.statistics.BrawlerEventResultCount
import com.imstargg.core.domain.statistics.BrawlerPairResultCount
import com.imstargg.core.domain.statistics.BrawlerPairResultStatistics
import com.imstargg.core.domain.statistics.BrawlerResultCount
import com.imstargg.core.domain.statistics.BrawlerResultDailyCount
import com.imstargg.core.domain.statistics.BrawlerResultDailyStatistics
import com.imstargg.core.domain.statistics.BrawlerResultStatistics
import com.imstargg.core.domain.statistics.BrawlerTrioResultCount
import com.imstargg.core.domain.statistics.BrawlerTrioResultStatistics
import com.imstargg.core.domain.statistics.ComboStatsPolicy
import com.imstargg.core.domain.statistics.PairResultRanking
import com.imstargg.core.domain.statistics.ResultCount
import com.imstargg.core.domain.statistics.StatsRate
import com.imstargg.core.domain.statistics.TrioResultRanking
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.core.infrastructure.jpa.toResultCount
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerBattleResultStatsBackupJpaRepository
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerEnemyBattleResultStatsBackupJpaRepository
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerPairBattleResultStatsBackupJpaRepository
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerPairResultProjection
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerTrioBattleResultStatsBackupJpaRepository
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerTrioResultProjection
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Repository
@Transactional(readOnly = true)
class BattleResultStatisticsRepository(
    private val battleResultJpaRepository: BrawlerBattleResultStatsBackupJpaRepository,
    private val enemyBattleResultJpaRepository: BrawlerEnemyBattleResultStatsBackupJpaRepository,
    private val pairBattleResultJpaRepository: BrawlerPairBattleResultStatsBackupJpaRepository,
    private val trioBattleResultJpaRepository: BrawlerTrioBattleResultStatsBackupJpaRepository,
) {
    fun findBrawlerResultStatistics(
        eventId: BrawlStarsId?,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerResultStatistics {
        val rows: List<BrawlerEventResultCount> =
            battleResultJpaRepository
                .findGroupedByEventAndBrawler(
                    eventBrawlStarsId = eventId?.value,
                    tierRange = tierRange,
                    startDate = startDate,
                    endDate = endDate,
                ).map { projection ->
                    BrawlerEventResultCount(
                        eventId = BrawlStarsId(projection.eventBrawlStarsId),
                        brawlerId = BrawlStarsId(projection.brawlerBrawlStarsId),
                        resultCount =
                            ResultCount(
                                victoryCount = projection.victoryCount,
                                defeatCount = projection.defeatCount,
                                drawCount = projection.drawCount,
                            ),
                        starPlayerCount = projection.starPlayerCount,
                    )
                }
        return BrawlerResultStatistics.create(rows = rows)
    }

    fun findBrawlerPairResultStatistics(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerPairResultStatistics {
        val counts: List<BrawlerPairResultCount> =
            pairBattleResultJpaRepository
                .findAllByBrawler(
                    brawlerBrawlStarsId = brawlerId.value,
                    tierRange = tierRange,
                    startDate = startDate,
                    endDate = endDate,
                ).map { projection ->
                    BrawlerPairResultCount(
                        brawler1Id = BrawlStarsId(projection.brawler1BrawlStarsId),
                        brawler2Id = BrawlStarsId(projection.brawler2BrawlStarsId),
                        resultCount =
                            ResultCount(
                                victoryCount = projection.victoryCount,
                                defeatCount = projection.defeatCount,
                                drawCount = projection.drawCount,
                            ),
                    )
                }
        val totalTeamCount =
            StatsRate.pairTeamCount(
                pairBattleResultJpaRepository.sumTotalCount(
                    tierRange = tierRange,
                    startDate = startDate,
                    endDate = endDate,
                ),
            )
        return BrawlerPairResultStatistics.create(
            brawlerId = brawlerId,
            totalTeamCount = totalTeamCount,
            counts = counts,
        )
    }

    fun findBrawlerTrioResultStatistics(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerTrioResultStatistics {
        val counts: List<BrawlerTrioResultCount> =
            trioBattleResultJpaRepository
                .findAllByBrawler(
                    brawlerBrawlStarsId = brawlerId.value,
                    tierRange = tierRange,
                    startDate = startDate,
                    endDate = endDate,
                ).map { it.toTrioResultCount() }
        // 3인 조합 행 하나가 팀 하나라 합이 곧 팀 수다.
        val totalTeamCount =
            trioBattleResultJpaRepository.sumTotalCount(
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
            )
        return BrawlerTrioResultStatistics.create(
            brawlerId = brawlerId,
            totalTeamCount = totalTeamCount,
            counts = counts,
        )
    }

    fun findPairResultRanking(
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): PairResultRanking {
        val counts: List<BrawlerPairResultCount> =
            pairBattleResultJpaRepository
                .findAllHavingTotalCountAtLeast(
                    tierRange = tierRange,
                    startDate = startDate,
                    endDate = endDate,
                    minTotalCount = ComboStatsPolicy.RANKING_MIN_BATTLE_COUNT,
                ).map { it.toPairResultCount() }
        // 분모는 하한(HAVING)을 걸지 않은 전체 합이다 — 하한 미만 조합도 팀 수에는 들어간다.
        val totalTeamCount =
            StatsRate.pairTeamCount(
                pairBattleResultJpaRepository.sumTotalCount(
                    tierRange = tierRange,
                    startDate = startDate,
                    endDate = endDate,
                ),
            )
        return PairResultRanking.create(totalTeamCount = totalTeamCount, counts = counts)
    }

    fun findTrioResultRanking(
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): TrioResultRanking {
        val counts: List<BrawlerTrioResultCount> =
            trioBattleResultJpaRepository
                .findAllHavingTotalCountAtLeast(
                    tierRange = tierRange,
                    startDate = startDate,
                    endDate = endDate,
                    minTotalCount = ComboStatsPolicy.RANKING_MIN_BATTLE_COUNT,
                ).map { it.toTrioResultCount() }
        // 분모는 하한(HAVING)을 걸지 않은 전체 합 그대로다(3인 조합 행 하나가 팀 하나).
        val totalTeamCount =
            trioBattleResultJpaRepository.sumTotalCount(
                tierRange = tierRange,
                startDate = startDate,
                endDate = endDate,
            )
        return TrioResultRanking.create(totalTeamCount = totalTeamCount, counts = counts)
    }

    fun findBrawlerEnemyResultStatistics(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerEnemyResultStatistics {
        val counts: List<BrawlerEnemyResultCount> =
            enemyBattleResultJpaRepository
                .findAllByBrawler(
                    brawlerBrawlStarsId = brawlerId.value,
                    tierRange = tierRange,
                    startDate = startDate,
                    endDate = endDate,
                ).map { projection ->
                    BrawlerEnemyResultCount(
                        brawlerId = BrawlStarsId(projection.brawlerBrawlStarsId),
                        enemyBrawlerId = BrawlStarsId(projection.enemyBrawlerBrawlStarsId),
                        resultCount =
                            ResultCount(
                                victoryCount = projection.victoryCount,
                                defeatCount = projection.defeatCount,
                                drawCount = projection.drawCount,
                            ),
                    )
                }
        return BrawlerEnemyResultStatistics.create(counts = counts)
    }

    fun findBrawlerDailyResultStatistics(
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        endDate: LocalDate,
    ): BrawlerResultDailyStatistics {
        val dailyCounts: List<BrawlerResultDailyCount> =
            battleResultJpaRepository
                .findAllByBrawlerBrawlStarsIdAndTierRangeAndBattleDateBetween(
                    brawlerBrawlStarsId = brawlerId.value,
                    tierRange = tierRange,
                    startDate = startDate,
                    endDate = endDate,
                ).groupBy { it.battleDate }
                .map { (battleDate, grouped) ->
                    BrawlerResultDailyCount(
                        date = battleDate,
                        brawlerResultCount =
                            BrawlerResultCount(
                                brawlerId = brawlerId,
                                resultCount =
                                    grouped
                                        .map { it.resultCount.toResultCount() }
                                        .reduce(ResultCount::merge),
                                starPlayerCount = grouped.sumOf { it.starPlayerCount },
                            ),
                    )
                }.sortedBy { it.date }
        return BrawlerResultDailyStatistics.create(brawlerId = brawlerId, dailyCounts = dailyCounts)
    }

    private fun BrawlerPairResultProjection.toPairResultCount() =
        BrawlerPairResultCount(
            brawler1Id = BrawlStarsId(brawler1BrawlStarsId),
            brawler2Id = BrawlStarsId(brawler2BrawlStarsId),
            resultCount =
                ResultCount(
                    victoryCount = victoryCount,
                    defeatCount = defeatCount,
                    drawCount = drawCount,
                ),
        )

    private fun BrawlerTrioResultProjection.toTrioResultCount() =
        BrawlerTrioResultCount(
            brawler1Id = BrawlStarsId(brawler1BrawlStarsId),
            brawler2Id = BrawlStarsId(brawler2BrawlStarsId),
            brawler3Id = BrawlStarsId(brawler3BrawlStarsId),
            resultCount =
                ResultCount(
                    victoryCount = victoryCount,
                    defeatCount = defeatCount,
                    drawCount = drawCount,
                ),
        )
}
