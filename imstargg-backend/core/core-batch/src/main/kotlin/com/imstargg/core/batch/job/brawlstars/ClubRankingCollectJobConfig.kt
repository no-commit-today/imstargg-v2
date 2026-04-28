package com.imstargg.core.batch.job.brawlstars

import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.brawlstars.ranking.ClubRankingEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.ClubRankingItemEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.ClubRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.ClubRankingJpaRepository
import org.slf4j.LoggerFactory
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
        private const val SNAPSHOT_RETENTION_COUNT = 2
        private val log = LoggerFactory.getLogger(ClubRankingCollectJobConfig::class.java)
    }

    @Bean(JOB_NAME)
    fun clubRankingCollectJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .start(clubRankingCollectStep())
            .incrementer(RunIdIncrementer())
            .build()

    @Bean(STEP_NAME)
    fun clubRankingCollectStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet(
                BrawlStarsApiTasklet(JOB_NAME) {
                    log.info("[{}] 시작", JOB_NAME)
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

                        val outdated =
                            clubRankingJpaRepository
                                .findAllByCountryOrderByCollectedAtDesc(country)
                                .drop(SNAPSHOT_RETENTION_COUNT)
                        if (outdated.isNotEmpty()) {
                            val outdatedIds = outdated.mapNotNull { it.id }
                            val outdatedItems =
                                clubRankingItemJpaRepository.findAllByClubRankingIdIn(outdatedIds)
                            clubRankingItemJpaRepository.deleteAllInBatch(outdatedItems)
                            clubRankingJpaRepository.deleteAllInBatch(outdated)
                        }
                        log.debug(
                            "[{}] 국가 {} 수집 - {}건 (정리: {}건)",
                            JOB_NAME,
                            country,
                            items.size,
                            outdated.size,
                        )
                    }
                    log.info("[{}] 완료", JOB_NAME)
                },
                txManager,
            ).build()
}
