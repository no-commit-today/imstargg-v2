package com.imstargg.core.batch.job.statistics.patch

import com.imstargg.CoreBatchContextTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.parameters.JobParameters
import org.springframework.batch.core.job.parameters.RunIdIncrementer
import org.springframework.context.ApplicationContext

class PatchStatsJobConfigTest(
    private val context: ApplicationContext,
) : CoreBatchContextTest() {
    // 이름 표를 그대로 옮겨 적는다. 상수를 참조하면 상수가 틀려도 테스트가 통과한다.
    private val jobNames =
        listOf(
            "brawlerBattleResultPatchStatsJob",
            "brawlerBattleRankPatchStatsJob",
            "brawlerEnemyBattleResultPatchStatsJob",
            "brawlerPairBattleResultPatchStatsJob",
            "brawlerTrioBattleResultPatchStatsJob",
        )

    @Test
    fun `패치 통계 잡 다섯 개가 이름표의 이름으로 있다`() {
        // when
        val jobs = context.getBeansOfType(Job::class.java)

        // then
        assertThat(jobs.keys).containsAll(jobNames)
        jobNames.forEach { assertThat(jobs.getValue(it).name).isEqualTo(it) }
    }

    @Test
    fun `파라미터 없이 시작할 수 있고 실행마다 새 인스턴스가 된다`() {
        jobNames.forEach { jobName ->
            // given
            val job = context.getBean(jobName, Job::class.java)

            // when
            val validation = runCatching { job.jobParametersValidator.validate(JobParameters()) }

            // then
            assertThat(validation.isSuccess).`as`(jobName).isTrue()
            assertThat(job.jobParametersIncrementer).`as`(jobName).isInstanceOf(RunIdIncrementer::class.java)
        }
    }
}
