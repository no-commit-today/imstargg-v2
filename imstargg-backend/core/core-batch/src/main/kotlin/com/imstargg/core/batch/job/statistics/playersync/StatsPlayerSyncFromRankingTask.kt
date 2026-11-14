package com.imstargg.core.batch.job.statistics.playersync

import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.BrawlerRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.BrawlerRankingJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.PlayerRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.PlayerRankingJpaRepository
import com.imstargg.infrastructure.db.core.statistics.battle.StatsPlayerEntity
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsPlayerJpaRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class StatsPlayerSyncFromRankingTask(
    private val statsPlayerJpaRepository: StatsPlayerJpaRepository,
    private val playerRankingJpaRepository: PlayerRankingJpaRepository,
    private val playerRankingItemJpaRepository: PlayerRankingItemJpaRepository,
    private val brawlerRankingJpaRepository: BrawlerRankingJpaRepository,
    private val brawlerRankingItemJpaRepository: BrawlerRankingItemJpaRepository,
) {
    fun sync() {
        log.info { "[$JOB_NAME] 시작" }
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

        log.info { "[$JOB_NAME] 완료 - 대상 태그: ${allTags.size}건, 신규: ${totalNew}건" }
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
    }
}
