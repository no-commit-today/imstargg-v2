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
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
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

    /**
     * 한 틱의 플레이어를 동시에 수집하는 전용 풀. 순차 호출은 응답 지연(약 0.2초)에 묶여 초당 5건 남짓이 한계다.
     * 가상 스레드는 Java 21 에서 synchronized 구간에 캐리어가 고정돼 커넥션 풀과 교착될 수 있어 플랫폼 스레드를 쓰고,
     * Executor 빈으로 등록하면 [com.imstargg.core.worker.config.AsyncConfig] 의 Executor 빈과 타입 주입이 모호해지므로
     * 서비스가 직접 소유한다.
     */
    private val executor =
        Executors.newFixedThreadPool(properties.playerCount, Thread.ofPlatform().name("stats-battle-", 0).factory())

    fun accumulate() {
        if (brawlStarsApiMaintenanceChecker.isUnderMaintenance()) {
            log.info("[배틀 통계 축적] API 점검 중으로 건너뜀")
            return
        }

        val statsPlayers = findRandomPlayers()
        log.debug("[배틀 통계 축적] {}명 플레이어 처리 시작: {}", statsPlayers.size, statsPlayers.map { it.brawlStarsTag })

        val maintenanceDetected = AtomicBoolean(false)
        statsPlayers
            .map { statsPlayer -> CompletableFuture.runAsync({ accumulate(statsPlayer, maintenanceDetected) }, executor) }
            .forEach { it.join() }

        log.debug("[배틀 통계 축적] 처리 완료")
    }

    private fun accumulate(
        statsPlayer: StatsPlayerEntity,
        maintenanceDetected: AtomicBoolean,
    ) {
        if (maintenanceDetected.get()) return
        try {
            val battleResponses = brawlStarsClient.getPlayerRecentBattles(statsPlayer.brawlStarsTag).items
            val battles = battleResponses.map { BattleBuilder(statsPlayer.brawlStarsTag, it).build() }
            statsBattleAggregator.aggregate(battles)
        } catch (ex: BrawlStarsApiClientException.NotFound) {
            log.warn("[배틀 통계 축적] 플레이어 삭제됨 tag={}", statsPlayer.brawlStarsTag)
            playerDeactivator.deactivate(statsPlayer.brawlStarsTag)
        } catch (ex: BrawlStarsApiServerException.ServiceUnavailable) {
            // 동시에 받은 점검 응답마다 등록하면 등록기의 조회 후 저장 사이 경합으로 점검이 중복 등록된다
            if (maintenanceDetected.compareAndSet(false, true)) {
                log.warn("[배틀 통계 축적] API 점검 중 감지 tag={}", statsPlayer.brawlStarsTag, ex)
                brawlStarsApiMaintenanceAutoRegistrar.register()
            }
        } catch (ex: BrawlStarsApiClientException) {
            log.warn("[배틀 통계 축적] API 클라이언트 오류 tag={}", statsPlayer.brawlStarsTag, ex)
        } catch (ex: BrawlStarsApiServerException) {
            log.warn("[배틀 통계 축적] API 서버 오류 tag={}", statsPlayer.brawlStarsTag, ex)
        } catch (ex: Exception) {
            log.error("[배틀 통계 축적] 처리 실패 tag={}", statsPlayer.brawlStarsTag, ex)
        }
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

    @PreDestroy
    fun shutdown() {
        executor.shutdown()
        executor.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    companion object {
        private const val SHUTDOWN_TIMEOUT_SECONDS = 30L
    }
}
