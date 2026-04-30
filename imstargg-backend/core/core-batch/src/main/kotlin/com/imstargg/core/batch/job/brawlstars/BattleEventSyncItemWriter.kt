package com.imstargg.core.batch.job.brawlstars

import com.imstargg.core.batch.support.alert.AlertCommand
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.core.enums.BattleType
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventSoloRankEventEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventSoloRankEventJpaRepository
import com.imstargg.infrastructure.db.core.statistics.battle.BaseStatsBattleEntity
import org.slf4j.LoggerFactory
import org.springframework.batch.infrastructure.item.Chunk
import org.springframework.batch.infrastructure.item.ItemWriter

open class BattleEventSyncItemWriter<T : BaseStatsBattleEntity>(
    private val alertManager: AlertManager,
    private val battleEventJpaRepository: BattleEventJpaRepository,
    private val battleEventSoloRankEventJpaRepository: BattleEventSoloRankEventJpaRepository,
) : ItemWriter<T> {
    private val log = LoggerFactory.getLogger(BattleEventSyncItemWriter::class.java)
    private val checkedEventIds: MutableSet<Long> = mutableSetOf()
    private val soloRankSeasonCache = SoloRankSeasonCache()
    private val existingSoloRankEventIds: MutableMap<String, MutableSet<Long>> = mutableMapOf()

    override fun write(chunk: Chunk<out T>) {
        detectEventChanges(chunk)
        createSoloRankEvents(chunk)
    }

    private fun detectEventChanges(chunk: Chunk<out BaseStatsBattleEntity>) {
        val uncheckedIds =
            chunk.items
                .map { it.eventBrawlStarsId }
                .distinct()
                .filter { it !in checkedEventIds }

        if (uncheckedIds.isEmpty()) return

        val events =
            battleEventJpaRepository
                .findAllByBrawlStarsIdIn(uncheckedIds)
                .associateBy { it.brawlStarsId }

        for (battle in chunk.items) {
            if (battle.eventBrawlStarsId in checkedEventIds) continue

            val event = events[battle.eventBrawlStarsId]
            if (event == null) {
                alertUnregisteredEvent(battle)
            } else if (!matches(battle, event)) {
                alertMismatch(battle, event)
            }
        }

        checkedEventIds.addAll(uncheckedIds)
    }

    private fun matches(
        battle: BaseStatsBattleEntity,
        event: BattleEventEntity,
    ): Boolean {
        val mode = event.mode
        if (mode != null && mode.brawlStarsId != battle.eventModeBrawlStarsId) return false
        if (event.mapName != null && event.mapName != battle.eventMap) return false
        return true
    }

    private fun alertUnregisteredEvent(battle: BaseStatsBattleEntity) {
        log.warn(
            "[{}] 미등록 배틀 이벤트 eventBrawlStarsId={}, modeBrawlStarsId={}, map={}",
            BattleEventSyncJobConfig.JOB_NAME,
            battle.eventBrawlStarsId,
            battle.eventModeBrawlStarsId,
            battle.eventMap,
        )
        alertManager.alert(
            AlertCommand
                .builder()
                .warn()
                .title("[${BattleEventSyncJobConfig.JOB_NAME}] 미등록 배틀 이벤트 감지")
                .content(
                    """
                    eventBrawlStarsId=${battle.eventBrawlStarsId},
                    modeBrawlStarsId=${battle.eventModeBrawlStarsId},
                    map=${battle.eventMap}
                    """.trimIndent(),
                ).build(),
        )
    }

    private fun alertMismatch(
        battle: BaseStatsBattleEntity,
        event: BattleEventEntity,
    ) {
        log.warn("[{}] 배틀 이벤트 불일치 eventBrawlStarsId={}", BattleEventSyncJobConfig.JOB_NAME, event.brawlStarsId)
        alertManager.alert(
            AlertCommand
                .builder()
                .warn()
                .title("[${BattleEventSyncJobConfig.JOB_NAME}] 배틀 이벤트 불일치")
                .content(
                    """
                    등록된 데이터
                    - eventBrawlStarsId=${event.brawlStarsId}
                    - modeBrawlStarsId=${event.mode?.brawlStarsId}
                    - map=${event.mapName}

                    실제 데이터
                    - eventBrawlStarsId=${battle.eventBrawlStarsId}
                    - modeBrawlStarsId=${battle.eventModeBrawlStarsId}
                    - map=${battle.eventMap}
                    """.trimIndent(),
                ).build(),
        )
    }

    private fun createSoloRankEvents(chunk: Chunk<out BaseStatsBattleEntity>) {
        val soloRankBattles = chunk.items.filter { it.type == BattleType.SOLO_RANKED.code }

        for ((seasonKey, battles) in soloRankBattles.groupBy { soloRankSeasonCache.get(it.battleAt).key }) {
            val existingIds =
                existingSoloRankEventIds.getOrPut(seasonKey) {
                    battleEventSoloRankEventJpaRepository
                        .findAllBySoloRankSeasonKey(seasonKey)
                        .map { it.eventBrawlStarsId }
                        .toMutableSet()
                }

            val newEventIds =
                battles
                    .map { it.eventBrawlStarsId }
                    .distinct()
                    .filter { it !in existingIds }

            if (newEventIds.isEmpty()) continue

            val entities =
                newEventIds.map { eventBrawlStarsId ->
                    BattleEventSoloRankEventEntity(
                        soloRankSeasonKey = seasonKey,
                        eventBrawlStarsId = eventBrawlStarsId,
                    )
                }

            battleEventSoloRankEventJpaRepository.saveAll(entities)
            existingIds.addAll(newEventIds)
        }
    }
}
