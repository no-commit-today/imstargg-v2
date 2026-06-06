package com.imstargg.core.batch.job.brawlstars.task

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.brawlstars.ranking.ClubRankingEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.ClubRankingItemEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.ClubRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.ClubRankingJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Clock

@Component
class ClubRankingCollectTask(
    private val clock: Clock,
    private val brawlStarsClient: BrawlStarsClient,
    private val clubRankingJpaRepository: ClubRankingJpaRepository,
    private val clubRankingItemJpaRepository: ClubRankingItemJpaRepository,
) {
    fun collect() {
        log.info("[{}] 시작", STEP_NAME)
        Country.entries.forEach { country ->
            val collectedAt = clock.instant()
            val response = brawlStarsClient.getClubRanking(country.code)
            val ranking =
                clubRankingJpaRepository.save(
                    ClubRankingEntity(country = country, collectedAt = collectedAt),
                )
            val items =
                response.items.map { resp ->
                    ClubRankingItemEntity(
                        clubRanking = ranking,
                        rank = resp.rank,
                        brawlStarsTag = resp.tag,
                        name = resp.name,
                        badgeBrawlStarsId = resp.badgeId.toLong(),
                        trophies = resp.trophies,
                        memberCount = resp.memberCount,
                    )
                }
            clubRankingItemJpaRepository.saveAll(items)

            val outdated = deleteOutdated(country)
            log.debug(
                "[{}] 국가 {} 수집 - {}건 (정리: {}건)",
                STEP_NAME,
                country,
                items.size,
                outdated,
            )
        }
        log.info("[{}] 완료", STEP_NAME)
    }

    private fun deleteOutdated(country: Country): Int {
        val outdated =
            clubRankingJpaRepository
                .findAllByCountryOrderByCollectedAtDesc(country)
                .drop(SNAPSHOT_RETENTION_COUNT)
        if (outdated.isEmpty()) return 0
        val outdatedItems = clubRankingItemJpaRepository.findAllByClubRankingIn(outdated)
        clubRankingItemJpaRepository.deleteAllInBatch(outdatedItems)
        clubRankingJpaRepository.deleteAllInBatch(outdated)
        return outdated.size
    }

    companion object {
        private const val STEP_NAME = "clubRankingCollectStep"
        private const val SNAPSHOT_RETENTION_COUNT = 2
        private val log = LoggerFactory.getLogger(ClubRankingCollectTask::class.java)
    }
}
