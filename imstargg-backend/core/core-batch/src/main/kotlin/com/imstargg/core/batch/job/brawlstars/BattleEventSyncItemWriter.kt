package com.imstargg.core.batch.job.brawlstars

import com.imstargg.core.batch.domain.battle.SoloRankSeasonCache
import com.imstargg.core.batch.support.alert.AlertCommand
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.core.enums.BattleType
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventSoloRankEventEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventSoloRankEventJpaRepository
import com.imstargg.infrastructure.db.core.statistics.battle.BaseStatsBattleEntity
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.batch.infrastructure.item.Chunk
import org.springframework.batch.infrastructure.item.ItemWriter

private val log = KotlinLogging.logger {}

open class BattleEventSyncItemWriter<T : BaseStatsBattleEntity>(
    private val alertManager: AlertManager,
    private val battleEventJpaRepository: BattleEventJpaRepository,
    private val battleEventSoloRankEventJpaRepository: BattleEventSoloRankEventJpaRepository,
) : ItemWriter<T> {
    private val checkedEventIds: MutableSet<Long> = mutableSetOf()
    private val soloRankSeasonCache = SoloRankSeasonCache()
    private val existingSoloRankEventIds: MutableMap<String, MutableSet<Long>> = mutableMapOf()

    override fun write(chunk: Chunk<out T>) {
        detectEventChanges(chunk)
        createSoloRankEvents(chunk)
    }

    /**
     * 이벤트마다 경고는 한 번만 보낸다. 경고한 이벤트는 그 자리에서 확인한 것으로 두어 같은 청크의 뒤 전투에서 다시
     * 경고하지 않는다(예전에는 루프가 끝난 뒤에야 확인 목록에 넣어, 한 청크에 같은 이벤트 전투가 N판이면 경고도 N건이
     * 나갔다). 확인 목록에는 경고한 그 이벤트만 넣는다. 같은 청크의 다른 이벤트는 따로 대조해야 하기 때문이다.
     * 일치한 이벤트는 청크 끝까지 대조를 이어가 뒤 전투의 불일치도 잡는다.
     */
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
                checkedEventIds.add(battle.eventBrawlStarsId)
            } else if (!matches(battle, event)) {
                alertMismatch(battle, event)
                checkedEventIds.add(battle.eventBrawlStarsId)
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
        log.warn {
            "[${BattleEventSyncJobConfig.JOB_NAME}] 미등록 배틀 이벤트 eventBrawlStarsId=${battle.eventBrawlStarsId}, " +
                "modeBrawlStarsId=${battle.eventModeBrawlStarsId}, map=${battle.eventMap}"
        }
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
        log.warn { "[${BattleEventSyncJobConfig.JOB_NAME}] 배틀 이벤트 불일치 eventBrawlStarsId=${event.brawlStarsId}" }
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

    /**
     * 경쟁전 맵 목록은 통계 전투에서만 만든다. 경쟁전에 허용 목록 밖 모드가 들어오면 그 모드의 전투는
     * 통계 전투에 없으므로 그 맵이 목록에서 빠진다.
     */
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
