package com.imstargg.core.batch.job.operation

import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParametersBuilder
import org.springframework.batch.core.launch.JobOperator
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.OffsetDateTime

@Component
@ConditionalOnProperty(prefix = "app.schedule.feedback-digest", name = ["enabled"], havingValue = "true")
class FeedbackDailyDigestScheduler(
    private val jobOperator: JobOperator,
    @Qualifier(FeedbackDailyDigestJobConfig.JOB_NAME) private val job: Job,
    private val clock: Clock,
) {
    @Scheduled(cron = "\${app.schedule.feedback-digest.cron}", zone = "Asia/Seoul")
    fun runDailyDigest() {
        val params =
            JobParametersBuilder()
                .addString("runAt", OffsetDateTime.now(clock).toString())
                .toJobParameters()
        jobOperator.start(job, params)
    }
}
