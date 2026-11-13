package com.imstargg.core.batch.job.statistics.patch

import com.imstargg.core.batch.job.statistics.BattleStatsConstants
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.infrastructure.db.core.brawlstars.patch.repository.BrawlStarsPatchJpaRepository
import com.imstargg.infrastructure.db.core.statistics.repository.StatsCheckpointJpaRepository
import jakarta.persistence.EntityManagerFactory
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.parameters.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

/**
 * 패치 기준 통계 잡. 통계 유형마다 잡 하나와 스텝 하나다.
 *
 * 파라미터가 없어 `RunIdIncrementer` 가 없으면 두 번째 실행이 거부된다.
 * 무엇을 읽을지는 체크포인트와 패치 목록이 정한다. 스텝에 트랜잭션 매니저를 넘기지 않는다.
 * 읽기는 트랜잭션 밖에서 하고, 쓰기는 `PatchStatsWriter` 가 저장마다 자기 트랜잭션을 연다.
 * 실패 알림은 `JobFailureAlertListener` 가 보낸다. 봇 감시의 실패는 [PatchStatsTask] 가 로그로만 남긴다.
 */
@Configuration
class PatchStatsJobConfig(
    private val jobRepository: JobRepository,
) {
    companion object {
        const val BRAWLER_BATTLE_RESULT_JOB_NAME = "brawlerBattleResultPatchStatsJob"
        const val BRAWLER_BATTLE_RESULT_STEP_NAME = "brawlerBattleResultPatchStatsStep"
        const val BRAWLER_BATTLE_RANK_JOB_NAME = "brawlerBattleRankPatchStatsJob"
        const val BRAWLER_BATTLE_RANK_STEP_NAME = "brawlerBattleRankPatchStatsStep"
        const val BRAWLER_ENEMY_BATTLE_RESULT_JOB_NAME = "brawlerEnemyBattleResultPatchStatsJob"
        const val BRAWLER_ENEMY_BATTLE_RESULT_STEP_NAME = "brawlerEnemyBattleResultPatchStatsStep"
        const val BRAWLER_PAIR_BATTLE_RESULT_JOB_NAME = "brawlerPairBattleResultPatchStatsJob"
        const val BRAWLER_PAIR_BATTLE_RESULT_STEP_NAME = "brawlerPairBattleResultPatchStatsStep"
        const val BRAWLER_TRIO_BATTLE_RESULT_JOB_NAME = "brawlerTrioBattleResultPatchStatsJob"
        const val BRAWLER_TRIO_BATTLE_RESULT_STEP_NAME = "brawlerTrioBattleResultPatchStatsStep"
    }

    @Bean
    fun patchStatsResultBattleReader(entityManagerFactory: EntityManagerFactory): StatsResultBattleReader =
        StatsResultBattleReader(entityManagerFactory = entityManagerFactory, pageSize = BattleStatsConstants.READ_PAGE_SIZE)

    @Bean
    fun patchStatsRankBattleReader(entityManagerFactory: EntityManagerFactory): StatsRankBattleReader =
        StatsRankBattleReader(entityManagerFactory = entityManagerFactory, pageSize = BattleStatsConstants.READ_PAGE_SIZE)

    @Bean
    fun patchStatsAggregations(
        patchStatsResultBattleReader: StatsResultBattleReader,
        patchStatsRankBattleReader: StatsRankBattleReader,
    ): PatchStatsAggregations =
        PatchStatsAggregations(
            resultBattleReader = patchStatsResultBattleReader,
            rankBattleReader = patchStatsRankBattleReader,
        )

    @Bean
    fun patchStatsTask(
        patchJpaRepository: BrawlStarsPatchJpaRepository,
        checkpointJpaRepository: StatsCheckpointJpaRepository,
        writer: PatchStatsWriter,
        rangeAligner: PatchStatsRangeAligner,
        alertManager: AlertManager,
        clock: Clock,
    ): PatchStatsTask =
        PatchStatsTask(
            patchJpaRepository = patchJpaRepository,
            checkpointJpaRepository = checkpointJpaRepository,
            writer = writer,
            rangeAligner = rangeAligner,
            alertManager = alertManager,
            clock = clock,
            checkpointBattleCount = PatchStatsConstants.CHECKPOINT_BATTLE_COUNT,
        )

    @Bean(BRAWLER_BATTLE_RESULT_JOB_NAME)
    fun brawlerBattleResultPatchStatsJob(
        @Qualifier(BRAWLER_BATTLE_RESULT_STEP_NAME) step: Step,
    ): Job = job(BRAWLER_BATTLE_RESULT_JOB_NAME, step)

    @Bean(BRAWLER_BATTLE_RESULT_STEP_NAME)
    fun brawlerBattleResultPatchStatsStep(
        task: PatchStatsTask,
        aggregations: PatchStatsAggregations,
    ): Step = step(BRAWLER_BATTLE_RESULT_STEP_NAME) { task.execute(aggregations.brawlerBattleResult) }

    @Bean(BRAWLER_BATTLE_RANK_JOB_NAME)
    fun brawlerBattleRankPatchStatsJob(
        @Qualifier(BRAWLER_BATTLE_RANK_STEP_NAME) step: Step,
    ): Job = job(BRAWLER_BATTLE_RANK_JOB_NAME, step)

    @Bean(BRAWLER_BATTLE_RANK_STEP_NAME)
    fun brawlerBattleRankPatchStatsStep(
        task: PatchStatsTask,
        aggregations: PatchStatsAggregations,
    ): Step = step(BRAWLER_BATTLE_RANK_STEP_NAME) { task.execute(aggregations.brawlerBattleRank) }

    @Bean(BRAWLER_ENEMY_BATTLE_RESULT_JOB_NAME)
    fun brawlerEnemyBattleResultPatchStatsJob(
        @Qualifier(BRAWLER_ENEMY_BATTLE_RESULT_STEP_NAME) step: Step,
    ): Job = job(BRAWLER_ENEMY_BATTLE_RESULT_JOB_NAME, step)

    @Bean(BRAWLER_ENEMY_BATTLE_RESULT_STEP_NAME)
    fun brawlerEnemyBattleResultPatchStatsStep(
        task: PatchStatsTask,
        aggregations: PatchStatsAggregations,
    ): Step = step(BRAWLER_ENEMY_BATTLE_RESULT_STEP_NAME) { task.execute(aggregations.brawlerEnemyBattleResult) }

    @Bean(BRAWLER_PAIR_BATTLE_RESULT_JOB_NAME)
    fun brawlerPairBattleResultPatchStatsJob(
        @Qualifier(BRAWLER_PAIR_BATTLE_RESULT_STEP_NAME) step: Step,
    ): Job = job(BRAWLER_PAIR_BATTLE_RESULT_JOB_NAME, step)

    @Bean(BRAWLER_PAIR_BATTLE_RESULT_STEP_NAME)
    fun brawlerPairBattleResultPatchStatsStep(
        task: PatchStatsTask,
        aggregations: PatchStatsAggregations,
    ): Step = step(BRAWLER_PAIR_BATTLE_RESULT_STEP_NAME) { task.execute(aggregations.brawlerPairBattleResult) }

    @Bean(BRAWLER_TRIO_BATTLE_RESULT_JOB_NAME)
    fun brawlerTrioBattleResultPatchStatsJob(
        @Qualifier(BRAWLER_TRIO_BATTLE_RESULT_STEP_NAME) step: Step,
    ): Job = job(BRAWLER_TRIO_BATTLE_RESULT_JOB_NAME, step)

    @Bean(BRAWLER_TRIO_BATTLE_RESULT_STEP_NAME)
    fun brawlerTrioBattleResultPatchStatsStep(
        task: PatchStatsTask,
        aggregations: PatchStatsAggregations,
    ): Step = step(BRAWLER_TRIO_BATTLE_RESULT_STEP_NAME) { task.execute(aggregations.brawlerTrioBattleResult) }

    private fun job(
        jobName: String,
        step: Step,
    ): Job =
        JobBuilder(jobName, jobRepository)
            .start(step)
            .incrementer(RunIdIncrementer())
            .build()

    private fun step(
        stepName: String,
        execute: () -> Unit,
    ): Step =
        StepBuilder(stepName, jobRepository)
            .tasklet({ _, _ ->
                execute()
                RepeatStatus.FINISHED
            })
            .build()
}
