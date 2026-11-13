package com.imstargg.core.batch.domain.statistics.patch

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PatchDateRangeTest {
    private fun d(day: Int): LocalDate = LocalDate.of(2026, 11, day)

    @Test
    fun `패치일 다음 날부터 다음 패치일 전날까지다`() {
        // when
        val ranges = PatchDateRange.resolveAll(mapOf(1L to d(3), 2L to d(11)))

        // then
        assertThat(ranges[1L]).isEqualTo(PatchDateRange(d(4), d(10)))
    }

    @Test
    fun `마지막 패치는 끝이 없다`() {
        // when
        val ranges = PatchDateRange.resolveAll(mapOf(1L to d(3), 2L to d(11)))

        // then
        assertThat(ranges[2L]).isEqualTo(PatchDateRange(d(12), null))
    }

    @Test
    fun `패치일이 연이으면 앞 패치의 범위는 비어 있다`() {
        // when
        val ranges = PatchDateRange.resolveAll(mapOf(1L to d(10), 2L to d(11)))

        // then
        assertThat(ranges[1L]).isEqualTo(PatchDateRange(d(11), d(10)))
        assertThat(ranges.getValue(1L).isEmpty).isTrue()
        assertThat(ranges.getValue(2L).isEmpty).isFalse()
    }

    @Test
    fun `패치 당일은 어느 범위에도 없다`() {
        // when
        val ranges = PatchDateRange.resolveAll(mapOf(1L to d(3), 2L to d(11)))

        // then
        assertThat(ranges.values).allSatisfy {
            assertThat(it.contains(d(11))).isFalse()
            assertThat(it.contains(d(3))).isFalse()
        }
    }

    @Test
    fun `가장 이른 패치보다 앞선 날짜는 어느 범위에도 없다`() {
        // when
        val ranges = PatchDateRange.resolveAll(mapOf(1L to d(3), 2L to d(11)))

        // then
        assertThat(ranges.values).allSatisfy {
            assertThat(it.contains(d(2))).isFalse()
        }
    }

    @Test
    fun `끝이 없는 범위는 시작 뒤의 모든 날짜를 담는다`() {
        // given
        val range = PatchDateRange(d(12), null)

        // then
        assertThat(range.contains(d(11))).isFalse()
        assertThat(range.contains(d(12))).isTrue()
        assertThat(range.contains(LocalDate.of(2099, 1, 1))).isTrue()
    }

    @Test
    fun `끝이 있는 범위는 시작과 끝을 포함한다`() {
        // given
        val range = PatchDateRange(d(4), d(10))

        // then
        assertThat(range.contains(d(4))).isTrue()
        assertThat(range.contains(d(10))).isTrue()
        assertThat(range.contains(d(11))).isFalse()
    }

    @Test
    fun `패치가 없으면 범위도 없다`() {
        // when
        val ranges = PatchDateRange.resolveAll(emptyMap())

        // then
        assertThat(ranges).isEmpty()
    }

    @Test
    fun `입력 순서와 무관하다`() {
        // given
        val patchDates = linkedMapOf(1L to d(3), 2L to d(11), 3L to d(20))
        val reversed = linkedMapOf(3L to d(20), 2L to d(11), 1L to d(3))

        // then
        assertThat(PatchDateRange.resolveAll(reversed)).isEqualTo(PatchDateRange.resolveAll(patchDates))
    }
}
