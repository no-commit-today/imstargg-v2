package com.imstargg.core.batch.job.brawlstars

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.client.brawlstars.response.BrawlerResponse
import com.imstargg.core.batch.support.alert.AlertCommand
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.BrawlerGearJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.BrawlerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.GadgetJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.HyperChargeJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.StarPowerJpaRepository
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.parameters.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import tools.jackson.databind.json.JsonMapper

@Configuration
class BrawlerSyncJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val brawlStarsClient: BrawlStarsClient,
    private val brawlerJpaRepository: BrawlerJpaRepository,
    private val starPowerJpaRepository: StarPowerJpaRepository,
    private val gadgetJpaRepository: GadgetJpaRepository,
    private val hyperChargeJpaRepository: HyperChargeJpaRepository,
    private val brawlerGearJpaRepository: BrawlerGearJpaRepository,
    private val alertManager: AlertManager,
) {
    companion object {
        const val JOB_NAME = "brawlerSyncJob"
        const val STEP_NAME = "brawlerSyncStep"
    }

    enum class BrawlerChangeType {
        ADDED,
        REMOVED,
    }

    enum class BrawlerChangeItemType {
        STAR_POWER,
        GADGET,
        HYPER_CHARGE,
        GEAR,
    }

    data class BrawlerChange(
        val brawlerId: Long,
        val brawlerName: String,
        val changeType: BrawlerChangeType,
        val itemType: BrawlerChangeItemType,
        val itemJson: String = "",
    )

    @Bean(JOB_NAME)
    fun brawlerSyncJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .start(brawlerSyncStep())
            .incrementer(RunIdIncrementer())
            .build()

    @Bean(STEP_NAME)
    fun brawlerSyncStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet(
                BrawlStarsApiTasklet(JOB_NAME) {
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
                },
                txManager,
            ).build()

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
            detectItemChanges(
                brawler = brawler,
                itemType = BrawlerChangeItemType.STAR_POWER,
                apiItemIds = brawler.starPowers.map { it.id }.toSet(),
                dbItemIds =
                    starPowerJpaRepository
                        .findAllByBrawlerBrawlStarsId(brawler.id)
                        .map { it.brawlStarsId }
                        .toSet(),
                toJson = { id -> jsonMapper.writeValueAsString(brawler.starPowers.first { it.id == id }) },
                changes = changes,
            )

            detectItemChanges(
                brawler = brawler,
                itemType = BrawlerChangeItemType.GADGET,
                apiItemIds = brawler.gadgets.map { it.id }.toSet(),
                dbItemIds =
                    gadgetJpaRepository
                        .findAllByBrawlerBrawlStarsId(brawler.id)
                        .map { it.brawlStarsId }
                        .toSet(),
                toJson = { id -> jsonMapper.writeValueAsString(brawler.gadgets.first { it.id == id }) },
                changes = changes,
            )

            detectItemChanges(
                brawler = brawler,
                itemType = BrawlerChangeItemType.HYPER_CHARGE,
                apiItemIds = brawler.hyperCharges.map { it.id }.toSet(),
                dbItemIds =
                    hyperChargeJpaRepository
                        .findAllByBrawlerBrawlStarsId(brawler.id)
                        .map { it.brawlStarsId }
                        .toSet(),
                toJson = { id -> jsonMapper.writeValueAsString(brawler.hyperCharges.first { it.id == id }) },
                changes = changes,
            )

            detectItemChanges(
                brawler = brawler,
                itemType = BrawlerChangeItemType.GEAR,
                apiItemIds = brawler.gears.map { it.id }.toSet(),
                dbItemIds =
                    brawlerGearJpaRepository
                        .findAllByBrawlerBrawlStarsId(brawler.id)
                        .map { it.gearBrawlStarsId }
                        .toSet(),
                toJson = { id -> jsonMapper.writeValueAsString(brawler.gears.first { it.id == id }) },
                changes = changes,
            )
        }

        return changes
    }

    private fun detectItemChanges(
        brawler: BrawlerResponse,
        itemType: BrawlerChangeItemType,
        apiItemIds: Set<Long>,
        dbItemIds: Set<Long>,
        toJson: (Long) -> String,
        changes: MutableList<BrawlerChange>,
    ) {
        val addedIds = apiItemIds - dbItemIds
        val removedIds = dbItemIds - apiItemIds

        addedIds.forEach { id ->
            changes.add(
                BrawlerChange(
                    brawlerId = brawler.id,
                    brawlerName = brawler.name,
                    changeType = BrawlerChangeType.ADDED,
                    itemType = itemType,
                    itemJson = toJson(id),
                ),
            )
        }
        removedIds.forEach { id ->
            changes.add(
                BrawlerChange(
                    brawlerId = brawler.id,
                    brawlerName = brawler.name,
                    changeType = BrawlerChangeType.REMOVED,
                    itemType = itemType,
                    itemJson = "brawlStarsId=$id",
                ),
            )
        }
    }

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
}
