package com.imstargg.core.worker.application

import com.imstargg.client.brawlstars.BrawlStarsApiClientException
import com.imstargg.client.brawlstars.BrawlStarsApiServerException
import com.imstargg.client.brawlstars.BrawlStarsClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ClubRenewalService(
    private val brawlStarsClient: BrawlStarsClient,
    private val clubRenewalProcessor: ClubRenewalProcessor,
    private val clubUpdater: ClubUpdater,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun renew(brawlStarsTag: String) {
        clubRenewalProcessor.process(brawlStarsTag)

        try {
            val clubResponse = brawlStarsClient.getClub(brawlStarsTag)
            clubUpdater.update(clubResponse)
            clubRenewalProcessor.complete(brawlStarsTag)
        } catch (ex: BrawlStarsApiClientException.NotFound) {
            clubRenewalProcessor.notFound(brawlStarsTag)
        } catch (ex: BrawlStarsApiServerException.ServiceUnavailable) {
            log.warn("[클럽 갱신] ServiceUnavailable tag={}", brawlStarsTag, ex)
            clubRenewalProcessor.fail(brawlStarsTag)
        } catch (ex: Exception) {
            clubRenewalProcessor.fail(brawlStarsTag)
            throw ex
        }
    }
}
