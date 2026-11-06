package com.imstargg.core.batch.job.brawlstars.task

import com.imstargg.core.batch.job.brawlstars.BattleEventSyncItemWriter
import com.imstargg.core.batch.job.brawlstars.BattleEventSyncJobConfig
import com.imstargg.core.batch.support.alert.AlertCommand
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.core.enums.StatsEventMode
import com.imstargg.infrastructure.db.core.KOREA_ZONE
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventRotationItemEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventRotationItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventRotationJpaRepository
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsRankBattleJpaRepository
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsResultBattleJpaRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import java.time.LocalDate

private val log = KotlinLogging.logger {}

/**
 * 그날 로테이션에 나온 미등록 이벤트를 알린다.
 *
 * - 통계 전투는 허용 목록([StatsEventMode])의 모드만 담는다. 전투 단계(`battleEventSyncFromRankBattleStep`,
 *   `battleEventSyncFromResultBattleStep`)만으로는 미지원 모드의 새 맵을 놓친다. 이벤트를 등록하지 않으면
 *   core-api 가 로테이션에서 그 이벤트를 버리므로, 등록 알림이 끊기면 새 맵이 화면에 나오지 않는다.
 * - 로테이션 동기화 잡(`battleEventRotationSyncJob`, 매시)에 두지 않는다. 미등록 맵마다 하루 24건이 오기 때문이다.
 *   이 단계는 하루에 한 번, 이벤트마다 한 건만 알린다.
 * - 전투 단계와 중복을 피하는 방식: 전투 단계의 `checkedEventIds` 는 `@StepScope` 라이터마다 따로 있어 이 단계가
 *   볼 수 없다. 대신 그날 통계 전투에 나온 이벤트는 전투 단계가 이미 알렸다는 사실을 쓴다. 그런 이벤트는 여기서
 *   뺀다. 이 단계는 전투 단계가 다룬 이벤트를 다시 알리지 않는다. 전투 단계도 스텝마다 이벤트당 한 번만 알리고
 *   ([BattleEventSyncItemWriter]). 또 core-worker 가 전투 모드의 [StatsEventMode.resultType] 으로 두 통계 테이블 중
 *   한쪽만 고르므로 한 이벤트의 전투는 한 테이블에만 들어간다. 그래서 세 단계를 합쳐 그날 미등록 이벤트마다 많아야
 *   한 건이 온다.
 *
 * 스텝이 트랜잭션 없이 돌므로 항목의 LAZY `battleEventRotation` 은 따라가지 않는다. 날짜 조건은 로테이션
 * 조회 쿼리에서 건다.
 */
@Component
class UnregisteredRotationEventAlertTask(
    private val alertManager: AlertManager,
    private val battleEventJpaRepository: BattleEventJpaRepository,
    private val battleEventRotationJpaRepository: BattleEventRotationJpaRepository,
    private val battleEventRotationItemJpaRepository: BattleEventRotationItemJpaRepository,
    private val statsRankBattleJpaRepository: StatsRankBattleJpaRepository,
    private val statsResultBattleJpaRepository: StatsResultBattleJpaRepository,
) {
    /**
     * @param date 로테이션을 수집한 KST 날짜. 이 날 0시 이상, 다음 날 0시 미만에 수집된 로테이션을 본다.
     */
    fun alert(date: LocalDate) {
        log.info { "[$JOB_NAME] 로테이션 기반 미등록 이벤트 감지 시작 date=$date" }
        val rotations =
            battleEventRotationJpaRepository.findAllByCollectedAtGreaterThanEqualAndCollectedAtLessThan(
                start = date.atStartOfDay(KOREA_ZONE).toInstant(),
                end = date.plusDays(1).atStartOfDay(KOREA_ZONE).toInstant(),
            )
        if (rotations.isEmpty()) {
            log.info { "[$JOB_NAME] 로테이션 기반 미등록 이벤트 감지 완료 - 수집된 로테이션 없음 date=$date" }
            return
        }

        // 같은 이벤트는 마지막(가장 나중에 저장된) 항목 하나만 남긴다.
        val latestItems =
            battleEventRotationItemJpaRepository
                .findAllByBattleEventRotationIn(rotations)
                .sortedBy { it.id }
                .associateBy { it.eventBrawlStarsId }

        val registeredIds =
            battleEventJpaRepository
                .findAllByBrawlStarsIdIn(latestItems.keys)
                .map { it.brawlStarsId }
                .toSet()

        val unregisteredItems =
            latestItems.values
                .filter { it.eventBrawlStarsId !in registeredIds }
                .filterNot { appearedInBattle(date, it.eventBrawlStarsId) }

        unregisteredItems.forEach { alertUnregisteredEvent(it) }
        log.info { "[$JOB_NAME] 로테이션 기반 미등록 이벤트 감지 완료 date=$date, 알림=${unregisteredItems.size}건" }
    }

    private fun appearedInBattle(
        date: LocalDate,
        eventBrawlStarsId: Long,
    ): Boolean =
        statsRankBattleJpaRepository.existsByBattleDateAndEventBrawlStarsId(date, eventBrawlStarsId) ||
            statsResultBattleJpaRepository.existsByBattleDateAndEventBrawlStarsId(date, eventBrawlStarsId)

    private fun alertUnregisteredEvent(item: BattleEventRotationItemEntity) {
        log.warn {
            "[$JOB_NAME] 미등록 배틀 이벤트 eventBrawlStarsId=${item.eventBrawlStarsId}, " +
                "modeBrawlStarsId=${item.eventModeBrawlStarsId}, map=${item.eventMapName}"
        }
        alertManager.alert(
            AlertCommand
                .builder()
                .warn()
                .title("[$JOB_NAME] 미등록 배틀 이벤트 감지")
                .content(
                    """
                    eventBrawlStarsId=${item.eventBrawlStarsId},
                    modeBrawlStarsId=${item.eventModeBrawlStarsId},
                    map=${item.eventMapName}
                    """.trimIndent(),
                ).build(),
        )
    }

    companion object {
        private const val JOB_NAME = BattleEventSyncJobConfig.JOB_NAME
    }
}
