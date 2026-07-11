package com.imstargg.core.batch.job.renewal

import com.imstargg.core.batch.job.renewal.task.PlayerRankingRenewalTask
import com.imstargg.core.batch.support.parameter.DateJobParameter
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.infrastructure.repeat.RepeatStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 랭킹 플레이어 사전 갱신 잡. 일 1회, rankingCollectJob 이후 실행하도록 스케줄한다
 * (예: --job.name=playerRankingRenewalJob --date=2026-07-06).
 *
 * `date` 파라미터가 JobInstance 정체성을 부여한다 — 같은 날 재시도는 같은 인스턴스
 * 재시작으로 인식된다. 스텝에 트랜잭션 매니저를 넘기지 않는 이유는 RankingCollectJobConfig 와
 * 동일: 태스크가 태그당 1초 간격의 느린 HTTP 호출을 수천 번 수행하므로 스텝 전체를
 * 한 트랜잭션으로 묶으면 커넥션이 장시간 점유된다.
 */
@Configuration
class PlayerRankingRenewalJobConfig(
    private val jobRepository: JobRepository,
    private val playerRankingRenewalTask: PlayerRankingRenewalTask,
) {
    companion object {
        const val JOB_NAME = "playerRankingRenewalJob"
        const val DATE_PARAMETER_NAME = DateJobParameter.KEY
        private const val STEP_NAME = "playerRankingRenewalStep"
    }

    @Bean(JOB_NAME)
    fun playerRankingRenewalJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .validator(
                DefaultJobParametersValidator(
                    arrayOf(DATE_PARAMETER_NAME),
                    arrayOf(),
                ),
            ).start(playerRankingRenewalStep())
            .build()

    @Bean(STEP_NAME)
    fun playerRankingRenewalStep(): Step =
        StepBuilder(STEP_NAME, jobRepository)
            .tasklet { _, _ ->
                playerRankingRenewalTask.renewAll()
                RepeatStatus.FINISHED
            }.build()
}
