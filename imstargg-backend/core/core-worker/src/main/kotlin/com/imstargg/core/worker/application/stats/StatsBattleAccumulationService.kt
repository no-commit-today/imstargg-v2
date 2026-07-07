package com.imstargg.core.worker.application.stats

import com.imstargg.client.brawlstars.BrawlStarsApiClientException
import com.imstargg.client.brawlstars.BrawlStarsApiServerException
import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.core.worker.application.maintenance.BrawlStarsApiMaintenanceAutoRegistrar
import com.imstargg.core.worker.application.maintenance.BrawlStarsApiMaintenanceChecker
import com.imstargg.core.worker.application.player.PlayerDeactivator
import com.imstargg.core.worker.config.StatsBattleScheduleProperties
import com.imstargg.core.worker.domain.BattleBuilder
import com.imstargg.infrastructure.db.core.statistics.battle.StatsPlayerEntity
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsPlayerJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
class StatsBattleAccumulationService(
    private val properties: StatsBattleScheduleProperties,
    private val statsPlayerRepository: StatsPlayerJpaRepository,
    private val brawlStarsClient: BrawlStarsClient,
    private val statsBattleAggregator: StatsBattleAggregator,
    private val playerDeactivator: PlayerDeactivator,
    private val brawlStarsApiMaintenanceChecker: BrawlStarsApiMaintenanceChecker,
    private val brawlStarsApiMaintenanceAutoRegistrar: BrawlStarsApiMaintenanceAutoRegistrar,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun accumulate() {
        if (brawlStarsApiMaintenanceChecker.isUnderMaintenance()) {
            log.info("[배틀 통계 축적] API 점검 중으로 건너뜀")
            return
        }

        val statsPlayers = findRandomPlayers()
        log.debug("[배틀 통계 축적] {}명 플레이어 처리 시작: {}", statsPlayers.size, statsPlayers.map { it.brawlStarsTag })

        for (statsPlayer in statsPlayers) {
            try {
                val battleResponses = brawlStarsClient.getPlayerRecentBattles(statsPlayer.brawlStarsTag).items
                val battles = battleResponses.map { BattleBuilder(statsPlayer.brawlStarsTag, it).build() }
                statsBattleAggregator.aggregate(battles)
            } catch (ex: BrawlStarsApiClientException.NotFound) {
                log.warn("[배틀 통계 축적] 플레이어 삭제됨 tag={}", statsPlayer.brawlStarsTag)
                playerDeactivator.deactivate(statsPlayer.brawlStarsTag)
            } catch (ex: BrawlStarsApiServerException.ServiceUnavailable) {
                log.warn("[배틀 통계 축적] API 점검 중 감지 tag={}", statsPlayer.brawlStarsTag, ex)
                brawlStarsApiMaintenanceAutoRegistrar.register()
                break
            } catch (ex: BrawlStarsApiClientException) {
                log.warn("[배틀 통계 축적] API 클라이언트 오류 tag={}", statsPlayer.brawlStarsTag, ex)
            } catch (ex: BrawlStarsApiServerException) {
                log.warn("[배틀 통계 축적] API 서버 오류 tag={}", statsPlayer.brawlStarsTag, ex)
            } catch (ex: Exception) {
                log.error("[배틀 통계 축적] 처리 실패 tag={}", statsPlayer.brawlStarsTag, ex)
            }
        }

        log.debug("[배틀 통계 축적] 처리 완료")
    }

    private fun findRandomPlayers(): List<StatsPlayerEntity> {
        val maxId = statsPlayerRepository.findTopByOrderByIdDesc()?.id ?: return emptyList()
        val randomId = Random.nextLong(1, maxId + 1)
        return statsPlayerRepository.findAllByIdGreaterThanEqual(
            randomId,
            PageRequest.of(0, properties.playerCount),
        )
    }
}
