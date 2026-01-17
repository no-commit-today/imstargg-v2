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
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun renew(brawlStarsTag: String) {
        playerRenewalProcessor.process(brawlStarsTag)

        try {
            val playerResponse = brawlStarsClient.getPlayerInformation(brawlStarsTag)
            val battleResponses = brawlStarsClient.getPlayerRecentBattles(brawlStarsTag).items

            playerUpdater.update(playerResponse, battleResponses)

            playerRenewalProcessor.complete(brawlStarsTag)
        } catch (ex: BrawlStarsApiClientException.NotFound) {
            playerRenewalProcessor.notFound(brawlStarsTag)
        } catch (ex: BrawlStarsApiServerException.ServiceUnavailable) {
            log.warn("[플레이어 갱신] ServiceUnavailable tag={}", brawlStarsTag, ex)
            playerRenewalProcessor.fail(brawlStarsTag)
        } catch (ex: Exception) {
            playerRenewalProcessor.fail(brawlStarsTag)
            throw ex
        }
    }
}
