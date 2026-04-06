package com.imstargg.core.worker.application

import com.imstargg.client.brawlstars.BrawlStarsApiClientException
import com.imstargg.client.brawlstars.BrawlStarsApiServerException
import com.imstargg.client.brawlstars.BrawlStarsClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PlayerRenewalService(
    private val brawlStarsClient: BrawlStarsClient,
    private val playerRenewalFinder: PlayerRenewalFinder,
    private val playerRenewalProcessor: PlayerRenewalProcessor,
    private val playerUpdater: PlayerUpdater,
    private val playerDeactivator: PlayerDeactivator,
    private val brawlStarsApiMaintenanceChecker: BrawlStarsApiMaintenanceChecker,
    private val brawlStarsApiMaintenanceAutoRegistrar: BrawlStarsApiMaintenanceAutoRegistrar,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun renew(brawlStarsTag: String) {
        var renewal = playerRenewalFinder.findByTag(brawlStarsTag)

        if (brawlStarsApiMaintenanceChecker.isUnderMaintenance()) {
            playerRenewalProcessor.fail(renewal)
            log.info("[플레이어 갱신] API 점검 중 tag={}", brawlStarsTag)
            return
        }

        renewal = playerRenewalProcessor.executing(renewal)

        try {
            val playerResponse = brawlStarsClient.getPlayerInformation(brawlStarsTag)
            val battleResponses = brawlStarsClient.getPlayerRecentBattles(brawlStarsTag).items

            playerUpdater.update(playerResponse, battleResponses)

            playerRenewalProcessor.complete(renewal)
        } catch (ex: BrawlStarsApiClientException.NotFound) {
            playerDeactivator.deactivate(brawlStarsTag)
            playerRenewalProcessor.notFound(renewal)
        } catch (ex: BrawlStarsApiServerException.ServiceUnavailable) {
            log.warn("[플레이어 갱신] ServiceUnavailable tag={}", brawlStarsTag, ex)
            brawlStarsApiMaintenanceAutoRegistrar.register()
            playerRenewalProcessor.fail(renewal)
        } catch (ex: Exception) {
            playerRenewalProcessor.fail(renewal)
            throw ex
        }
    }
}
