package com.imstargg.core.application.brawlstars

import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.event.BattleEvent
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

    /**
     * 맵 이름 부분 일치(대소문자 무시) 검색. 모든 언어의 이름 메시지를 대상으로 한다.
     *
     * `map_name` 컬럼이 null 인 이벤트는 이름 메시지가 있어도 [BattleEvent.mapName] 이 비어
     * 검색에 노출되지 않는다 — admin 목록의 mapName null 게이팅과 동일한 의도된 규약.
     */
    fun search(name: String): List<BattleEvent> =
        battleEventRepositoryWithCache
            .findAll()
            .filter { it.mapName.containsIgnoreCase(name) }
}
