package com.imstargg.core.batch.job

import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.brawlstars.ranking.PlayerDailyRankingEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.PlayerDailyRankingItemEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.PlayerDailyRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.PlayerDailyRankingJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.PlayerRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.PlayerRankingJpaRepository
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
class PlayerRankingDailyJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val clock: Clock,
    private val playerRankingJpaRepository: PlayerRankingJpaRepository,
    private val playerRankingItemJpaRepository: PlayerRankingItemJpaRepository,
    private val playerDailyRankingJpaRepository: PlayerDailyRankingJpaRepository,
    private val playerDailyRankingItemJpaRepository: PlayerDailyRankingItemJpaRepository,
) {
    companion object {
        const val JOB_NAME = "playerRankingDailyJob"
        private const val STEP_NAME = "playerRankingDailyStep"
    }

    @Bean(JOB_NAME)
    fun playerRankingDailyJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .start(playerRankingDailyStep())
            .build()

    @Bean(STEP_NAME)
    fun playerRankingDailyStep(): Step =
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
                        playerRankingJpaRepository
                            .findAllByCountryAndCollectedAtGreaterThanEqualAndCollectedAtLessThan(
                                country,
                                startTime,
                                endTime,
                            )
                    if (rankings.isEmpty()) return@forEach

                    val latest = rankings.maxByOrNull { it.collectedAt }!!
                    val latestItems = playerRankingItemJpaRepository.findAllByPlayerRankingId(latest.id!!)

                    val dailyRanking =
                        playerDailyRankingJpaRepository.save(
                            PlayerDailyRankingEntity(
                                country = country,
                                date = date,
                                startTime = startTime,
                                endTime = endTime,
                            ),
                        )
                    val dailyItems =
                        latestItems.map {
                            PlayerDailyRankingItemEntity(
                                playerDailyRankingId = dailyRanking.id!!,
                                rank = it.rank,
                                player = it.player,
                                trophies = it.trophies,
                            )
                        }
                    playerDailyRankingItemJpaRepository.saveAll(dailyItems)

                    val rankingIds = rankings.mapNotNull { it.id }
                    val allItems = playerRankingItemJpaRepository.findAllByPlayerRankingIdIn(rankingIds)
                    playerRankingItemJpaRepository.deleteAllInBatch(allItems)
                    playerRankingJpaRepository.deleteAllInBatch(rankings)
                }

                RepeatStatus.FINISHED
            }, txManager)
            .build()
}
