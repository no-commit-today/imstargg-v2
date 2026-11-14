package com.imstargg.infrastructure.db.core.brawlstars.patch

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate

class BrawlStarsPatchRegistrationPolicyTest {
    private val today = LocalDate.of(2026, 12, 1)

    @ParameterizedTest
    @CsvSource(
        "2026-12-01, true",
        "2026-11-01, true",
        "2026-10-31, false",
        "2026-12-02, false",
    )
    fun `오늘에서 0일 이상 30일 이하인 날짜만 등록할 수 있다`(
        patchDate: LocalDate,
        expected: Boolean,
    ) {
        assertThat(BrawlStarsPatchRegistrationPolicy.isRegistrable(today, patchDate)).isEqualTo(expected)
    }

    @Test
    fun `등록할 수 있는 가장 이른 날짜는 30일 전이다`() {
        assertThat(BrawlStarsPatchRegistrationPolicy.earliestRegistrableDate(today))
            .isEqualTo(LocalDate.of(2026, 11, 1))
    }
}
