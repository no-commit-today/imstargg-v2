package com.imstargg.core.batch.job.brawlstars

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.client.brawlstars.response.BrawlerResponse
import com.imstargg.core.batch.support.alert.AlertCommand
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.BrawlerJpaRepository
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class BrawlerSyncJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val brawlStarsClient: BrawlStarsClient,
    private val brawlerJpaRepository: BrawlerJpaRepository,
    private val alertManager: AlertManager,
) {
    companion object {
        const val JOB_NAME = "brawlerSyncJob"
        const val STEP_NAME = "brawlerSyncStep"
    }

    @Bean(JOB_NAME)
    fun brawlerSyncJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .start(brawlerSyncStep())
            .build()

    @Bean(STEP_NAME)
    fun brawlerSyncStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet(
                BrawlStarsApiTasklet(JOB_NAME) {
                    val brawlers = brawlStarsClient.getBrawlers().items
                    val existingIds =
                        brawlerJpaRepository
                            .findAll()
                            .map { it.brawlStarsId }
                            .toSet()

                    val newBrawlers =
                        brawlers.filter { it.id !in existingIds }

                    alertNewBrawlers(newBrawlers)
                },
                txManager,
            ).build()

    private fun alertNewBrawlers(newBrawlers: List<BrawlerResponse>) {
        if (newBrawlers.isEmpty()) return
        val content = newBrawlers.joinToString("\n") { "- $it" }
        alertManager.alert(
            AlertCommand
                .builder()
                .title("[$JOB_NAME] 새로운 브롤러 발견")
                .content(content)
                .build(),
        )
    }
}
