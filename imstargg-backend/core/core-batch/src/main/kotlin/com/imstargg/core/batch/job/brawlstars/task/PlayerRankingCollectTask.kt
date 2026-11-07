package com.imstargg.core.batch.job.brawlstars.task

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.brawlstars.ranking.PlayerRankingEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.PlayerRankingItemEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.RankingEntityPlayer
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.PlayerRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.PlayerRankingJpaRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.time.Clock

private val log = KotlinLogging.logger {}

@Component
class PlayerRankingCollectTask(
    private val clock: Clock,
    private val brawlStarsClient: BrawlStarsClient,
    private val playerRankingJpaRepository: PlayerRankingJpaRepository,
    private val playerRankingItemJpaRepository: PlayerRankingItemJpaRepository,
) {
    fun collect() {
        log.info { "[$STEP_NAME] 시작" }
        Country.entries.forEach { country ->
            val collectedAt = clock.instant()
            val response = brawlStarsClient.getPlayerRanking(country.code)
            val ranking =
                playerRankingJpaRepository.save(
                    PlayerRankingEntity(country = country, collectedAt = collectedAt),
                )
            val items =
                response.items.map { resp ->
                    PlayerRankingItemEntity(
                        playerRanking = ranking,
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
            playerRankingItemJpaRepository.saveAll(items)

            val outdated = deleteOutdated(country)
            log.debug { "[$STEP_NAME] 국가 $country 수집 - ${items.size}건 (정리: ${outdated}건)" }
        }
        log.info { "[$STEP_NAME] 완료" }
    }

    private fun deleteOutdated(country: Country): Int {
        val outdated =
            playerRankingJpaRepository
                .findAllByCountryOrderByCollectedAtDesc(country)
                .drop(SNAPSHOT_RETENTION_COUNT)
        if (outdated.isEmpty()) return 0
        val outdatedItems = playerRankingItemJpaRepository.findAllByPlayerRankingIn(outdated)
        playerRankingItemJpaRepository.deleteAllInBatch(outdatedItems)
        playerRankingJpaRepository.deleteAllInBatch(outdated)
        return outdated.size
    }

    companion object {
        private const val STEP_NAME = "playerRankingCollectStep"
        private const val SNAPSHOT_RETENTION_COUNT = 2
    }
}
