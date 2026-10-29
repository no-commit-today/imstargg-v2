package com.imstargg.core.batch.job.brawlstars.task

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.client.brawlstars.response.BrawlerResponse
import com.imstargg.core.batch.domain.brawler.BrawlerChange
import com.imstargg.core.batch.domain.brawler.BrawlerChangeDetector
import com.imstargg.core.batch.domain.brawler.BrawlerItemRef
import com.imstargg.core.batch.domain.brawler.BrawlerItemType
import com.imstargg.core.batch.domain.brawler.BrawlerSnapshot
import com.imstargg.core.batch.domain.brawler.StoredBrawlers
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

/**
 * 브롤스타즈 API 와 DB 를 비교해 브롤러/하위 항목의 생성·삭제를 감지하고 알림만 발송한다.
 * DB 반영은 하지 않는다 — 브롤러 등록은 core-admin 에서 관리자가 수동으로 수행한다(이미지·다국어 이름 등 수작업 필요).
 */
@Component
class BrawlerSyncTask(
    private val brawlStarsClient: BrawlStarsClient,
    private val brawlerJpaRepository: BrawlerJpaRepository,
    private val starPowerJpaRepository: StarPowerJpaRepository,
    private val gadgetJpaRepository: GadgetJpaRepository,
    private val hyperChargeJpaRepository: HyperChargeJpaRepository,
    private val brawlerGearJpaRepository: BrawlerGearJpaRepository,
    private val alertManager: AlertManager,
) {
    private val changeDetector = BrawlerChangeDetector()

    fun sync() {
        log.info("[{}] 시작", JOB_NAME)
        val apiBrawlers = brawlStarsClient.getBrawlers().items
        val changes =
            changeDetector.detect(
                apiBrawlers = apiBrawlers.map { it.toSnapshot() },
                stored = loadStoredBrawlers(),
            )

        val addedBrawlers = changes.filterIsInstance<BrawlerChange.BrawlerAdded>()
        val removedBrawlers = changes.filterIsInstance<BrawlerChange.BrawlerRemoved>()
        val itemChanges = changes - addedBrawlers.toSet() - removedBrawlers.toSet()

        alertAddedBrawlers(addedBrawlers, apiBrawlers.associateBy { it.id })
        alertRemovedBrawlers(removedBrawlers)
        alertItemChanges(itemChanges)
        log.info(
            "[{}] 완료 - 신규: {}건, 삭제: {}건, 항목 변경: {}건",
            JOB_NAME,
            addedBrawlers.size,
            removedBrawlers.size,
            itemChanges.size,
        )
    }

    private fun BrawlerResponse.toSnapshot(): BrawlerSnapshot =
        BrawlerSnapshot(
            id = id,
            name = name,
            items =
                mapOf(
                    BrawlerItemType.STAR_POWER to starPowers.map { BrawlerItemRef(id = it.id, name = it.name) }.toSet(),
                    BrawlerItemType.GADGET to gadgets.map { BrawlerItemRef(id = it.id, name = it.name) }.toSet(),
                    BrawlerItemType.HYPER_CHARGE to hyperCharges.map { BrawlerItemRef(id = it.id, name = it.name) }.toSet(),
                    BrawlerItemType.GEAR to gears.map { BrawlerItemRef(id = it.id, name = it.name) }.toSet(),
                ),
        )

    /**
     * 삭제(soft-delete)된 행까지 모두 읽어 활성/삭제로 나눈다 — 삭제된 항목도 "이미 알고 있는 항목"이어야
     * API 가 계속 응답하는 제거된 항목을 신규로 오인하지 않는다.
     */
    private fun loadStoredBrawlers(): StoredBrawlers {
        val (activeBrawlers, deletedBrawlers) = brawlerJpaRepository.findAll().partition { !it.isDeleted }
        val (activeStarPowers, deletedStarPowers) = starPowerJpaRepository.findAll().partition { !it.isDeleted }
        val (activeGadgets, deletedGadgets) = gadgetJpaRepository.findAll().partition { !it.isDeleted }
        val (activeHyperCharges, deletedHyperCharges) = hyperChargeJpaRepository.findAll().partition { !it.isDeleted }
        val (activeGears, deletedGears) = brawlerGearJpaRepository.findAll().partition { !it.isDeleted }

        return StoredBrawlers(
            brawlerIds = activeBrawlers.map { it.brawlStarsId }.toSet(),
            itemIds =
                toItemIds(
                    mapOf(
                        BrawlerItemType.STAR_POWER to activeStarPowers.groupBy({ it.brawlerBrawlStarsId }) { it.brawlStarsId },
                        BrawlerItemType.GADGET to activeGadgets.groupBy({ it.brawlerBrawlStarsId }) { it.brawlStarsId },
                        BrawlerItemType.HYPER_CHARGE to activeHyperCharges.groupBy({ it.brawlerBrawlStarsId }) { it.brawlStarsId },
                        BrawlerItemType.GEAR to activeGears.groupBy({ it.brawlerBrawlStarsId }) { it.gearBrawlStarsId },
                    ),
                ),
            deletedBrawlerIds = deletedBrawlers.map { it.brawlStarsId }.toSet(),
            deletedItemIds =
                toItemIds(
                    mapOf(
                        BrawlerItemType.STAR_POWER to deletedStarPowers.groupBy({ it.brawlerBrawlStarsId }) { it.brawlStarsId },
                        BrawlerItemType.GADGET to deletedGadgets.groupBy({ it.brawlerBrawlStarsId }) { it.brawlStarsId },
                        BrawlerItemType.HYPER_CHARGE to deletedHyperCharges.groupBy({ it.brawlerBrawlStarsId }) { it.brawlStarsId },
                        BrawlerItemType.GEAR to deletedGears.groupBy({ it.brawlerBrawlStarsId }) { it.gearBrawlStarsId },
                    ),
                ),
        )
    }

    private fun toItemIds(itemIdsByType: Map<BrawlerItemType, Map<Long, List<Long>>>): Map<Long, Map<BrawlerItemType, Set<Long>>> =
        itemIdsByType
            .flatMap { (itemType, idsByBrawler) ->
                idsByBrawler.map { (brawlerId, ids) -> Triple(brawlerId, itemType, ids.toSet()) }
            }.groupBy({ it.first }) { it.second to it.third }
            .mapValues { (_, typeToIds) -> typeToIds.toMap() }

    private fun alertAddedBrawlers(
        addedBrawlers: List<BrawlerChange.BrawlerAdded>,
        apiBrawlersById: Map<Long, BrawlerResponse>,
    ) {
        if (addedBrawlers.isEmpty()) return
        val content =
            addedBrawlers.joinToString("\n") { added ->
                jsonMapper.writeValueAsString(apiBrawlersById.getValue(added.brawlerId))
            }
        alertManager.alert(
            AlertCommand
                .builder()
                .title("[$JOB_NAME] 새로운 브롤러 발견")
                .content(content)
                .build(),
        )
    }

    private fun alertRemovedBrawlers(removedBrawlers: List<BrawlerChange.BrawlerRemoved>) {
        if (removedBrawlers.isEmpty()) return
        val content = removedBrawlers.joinToString("\n") { "brawlStarsId=${it.brawlerId}" }
        alertManager.alert(
            AlertCommand
                .builder()
                .title("[$JOB_NAME] 브롤러 삭제 감지")
                .content(content)
                .build(),
        )
    }

    private fun alertItemChanges(itemChanges: List<BrawlerChange>) {
        if (itemChanges.isEmpty()) return
        val content =
            itemChanges.joinToString("\n") { change ->
                when (change) {
                    is BrawlerChange.ItemAdded ->
                        "${change.brawlerName}(${change.brawlerId}) ${change.itemType.label} 추가 " +
                            "${change.itemName}(brawlStarsId=${change.itemId})"
                    is BrawlerChange.ItemRemoved ->
                        "${change.brawlerName}(${change.brawlerId}) ${change.itemType.label} 제거 " +
                            "brawlStarsId=${change.itemId}"
                    is BrawlerChange.BrawlerAdded, is BrawlerChange.BrawlerRemoved ->
                        error("브롤러 단위 변경은 항목 변경 알림 대상이 아닙니다: $change")
                }
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
        private val jsonMapper = JsonMapper.builder().build()
    }
}
