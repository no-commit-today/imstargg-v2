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
 * 브롤스타즈 API 스펙(`GET /v1`)을 수집해 정규화본 기준으로 직전 스냅샷과 비교한다.
 *
 * 응답 YAML 은 요청마다 맵 키 순서가 달라지므로 원문이 아닌 [YamlCanonicalizer] 정규화본을
 * 비교·저장한다 (원문 문자열 비교 시절에는 순서 차이만으로 매 실행 오탐 알림이 발생했다).
 *
 * - 의미가 같으면 저장/알림 없이 로그만 남긴다. 단 직전 스냅샷이 정규화 도입 전
 *   원문 형태면 알림 없이 정규화본으로 1회 이관 저장한다 (이후 어드민 diff 가
 *   항상 정규화본끼리 비교되도록).
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
        val canonical = YamlCanonicalizer.canonicalize(content)
        val previous = brawlStarsApiSpecJpaRepository.findTopByOrderByCollectedAtDesc()

        if (previous == null) {
            saveSnapshot(canonical)
            log.info("[{}] 완료 - 최초 스냅샷 저장", JOB_NAME)
            return
        }

        if (YamlCanonicalizer.canonicalize(previous.content) == canonical) {
            if (previous.content != canonical) {
                saveSnapshot(canonical)
                log.info("[{}] 완료 - 스펙 변경 없음, 레거시 원문 스냅샷을 정규화본으로 이관", JOB_NAME)
                return
            }
            log.info("[{}] 완료 - 스펙 변경 없음", JOB_NAME)
            return
        }

        saveSnapshot(canonical)
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

    private fun saveSnapshot(content: String) {
        brawlStarsApiSpecJpaRepository.save(
            BrawlStarsApiSpecEntity(content = content, collectedAt = clock.instant()),
        )
    }

    companion object {
        private const val JOB_NAME = "brawlStarsApiSpecTrackJob"
        private val log = LoggerFactory.getLogger(BrawlStarsApiSpecTrackTask::class.java)
    }
}
