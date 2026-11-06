package com.imstargg.core.batch.job.statistics.ownership

import com.imstargg.core.batch.domain.statistics.ownership.BrawlerItemOwnershipStatsAccumulator
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class BrawlerItemOwnershipStatsTask(
    private val reader: BrawlerItemOwnershipStatsReader,
    private val writer: BrawlerItemOwnershipStatsWriter,
) {
    fun rebuild() {
        log.info { "[$JOB_NAME] 시작" }

        val accumulator = BrawlerItemOwnershipStatsAccumulator()
        var totalSnapshots = 0L
        val totalRead =
            reader.read { snapshots ->
                snapshots.forEach { accumulator.add(it) }
                totalSnapshots += snapshots.size
                log.debug { "[$JOB_NAME] 읽기 진행 - 누적 브롤러: ${totalSnapshots}건" }
            }

        val result = accumulator.toResult()
        writer.write(result)

        log.info {
            "[$JOB_NAME] 완료 - 플레이어: ${totalRead}건, 집계: ${result.totalEntityCount}건 " +
                "(기어: ${result.gears.size}, 가젯: ${result.gadgets.size}, 스타파워: ${result.starPowers.size}, " +
                "하이퍼차지: ${result.hyperCharges.size}, 버피: ${result.buffies.size})"
        }
    }

    companion object {
        private const val JOB_NAME = "brawlerItemOwnershipStatsJob"
    }
}
