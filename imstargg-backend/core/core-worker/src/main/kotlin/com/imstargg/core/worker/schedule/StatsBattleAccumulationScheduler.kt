package com.imstargg.core.worker.schedule

import com.imstargg.client.brawlstars.BrawlStarsApiClientException
import com.imstargg.client.brawlstars.BrawlStarsApiServerException
import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.core.worker.application.StatsBattleAggregator
import com.imstargg.core.worker.config.StatsBattleScheduleProperties
import com.imstargg.core.worker.domain.BattleBuilder
import com.imstargg.infrastructure.db.core.statistics.StatsPlayerJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
@ConditionalOnProperty(prefix = "app.schedule.stats-battle", name = ["enabled"], havingValue = "true")
class StatsBattleAccumulationScheduler(
    private val properties: StatsBattleScheduleProperties,
    private val statsPlayerRepository: StatsPlayerJpaRepository,
    private val brawlStarsClient: BrawlStarsClient,
    private val statsBattleAggregator: StatsBattleAggregator,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${app.schedule.stats-battle.interval}")
    fun accumulate() {
        val statsPlayers = findRandomPlayers()
        log.info("[배틀 통계 축적] {}명 플레이어 처리 시작", statsPlayers.size)

        statsPlayers.forEach { statsPlayer ->
            try {
                val battleResponses = brawlStarsClient.getPlayerRecentBattles(statsPlayer.brawlStarsTag).items
                val battles = battleResponses.map { BattleBuilder(statsPlayer.brawlStarsTag, it).build() }
                statsBattleAggregator.aggregate(battles)
            } catch (ex: BrawlStarsApiClientException) {
                log.warn("[배틀 통계 축적] API 클라이언트 오류 tag={}", statsPlayer.brawlStarsTag, ex)
            } catch (ex: BrawlStarsApiServerException) {
                log.warn("[배틀 통계 축적] API 서버 오류 tag={}", statsPlayer.brawlStarsTag, ex)
            } catch (ex: Exception) {
                log.error("[배틀 통계 축적] 처리 실패 tag={}", statsPlayer.brawlStarsTag, ex)
            }
        }

        log.info("[배틀 통계 축적] 처리 완료")
    }

    private fun findRandomPlayers(): List<com.imstargg.infrastructure.db.core.statistics.StatsPlayerEntity> {
        val maxId = statsPlayerRepository.findTopByOrderByIdDesc()?.id ?: return emptyList()
        val randomId = Random.nextLong(1, maxId + 1)
        return statsPlayerRepository.findAllByIdGreaterThanEqual(
            randomId,
            PageRequest.of(0, properties.playerCount),
        )
    }
}
