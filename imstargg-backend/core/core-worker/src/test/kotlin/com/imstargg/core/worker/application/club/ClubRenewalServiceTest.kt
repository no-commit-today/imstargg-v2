package com.imstargg.core.worker.application.club

import com.imstargg.CoreWorkerContextTest
import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.core.enums.ClubStatus
import com.imstargg.core.enums.RenewalStatus
import com.imstargg.core.enums.RenewalType
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.ClubJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.system.repository.BrawlStarsApiMaintenanceJpaRepository
import com.imstargg.infrastructure.db.core.renewal.repository.RenewalJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.ClubEntityFixture.Companion.aClubEntity
import com.imstargg.infrastructure.db.core.test.fixture.RenewalEntityFixture.Companion.aRenewalEntity
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException

class ClubRenewalServiceTest(
    private val sut: ClubRenewalService,
    private val brawlStarsClient: BrawlStarsClient,
    private val clubRepository: ClubJpaRepository,
    private val renewalRepository: RenewalJpaRepository,
    private val brawlStarsApiMaintenanceRepository: BrawlStarsApiMaintenanceJpaRepository,
    private val cleanUp: CleanUp,
) : CoreWorkerContextTest() {
    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `클럽 조회가 404 면 renewal 을 NOT_FOUND 로 끝내고 클럽을 DELETED 로 바꾼다`() {
        // given
        val tag = "#CLUB404"
        clubRepository.save(aClubEntity().withBrawlStarsTag(tag).build())
        val renewal = aRenewalEntity().withBrawlStarsTag(tag).withType(RenewalType.CLUB).build()
        renewal.toPending()
        renewalRepository.save(renewal)

        every { brawlStarsClient.getClub(tag) } throws notFound()

        // when
        sut.renew(tag)

        // then
        val saved = renewalRepository.findByBrawlStarsTagAndType(tag, RenewalType.CLUB)!!
        assertThat(saved.status).isEqualTo(RenewalStatus.NOT_FOUND)
        assertThat(clubRepository.findByBrawlStarsTag(tag)!!.status).isEqualTo(ClubStatus.DELETED)
    }

    @Test
    fun `503 이면 점검을 등록하고 renewal 을 FAILED 로 끝낸다`() {
        // given
        val tag = "#CLUB503"
        val renewal = aRenewalEntity().withBrawlStarsTag(tag).withType(RenewalType.CLUB).build()
        renewal.toPending()
        renewalRepository.save(renewal)

        every { brawlStarsClient.getClub(tag) } throws serviceUnavailable()

        // when
        sut.renew(tag)

        // then
        val saved = renewalRepository.findByBrawlStarsTagAndType(tag, RenewalType.CLUB)!!
        assertThat(saved.status).isEqualTo(RenewalStatus.FAILED)
        assertThat(brawlStarsApiMaintenanceRepository.count()).isEqualTo(1)
    }

    private fun notFound(): HttpClientErrorException =
        HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders(), ByteArray(0), null)

    private fun serviceUnavailable(): HttpServerErrorException =
        HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", HttpHeaders(), ByteArray(0), null)
}
