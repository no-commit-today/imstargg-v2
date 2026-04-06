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
    private val brawlStarsApiMaintenanceChecker: BrawlStarsApiMaintenanceChecker,
    private val brawlStarsApiMaintenanceAutoRegistrar: BrawlStarsApiMaintenanceAutoRegistrar,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun renew(brawlStarsTag: String) {
        if (brawlStarsApiMaintenanceChecker.isUnderMaintenance()) {
            clubRenewalProcessor.failByTag(brawlStarsTag)
            log.info("[클럽 갱신] API 점검 중 tag={}", brawlStarsTag)
            return
        }

        val renewalId = clubRenewalProcessor.executing(brawlStarsTag)

        try {
            val clubResponse = brawlStarsClient.getClub(brawlStarsTag)
            clubUpdater.update(clubResponse)
            clubRenewalProcessor.complete(renewalId)
        } catch (ex: BrawlStarsApiClientException.NotFound) {
            clubRenewalProcessor.notFound(renewalId)
        } catch (ex: BrawlStarsApiServerException.ServiceUnavailable) {
            log.warn("[클럽 갱신] ServiceUnavailable tag={}", brawlStarsTag, ex)
            brawlStarsApiMaintenanceAutoRegistrar.register()
            clubRenewalProcessor.fail(renewalId)
        } catch (ex: Exception) {
            clubRenewalProcessor.fail(renewalId)
            throw ex
        }
    }
}
