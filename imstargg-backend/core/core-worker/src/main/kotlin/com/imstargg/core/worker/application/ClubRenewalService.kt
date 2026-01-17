package com.imstargg.core.worker.application

import com.imstargg.client.brawlstars.BrawlStarsApiClientException
import com.imstargg.client.brawlstars.BrawlStarsClient
import org.springframework.stereotype.Service

@Service
class ClubRenewalService(
    private val brawlStarsClient: BrawlStarsClient,
    private val clubRenewalProcessor: ClubRenewalProcessor,
    private val clubUpdater: ClubUpdater,
) {
    fun renew(brawlStarsTag: String) {
        clubRenewalProcessor.process(brawlStarsTag)

        try {
            val clubResponse = brawlStarsClient.getClub(brawlStarsTag)
            clubUpdater.update(clubResponse)
            clubRenewalProcessor.complete(brawlStarsTag)
        } catch (ex: BrawlStarsApiClientException.NotFound) {
            clubRenewalProcessor.notFound(brawlStarsTag)
        } catch (ex: Exception) {
            clubRenewalProcessor.fail(brawlStarsTag)
            throw ex
        }
    }
}
