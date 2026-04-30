package com.imstargg.core.batch.job.brawlstars.task

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.brawlstars.ranking.PlayerRankingEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.PlayerRankingItemEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.RankingEntityPlayer
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.PlayerRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.PlayerRankingJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.OffsetDateTime

@Component
class PlayerRankingCollectTask(
    private val clock: Clock,
    private val brawlStarsClient: BrawlStarsClient,
    private val playerRankingJpaRepository: PlayerRankingJpaRepository,
    private val playerRankingItemJpaRepository: PlayerRankingItemJpaRepository,
) {
    fun collect() {
        log.info("[{}] 시작", JOB_NAME)
        Country.entries.forEach { country ->
            val collectedAt = OffsetDateTime.now(clock)
            val response = brawlStarsClient.getPlayerRanking(country.code)
            val ranking =
                playerRankingJpaRepository.save(
                    PlayerRankingEntity(country = country, collectedAt = collectedAt),
                )
            val items =
                response.items.map { resp ->
                    PlayerRankingItemEntity(
                        playerRankingId = ranking.id!!,
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
            log.debug(
                "[{}] 국가 {} 수집 - {}건 (정리: {}건)",
                JOB_NAME,
                country,
                items.size,
                outdated,
            )
        }
        log.info("[{}] 완료", JOB_NAME)
    }

    private fun deleteOutdated(country: Country): Int {
        val outdated =
            playerRankingJpaRepository
                .findAllByCountryOrderByCollectedAtDesc(country)
                .drop(SNAPSHOT_RETENTION_COUNT)
        if (outdated.isEmpty()) return 0
        val outdatedIds = outdated.mapNotNull { it.id }
        val outdatedItems = playerRankingItemJpaRepository.findAllByPlayerRankingIdIn(outdatedIds)
        playerRankingItemJpaRepository.deleteAllInBatch(outdatedItems)
        playerRankingJpaRepository.deleteAllInBatch(outdated)
        return outdated.size
    }

    companion object {
        private const val JOB_NAME = "playerRankingCollectJob"
        private const val SNAPSHOT_RETENTION_COUNT = 2
        private val log = LoggerFactory.getLogger(PlayerRankingCollectTask::class.java)
    }
}
