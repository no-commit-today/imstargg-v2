package com.imstargg.core.batch.job

import com.imstargg.core.enums.StatsDateRange
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

class MonthPartitionRetentionTest {
    /**
     * 조합 통계 3종(pair·trio·enemy)의 월 파티션은 실행일이 속한 달의 1일에서 한 달을 뺀 날짜(경계) 이전 달을 드롭한다.
     * SQL `drop_old_month_partitions` 의 경계 [PartitionManageJobConfig.monthRetentionCutoff] 가 Kotlin 쪽 쌍둥이다.
     */
    @Test
    fun `11월 1일 실행의 경계는 10월 1일이다 - 9월 파티션은 지우고 10월은 남긴다`() {
        // given
        val date = LocalDate.of(2026, 11, 1)

        // when
        val result = PartitionManageJobConfig.monthRetentionCutoff(date)

        // then
        assertThat(result).isEqualTo(LocalDate.of(2026, 10, 1))
    }

    /**
     * API 가 읽을 수 있는 가장 오래된 날짜는 [StatsDateRange.SINCE_LAST_MONTH] 의 시작일(지난달 1일)이다.
     * 보존 경계가 이보다 늦으면 API 가 조회하는 구간의 파티션이 이미 드롭되어 통계가 조용히 불완전해진다.
     */
    @Test
    fun `보존 경계는 API 최대 조회 범위(SINCE_LAST_MONTH)의 시작일을 넘지 않는다`() {
        // given - 월 경계·연 경계를 모두 포함하는 2년 치 기준일
        val start = LocalDate.of(2026, 1, 1)
        val dates = generateSequence(start) { it.plusDays(1) }.takeWhile { it <= start.plusYears(2) }

        dates.forEach { date ->
            val zone = ZoneId.of("Asia/Seoul")
            val clock = Clock.fixed(date.atStartOfDay(zone).toInstant(), zone)

            // when
            val (apiStart, _) = StatsDateRange.SINCE_LAST_MONTH.dateRange(clock)

            // then
            assertThat(PartitionManageJobConfig.monthRetentionCutoff(date)).isBeforeOrEqualTo(apiStart)
        }
    }
}
