package com.imstargg.core.batch.job.brawlstars

import com.imstargg.client.brawlstars.BrawlStarsApiServerException
import org.slf4j.LoggerFactory
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.StepContribution
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.infrastructure.repeat.RepeatStatus

/**
 * Wraps BrawlStars API batch actions with the service-maintenance policy.
 *
 * `ServiceUnavailable` from the BrawlStars API means the upstream API is under maintenance.
 * Jobs using this tasklet must not fail in that case: they log the maintenance state and
 * finish normally so scheduled batch orchestration can continue.
 */
class BrawlStarsApiTasklet(
    private val name: String,
    private val action: () -> Unit,
) : Tasklet {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        try {
            action()
        } catch (ex: BrawlStarsApiServerException.ServiceUnavailable) {
            log.warn("[$name] 브롤스타즈 API 점검 중", ex)
        }
        return RepeatStatus.FINISHED
    }
}
