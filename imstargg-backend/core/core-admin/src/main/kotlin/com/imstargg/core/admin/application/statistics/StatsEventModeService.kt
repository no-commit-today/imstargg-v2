package com.imstargg.core.admin.application.statistics

import com.imstargg.core.enums.StatsEventModeType
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventModeJpaRepository
import com.imstargg.infrastructure.db.core.statistics.StatsEventModeEntity
import com.imstargg.infrastructure.db.core.statistics.repository.StatsEventModeJpaRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class StatsEventModeService(
    private val statsEventModeJpaRepository: StatsEventModeJpaRepository,
    private val battleEventModeJpaRepository: BattleEventModeJpaRepository,
) {
    /** 통계를 수집하는 모드의 유형을 모드 브롤스타즈 ID 별로 돌려준다. */
    @Transactional(readOnly = true)
    fun findAllTypes(): Map<Long, StatsEventModeType> =
        statsEventModeJpaRepository.findAll().associate { it.eventModeBrawlStarsId to it.type }

    /** 모드의 통계 수집 유형을 지정한다. [type] 이 null 이면 수집하지 않도록 행을 지운다. */
    @Transactional
    fun updateType(
        eventModeBrawlStarsId: Long,
        type: StatsEventModeType?,
    ) {
        if (battleEventModeJpaRepository.findByBrawlStarsId(eventModeBrawlStarsId) == null) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Event mode not found: brawlStarsId=$eventModeBrawlStarsId")
        }
        val existing = statsEventModeJpaRepository.findByEventModeBrawlStarsId(eventModeBrawlStarsId)
        when {
            type == null -> existing?.let { statsEventModeJpaRepository.delete(it) }
            existing != null -> existing.updateType(type)
            else -> statsEventModeJpaRepository.save(StatsEventModeEntity(eventModeBrawlStarsId, type))
        }
    }
}
