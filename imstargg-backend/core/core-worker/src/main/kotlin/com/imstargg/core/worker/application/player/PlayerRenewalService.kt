package com.imstargg.core.worker.application.player

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.client.brawlstars.response.BattleResponse
import com.imstargg.core.enums.RenewalStatus
import com.imstargg.core.enums.RenewalType
import com.imstargg.core.worker.application.maintenance.BrawlStarsApiMaintenanceAutoRegistrar
import com.imstargg.core.worker.application.maintenance.BrawlStarsApiMaintenanceChecker
import com.imstargg.core.worker.application.renewal.RenewalFinder
import com.imstargg.core.worker.application.renewal.RenewalProcessor
import com.imstargg.core.worker.application.stats.StatsBattleAggregator
import com.imstargg.core.worker.domain.Battle
import com.imstargg.core.worker.domain.BattleBuilder
import com.imstargg.infrastructure.db.core.renewal.RenewalEntity
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException

private val log = KotlinLogging.logger {}

@Service
class PlayerRenewalService(
    private val brawlStarsClient: BrawlStarsClient,
    private val renewalFinder: RenewalFinder,
    private val renewalProcessor: RenewalProcessor,
    private val playerUpdater: PlayerUpdater,
    private val playerDeactivator: PlayerDeactivator,
    private val statsBattleAggregator: StatsBattleAggregator,
    private val brawlStarsApiMaintenanceChecker: BrawlStarsApiMaintenanceChecker,
    private val brawlStarsApiMaintenanceAutoRegistrar: BrawlStarsApiMaintenanceAutoRegistrar,
) {
    fun renew(brawlStarsTag: String) {
        var renewal = renewalFinder.findByTag(brawlStarsTag, RenewalType.PLAYER)

        if (renewal.status != RenewalStatus.PENDING) {
            log.info { "[플레이어 갱신] 이미 처리됨 tag=$brawlStarsTag, status=${renewal.status}" }
            return
        }

        if (brawlStarsApiMaintenanceChecker.isUnderMaintenance()) {
            updateRenewalStatus(renewal) { renewalProcessor.fail(it) }
            log.info { "[플레이어 갱신] API 점검 중 tag=$brawlStarsTag" }
            return
        }

        try {
            renewal = renewalProcessor.executing(renewal)
        } catch (ex: ObjectOptimisticLockingFailureException) {
            log.warn(ex) { "[플레이어 갱신] 중복 처리 감지 tag=$brawlStarsTag" }
            return
        }

        try {
            val playerResponse = brawlStarsClient.getPlayerInformation(brawlStarsTag)
            val battleResponses = findRecentBattles(brawlStarsTag)
            val battles = battleResponses.map { BattleBuilder(playerResponse.tag, it).build() }

            val savedBattles = playerUpdater.update(playerResponse, battles)

            if (updateRenewalStatus(renewal) { renewalProcessor.complete(it) }) {
                aggregateStats(brawlStarsTag, savedBattles)
            }
        } catch (ex: HttpClientErrorException.NotFound) {
            updateRenewalStatus(renewal) {
                renewalProcessor.notFound(it) { playerDeactivator.deactivate(brawlStarsTag) }
            }
        } catch (ex: HttpServerErrorException.ServiceUnavailable) {
            log.warn(ex) { "[플레이어 갱신] ServiceUnavailable tag=$brawlStarsTag" }
            brawlStarsApiMaintenanceAutoRegistrar.register()
            updateRenewalStatus(renewal) { renewalProcessor.fail(it) }
        } catch (ex: Exception) {
            log.error(ex) { "[플레이어 갱신] 실패 tag=$brawlStarsTag" }
            updateRenewalStatus(renewal) { renewalProcessor.fail(it) }
        }
    }

    /**
     * battlelog 만 404 이고 `/players` 는 성공한 계정은 전투 0건으로 갱신을 완료한다.
     * `/players` 404 는 여기서 다루지 않고 호출부 catch 가 받는다.
     */
    private fun findRecentBattles(brawlStarsTag: String): List<BattleResponse> =
        try {
            brawlStarsClient.getPlayerRecentBattles(brawlStarsTag).items
        } catch (ex: HttpClientErrorException.NotFound) {
            log.info { "[플레이어 갱신] battlelog 없음(404) — 전투 0건으로 완료 tag=$brawlStarsTag" }
            emptyList()
        }

    /**
     * 통계 집계는 renewal COMPLETED 확정 이후의 부가 작업이며 비동기로 처리한다. 집계 로직 내부
     * 예외는 @Async 스레드에서 AsyncExceptionHandler 가 잡아 로그만 남긴다(허용된 드문 유실 —
     * 대상 플레이어가 stats_player 에 ACTIVE 로 있으면 축적 스케줄러가 7일 내 재수집 가능).
     *
     * 다만 태스크 제출 자체의 실패(executor 큐 포화 시 TaskRejectedException, 종료 중 거부 등)는
     * 호출 스레드로 동기 전파되므로 여기서 격리한다 — 격리하지 않으면 이미 COMPLETED 인 renewal 에
     * fail() 전이를 시도(IllegalStateException)해 SQS 재전달로 번진다.
     */
    private fun aggregateStats(
        brawlStarsTag: String,
        battles: List<Battle>,
    ) {
        try {
            statsBattleAggregator.asyncAggregate(battles)
        } catch (ex: Exception) {
            log.error(ex) { "[플레이어 갱신] 통계 집계 태스크 제출 실패 (renewal 은 COMPLETED 유지) tag=$brawlStarsTag" }
        }
    }

    private fun updateRenewalStatus(
        renewal: RenewalEntity,
        action: (RenewalEntity) -> Unit,
    ): Boolean =
        try {
            action(renewal)
            true
        } catch (ex: ObjectOptimisticLockingFailureException) {
            log.warn(ex) { "[플레이어 갱신] 갱신 상태 업데이트 중 버전 충돌 tag=${renewal.brawlStarsTag}" }
            false
        }
}
