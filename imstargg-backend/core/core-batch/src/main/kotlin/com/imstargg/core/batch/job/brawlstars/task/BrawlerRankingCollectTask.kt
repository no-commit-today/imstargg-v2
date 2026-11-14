package com.imstargg.core.batch.job.brawlstars.task

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.brawlstars.ranking.BrawlerRankingEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.BrawlerRankingItemEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.RankingEntityPlayer
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.BrawlerRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.BrawlerRankingJpaRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Clock

private val log = KotlinLogging.logger {}

@Component
class BrawlerRankingCollectTask(
    private val clock: Clock,
    private val brawlStarsClient: BrawlStarsClient,
    private val brawlerRankingJpaRepository: BrawlerRankingJpaRepository,
    private val brawlerRankingItemJpaRepository: BrawlerRankingItemJpaRepository,
) {
    fun collect() {
        log.info { "[$STEP_NAME] 시작" }
        val brawlers = brawlStarsClient.getBrawlers().items
        Country.entries.forEach { country ->
            log.info { "[$STEP_NAME] 국가 $country 진행 - 브롤러 ${brawlers.size}개" }
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
                            brawlerRanking = ranking,
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
        log.info { "[$STEP_NAME] 완료" }
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
        val outdatedItems = brawlerRankingItemJpaRepository.findAllByBrawlerRankingIn(outdated)
        brawlerRankingItemJpaRepository.deleteAllInBatch(outdatedItems)
        brawlerRankingJpaRepository.deleteAllInBatch(outdated)
    }

    companion object {
        private const val STEP_NAME = "brawlerRankingCollectStep"
        private const val SNAPSHOT_RETENTION_COUNT = 2
    }
}
