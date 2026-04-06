package com.imstargg.core.batch.job

import com.imstargg.client.brawlstars.BrawlStarsApiServerException
import org.slf4j.LoggerFactory
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.StepContribution
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.infrastructure.repeat.RepeatStatus

class BrawlStarsApiTasklet(
    private val jobName: String,
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
            log.warn("[$jobName] 브롤스타즈 API 점검 중", ex)
        }
        return RepeatStatus.FINISHED
    }
}
