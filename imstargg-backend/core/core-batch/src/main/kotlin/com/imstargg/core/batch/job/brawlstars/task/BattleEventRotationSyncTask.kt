package com.imstargg.core.batch.job.brawlstars.task

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.core.batch.support.transaction.JpaBatchTransactionRunner
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
    private val transactionRunner: JpaBatchTransactionRunner,
) {
    /**
     * 스텝은 ResourcelessTransactionManager 로 동작(잡 시작~끝 트랜잭션 미사용)하므로, 느린 API
     * 호출은 트랜잭션 밖에 두고 부모(rotation)+자식(items) 저장만 [transactionRunner] 의 짧은
     * 트랜잭션으로 묶어 부분 커밋(고아 부모)을 방지한다.
     */
    fun sync() {
        log.info("[{}] 시작", JOB_NAME)
        val now = clock.instant()
        val rotations = brawlStarsClient.getEventRotation()

        val savedCount =
            transactionRunner.run { _, _ ->
                val rotation =
                    battleEventRotationJpaRepository.save(
                        BattleEventRotationEntity(collectedAt = now),
                    )
                val items =
                    rotations.map {
                        BattleEventRotationItemEntity(
                            battleEventRotation = rotation,
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
                items.size
            }
        log.info("[{}] 완료 - 로테이션 슬롯 {}건 저장", JOB_NAME, savedCount)
    }

    companion object {
        private const val JOB_NAME = "battleEventRotationSyncJob"
        private val log = LoggerFactory.getLogger(BattleEventRotationSyncTask::class.java)
    }
}
