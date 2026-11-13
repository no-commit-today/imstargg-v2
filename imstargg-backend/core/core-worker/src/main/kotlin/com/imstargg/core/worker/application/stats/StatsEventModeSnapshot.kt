package com.imstargg.core.worker.application.stats

import com.imstargg.core.enums.StatsEventModeType
import com.imstargg.infrastructure.db.core.statistics.repository.StatsEventModeJpaRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

/**
 * 통계로 수집할 모드(`stats_event_mode`)를 메모리에 들고 있는 스냅샷.
 *
 * - 기동할 때 한 번 읽는다. 읽기에 실패하면 기동이 실패한다.
 * - 이후 [StatsEventModeReloadScheduler][com.imstargg.core.worker.schedule.StatsEventModeReloadScheduler] 가
 *   주기적으로 다시 읽는다. 관리자가 바꾼 모드는 그 주기만큼 늦게 반영된다.
 * - 다시 읽기에 실패하면 예외를 던지고 이전 목록을 그대로 쓴다.
 * - 목록이 비면 아무 전투도 수집하지 않으므로 WARN 을 남긴다.
 */
@Component
class StatsEventModeSnapshot(
    private val statsEventModeJpaRepository: StatsEventModeJpaRepository,
) {
    @Volatile
    private var typesByModeId: Map<Long, StatsEventModeType> = emptyMap()

    @Volatile
    private var loaded = false

    @PostConstruct
    fun init() {
        reload()
    }

    fun find(eventModeBrawlStarsId: Long): StatsEventModeType? = typesByModeId[eventModeBrawlStarsId]

    fun reload() {
        val next =
            statsEventModeJpaRepository
                .findAll()
                .associate { it.eventModeBrawlStarsId to it.type }
        val previous = typesByModeId
        typesByModeId = next

        if (next.isEmpty()) {
            log.warn { "통계 수집 모드가 비어 있다. 아무 전투도 수집하지 않는다" }
        }
        if (!loaded || next != previous) {
            loaded = true
            log.info {
                "통계 수집 모드 ${next.size}개: " +
                    next.entries.sortedBy { it.key }.joinToString(", ") { "${it.key}=${it.value}" }
            }
        }
    }
}
