package com.imstargg.core.batch.job.operation

import com.imstargg.core.batch.domain.operation.FeedbackDigestMessage
import com.imstargg.core.batch.support.alert.AlertCommand
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.core.enums.operation.FeedbackRating
import com.imstargg.infrastructure.db.core.operation.FeedbackJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.Duration

@Configuration
class FeedbackDailyDigestJobConfig(
    private val jobRepository: JobRepository,
    private val feedbackJpaRepository: FeedbackJpaRepository,
    private val alertManager: AlertManager,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val JOB_NAME = "feedbackDailyDigestJob"
        const val STEP_NAME = "feedbackDailyDigestStep"
    }

    @Bean(JOB_NAME)
    fun job(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .start(step())
            .build()

    @Bean(STEP_NAME)
    fun step(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet({ _, _ ->
                val now = clock.instant()
                val from = now.minus(Duration.ofHours(24))
                val recent =
                    feedbackJpaRepository.findAllBySubmittedAtGreaterThanEqualAndSubmittedAtLessThan(from = from, to = now)

                if (recent.isEmpty()) {
                    log.info("피드백 24시간 다이제스트: 0건 — 알림 미발송")
                    return@tasklet RepeatStatus.FINISHED
                }

                val counts: Map<FeedbackRating, Long> =
                    FeedbackRating.entries.associateWith { r -> recent.count { it.rating == r }.toLong() }

                val digest = FeedbackDigestMessage.build(countByRating = counts)
                if (digest != null) {
                    alertManager.alert(AlertCommand(title = digest.title, content = digest.content, ex = null))
                    log.info("피드백 24시간 다이제스트: 총 {}건 알림 발송", recent.size)
                }

                RepeatStatus.FINISHED
            })
            .build()
}
