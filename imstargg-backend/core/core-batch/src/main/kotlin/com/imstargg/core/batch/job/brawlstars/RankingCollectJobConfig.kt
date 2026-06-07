package com.imstargg.core.batch.job.brawlstars

import com.imstargg.core.batch.job.brawlstars.task.BrawlerRankingCollectTask
import com.imstargg.core.batch.job.brawlstars.task.ClubRankingCollectTask
import com.imstargg.core.batch.job.brawlstars.task.PlayerRankingCollectTask
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 클럽/플레이어/브롤러 랭킹 수집을 하나의 잡에서 순차 실행한다 (싼 스텝 먼저, 비싼 brawler 마지막).
 *
 * 각 스텝은 트랜잭션 매니저를 넘기지 않아 ResourcelessTransactionManager(no-op)로 동작한다.
 * 스텝에 실 트랜잭션을 걸면 tasklet 전체(국가×브롤러 수천 회의 느린 API 호출 + insert)가 한
 * 트랜잭션으로 묶여 API 응답 대기 동안 커넥션이 `idle in transaction` 으로 장시간 점유된다.
 * 매니저를 제거하면 각 save/saveAll/deleteAllInBatch 가 자체 짧은 트랜잭션으로 즉시 커밋·반환된다
 * (스텝 단위 원자성은 포기되지만 스냅샷 보존 + 윈도우 재시작으로 자가 치유).
 *
 * `RunIdIncrementer` 를 쓰지 않는 이유: Spring Boot 4 의 `JobLauncherApplicationRunner` 는
 * incrementer 가 있으면 직전 실행이 FAILED 여도 무조건 run.id+1 새 JobInstance 를 만들어
 * 이미 성공한 스텝까지 재실행한다 (중복 스냅샷 수집). 대신 30분 수집 창 인덱스를
 * `window` 파라미터로 받아 인스턴스 정체성을 부여한다 — 같은 창 내 파드 재시도는
 * 같은 JobInstance 재시작으로 인식되어 성공한 스텝을 건너뛰고 실패 스텝부터 재개한다.
 */
@Configuration
class RankingCollectJobConfig(
    private val jobRepository: JobRepository,
    private val clubRankingCollectTask: ClubRankingCollectTask,
    private val playerRankingCollectTask: PlayerRankingCollectTask,
    private val brawlerRankingCollectTask: BrawlerRankingCollectTask,
) {
    companion object {
        const val JOB_NAME = "rankingCollectJob"
        const val WINDOW_PARAMETER_NAME = "window"
        private const val CLUB_STEP_NAME = "clubRankingCollectStep"
        private const val PLAYER_STEP_NAME = "playerRankingCollectStep"
        private const val BRAWLER_STEP_NAME = "brawlerRankingCollectStep"
    }

    @Bean(JOB_NAME)
    fun rankingCollectJob(): Job =
        JobBuilder(JOB_NAME, jobRepository)
            .validator(
                DefaultJobParametersValidator(
                    arrayOf(WINDOW_PARAMETER_NAME),
                    arrayOf(),
                ),
            ).start(clubRankingCollectStep())
            .next(playerRankingCollectStep())
            .next(brawlerRankingCollectStep())
            .build()

    @Bean(CLUB_STEP_NAME)
    fun clubRankingCollectStep(): Step =
        StepBuilder(CLUB_STEP_NAME, jobRepository)
            .tasklet(
                BrawlStarsApiTasklet(CLUB_STEP_NAME) {
                    clubRankingCollectTask.collect()
                },
            ).build()

    @Bean(PLAYER_STEP_NAME)
    fun playerRankingCollectStep(): Step =
        StepBuilder(PLAYER_STEP_NAME, jobRepository)
            .tasklet(
                BrawlStarsApiTasklet(PLAYER_STEP_NAME) {
                    playerRankingCollectTask.collect()
                },
            ).build()

    @Bean(BRAWLER_STEP_NAME)
    fun brawlerRankingCollectStep(): Step =
        StepBuilder(BRAWLER_STEP_NAME, jobRepository)
            .tasklet(
                BrawlStarsApiTasklet(BRAWLER_STEP_NAME) {
                    brawlerRankingCollectTask.collect()
                },
            ).build()
}
