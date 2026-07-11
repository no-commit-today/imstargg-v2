package com.imstargg.core.batch.job.renewal.task

import com.imstargg.core.batch.support.coreapi.CoreApiClient
import com.imstargg.core.batch.support.coreapi.PlayerRenewalResult
import com.imstargg.core.enums.Country
import com.imstargg.core.enums.RenewalStatus
import com.imstargg.core.enums.RenewalType
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.PlayerRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.PlayerRankingJpaRepository
import com.imstargg.infrastructure.db.core.renewal.repository.RenewalJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration

/**
 * 랭킹에 노출되는 플레이어를 사전 갱신한다 — 랭킹 페이지에서 링크를 타고 들어간
 * 플레이어 페이지가 빈 화면("갱신 필요")이 되지 않도록 (AdSense 저가치 콘텐츠 대응).
 *
 * batch 는 renewal 파이프라인을 직접 조작하지 않고 core-api 를 호출한다.
 * 24시간 이내 COMPLETED 인 태그만 배치에서 제외하고, 나머지 세부 판정
 * (쿨다운·삭제·중복)은 core-api 상태 머신에 위임한다 — COMPLETED renewal 존재가
 * 곧 플레이어 데이터 존재이므로 players 테이블 조회 없이 renewal 테이블만으로 판정한다.
 */
@Component
class PlayerRankingRenewalTask(
    private val clock: Clock,
    private val playerRankingJpaRepository: PlayerRankingJpaRepository,
    private val playerRankingItemJpaRepository: PlayerRankingItemJpaRepository,
    private val renewalJpaRepository: RenewalJpaRepository,
    private val coreApiClient: CoreApiClient,
    @Value("\${app.player-ranking-renewal.request-interval-millis:1000}")
    private val requestIntervalMillis: Long,
) {
    fun renewAll() {
        val tags = collectRankedTags()
        val targets = filterFreshlyRenewed(tags)
        log.info("[{}] 랭킹 태그 {}건 중 갱신 대상 {}건", STEP_NAME, tags.size, targets.size)

        var accepted = 0
        var skipped = 0
        var failed = 0
        for ((index, tag) in targets.withIndex()) {
            when (coreApiClient.renewPlayer(tag)) {
                PlayerRenewalResult.ACCEPTED -> accepted++
                PlayerRenewalResult.ALREADY_IN_PROGRESS, PlayerRenewalResult.PLAYER_DELETED -> skipped++
                PlayerRenewalResult.API_UNDER_MAINTENANCE -> {
                    log.warn("[{}] 브롤스타즈 API 점검 중 - 남은 {}건 중단", STEP_NAME, targets.size - index)
                    break
                }
                PlayerRenewalResult.FAILED -> failed++
            }
            if (index < targets.lastIndex && requestIntervalMillis > 0) {
                Thread.sleep(requestIntervalMillis)
            }
        }
        log.info("[{}] 완료 - 접수: {}건, 스킵: {}건, 실패: {}건", STEP_NAME, accepted, skipped, failed)
    }

    private fun collectRankedTags(): Set<String> =
        Country.entries
            .mapNotNull { playerRankingJpaRepository.findTopByCountryOrderByCollectedAtDesc(it) }
            .flatMap { playerRankingItemJpaRepository.findAllByPlayerRanking(it) }
            .mapTo(LinkedHashSet()) { it.player.brawlStarsTag }

    private fun filterFreshlyRenewed(tags: Set<String>): List<String> {
        val threshold = clock.instant().minus(FRESH_DURATION)
        val fresh =
            tags
                .chunked(CHUNK_SIZE)
                .flatMap { chunk ->
                    renewalJpaRepository.findAllByTypeAndBrawlStarsTagIn(
                        type = RenewalType.PLAYER,
                        brawlStarsTags = chunk,
                    )
                }.filter { it.status == RenewalStatus.COMPLETED && it.completedAt?.isAfter(threshold) == true }
                .mapTo(HashSet()) { it.brawlStarsTag }
        return tags.filterNot { it in fresh }
    }

    companion object {
        private const val STEP_NAME = "playerRankingRenewalStep"
        private const val CHUNK_SIZE = 1000
        private val FRESH_DURATION: Duration = Duration.ofHours(24)
        private val log = LoggerFactory.getLogger(PlayerRankingRenewalTask::class.java)
    }
}
