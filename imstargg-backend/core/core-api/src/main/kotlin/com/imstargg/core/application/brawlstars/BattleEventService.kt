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

    /**
     * 경쟁전 이벤트는 시즌마다 맵 풀이 달라지므로 시즌 단위로 조회한다.
     * [seasonKey] 를 주지 않으면 현재 시즌을 조회한다.
     */
    fun getSoloRankEvents(seasonKey: String? = null): List<BattleEvent> {
        val season = resolveSoloRankSeason(seasonKey)
        return battleEventRepositoryWithCache
            .findAllSoloRankEventBrawlStarsIds(season.key)
            .mapNotNull { battleEventRepositoryWithCache.find(it) }
    }

    /**
     * [SoloRankSeason.ofKey] 대신 [SoloRankSeason.all] 매칭을 쓰는 이유:
     * `ofKey` 는 목표 key 에 도달할 때까지 [SoloRankSeason.next] 를 반복하므로
     * 큰 number 가 담긴 key(예: `"999999999-1"`) 에 사실상 무한 루프가 된다.
     * 첫 시즌~현재 시즌으로 범위가 닫힌 `all` 에서 찾으면 형식 오류·미래 시즌·범위 밖 키가
     * 모두 같은 경로로 즉시 거부된다.
     */
    private fun resolveSoloRankSeason(seasonKey: String?): SoloRankSeason {
        if (seasonKey == null) {
            return SoloRankSeason.current(clock)
        }
        return SoloRankSeason.all(clock).find { it.key == seasonKey }
            ?: throw CoreException(ErrorType.INVALID_REQUEST, "seasonKey=$seasonKey")
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
