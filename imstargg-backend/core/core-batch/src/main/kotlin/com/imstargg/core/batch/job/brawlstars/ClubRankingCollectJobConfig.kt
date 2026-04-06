package com.imstargg.core.batch.job.brawlstars

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.brawlstars.ranking.ClubRankingEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.ClubRankingItemEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.ClubRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.ClubRankingJpaRepository
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
class ClubRankingCollectJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val clock: Clock,
    private val brawlStarsClient: BrawlStarsClient,
    private val clubRankingJpaRepository: ClubRankingJpaRepository,
    private val clubRankingItemJpaRepository: ClubRankingItemJpaRepository,
) {
    companion object {
        const val JOB_NAME = "clubRankingCollectJob"
        private const val STEP_NAME = "clubRankingCollectStep"
    }

    @Bean(JOB_NAME)
    fun clubRankingCollectJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .start(clubRankingCollectStep())
            .build()

    @Bean(STEP_NAME)
    fun clubRankingCollectStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet(
                BrawlStarsApiTasklet(JOB_NAME) {
                    Country.entries.forEach { country ->
                        val collectedAt = OffsetDateTime.now(clock)
                        val response = brawlStarsClient.getClubRanking(country.code)
                        val ranking =
                            clubRankingJpaRepository.save(
                                ClubRankingEntity(country = country, collectedAt = collectedAt),
                            )
                        val items =
                            response.items.map { resp ->
                                ClubRankingItemEntity(
                                    clubRankingId = ranking.id!!,
                                    rank = resp.rank,
                                    brawlStarsTag = resp.tag,
                                    name = resp.name,
                                    badgeBrawlStarsId = resp.badgeId.toLong(),
                                    trophies = resp.trophies,
                                    memberCount = resp.memberCount,
                                )
                            }
                        clubRankingItemJpaRepository.saveAll(items)
                    }
                },
                txManager,
            ).build()
}
