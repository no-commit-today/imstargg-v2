package com.imstargg.core.batch.job.brawlstars

import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.brawlstars.ranking.BrawlerDailyRankingEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.BrawlerDailyRankingItemEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.BrawlerDailyRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.BrawlerDailyRankingJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.BrawlerRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.BrawlerRankingJpaRepository
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.parameters.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime

@Configuration
class BrawlerRankingDailyJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val clock: Clock,
    private val brawlerRankingJpaRepository: BrawlerRankingJpaRepository,
    private val brawlerRankingItemJpaRepository: BrawlerRankingItemJpaRepository,
    private val brawlerDailyRankingJpaRepository: BrawlerDailyRankingJpaRepository,
    private val brawlerDailyRankingItemJpaRepository: BrawlerDailyRankingItemJpaRepository,
) {
    companion object {
        const val JOB_NAME = "brawlerRankingDailyJob"
        private const val STEP_NAME = "brawlerRankingDailyStep"
    }

    @Bean(JOB_NAME)
    fun brawlerRankingDailyJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(brawlerRankingDailyStep())
            .build()

    @Bean(STEP_NAME)
    fun brawlerRankingDailyStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet({ _, _ ->
                val date = LocalDate.now(clock)
                val endTime =
                    date
                        .atTime(LocalTime.of(18, 0))
                        .atZone(clock.zone)
                        .toOffsetDateTime()
                val startTime = endTime.minusDays(1)

                Country.entries.forEach { country ->
                    val rankings =
                        brawlerRankingJpaRepository
                            .findAllByCountryAndCollectedAtGreaterThanEqualAndCollectedAtLessThan(
                                country,
                                startTime,
                                endTime,
                            )
                    if (rankings.isEmpty()) return@forEach

                    rankings
                        .groupBy { it.brawlerBrawlStarsId }
                        .forEach { (brawlerBrawlStarsId, group) ->
                            val latest = group.maxByOrNull { it.collectedAt } ?: return@forEach
                            val latestItems =
                                brawlerRankingItemJpaRepository.findAllByBrawlerRankingId(latest.id!!)

                            val dailyRanking =
                                brawlerDailyRankingJpaRepository.save(
                                    BrawlerDailyRankingEntity(
                                        country = country,
                                        brawlerBrawlStarsId = brawlerBrawlStarsId,
                                        date = date,
                                        startTime = startTime,
                                        endTime = endTime,
                                    ),
                                )
                            val dailyItems =
                                latestItems.map {
                                    BrawlerDailyRankingItemEntity(
                                        brawlerDailyRankingId = dailyRanking.id!!,
                                        rank = it.rank,
                                        player = it.player,
                                        trophies = it.trophies,
                                    )
                                }
                            brawlerDailyRankingItemJpaRepository.saveAll(dailyItems)
                        }

                    val rankingIds = rankings.mapNotNull { it.id }
                    val allItems = brawlerRankingItemJpaRepository.findAllByBrawlerRankingIdIn(rankingIds)
                    brawlerRankingItemJpaRepository.deleteAllInBatch(allItems)
                    brawlerRankingJpaRepository.deleteAllInBatch(rankings)
                }

                RepeatStatus.FINISHED
            }, txManager)
            .build()
}
