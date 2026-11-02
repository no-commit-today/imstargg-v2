package com.imstargg.core.worker.application.stats

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.core.worker.application.maintenance.BrawlStarsApiMaintenanceAutoRegistrar
import com.imstargg.core.worker.application.maintenance.BrawlStarsApiMaintenanceChecker
import com.imstargg.core.worker.application.player.PlayerDeactivator
import com.imstargg.core.worker.config.StatsBattleScheduleProperties
import com.imstargg.core.worker.domain.BattleBuilder
import com.imstargg.infrastructure.db.core.statistics.battle.StatsPlayerEntity
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsPlayerJpaRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientResponseException
import kotlin.random.Random

private val log = KotlinLogging.logger {}

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
    fun accumulate() {
        if (brawlStarsApiMaintenanceChecker.isUnderMaintenance()) {
            log.info { "[배틀 통계 축적] API 점검 중으로 건너뜀" }
            return
        }

        val statsPlayers = findRandomPlayers()
        log.debug { "[배틀 통계 축적] ${statsPlayers.size}명 플레이어 처리 시작: ${statsPlayers.map { it.brawlStarsTag }}" }

        // 요청은 클라이언트가 한 번에 함께 보내고, 결과는 이 스레드에서 플레이어 순서대로 처리해 집계 DB 쓰기를 순차로 한다
        val results = brawlStarsClient.getPlayersRecentBattles(statsPlayers.map { it.brawlStarsTag })
        // 한 틱의 요청이 함께 나가 점검 응답이 여러 개 섞일 수 있다 — 점검은 틱당 한 번만 등록하고,
        // 남은 플레이어는 중단하지 않고 각자의 결과대로 처리한다(다음 틱부터 사전 확인이 호출을 막는다)
        var maintenanceRegistered = false
        for (statsPlayer in statsPlayers) {
            val tag = statsPlayer.brawlStarsTag
            try {
                val result = results.getValue(tag)
                when (val ex = result.exceptionOrNull()) {
                    null -> {
                        val battles = result.getOrThrow().items.map { BattleBuilder(tag, it).build() }
                        statsBattleAggregator.aggregate(battles)
                    }
                    is HttpClientErrorException.NotFound -> {
                        log.warn { "[배틀 통계 축적] 플레이어 삭제됨 tag=$tag" }
                        playerDeactivator.deactivate(tag)
                    }
                    is HttpServerErrorException.ServiceUnavailable -> {
                        if (!maintenanceRegistered) {
                            maintenanceRegistered = true
                            log.warn(ex) { "[배틀 통계 축적] API 점검 중 감지 tag=$tag" }
                            brawlStarsApiMaintenanceAutoRegistrar.register()
                        }
                    }
                    is RestClientResponseException -> log.warn(ex) { "[배틀 통계 축적] API 오류 status=${ex.statusCode.value()} tag=$tag" }
                    is ResourceAccessException -> log.warn(ex) { "[배틀 통계 축적] API 통신 실패 tag=$tag" }
                    else -> log.error(ex) { "[배틀 통계 축적] 처리 실패 tag=$tag" }
                }
            } catch (ex: Exception) {
                log.error(ex) { "[배틀 통계 축적] 처리 실패 tag=$tag" }
            }
        }

        log.debug { "[배틀 통계 축적] 처리 완료" }
    }

    /**
     * 플레이어마다 무작위 ID 를 따로 뽑는다. stats_player 는 같은 전투의 팀원·상대가 연속 ID 로 적재되므로
     * (core-batch `StatsPlayerSyncJobItemWriter`), 한 ID 에서 연속 N명을 읽으면 전투 로그가 겹치는 플레이어가
     * 묶여 battle_key 중복으로 걸러지고 수집량이 거의 늘지 않는다.
     */
    private fun findRandomPlayers(): List<StatsPlayerEntity> {
        val maxId = statsPlayerRepository.findTopByOrderByIdDesc()?.id ?: return emptyList()
        return List(properties.playerCount) {
            statsPlayerRepository.findFirstByIdGreaterThanEqualOrderByIdAsc(Random.nextLong(1, maxId + 1))
        }.filterNotNull()
            .distinctBy { it.id }
    }
}
