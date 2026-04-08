package com.imstargg.core.application.statistics

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.statistics.BrawlerTeamResultCount
import com.imstargg.core.domain.statistics.ResultCount
import com.imstargg.core.enums.StatsPeriodType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.BrawlerTeamBattleResultStatsEntity
import com.imstargg.infrastructure.db.core.statistics.repository.BrawlerTeamBattleResultStatsJpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Repository
@Transactional(readOnly = true)
class BrawlerTeamResultStatisticsRepository(
    private val teamBattleResultJpaRepository: BrawlerTeamBattleResultStatsJpaRepository,
) {
    fun findBrawlerTeamResultCounts(
        eventId: BrawlStarsId?,
        brawlerId: BrawlStarsId,
        tierRange: StatsTierRange,
        startDate: LocalDate,
        teamSize: Int,
    ): List<BrawlerTeamResultCount> {
        val entities =
            teamBattleResultJpaRepository.findAllByConditionsAndBrawler(
                eventBrawlStarsId = eventId?.value,
                periodType = StatsPeriodType.WEEKLY,
                tierRange = tierRange,
                startBattleDate = startDate,
                brawlerBrawlStarsId = brawlerId.value,
                teamSize = teamSize,
            )
        return when (teamSize) {
            2 -> extractDuoResultCounts(entities, brawlerId)
            3 ->
                entities.map {
                    BrawlerTeamResultCount(
                        brawlerIds = it.brawlerBrawlStarsIds.map { id -> BrawlStarsId(id) },
                        resultCount =
                            ResultCount(
                                it.resultCount.victoryCount,
                                it.resultCount.defeatCount,
                                it.resultCount.drawCount,
                            ),
                    )
                }
            else -> throw IllegalArgumentException("teamSize must be 2 or 3, but was $teamSize")
        }
    }

    private fun extractDuoResultCounts(
        entities: List<BrawlerTeamBattleResultStatsEntity>,
        brawlerId: BrawlStarsId,
    ): List<BrawlerTeamResultCount> {
        val otherBrawlerIdToResultCount = mutableMapOf<BrawlStarsId, ResultCount>()

        for (entity in entities) {
            val resultCount = ResultCount(entity.resultCount.victoryCount, entity.resultCount.defeatCount, entity.resultCount.drawCount)

            for (otherId in entity.brawlerBrawlStarsIds.filter { it != brawlerId.value }) {
                val otherBrawlerId = BrawlStarsId(otherId)
                otherBrawlerIdToResultCount.merge(otherBrawlerId, resultCount) { existing, new ->
                    existing.merge(new)
                }
            }
        }

        return otherBrawlerIdToResultCount.map { (otherBrawlerId, resultCount) ->
            BrawlerTeamResultCount(
                brawlerIds = listOf(brawlerId, otherBrawlerId),
                resultCount = resultCount,
            )
        }
    }
}
