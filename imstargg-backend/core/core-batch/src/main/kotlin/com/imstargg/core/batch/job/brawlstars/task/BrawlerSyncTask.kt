package com.imstargg.core.batch.job.brawlstars.task

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.client.brawlstars.response.BrawlerResponse
import com.imstargg.core.batch.job.brawlstars.BrawlerChange
import com.imstargg.core.batch.job.brawlstars.BrawlerChangeDetector
import com.imstargg.core.batch.job.brawlstars.BrawlerChangeItemType
import com.imstargg.core.batch.job.brawlstars.BrawlerChangeType
import com.imstargg.core.batch.support.alert.AlertCommand
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.BrawlerGearJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.BrawlerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.GadgetJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.HyperChargeJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.StarPowerJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

@Component
class BrawlerSyncTask(
    private val brawlStarsClient: BrawlStarsClient,
    private val brawlerJpaRepository: BrawlerJpaRepository,
    private val starPowerJpaRepository: StarPowerJpaRepository,
    private val gadgetJpaRepository: GadgetJpaRepository,
    private val hyperChargeJpaRepository: HyperChargeJpaRepository,
    private val brawlerGearJpaRepository: BrawlerGearJpaRepository,
    private val alertManager: AlertManager,
    private val changeDetector: BrawlerChangeDetector,
) {
    fun sync() {
        log.info("[{}] 시작", JOB_NAME)
        val brawlers = brawlStarsClient.getBrawlers().items
        val existingBrawlerEntities = brawlerJpaRepository.findAll().toList()
        val existingIds = existingBrawlerEntities.map { it.brawlStarsId }.toSet()
        val apiBrawlerIds = brawlers.map { it.id }.toSet()

        val newBrawlers = brawlers.filter { it.id !in existingIds }
        alertNewBrawlers(newBrawlers)

        val deletedBrawlerIds = existingIds - apiBrawlerIds
        alertDeletedBrawlers(deletedBrawlerIds)

        val existingBrawlers = brawlers.filter { it.id in existingIds }
        val changes = detectBrawlerChanges(existingBrawlers)
        alertBrawlerChanges(changes)
        log.info(
            "[{}] 완료 - 신규: {}건, 삭제: {}건, 변경: {}건",
            JOB_NAME,
            newBrawlers.size,
            deletedBrawlerIds.size,
            changes.size,
        )
    }

    private fun alertNewBrawlers(newBrawlers: List<BrawlerResponse>) {
        if (newBrawlers.isEmpty()) return
        val jsonMapper = JsonMapper.builder().build()
        val content = newBrawlers.joinToString("\n") { jsonMapper.writeValueAsString(it) }
        alertManager.alert(
            AlertCommand
                .builder()
                .title("[$JOB_NAME] 새로운 브롤러 발견")
                .content(content)
                .build(),
        )
    }

    private fun alertDeletedBrawlers(deletedBrawlerIds: Set<Long>) {
        if (deletedBrawlerIds.isEmpty()) return
        val content = deletedBrawlerIds.joinToString("\n") { "brawlStarsId=$it" }
        alertManager.alert(
            AlertCommand
                .builder()
                .title("[$JOB_NAME] 브롤러 삭제 감지")
                .content(content)
                .build(),
        )
    }

    private fun detectBrawlerChanges(existingBrawlers: List<BrawlerResponse>): List<BrawlerChange> {
        val jsonMapper = JsonMapper.builder().build()
        val changes = mutableListOf<BrawlerChange>()

        for (brawler in existingBrawlers) {
            changes +=
                detectFor(
                    brawler = brawler,
                    jsonMapper = jsonMapper,
                    itemType = BrawlerChangeItemType.STAR_POWER,
                    apiItems = brawler.starPowers,
                    apiItemId = { it.id },
                    dbItemIds =
                        starPowerJpaRepository
                            .findAllByBrawlerBrawlStarsId(brawler.id)
                            .map { it.brawlStarsId }
                            .toSet(),
                )

            changes +=
                detectFor(
                    brawler = brawler,
                    jsonMapper = jsonMapper,
                    itemType = BrawlerChangeItemType.GADGET,
                    apiItems = brawler.gadgets,
                    apiItemId = { it.id },
                    dbItemIds =
                        gadgetJpaRepository
                            .findAllByBrawlerBrawlStarsId(brawler.id)
                            .map { it.brawlStarsId }
                            .toSet(),
                )

            changes +=
                detectFor(
                    brawler = brawler,
                    jsonMapper = jsonMapper,
                    itemType = BrawlerChangeItemType.HYPER_CHARGE,
                    apiItems = brawler.hyperCharges,
                    apiItemId = { it.id },
                    dbItemIds =
                        hyperChargeJpaRepository
                            .findAllByBrawlerBrawlStarsId(brawler.id)
                            .map { it.brawlStarsId }
                            .toSet(),
                )

            changes +=
                detectFor(
                    brawler = brawler,
                    jsonMapper = jsonMapper,
                    itemType = BrawlerChangeItemType.GEAR,
                    apiItems = brawler.gears,
                    apiItemId = { it.id },
                    dbItemIds =
                        brawlerGearJpaRepository
                            .findAllByBrawlerBrawlStarsId(brawler.id)
                            .map { it.gearBrawlStarsId }
                            .toSet(),
                )
        }

        return changes
    }

    private fun <T> detectFor(
        brawler: BrawlerResponse,
        jsonMapper: JsonMapper,
        itemType: BrawlerChangeItemType,
        apiItems: List<T>,
        apiItemId: (T) -> Long,
        dbItemIds: Set<Long>,
    ): List<BrawlerChange> =
        changeDetector.detectItemChanges(
            brawlerId = brawler.id,
            brawlerName = brawler.name,
            itemType = itemType,
            apiItemIds = apiItems.map(apiItemId).toSet(),
            dbItemIds = dbItemIds,
            toJson = { id -> jsonMapper.writeValueAsString(apiItems.first { apiItemId(it) == id }) },
        )

    private fun alertBrawlerChanges(changes: List<BrawlerChange>) {
        if (changes.isEmpty()) return
        val content =
            changes.joinToString("\n") { change ->
                val typeLabel =
                    when (change.itemType) {
                        BrawlerChangeItemType.STAR_POWER -> "스타파워"
                        BrawlerChangeItemType.GADGET -> "가젯"
                        BrawlerChangeItemType.HYPER_CHARGE -> "하이퍼차지"
                        BrawlerChangeItemType.GEAR -> "기어"
                    }
                val changeLabel =
                    when (change.changeType) {
                        BrawlerChangeType.ADDED -> "추가"
                        BrawlerChangeType.REMOVED -> "제거"
                    }
                val jsonPart = if (change.itemJson.isNotEmpty()) " ${change.itemJson}" else ""
                "${change.brawlerName}(${change.brawlerId}) $typeLabel $changeLabel$jsonPart"
            }
        alertManager.alert(
            AlertCommand
                .builder()
                .title("[$JOB_NAME] 브롤러 변경 감지")
                .content(content)
                .build(),
        )
    }

    companion object {
        private const val JOB_NAME = "brawlerSyncJob"
        private val log = LoggerFactory.getLogger(BrawlerSyncTask::class.java)
    }
}
