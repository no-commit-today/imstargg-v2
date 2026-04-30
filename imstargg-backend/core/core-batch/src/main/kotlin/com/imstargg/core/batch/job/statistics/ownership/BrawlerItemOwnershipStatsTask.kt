package com.imstargg.core.batch.job.statistics.ownership

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class BrawlerItemOwnershipStatsTask(
    private val reader: BrawlerItemOwnershipStatsReader,
    private val writer: BrawlerItemOwnershipStatsWriter,
) {
    fun rebuild() {
        log.info("[{}] 시작", JOB_NAME)

        val accumulator = BrawlerItemOwnershipStatsAccumulator()
        var totalSnapshots = 0L
        val totalRead =
            reader.read { snapshots ->
                snapshots.forEach { accumulator.add(it) }
                totalSnapshots += snapshots.size
                log.debug("[{}] 읽기 진행 - 누적 브롤러: {}건", JOB_NAME, totalSnapshots)
            }

        val result = accumulator.toResult()
        writer.write(result)

        log.info(
            "[{}] 완료 - 플레이어: {}건, 집계: {}건 (기어: {}, 가젯: {}, 스타파워: {}, 하이퍼차지: {}, 버피: {})",
            JOB_NAME,
            totalRead,
            result.totalEntityCount,
            result.gears.size,
            result.gadgets.size,
            result.starPowers.size,
            result.hyperCharges.size,
            result.buffies.size,
        )
    }

    companion object {
        private const val JOB_NAME = "brawlerItemOwnershipStatsJob"
        private val log = LoggerFactory.getLogger(BrawlerItemOwnershipStatsTask::class.java)
    }
}
