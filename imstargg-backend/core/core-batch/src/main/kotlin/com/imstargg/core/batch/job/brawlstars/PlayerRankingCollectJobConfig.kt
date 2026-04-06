package com.imstargg.core.batch.job.brawlstars

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.brawlstars.ranking.PlayerRankingEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.PlayerRankingItemEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.RankingEntityPlayer
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.PlayerRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.PlayerRankingJpaRepository
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.parameters.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import java.time.Clock
import java.time.OffsetDateTime

@Configuration
class PlayerRankingCollectJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val clock: Clock,
    private val brawlStarsClient: BrawlStarsClient,
    private val playerRankingJpaRepository: PlayerRankingJpaRepository,
    private val playerRankingItemJpaRepository: PlayerRankingItemJpaRepository,
) {
    companion object {
        const val JOB_NAME = "playerRankingCollectJob"
        private const val STEP_NAME = "playerRankingCollectStep"
    }

    @Bean(JOB_NAME)
    fun playerRankingCollectJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(playerRankingCollectStep())
            .build()

    @Bean(STEP_NAME)
    fun playerRankingCollectStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet(
                BrawlStarsApiTasklet(JOB_NAME) {
                    Country.entries.forEach { country ->
                        val collectedAt = OffsetDateTime.now(clock)
                        val response = brawlStarsClient.getPlayerRanking(country.code)
                        val ranking =
                            playerRankingJpaRepository.save(
                                PlayerRankingEntity(country = country, collectedAt = collectedAt),
                            )
                        val items =
                            response.items.map { resp ->
                                PlayerRankingItemEntity(
                                    playerRankingId = ranking.id!!,
                                    rank = resp.rank,
                                    player =
                                        RankingEntityPlayer(
                                            brawlStarsTag = resp.tag,
                                            name = resp.name,
                                            nameColor = resp.nameColor,
                                            iconBrawlStarsId = resp.icon.id,
                                            clubName = resp.club?.name ?: "",
                                        ),
                                    trophies = resp.trophies,
                                )
                            }
                        playerRankingItemJpaRepository.saveAll(items)
                    }
                },
                txManager,
            ).build()
}
