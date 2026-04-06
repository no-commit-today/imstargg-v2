package com.imstargg.core.worker.application

import com.imstargg.client.brawlstars.BrawlStarsApiClientException
import com.imstargg.client.brawlstars.BrawlStarsApiServerException
import com.imstargg.client.brawlstars.BrawlStarsClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ClubRenewalService(
    private val brawlStarsClient: BrawlStarsClient,
    private val clubRenewalFinder: ClubRenewalFinder,
    private val clubRenewalProcessor: ClubRenewalProcessor,
    private val clubUpdater: ClubUpdater,
    private val brawlStarsApiMaintenanceChecker: BrawlStarsApiMaintenanceChecker,
    private val brawlStarsApiMaintenanceAutoRegistrar: BrawlStarsApiMaintenanceAutoRegistrar,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun renew(brawlStarsTag: String) {
        var renewal = clubRenewalFinder.findByTag(brawlStarsTag)

        if (brawlStarsApiMaintenanceChecker.isUnderMaintenance()) {
            clubRenewalProcessor.fail(renewal)
            log.info("[클럽 갱신] API 점검 중 tag={}", brawlStarsTag)
            return
        }

        renewal = clubRenewalProcessor.executing(renewal)

        try {
            val clubResponse = brawlStarsClient.getClub(brawlStarsTag)
            clubUpdater.update(clubResponse)
            clubRenewalProcessor.complete(renewal)
        } catch (ex: BrawlStarsApiClientException.NotFound) {
            clubRenewalProcessor.notFound(renewal)
        } catch (ex: BrawlStarsApiServerException.ServiceUnavailable) {
            log.warn("[클럽 갱신] ServiceUnavailable tag={}", brawlStarsTag, ex)
            brawlStarsApiMaintenanceAutoRegistrar.register()
            clubRenewalProcessor.fail(renewal)
        } catch (ex: Exception) {
            clubRenewalProcessor.fail(renewal)
            throw ex
        }
    }
}
