package com.imstargg.core.batch.job

import com.imstargg.core.enums.StatsWeekRange
import com.imstargg.infrastructure.db.core.brawlstars.patch.BrawlStarsPatchRegistrationPolicy
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class PartitionRetentionLookbackTest {
    /**
     * 주간 통계 잡(pair/enemy)은 date 파라미터(D = 실행일 T-1) 기준
     * [StatsWeekRange.currentAndPrevious] 범위의 원본 배틀 파티션을 재집계한다.
     * 파티션 삭제 잡은 실행일 T 기준 [PartitionManageJobConfig.RETENTION_DAYS] 이전을 드롭하므로,
     * 이 관계가 깨지면 주간 통계가 드롭된 파티션 구간만큼 조용히 불완전해진다.
     */
    @Test
    fun `주간 통계 최대 룩백이 파티션 보존기간 안에 있다`() {
        // given - 월 경계·연 경계를 모두 포함하는 2년 치 기준일
        val start = LocalDate.of(2026, 1, 1)
        val dates = generateSequence(start) { it.plusDays(1) }.takeWhile { it <= start.plusYears(2) }

        // when
        val maxLookbackFromDate =
            dates.maxOf { date ->
                val earliestStart = StatsWeekRange.currentAndPrevious(date).minOf { it.start }
                ChronoUnit.DAYS.between(earliestStart, date)
            }

        // then - 실행일 보정(+1일) + 안전 여유(+1일)까지 보존기간이 커버해야 한다
        val executionDateOffset = 1L
        val safetyMarginDays = 1L
        assertThat(maxLookbackFromDate + executionDateOffset + safetyMarginDays)
            .isLessThanOrEqualTo(PartitionManageJobConfig.RETENTION_DAYS.toLong())
    }

    /**
     * 범위 맞추기는 패치일 등록 기한 안의 날짜를 원본에서 다시 읽는다.
     * 원본 보관이 기한보다 짧으면 기한 안에 등록된 패치의 통계가 지워진 원본으로 조용히 불완전해진다.
     */
    @Test
    fun `원본 보관 일수가 패치일 등록 기한보다 길다`() {
        assertThat(PartitionManageJobConfig.RETENTION_DAYS.toLong())
            .isGreaterThan(BrawlStarsPatchRegistrationPolicy.REGISTRATION_DEADLINE_DAYS)
    }
}
