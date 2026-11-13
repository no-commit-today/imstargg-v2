package com.imstargg.infrastructure.db.core.brawlstars.patch

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 패치일을 등록할 수 있는 기한 규칙.
 *
 * 두 번째 스펙은 이 기한 안의 날짜를 원본 전투에서 다시 읽는다.
 * 원본 전투 보관 일수가 이 값보다 길어야 한다.
 */
object BrawlStarsPatchRegistrationPolicy {
    const val REGISTRATION_DEADLINE_DAYS = 30L

    /** 오늘에서 패치일까지가 0일 이상 30일 이하일 때만 참이다. 미래 날짜는 거짓이다. */
    fun isRegistrable(today: LocalDate, patchDate: LocalDate): Boolean {
        val days = ChronoUnit.DAYS.between(patchDate, today)
        return days in 0..REGISTRATION_DEADLINE_DAYS
    }

    fun earliestRegistrableDate(today: LocalDate): LocalDate =
        today.minusDays(REGISTRATION_DEADLINE_DAYS)
}
