package com.imstargg.core.batch.job.brawlstars

import com.imstargg.core.batch.support.parameter.DateJobParameter
import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.brawlstars.ranking.ClubDailyRankingEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.ClubDailyRankingItemEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.ClubDailyRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.ClubDailyRankingJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.ClubRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.ClubRankingJpaRepository
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator
import org.springframework.batch.core.job.parameters.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import java.time.Clock
import java.time.LocalTime

@Configuration
class ClubRankingDailyJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val clock: Clock,
    private val clubRankingJpaRepository: ClubRankingJpaRepository,
    private val clubRankingItemJpaRepository: ClubRankingItemJpaRepository,
    private val clubDailyRankingJpaRepository: ClubDailyRankingJpaRepository,
    private val clubDailyRankingItemJpaRepository: ClubDailyRankingItemJpaRepository,
) {
    companion object {
        const val JOB_NAME = "clubRankingDailyJob"
        const val DATE_PARAMETER_NAME = "clubRankingDailyJobDateJobParameter"
        private const val STEP_NAME = "clubRankingDailyStep"
    }

    @Bean(DATE_PARAMETER_NAME)
    @JobScope
    fun dateJobParameter() = DateJobParameter()

    @Bean(JOB_NAME)
    fun clubRankingDailyJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .incrementer(RunIdIncrementer())
            .validator(DefaultJobParametersValidator(arrayOf(DateJobParameter.KEY), arrayOf()))
            .start(clubRankingDailyStep())
            .build()

    @Bean(STEP_NAME)
    @JobScope
    fun clubRankingDailyStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet({ _, _ ->
                val date = dateJobParameter().date!!
                val endTime =
                    date
                        .atTime(LocalTime.of(18, 0))
                        .atZone(clock.zone)
                        .toOffsetDateTime()
                val startTime = endTime.minusDays(1)

                Country.entries.forEach { country ->
                    val rankings =
                        clubRankingJpaRepository
                            .findAllByCountryAndCollectedAtGreaterThanEqualAndCollectedAtLessThan(
                                country,
                                startTime,
                                endTime,
                            )
                    if (rankings.isEmpty()) return@forEach

                    val latest = rankings.maxByOrNull { it.collectedAt }!!
                    val latestItems = clubRankingItemJpaRepository.findAllByClubRankingId(latest.id!!)

                    val dailyRanking =
                        clubDailyRankingJpaRepository.save(
                            ClubDailyRankingEntity(
                                country = country,
                                date = date,
                                startTime = startTime,
                                endTime = endTime,
                            ),
                        )
                    val dailyItems =
                        latestItems.map {
                            ClubDailyRankingItemEntity(
                                clubDailyRankingId = dailyRanking.id!!,
                                rank = it.rank,
                                brawlStarsTag = it.brawlStarsTag,
                                name = it.name,
                                badgeBrawlStarsId = it.badgeBrawlStarsId,
                                trophies = it.trophies,
                                memberCount = it.memberCount,
                            )
                        }
                    clubDailyRankingItemJpaRepository.saveAll(dailyItems)

                    val rankingIds = rankings.mapNotNull { it.id }
                    val allItems = clubRankingItemJpaRepository.findAllByClubRankingIdIn(rankingIds)
                    clubRankingItemJpaRepository.deleteAllInBatch(allItems)
                    clubRankingJpaRepository.deleteAllInBatch(rankings)
                }

                RepeatStatus.FINISHED
            }, txManager)
            .build()
}
