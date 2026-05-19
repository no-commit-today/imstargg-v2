package com.imstargg.core.application.brawlstars

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.event.BattleEvent
import com.imstargg.core.domain.brawlstars.event.BattleEventMode
import com.imstargg.core.domain.brawlstars.event.RotationBattleEvent
import com.imstargg.core.enums.SoloRankSeason
import com.imstargg.core.error.CoreException
import com.imstargg.core.error.ErrorType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
@Transactional(readOnly = true)
class BattleEventService(
    private val battleEventRepository: BattleEventRepository,
    private val battleEventRepositoryWithCache: BattleEventRepositoryWithCache,
    private val clock: Clock,
) {
    fun get(id: BrawlStarsId): BattleEvent =
        battleEventRepositoryWithCache.find(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "id=$id")

    fun getCurrentRotationEvents(): List<RotationBattleEvent> = battleEventRepositoryWithCache.findAllCurrentRotationEvents()

    fun getSoloRankEvents(): List<BattleEvent> {
        val season = SoloRankSeason.current(clock)
        return battleEventRepositoryWithCache
            .findAllSoloRankEventBrawlStarsIds(season.key)
            .mapNotNull { battleEventRepositoryWithCache.find(it) }
    }

    fun search(name: String): List<BattleEvent> = battleEventRepository.searchByMapName(name)

    /**
     * 여러 이벤트 ID 에 대해 한 번에 mode 정보를 조회한다.
     *
     * 결과형 통계 픽률 계산에서 event 단위 slotsPerGame 환산에 사용된다.
     * 결과 Map은 mode 정보가 존재하는 이벤트만 포함한다 (mode 미연결 이벤트는 제외).
     */
    fun findModesByEventIds(eventIds: Collection<BrawlStarsId>): Map<BrawlStarsId, BattleEventMode> =
        eventIds.toSet()
            .mapNotNull { id -> battleEventRepositoryWithCache.find(id)?.mode?.let { id to it } }
            .toMap()
}
