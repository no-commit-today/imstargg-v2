package com.imstargg.core.application.renewal

import com.imstargg.core.application.brawlstars.BrawlStarsApiMaintenanceRepository
import com.imstargg.core.application.brawlstars.ClubRepository
import com.imstargg.core.application.brawlstars.PlayerRepository
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.domain.renewal.RenewalFixture.Companion.aRenewal
import com.imstargg.core.enums.RenewalStatus
import com.imstargg.core.enums.RenewalType
import com.imstargg.core.error.CoreException
import com.imstargg.core.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class RenewalServiceTest {
    private val renewalRepository = mockk<RenewalRepository>()
    private val renewalProducer = mockk<RenewalMessageProducer>(relaxed = true)
    private val brawlStarsApiMaintenanceRepository = mockk<BrawlStarsApiMaintenanceRepository>()
    private val playerRepository = mockk<PlayerRepository>()
    private val clubRepository = mockk<ClubRepository>()
    private val clock = Clock.fixed(Instant.parse("2025-01-17T10:00:00Z"), ZoneOffset.UTC)

    private val sut =
        RenewalService(
            renewalRepository = renewalRepository,
            renewalProducer = renewalProducer,
            brawlStarsApiMaintenanceRepository = brawlStarsApiMaintenanceRepository,
            playerRepository = playerRepository,
            clubRepository = clubRepository,
            clock = clock,
        )

    @Nested
    inner class RenewPlayer {
        @Test
        fun `삭제된 플레이어에 대한 갱신 요청 시 PLAYER_DELETED 예외가 발생한다`() {
            // given
            val tag = BrawlStarsTag("#TEST")
            every { playerRepository.isDeleted(tag) } returns true

            // when
            val ex = catchThrowableOfType(CoreException::class.java) { sut.renewPlayer(tag) }

            // then
            assertThat(ex.errorType).isEqualTo(ErrorType.PLAYER_DELETED)
        }

        @Test
        fun `존재하지 않는 플레이어에 대한 갱신 요청은 정상 처리된다`() {
            // given
            val tag = BrawlStarsTag("#TEST")
            val renewal =
                aRenewal()
                    .withTag(tag)
                    .withType(RenewalType.PLAYER)
                    .withStatus(RenewalStatus.PENDING)
                    .build()
            every { playerRepository.isDeleted(tag) } returns false
            every { brawlStarsApiMaintenanceRepository.isUnderMaintenance() } returns false
            every { renewalRepository.find(tag, RenewalType.PLAYER) } returns null
            every { renewalRepository.request(tag, RenewalType.PLAYER, any()) } returns renewal
            every { renewalRepository.toPending(renewal) } returns renewal

            // when
            val result = sut.renewPlayer(tag)

            // then
            assertThat(result).isNotNull
            verify { renewalProducer.producePlayerRenewal(tag) }
        }
    }

    @Nested
    inner class RenewClub {
        @Test
        fun `삭제된 클럽에 대한 갱신 요청 시 CLUB_DELETED 예외가 발생한다`() {
            // given
            val tag = BrawlStarsTag("#TEST")
            every { clubRepository.isDeleted(tag) } returns true

            // when
            val ex = catchThrowableOfType(CoreException::class.java) { sut.renewClub(tag) }

            // then
            assertThat(ex.errorType).isEqualTo(ErrorType.CLUB_DELETED)
        }

        @Test
        fun `존재하지 않는 클럽에 대한 갱신 요청은 정상 처리된다`() {
            // given
            val tag = BrawlStarsTag("#TEST")
            val renewal =
                aRenewal()
                    .withTag(tag)
                    .withType(RenewalType.CLUB)
                    .withStatus(RenewalStatus.PENDING)
                    .build()
            every { clubRepository.isDeleted(tag) } returns false
            every { brawlStarsApiMaintenanceRepository.isUnderMaintenance() } returns false
            every { renewalRepository.find(tag, RenewalType.CLUB) } returns null
            every { renewalRepository.request(tag, RenewalType.CLUB, any()) } returns renewal
            every { renewalRepository.toPending(renewal) } returns renewal

            // when
            val result = sut.renewClub(tag)

            // then
            assertThat(result).isNotNull
            verify { renewalProducer.produceClubRenewal(tag) }
        }
    }
}
