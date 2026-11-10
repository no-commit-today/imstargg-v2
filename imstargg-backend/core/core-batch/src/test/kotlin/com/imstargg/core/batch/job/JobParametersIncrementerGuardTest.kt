package com.imstargg.core.batch.job

import com.imstargg.CoreBatchContextTest
import com.imstargg.core.batch.job.brawlstars.BattleEventSyncJobConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.InvalidJobParametersException
import org.springframework.batch.core.job.parameters.JobParameters

/**
 * 필수 파라미터가 있는 잡에 incrementer 를 달면 CronJob 이 넘긴 파라미터가 사라진다.
 * Spring Batch 6 의 `JobOperator.start(job, params)` 는 incrementer 가 있으면 넘긴 파라미터를 버리고
 * 다음 인스턴스 파라미터로 시작한다. 그래서 검증기가 잡 시작 전에 실패한다
 * (2026-04-19 ~ 11-10 `battleEventSyncJob` 이 이렇게 모든 실행에서 실패했다).
 */
class JobParametersIncrementerGuardTest(
    private val jobs: List<Job>,
) : CoreBatchContextTest() {
    @Test
    fun `빈 파라미터를 검증기가 거절하는 잡에는 incrementer 가 없다`() {
        // given
        val jobsWithRequiredParameters = jobs.filter { it.rejectsEmptyParameters() }

        // when
        val violations = jobsWithRequiredParameters.filter { it.jobParametersIncrementer != null }

        // then
        assertThat(jobsWithRequiredParameters.map { it.name }).contains(BattleEventSyncJobConfig.JOB_NAME)
        assertThat(violations.map { it.name }).isEmpty()
    }

    private fun Job.rejectsEmptyParameters(): Boolean =
        try {
            jobParametersValidator.validate(JobParameters())
            false
        } catch (_: InvalidJobParametersException) {
            true
        }
}
