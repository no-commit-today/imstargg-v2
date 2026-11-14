package com.imstargg.core.batch.job.statistics.patch

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.sql.Statement

class BatchUpdateCountsTest {
    @Test
    fun `갱신된 줄이 없는 자리를 준다`() {
        // when
        val indexes = BatchUpdateCounts(intArrayOf(1, 0, 1, 0)).zeroIndexes()

        // then
        assertThat(indexes).containsExactly(1, 3)
    }

    @Test
    fun `줄별 결과가 0 이나 1 이 아니면 실패한다`() {
        listOf(
            intArrayOf(1, Statement.SUCCESS_NO_INFO),
            intArrayOf(2),
            intArrayOf(Statement.EXECUTE_FAILED),
        ).forEach { counts ->
            assertThatThrownBy { BatchUpdateCounts(counts).zeroIndexes() }
                .isInstanceOf(IllegalStateException::class.java)
        }
    }

    @Test
    fun `모두 1 이 아니면 실패한다`() {
        assertThatThrownBy { BatchUpdateCounts(intArrayOf(1, 0)).requireAllOne() }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `모두 1 이면 통과한다`() {
        BatchUpdateCounts(intArrayOf(1, 1)).requireAllOne()
    }
}
