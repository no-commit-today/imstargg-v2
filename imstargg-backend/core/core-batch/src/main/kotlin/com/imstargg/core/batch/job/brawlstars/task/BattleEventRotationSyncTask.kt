package com.imstargg.core.batch.job.brawlstars.task

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventRotationEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventRotationItemEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventRotationItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventRotationJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Clock

@Component
class BattleEventRotationSyncTask(
    private val clock: Clock,
    private val brawlStarsClient: BrawlStarsClient,
    private val battleEventRotationJpaRepository: BattleEventRotationJpaRepository,
    private val battleEventRotationItemJpaRepository: BattleEventRotationItemJpaRepository,
) {
    fun sync() {
        log.info("[{}] 시작", JOB_NAME)
        val now = clock.instant()
        val rotations = brawlStarsClient.getEventRotation()

        val rotation =
            battleEventRotationJpaRepository.save(
                BattleEventRotationEntity(collectedAt = now),
            )

        val items =
            rotations.map {
                BattleEventRotationItemEntity(
                    battleEventRotationId = rotation.id!!,
                    eventBrawlStarsId = it.event.id,
                    eventModeName = it.event.mode,
                    eventModeBrawlStarsId = it.event.modeId.toLong(),
                    eventMapName = it.event.map,
                    modifiers = it.event.modifiers ?: emptyList(),
                    slotId = it.slotId,
                    startTime = it.startTime.toInstant(),
                    endTime = it.endTime.toInstant(),
                )
            }
        battleEventRotationItemJpaRepository.saveAll(items)
        log.info("[{}] 완료 - 로테이션 슬롯 {}건 저장", JOB_NAME, items.size)
    }

    companion object {
        private const val JOB_NAME = "battleEventRotationSyncJob"
        private val log = LoggerFactory.getLogger(BattleEventRotationSyncTask::class.java)
    }
}
