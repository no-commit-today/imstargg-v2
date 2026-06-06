package com.imstargg.core.batch.job.brawlstars.task

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.brawlstars.ranking.BrawlerRankingEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.BrawlerRankingItemEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.RankingEntityPlayer
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.BrawlerRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.BrawlerRankingJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Clock

@Component
class BrawlerRankingCollectTask(
    private val clock: Clock,
    private val brawlStarsClient: BrawlStarsClient,
    private val brawlerRankingJpaRepository: BrawlerRankingJpaRepository,
    private val brawlerRankingItemJpaRepository: BrawlerRankingItemJpaRepository,
) {
    fun collect() {
        log.info("[{}] 시작", JOB_NAME)
        val brawlers = brawlStarsClient.getBrawlers().items
        Country.entries.forEach { country ->
            log.info("[{}] 국가 {} 진행 - 브롤러 {}개", JOB_NAME, country, brawlers.size)
            brawlers.forEach { brawler ->
                val collectedAt = clock.instant()
                val response = brawlStarsClient.getBrawlerRanking(country.code, brawler.id)
                val ranking =
                    brawlerRankingJpaRepository.save(
                        BrawlerRankingEntity(
                            country = country,
                            brawlerBrawlStarsId = brawler.id,
                            collectedAt = collectedAt,
                        ),
                    )
                val items =
                    response.items.map { resp ->
                        BrawlerRankingItemEntity(
                            brawlerRankingId = ranking.id!!,
                            rank = resp.rank,
                            player =
                                RankingEntityPlayer(
                                    brawlStarsTag = resp.tag,
                                    name = resp.name,
                                    nameColor = resp.nameColor,
                                    iconBrawlStarsId = resp.icon.id,
                                    clubName = resp.club?.name ?: "",
                                ),
                            trophies = resp.trophies,
                        )
                    }
                brawlerRankingItemJpaRepository.saveAll(items)

                deleteOutdated(country, brawler.id)
            }
        }
        log.info("[{}] 완료", JOB_NAME)
    }

    private fun deleteOutdated(
        country: Country,
        brawlerBrawlStarsId: Long,
    ) {
        val outdated =
            brawlerRankingJpaRepository
                .findAllByCountryAndBrawlerBrawlStarsIdOrderByCollectedAtDesc(
                    country,
                    brawlerBrawlStarsId,
                ).drop(SNAPSHOT_RETENTION_COUNT)
        if (outdated.isEmpty()) return
        val outdatedIds = outdated.mapNotNull { it.id }
        val outdatedItems = brawlerRankingItemJpaRepository.findAllByBrawlerRankingIdIn(outdatedIds)
        brawlerRankingItemJpaRepository.deleteAllInBatch(outdatedItems)
        brawlerRankingJpaRepository.deleteAllInBatch(outdated)
    }

    companion object {
        private const val JOB_NAME = "brawlerRankingCollectJob"
        private const val SNAPSHOT_RETENTION_COUNT = 2
        private val log = LoggerFactory.getLogger(BrawlerRankingCollectTask::class.java)
    }
}
