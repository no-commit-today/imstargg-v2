package com.imstargg.core.batch.job.statistics.playersync

import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.BrawlerRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.BrawlerRankingJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.PlayerRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.PlayerRankingJpaRepository
import com.imstargg.infrastructure.db.core.statistics.battle.StatsPlayerEntity
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsPlayerJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class StatsPlayerSyncFromRankingTask(
    private val statsPlayerJpaRepository: StatsPlayerJpaRepository,
    private val playerRankingJpaRepository: PlayerRankingJpaRepository,
    private val playerRankingItemJpaRepository: PlayerRankingItemJpaRepository,
    private val brawlerRankingJpaRepository: BrawlerRankingJpaRepository,
    private val brawlerRankingItemJpaRepository: BrawlerRankingItemJpaRepository,
) {
    fun sync() {
        log.info("[{}] 시작", JOB_NAME)
        val allTags = latestRankingTags()
        var totalNew = 0

        allTags.chunked(CHUNK_SIZE).forEach { batch ->
            val existingTags =
                statsPlayerJpaRepository
                    .findAllByBrawlStarsTagIn(batch)
                    .map { it.brawlStarsTag }
                    .toSet()
            val newPlayers =
                batch
                    .filter { it !in existingTags }
                    .map { StatsPlayerEntity(brawlStarsTag = it) }
            if (newPlayers.isNotEmpty()) {
                statsPlayerJpaRepository.saveAll(newPlayers)
                totalNew += newPlayers.size
            }
        }

        log.info("[{}] 완료 - 대상 태그: {}건, 신규: {}건", JOB_NAME, allTags.size, totalNew)
    }

    private fun latestRankingTags(): List<String> {
        val playerRankings =
            Country.entries.mapNotNull { country ->
                playerRankingJpaRepository.findTopByCountryOrderByCollectedAtDesc(country)
            }
        val brawlerRankings = brawlerRankingJpaRepository.findAllLatestPerCountryAndBrawler()

        return buildList {
            if (playerRankings.isNotEmpty()) {
                addAll(
                    playerRankingItemJpaRepository
                        .findAllByPlayerRankingIn(playerRankings)
                        .map { it.player.brawlStarsTag },
                )
            }
            if (brawlerRankings.isNotEmpty()) {
                addAll(
                    brawlerRankingItemJpaRepository
                        .findAllByBrawlerRankingIn(brawlerRankings)
                        .map { it.player.brawlStarsTag },
                )
            }
        }.distinct()
    }

    companion object {
        private const val JOB_NAME = "statsPlayerSyncFromRankingJob"
        private const val CHUNK_SIZE = 1000
        private val log = LoggerFactory.getLogger(StatsPlayerSyncFromRankingTask::class.java)
    }
}
