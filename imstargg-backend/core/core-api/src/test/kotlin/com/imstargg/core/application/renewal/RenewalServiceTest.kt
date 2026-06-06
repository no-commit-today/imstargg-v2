package com.imstargg.core.application.renewal

import com.imstargg.CoreApiContextTest
import com.imstargg.core.domain.brawlstars.BrawlStarsTag
import com.imstargg.core.enums.ClubStatus
import com.imstargg.core.enums.PlayerStatus
import com.imstargg.core.enums.RenewalStatus
import com.imstargg.core.enums.RenewalType
import com.imstargg.core.error.CoreException
import com.imstargg.core.error.ErrorType
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsApiMaintenanceEntity
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.ClubJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.repository.BrawlStarsApiMaintenanceJpaRepository
import com.imstargg.infrastructure.db.core.renewal.repository.RenewalJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.ClubEntityFixture.Companion.aClubEntity
import com.imstargg.infrastructure.db.core.test.fixture.PlayerEntityFixture.Companion.aPlayerEntity
import com.imstargg.infrastructure.db.core.test.fixture.RenewalEntityFixture.Companion.aRenewalEntity
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class RenewalServiceTest(
    private val sut: RenewalService,
    private val renewalProducer: RenewalMessageProducer,
    private val cleanUp: CleanUp,
    private val playerJpaRepository: PlayerJpaRepository,
    private val clubJpaRepository: ClubJpaRepository,
    private val renewalJpaRepository: RenewalJpaRepository,
    private val maintenanceJpaRepository: BrawlStarsApiMaintenanceJpaRepository,
) : CoreApiContextTest() {
    @MockkBean
    private lateinit var clock: Clock

    private val fixedInstant: Instant = Instant.parse("2025-01-17T10:00:00Z")
    private val zone: ZoneId = ZoneId.of("Asia/Seoul")
    private val now: Instant get() = fixedInstant

    @BeforeEach
    fun setUp() {
        cleanUp.all()
        every { clock.instant() } returns fixedInstant
        every { clock.zone } returns zone
    }

    @Nested
    inner class RenewPlayer {
        @Test
        fun `삭제된 플레이어 갱신 요청 시 PLAYER_DELETED 예외가 발생한다`() {
            // given
            val tag = BrawlStarsTag("#PDEL")
            playerJpaRepository.save(
                aPlayerEntity().withBrawlStarsTag(tag.value).withStatus(PlayerStatus.DELETED).build(),
            )

            // when
            val ex = catchThrowableOfType(CoreException::class.java) { sut.renewPlayer(tag) }

            // then
            assertThat(ex.errorType).isEqualTo(ErrorType.PLAYER_DELETED)
            verify(exactly = 0) { renewalProducer.producePlayerRenewal(tag) }
        }

        @Test
        fun `점검 중에는 BRAWLSTARS_API_UNDER_MAINTENANCE 예외가 발생한다`() {
            // given
            val tag = BrawlStarsTag("#PMNT")
            maintenanceJpaRepository.save(
                BrawlStarsApiMaintenanceEntity(
                    startTime = now.minus(Duration.ofHours(1)),
                    endTime = now.plus(Duration.ofHours(1)),
                ),
            )

            // when
            val ex = catchThrowableOfType(CoreException::class.java) { sut.renewPlayer(tag) }

            // then
            assertThat(ex.errorType).isEqualTo(ErrorType.BRAWLSTARS_API_UNDER_MAINTENANCE)
            verify(exactly = 0) { renewalProducer.producePlayerRenewal(tag) }
        }

        @Test
        fun `진행 중인 갱신이 있으면 RENEWAL_ALREADY_IN_PROGRESS 예외가 발생한다`() {
            // given - 5초 전 요청된 갱신 (stale threshold 10초 미만)
            val tag = BrawlStarsTag("#PINP")
            renewalJpaRepository.save(
                aRenewalEntity()
                    .withBrawlStarsTag(tag.value)
                    .withType(RenewalType.PLAYER)
                    .withRequestedAt(now.minusSeconds(5))
                    .build(),
            )

            // when
            val ex = catchThrowableOfType(CoreException::class.java) { sut.renewPlayer(tag) }

            // then
            assertThat(ex.errorType).isEqualTo(ErrorType.RENEWAL_ALREADY_IN_PROGRESS)
            verify(exactly = 0) { renewalProducer.producePlayerRenewal(tag) }
        }

        @Test
        fun `기존 갱신이 없으면 PENDING 상태로 저장되고 producer 가 호출된다`() {
            // given
            val tag = BrawlStarsTag("#POK")

            // when
            val result = sut.renewPlayer(tag)

            // then
            assertThat(result.status).isEqualTo(RenewalStatus.PENDING)
            assertThat(result.tag).isEqualTo(tag)
            assertThat(result.requestedAt).isEqualTo(now)

            val saved = renewalJpaRepository.findByBrawlStarsTagAndType(tag.value, RenewalType.PLAYER)
            assertThat(saved).isNotNull
            assertThat(saved!!.status).isEqualTo(RenewalStatus.PENDING)

            verify { renewalProducer.producePlayerRenewal(tag) }
        }
    }

    @Nested
    inner class RenewClub {
        @Test
        fun `삭제된 클럽 갱신 요청 시 CLUB_DELETED 예외가 발생한다`() {
            // given
            val tag = BrawlStarsTag("#CDEL")
            clubJpaRepository.save(
                aClubEntity().withBrawlStarsTag(tag.value).withStatus(ClubStatus.DELETED).build(),
            )

            // when
            val ex = catchThrowableOfType(CoreException::class.java) { sut.renewClub(tag) }

            // then
            assertThat(ex.errorType).isEqualTo(ErrorType.CLUB_DELETED)
            verify(exactly = 0) { renewalProducer.produceClubRenewal(tag) }
        }

        @Test
        fun `기존 갱신이 없으면 PENDING 상태로 저장되고 producer 가 호출된다`() {
            // given
            val tag = BrawlStarsTag("#COK")

            // when
            val result = sut.renewClub(tag)

            // then
            assertThat(result.status).isEqualTo(RenewalStatus.PENDING)
            assertThat(result.tag).isEqualTo(tag)

            val saved = renewalJpaRepository.findByBrawlStarsTagAndType(tag.value, RenewalType.CLUB)
            assertThat(saved).isNotNull
            assertThat(saved!!.status).isEqualTo(RenewalStatus.PENDING)

            verify { renewalProducer.produceClubRenewal(tag) }
        }
    }

    @Nested
    inner class Get {
        @Test
        fun `getPlayerRenewal 은 존재하지 않으면 NOT_FOUND 예외가 발생한다`() {
            // when
            val ex =
                catchThrowableOfType(CoreException::class.java) {
                    sut.getPlayerRenewal(BrawlStarsTag("#NONE"))
                }

            // then
            assertThat(ex.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        fun `getClubRenewal 은 존재하지 않으면 NOT_FOUND 예외가 발생한다`() {
            // when
            val ex =
                catchThrowableOfType(CoreException::class.java) {
                    sut.getClubRenewal(BrawlStarsTag("#NONE"))
                }

            // then
            assertThat(ex.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }
}
