package com.imstargg.core.worker.application

import com.imstargg.client.brawlstars.BrawlStarsApiClientException
import com.imstargg.client.brawlstars.BrawlStarsApiServerException
import com.imstargg.client.brawlstars.BrawlStarsClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PlayerRenewalService(
    private val brawlStarsClient: BrawlStarsClient,
    private val playerRenewalProcessor: PlayerRenewalProcessor,
    private val playerUpdater: PlayerUpdater,
    private val playerDeactivator: PlayerDeactivator,
    private val brawlStarsApiMaintenanceChecker: BrawlStarsApiMaintenanceChecker,
    private val brawlStarsApiMaintenanceAutoRegistrar: BrawlStarsApiMaintenanceAutoRegistrar,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun renew(brawlStarsTag: String) {
        if (brawlStarsApiMaintenanceChecker.isUnderMaintenance()) {
            playerRenewalProcessor.failByTag(brawlStarsTag)
            log.info("[플레이어 갱신] API 점검 중 tag={}", brawlStarsTag)
            return
        }

        val renewalId = playerRenewalProcessor.executing(brawlStarsTag)

        try {
            val playerResponse = brawlStarsClient.getPlayerInformation(brawlStarsTag)
            val battleResponses = brawlStarsClient.getPlayerRecentBattles(brawlStarsTag).items

            playerUpdater.update(playerResponse, battleResponses)

            playerRenewalProcessor.complete(renewalId)
        } catch (ex: BrawlStarsApiClientException.NotFound) {
            playerDeactivator.deactivate(brawlStarsTag)
            playerRenewalProcessor.notFound(renewalId)
        } catch (ex: BrawlStarsApiServerException.ServiceUnavailable) {
            log.warn("[플레이어 갱신] ServiceUnavailable tag={}", brawlStarsTag, ex)
            brawlStarsApiMaintenanceAutoRegistrar.register()
            playerRenewalProcessor.fail(renewalId)
        } catch (ex: Exception) {
            playerRenewalProcessor.fail(renewalId)
            throw ex
        }
    }
}
