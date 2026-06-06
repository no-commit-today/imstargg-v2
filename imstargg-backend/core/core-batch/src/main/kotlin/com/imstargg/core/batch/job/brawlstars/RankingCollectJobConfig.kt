package com.imstargg.core.batch.job.brawlstars

import com.imstargg.core.batch.job.brawlstars.task.BrawlerRankingCollectTask
import com.imstargg.core.batch.job.brawlstars.task.ClubRankingCollectTask
import com.imstargg.core.batch.job.brawlstars.task.PlayerRankingCollectTask
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.job.parameters.DefaultJobParametersValidator
import org.springframework.batch.core.job.parameters.RunIdIncrementer
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.Step
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

/**
 * 클럽/플레이어/브롤러 랭킹 수집을 하나의 잡에서 순차 실행한다 (싼 스텝 먼저, 비싼 brawler 마지막).
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
    private val txManager: PlatformTransactionManager,
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
        private const val LEGACY_CLUB_JOB_NAME = "clubRankingCollectJob"
        private const val LEGACY_PLAYER_JOB_NAME = "playerRankingCollectJob"
        private const val LEGACY_BRAWLER_JOB_NAME = "brawlerRankingCollectJob"
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
                txManager,
            ).build()

    @Bean(PLAYER_STEP_NAME)
    fun playerRankingCollectStep(): Step =
        StepBuilder(PLAYER_STEP_NAME, jobRepository)
            .tasklet(
                BrawlStarsApiTasklet(PLAYER_STEP_NAME) {
                    playerRankingCollectTask.collect()
                },
                txManager,
            ).build()

    @Bean(BRAWLER_STEP_NAME)
    fun brawlerRankingCollectStep(): Step =
        StepBuilder(BRAWLER_STEP_NAME, jobRepository)
            .tasklet(
                BrawlStarsApiTasklet(BRAWLER_STEP_NAME) {
                    brawlerRankingCollectTask.collect()
                },
                txManager,
            ).build()

    /**
     * 구 잡 이름 호환용 레거시 별칭 잡 (아래 3개) — values.yaml 의 CronJob args 변경은 main 머지
     * 시점에 클러스터에 반영되지만 이미지는 Deploy 워크플로 실행 시점에야 갱신되므로, 잡 이름
     * 교체를 한 번에 하면 구 이미지가 [JOB_NAME] 을 찾지 못하는 공백이 생긴다. 그래서 2단계로
     * 롤아웃한다: 이 클래스가 포함된 이미지를 먼저 배포한 뒤, values.yaml 의 랭킹 CronJob 3개를
     * `ranking-collect` 1개로 전환하면서 아래 레거시 잡 3개를 함께 제거한다 (values.yaml 의
     * TODO 주석 참조). 레거시 잡은 기존과 동일하게 단일 스텝 + [RunIdIncrementer] 로 동작한다.
     */
    @Bean(LEGACY_CLUB_JOB_NAME)
    fun clubRankingCollectJob(): Job =
        JobBuilder(LEGACY_CLUB_JOB_NAME, jobRepository)
            .start(clubRankingCollectStep())
            .incrementer(RunIdIncrementer())
            .build()

    @Bean(LEGACY_PLAYER_JOB_NAME)
    fun playerRankingCollectJob(): Job =
        JobBuilder(LEGACY_PLAYER_JOB_NAME, jobRepository)
            .start(playerRankingCollectStep())
            .incrementer(RunIdIncrementer())
            .build()

    @Bean(LEGACY_BRAWLER_JOB_NAME)
    fun brawlerRankingCollectJob(): Job =
        JobBuilder(LEGACY_BRAWLER_JOB_NAME, jobRepository)
            .start(brawlerRankingCollectStep())
            .incrementer(RunIdIncrementer())
            .build()
}
