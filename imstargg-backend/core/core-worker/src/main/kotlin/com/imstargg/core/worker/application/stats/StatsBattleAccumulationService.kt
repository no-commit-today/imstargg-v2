package com.imstargg.core.worker.application.stats

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.client.brawlstars.response.BattleResponse
import com.imstargg.client.brawlstars.response.ListResponse
import com.imstargg.core.enums.StatsPlayerStatus
import com.imstargg.core.worker.application.maintenance.BrawlStarsApiMaintenanceAutoRegistrar
import com.imstargg.core.worker.application.maintenance.BrawlStarsApiMaintenanceChecker
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
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore
import kotlin.random.Random

private val log = KotlinLogging.logger {}

@Service
class StatsBattleAccumulationService(
    private val properties: StatsBattleScheduleProperties,
    private val statsPlayerRepository: StatsPlayerJpaRepository,
    private val brawlStarsClient: BrawlStarsClient,
    private val statsBattleAggregator: StatsBattleAggregator,
    private val statsPlayerStatusUpdater: StatsPlayerStatusUpdater,
    private val brawlStarsApiMaintenanceChecker: BrawlStarsApiMaintenanceChecker,
    private val brawlStarsApiMaintenanceAutoRegistrar: BrawlStarsApiMaintenanceAutoRegistrar,
) {
    // 틱이 주기보다 길어 다음 틱과 겹쳐도 상한을 넘지 않도록 틱마다가 아니라 서비스 전체가 나눠 쓴다
    private val requestPermits = Semaphore(properties.concurrency)

    fun accumulate() {
        if (brawlStarsApiMaintenanceChecker.isUnderMaintenance()) {
            log.info { "[배틀 통계 축적] API 점검 중으로 건너뜀" }
            return
        }

        val statsPlayers = findRandomPlayers()
        if (statsPlayers.isEmpty()) {
            log.debug { "[배틀 통계 축적] 뽑을 플레이어가 없어 건너뜀" }
            return
        }
        log.debug { "[배틀 통계 축적] ${statsPlayers.size}명 플레이어 처리 시작: ${statsPlayers.map { it.brawlStarsTag }}" }

        // 요청은 함께 보내고, 결과는 이 스레드에서 플레이어 순서대로 처리해 집계 DB 쓰기를 순차로 한다
        val results = fetchRecentBattles(statsPlayers.map { it.brawlStarsTag })
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
                    // battlelog 404 는 /players 가 살아 있는 계정에서도 온다 — 계정을 지우지 않고 수집 대상에서만 뺀다
                    is HttpClientErrorException.NotFound -> {
                        statsPlayerStatusUpdater.markBattleLogUnavailable(statsPlayer.id!!)
                        log.info { "[배틀 통계 축적] battlelog 없음(404) — 수집 제외 tag=$tag" }
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
     * 태그마다 가상 스레드에서 battlelog 를 조회한다. 동시에 떠 있는 요청은 서비스 전체에서 [StatsBattleScheduleProperties.concurrency] 개까지다.
     *
     * 결과 맵은 입력 순서를 따른다. 태그별 `Exception` 은 [Result.failure] 로 담아 나머지 태그를 막지 않고(예외 타입은
     * [BrawlStarsClient.getPlayerRecentBattles] 와 같다), `Error` 는 그대로 던진다.
     * 모든 태그가 끝날 때까지 기다리며 전체 제한 시간은 두지 않는다 — 건별 read-timeout 과 레이트리밋 대기가 상한이다.
     */
    private fun fetchRecentBattles(tags: List<String>): Map<String, Result<ListResponse<BattleResponse>>> =
        Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("stats-battle-", 0).factory()).use { executor ->
            tags
                .associateWith { tag -> executor.submit(Callable { withRequestPermit { brawlStarsClient.getPlayerRecentBattles(tag) } }) }
                .mapValues { (_, future) ->
                    try {
                        Result.success(future.get())
                    } catch (ex: ExecutionException) {
                        // runCatching 은 Error 까지 Result 에 담으므로 쓰지 않는다
                        val cause = ex.cause ?: ex
                        if (cause is Error) throw cause
                        Result.failure(cause)
                    }
                }
        }

    private inline fun <T> withRequestPermit(block: () -> T): T {
        requestPermits.acquire()
        return try {
            block()
        } finally {
            requestPermits.release()
        }
    }

    /**
     * 플레이어마다 무작위 ID 를 따로 뽑는다. stats_player 는 같은 전투의 팀원·상대가 연속 ID 로 적재되므로
     * (core-batch `StatsPlayerSyncJobItemWriter`), 한 ID 에서 연속 N명을 읽으면 전투 로그가 겹치는 플레이어가
     * 묶여 battle_key 중복으로 걸러지고 수집량이 거의 늘지 않는다.
     * battlelog 404 로 표시된(`BATTLE_LOG_UNAVAILABLE`) 행은 뽑지 않는다 — 뽑은 ID 이상에 ACTIVE 행이 없으면 그 추출은 빈다.
     */
    private fun findRandomPlayers(): List<StatsPlayerEntity> {
        val maxId = statsPlayerRepository.findTopByOrderByIdDesc()?.id ?: return emptyList()
        return List(properties.playerCount) {
            statsPlayerRepository.findFirstByIdGreaterThanEqualAndStatusOrderByIdAsc(
                Random.nextLong(1, maxId + 1),
                StatsPlayerStatus.ACTIVE,
            )
        }.filterNotNull()
            .distinctBy { it.id }
    }
}
