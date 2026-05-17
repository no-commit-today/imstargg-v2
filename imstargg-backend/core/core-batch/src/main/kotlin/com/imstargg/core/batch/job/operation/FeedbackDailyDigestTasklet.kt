package com.imstargg.core.batch.job.operation

import com.imstargg.core.batch.support.alert.AlertCommand
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.core.enums.operation.FeedbackCategory
import com.imstargg.infrastructure.db.core.operation.FeedbackJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration

@Component
class FeedbackDailyDigestTasklet(
    private val feedbackJpaRepository: FeedbackJpaRepository,
    private val alertManager: AlertManager,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun run() {
        val now = clock.instant()
        val from = now.minus(Duration.ofHours(24))
        val recent =
            feedbackJpaRepository.findAllByCreatedAtGreaterThanEqualAndCreatedAtLessThan(from = from, to = now)

        if (recent.isEmpty()) {
            log.info("피드백 24시간 다이제스트: 0건 — 알림 미발송")
            return
        }

        val counts: Map<FeedbackCategory, Long> =
            FeedbackCategory.entries.associateWith { cat -> recent.count { it.category == cat }.toLong() }
        val emailCount = recent.count { !it.email.isNullOrBlank() }.toLong()

        val digest = FeedbackDigestMessage.build(countByCategory = counts, emailCount = emailCount) ?: return

        alertManager.alert(AlertCommand(title = digest.title, content = digest.content, ex = null))
        log.info("피드백 24시간 다이제스트: 총 {}건 알림 발송", recent.size)
    }
}
