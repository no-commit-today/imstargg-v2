package com.imstargg.core.application.renewal

import com.imstargg.core.application.brawlstars.BrawlStarsApiMaintenanceRepository
import com.imstargg.core.application.brawlstars.ClubRepository
import com.imstargg.core.application.brawlstars.PlayerRepository
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.error.CoreException
import com.imstargg.core.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * 최초 요청 동시 경합(uk_renewal__brawlstarstag_type 위반)의 409 매핑 검증.
 * 실제 동시성은 재현 불가하므로 이 케이스에 한해 리포지토리를 stub 한다.
 */
class RenewalServiceRaceTest {
    @Test
    fun `동시 최초 요청 경합의 제약 위반은 RENEWAL_ALREADY_IN_PROGRESS 로 매핑된다`() {
        // given
        val tag = BrawlStarsTag("#RACE")
        val renewalRepository = mockk<RenewalRepository>()
        val renewalProducer = mockk<RenewalMessageProducer>(relaxed = true)
        val maintenanceRepository = mockk<BrawlStarsApiMaintenanceRepository>()
        val playerRepository = mockk<PlayerRepository>()
        every { maintenanceRepository.isUnderMaintenance() } returns false
        every { playerRepository.isDeleted(tag) } returns false
        every { renewalRepository.request(tag, any(), any()) } throws DataIntegrityViolationException("duplicate key")
        val sut =
            RenewalService(
                renewalRepository = renewalRepository,
                renewalProducer = renewalProducer,
                brawlStarsApiMaintenanceRepository = maintenanceRepository,
                playerRepository = playerRepository,
                clubRepository = mockk<ClubRepository>(),
                clock = Clock.fixed(Instant.parse("2026-07-08T00:00:00Z"), ZoneId.of("Asia/Seoul")),
            )

        // when
        val ex = catchThrowableOfType(CoreException::class.java) { sut.renewPlayer(tag) }

        // then
        assertThat(ex.errorType).isEqualTo(ErrorType.RENEWAL_ALREADY_IN_PROGRESS)
        verify(exactly = 0) { renewalProducer.producePlayerRenewal(tag) }
    }
}
