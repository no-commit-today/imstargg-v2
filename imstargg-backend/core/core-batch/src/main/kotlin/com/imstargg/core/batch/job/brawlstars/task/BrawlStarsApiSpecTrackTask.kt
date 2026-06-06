package com.imstargg.core.batch.job.brawlstars.task

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.core.batch.support.alert.AlertCommand
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.infrastructure.db.core.brawlstars.system.BrawlStarsApiSpecEntity
import com.imstargg.infrastructure.db.core.brawlstars.system.repository.BrawlStarsApiSpecJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Clock

/**
 * 브롤스타즈 API 스펙(`GET /v1`)을 수집해 직전 스냅샷과 비교한다.
 *
 * - 내용이 같으면 저장/알림 없이 로그만 남긴다.
 * - 다르면 새 스냅샷을 저장하고, 최초 수집이 아닌 경우 변경 알림을 보낸다.
 */
@Component
class BrawlStarsApiSpecTrackTask(
    private val clock: Clock,
    private val brawlStarsClient: BrawlStarsClient,
    private val brawlStarsApiSpecJpaRepository: BrawlStarsApiSpecJpaRepository,
    private val alertManager: AlertManager,
) {
    fun track() {
        log.info("[{}] 시작", JOB_NAME)
        val content = brawlStarsClient.getApiSpec()
        check(content.isNotBlank()) { "브롤스타즈 API 스펙 응답이 비어있습니다." }
        val previous = brawlStarsApiSpecJpaRepository.findTopByOrderByCollectedAtDesc()

        if (previous != null && previous.content == content) {
            log.info("[{}] 완료 - 스펙 변경 없음", JOB_NAME)
            return
        }

        brawlStarsApiSpecJpaRepository.save(
            BrawlStarsApiSpecEntity(content = content, collectedAt = clock.instant()),
        )

        if (previous == null) {
            log.info("[{}] 완료 - 최초 스냅샷 저장", JOB_NAME)
            return
        }

        log.info("[{}] 완료 - 스펙 변경 감지, 새 스냅샷 저장 및 알림 발송", JOB_NAME)
        alertManager.alert(
            AlertCommand
                .builder()
                .warn()
                .title("[BrawlStarsApiSpecTrackJob] 브롤스타즈 API 스펙 변경 감지")
                .content("새 스냅샷을 저장했습니다. 어드민 API 스펙 추적 페이지에서 diff 를 확인하세요.")
                .build(),
        )
    }

    companion object {
        private const val JOB_NAME = "brawlStarsApiSpecTrackJob"
        private val log = LoggerFactory.getLogger(BrawlStarsApiSpecTrackTask::class.java)
    }
}
