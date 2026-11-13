package com.imstargg.core.batch.job

import com.imstargg.CoreBatchContextTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.batch.core.job.Job
import org.springframework.batch.core.job.JobExecution
import org.springframework.batch.core.job.JobInstance
import org.springframework.batch.core.job.SimpleJob
import org.springframework.batch.core.job.parameters.JobParameters
import org.springframework.batch.core.scope.context.JobSynchronizationManager
import org.springframework.beans.factory.annotation.Qualifier

class PartitionManageJobConfigTest(
    @param:Qualifier(PartitionManageJobConfig.JOB_NAME) private val job: Job,
) : CoreBatchContextTest() {
    @Test
    fun `원본 전투의 일 파티션만 관리한다`() {
        // 스텝이 잡 스코프 빈이라 이름을 읽으려면 잡 실행 컨텍스트가 필요하다.
        val jobExecution = JobExecution(1L, JobInstance(1L, job.name), JobParameters())
        val stepNames =
            JobSynchronizationManager.register(jobExecution).let {
                try {
                    (job as SimpleJob).stepNames
                } finally {
                    JobSynchronizationManager.close()
                }
            }

        assertThat(stepNames).containsExactly("partitionCreateStep", "partitionDropStep")
    }
}
