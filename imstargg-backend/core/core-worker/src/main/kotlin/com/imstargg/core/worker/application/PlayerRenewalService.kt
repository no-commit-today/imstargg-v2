package com.imstargg.core.worker.application

import com.imstargg.client.brawlstars.BrawlStarsApiClientException
import com.imstargg.client.brawlstars.BrawlStarsApiServerException
import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.core.enums.RenewalStatus
import com.imstargg.core.worker.domain.BattleBuilder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PlayerRenewalService(
    private val brawlStarsClient: BrawlStarsClient,
    private val playerRenewalFinder: PlayerRenewalFinder,
    private val playerRenewalProcessor: PlayerRenewalProcessor,
    private val playerUpdater: PlayerUpdater,
    private val statsBattleAggregator: StatsBattleAggregator,
    private val brawlStarsApiMaintenanceChecker: BrawlStarsApiMaintenanceChecker,
    private val brawlStarsApiMaintenanceAutoRegistrar: BrawlStarsApiMaintenanceAutoRegistrar,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun renew(brawlStarsTag: String) {
        var renewal = playerRenewalFinder.findByTag(brawlStarsTag)

        if (renewal.status != RenewalStatus.PENDING) {
            log.info("[플레이어 갱신] 이미 처리됨 tag={}, status={}", brawlStarsTag, renewal.status)
            return
        }

        if (brawlStarsApiMaintenanceChecker.isUnderMaintenance()) {
            playerRenewalProcessor.fail(renewal)
            log.info("[플레이어 갱신] API 점검 중 tag={}", brawlStarsTag)
            return
        }

        renewal = playerRenewalProcessor.executing(renewal)

        try {
            val playerResponse = brawlStarsClient.getPlayerInformation(brawlStarsTag)
            val battleResponses = brawlStarsClient.getPlayerRecentBattles(brawlStarsTag).items
            val battles = battleResponses.map { BattleBuilder(playerResponse.tag, it).build() }

            val savedBattles = playerUpdater.update(playerResponse, battles)

            playerRenewalProcessor.complete(renewal)
            statsBattleAggregator.asyncAggregate(savedBattles)
        } catch (ex: BrawlStarsApiClientException.NotFound) {
            playerRenewalProcessor.notFound(renewal)
        } catch (ex: BrawlStarsApiServerException.ServiceUnavailable) {
            log.warn("[플레이어 갱신] ServiceUnavailable tag={}", brawlStarsTag, ex)
            brawlStarsApiMaintenanceAutoRegistrar.register()
            playerRenewalProcessor.fail(renewal)
        } catch (ex: Exception) {
            log.error("[플레이어 갱신] 실패 tag={}", brawlStarsTag, ex)
            playerRenewalProcessor.fail(renewal)
        }
    }
}
