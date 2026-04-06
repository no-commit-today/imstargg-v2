package com.imstargg.core.batch.job.brawlstars

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.BrawlerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.BrawlerRankingEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.BrawlerRankingItemEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.RankingEntityPlayer
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.BrawlerRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.BrawlerRankingJpaRepository
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import java.time.Clock
import java.time.OffsetDateTime

@Configuration
class BrawlerRankingCollectJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val clock: Clock,
    private val brawlStarsClient: BrawlStarsClient,
    private val brawlerJpaRepository: BrawlerJpaRepository,
    private val brawlerRankingJpaRepository: BrawlerRankingJpaRepository,
    private val brawlerRankingItemJpaRepository: BrawlerRankingItemJpaRepository,
) {
    companion object {
        const val JOB_NAME = "brawlerRankingCollectJob"
        private const val STEP_NAME = "brawlerRankingCollectStep"
    }

    @Bean(JOB_NAME)
    fun brawlerRankingCollectJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .start(brawlerRankingCollectStep())
            .build()

    @Bean(STEP_NAME)
    fun brawlerRankingCollectStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet(
                BrawlStarsApiTasklet(JOB_NAME) {
                    val brawlers = brawlerJpaRepository.findAll()
                    Country.entries.forEach { country ->
                        brawlers.forEach { brawler ->
                            val collectedAt = OffsetDateTime.now(clock)
                            val response = brawlStarsClient.getBrawlerRanking(country.code, brawler.brawlStarsId)
                            val ranking =
                                brawlerRankingJpaRepository.save(
                                    BrawlerRankingEntity(
                                        country = country,
                                        brawlerBrawlStarsId = brawler.brawlStarsId,
                                        collectedAt = collectedAt,
                                    ),
                                )
                            val items =
                                response.items.map { resp ->
                                    BrawlerRankingItemEntity(
                                        brawlerRankingId = ranking.id!!,
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
                            brawlerRankingItemJpaRepository.saveAll(items)
                        }
                    }
                },
                txManager,
            ).build()
}
