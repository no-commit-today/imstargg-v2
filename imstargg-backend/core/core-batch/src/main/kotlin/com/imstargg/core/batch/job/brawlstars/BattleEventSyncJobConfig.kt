package com.imstargg.core.batch.job.brawlstars

import com.imstargg.core.batch.job.brawlstars.task.UnregisteredRotationEventAlertTask
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.core.batch.support.parameter.DateJobParameter
import com.imstargg.core.batch.support.querydsl.KeysetItemReader
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventSoloRankEventJpaRepository
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsRankBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsResultBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import jakarta.persistence.EntityManagerFactory
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator
import org.springframework.batch.core.job.parameters.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.ChunkOrientedStepBuilder
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

/**
 * 미등록 배틀 이벤트를 감지하는 잡. 전투 기반 두 스텝 뒤에 로테이션 기반 스텝이 붙는다.
 *
 * 통계 전투는 허용 목록의 모드만 담으므로 전투만 보면 미지원 모드의 새 맵을 놓친다.
 * 그래서 [STEP_3_NAME] 이 그날 로테이션에 나온 미등록 이벤트를 알린다. 자세한 이유와 전투 단계와의 중복 제거
 * 방식은 [UnregisteredRotationEventAlertTask] 를 본다.
 */
@Configuration
class BattleEventSyncJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val entityManagerFactory: EntityManagerFactory,
    private val alertManager: AlertManager,
    private val battleEventJpaRepository: BattleEventJpaRepository,
    private val battleEventSoloRankEventJpaRepository: BattleEventSoloRankEventJpaRepository,
    private val unregisteredRotationEventAlertTask: UnregisteredRotationEventAlertTask,
) {
    companion object {
        const val JOB_NAME = "battleEventSyncJob"
        const val DATE_PARAMETER_NAME = "battleEventSyncJobDateJobParameter"
        const val STEP_1_NAME = "battleEventSyncFromRankBattleStep"
        const val STEP_2_NAME = "battleEventSyncFromResultBattleStep"
        const val STEP_3_NAME = "battleEventSyncFromRotationStep"
        private const val CHUNK_SIZE = 1000
    }

    @Bean(DATE_PARAMETER_NAME)
    @JobScope
    fun dateJobParameter() = DateJobParameter()

    @Bean(JOB_NAME)
    fun battleEventSyncJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .validator(
                DefaultJobParametersValidator(
                    arrayOf(DateJobParameter.KEY),
                    arrayOf(),
                ),
            ).start(battleEventSyncFromRankBattleStep())
            .next(battleEventSyncFromResultBattleStep())
            .next(battleEventSyncFromRotationStep())
            .incrementer(RunIdIncrementer())
            .build()

    @Bean(STEP_1_NAME)
    @JobScope
    fun battleEventSyncFromRankBattleStep(): Step =
        ChunkOrientedStepBuilder<StatsRankBattleEntity, StatsRankBattleEntity>(STEP_1_NAME, jobRepository, CHUNK_SIZE)
            .transactionManager(txManager)
            .reader(battleEventSyncRankBattleReader())
            .writer(battleEventSyncFromRankBattleWriter())
            .build()

    @Bean(STEP_2_NAME)
    @JobScope
    fun battleEventSyncFromResultBattleStep(): Step =
        ChunkOrientedStepBuilder<StatsResultBattleEntity, StatsResultBattleEntity>(STEP_2_NAME, jobRepository, CHUNK_SIZE)
            .transactionManager(txManager)
            .reader(battleEventSyncResultBattleReader())
            .writer(battleEventSyncFromResultBattleWriter())
            .build()

    @Bean(STEP_3_NAME)
    @JobScope
    fun battleEventSyncFromRotationStep(): Step =
        StepBuilder(STEP_3_NAME, jobRepository)
            .tasklet({ _, _ ->
                unregisteredRotationEventAlertTask.alert(dateJobParameter().date!!)
                RepeatStatus.FINISHED
            })
            .build()

    @Bean
    @StepScope
    fun battleEventSyncRankBattleReader(): KeysetItemReader<StatsRankBattleEntity> {
        val rankBattle = QStatsRankBattleEntity.statsRankBattleEntity
        return KeysetItemReader(
            name = "battleEventSyncRankBattleReader",
            entityManagerFactory = entityManagerFactory,
            pageSize = CHUNK_SIZE.toLong(),
            idPath = rankBattle.id,
            idOf = { it.id!! },
        ) { queryFactory ->
            queryFactory
                .selectFrom(rankBattle)
                .where(rankBattle.battleDate.eq(dateJobParameter().date))
        }
    }

    @Bean
    @StepScope
    fun battleEventSyncResultBattleReader(): KeysetItemReader<StatsResultBattleEntity> {
        val resultBattle = QStatsResultBattleEntity.statsResultBattleEntity
        return KeysetItemReader(
            name = "battleEventSyncResultBattleReader",
            entityManagerFactory = entityManagerFactory,
            pageSize = CHUNK_SIZE.toLong(),
            idPath = resultBattle.id,
            idOf = { it.id!! },
        ) { queryFactory ->
            queryFactory
                .selectFrom(resultBattle)
                .where(resultBattle.battleDate.eq(dateJobParameter().date))
        }
    }

    @Bean
    @StepScope
    fun battleEventSyncFromRankBattleWriter() =
        BattleEventSyncItemWriter<StatsRankBattleEntity>(
            alertManager = alertManager,
            battleEventJpaRepository = battleEventJpaRepository,
            battleEventSoloRankEventJpaRepository = battleEventSoloRankEventJpaRepository,
        )

    @Bean
    @StepScope
    fun battleEventSyncFromResultBattleWriter() =
        BattleEventSyncItemWriter<StatsResultBattleEntity>(
            alertManager = alertManager,
            battleEventJpaRepository = battleEventJpaRepository,
            battleEventSoloRankEventJpaRepository = battleEventSoloRankEventJpaRepository,
        )
}
