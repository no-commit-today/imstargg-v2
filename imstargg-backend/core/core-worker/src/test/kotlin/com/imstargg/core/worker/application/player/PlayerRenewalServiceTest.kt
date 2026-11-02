package com.imstargg.core.worker.application.player

import com.imstargg.CoreWorkerContextTest
import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.client.brawlstars.response.BrawlerStatResponse
import com.imstargg.client.brawlstars.response.ListResponse
import com.imstargg.client.brawlstars.response.PlayerClubResponse
import com.imstargg.client.brawlstars.response.PlayerIconResponse
import com.imstargg.client.brawlstars.response.PlayerResponse
import com.imstargg.core.enums.PlayerStatus
import com.imstargg.core.enums.RenewalStatus
import com.imstargg.core.enums.RenewalType
import com.imstargg.core.worker.application.stats.StatsBattleAggregator
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.system.repository.BrawlStarsApiMaintenanceJpaRepository
import com.imstargg.infrastructure.db.core.renewal.repository.RenewalJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.PlayerEntityFixture.Companion.aPlayerEntity
import com.imstargg.infrastructure.db.core.test.fixture.RenewalEntityFixture.Companion.aRenewalEntity
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException

class PlayerRenewalServiceTest(
    private val sut: PlayerRenewalService,
    private val brawlStarsClient: BrawlStarsClient,
    private val renewalRepository: RenewalJpaRepository,
    private val playerRepository: PlayerJpaRepository,
    private val brawlStarsApiMaintenanceRepository: BrawlStarsApiMaintenanceJpaRepository,
    private val cleanUp: CleanUp,
) : CoreWorkerContextTest() {
    @MockkBean
    private lateinit var statsBattleAggregator: StatsBattleAggregator

    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `통계 집계 태스크 제출이 실패해도 renewal 은 COMPLETED 를 유지한다`() {
        // given
        val tag = "#RENEW1"
        val renewal = aRenewalEntity().withBrawlStarsTag(tag).build()
        renewal.toPending()
        renewalRepository.save(renewal)

        every { brawlStarsClient.getPlayerInformation(tag) } returns createPlayerResponse(tag)
        every { brawlStarsClient.getPlayerRecentBattles(tag) } returns
            ListResponse(items = emptyList(), paging = null)
        every { statsBattleAggregator.asyncAggregate(any()) } throws IllegalStateException("제출 실패")

        // when
        sut.renew(tag)

        // then
        val saved = renewalRepository.findByBrawlStarsTagAndType(tag, RenewalType.PLAYER)!!
        assertThat(saved.status).isEqualTo(RenewalStatus.COMPLETED)
    }

    @Test
    fun `플레이어 조회가 404 면 renewal 을 NOT_FOUND 로 끝내고 플레이어를 DELETED 로 바꾼다`() {
        // given
        val tag = "#RENEW404"
        playerRepository.save(aPlayerEntity().withBrawlStarsTag(tag).build())
        val renewal = aRenewalEntity().withBrawlStarsTag(tag).build()
        renewal.toPending()
        renewalRepository.save(renewal)

        every { brawlStarsClient.getPlayerInformation(tag) } throws notFound()

        // when
        sut.renew(tag)

        // then
        val saved = renewalRepository.findByBrawlStarsTagAndType(tag, RenewalType.PLAYER)!!
        assertThat(saved.status).isEqualTo(RenewalStatus.NOT_FOUND)
        assertThat(playerRepository.findByBrawlStarsTag(tag)!!.status).isEqualTo(PlayerStatus.DELETED)
    }

    @Test
    fun `503 이면 점검을 등록하고 renewal 을 FAILED 로 끝낸다`() {
        // given
        val tag = "#RENEW503"
        val renewal = aRenewalEntity().withBrawlStarsTag(tag).build()
        renewal.toPending()
        renewalRepository.save(renewal)

        every { brawlStarsClient.getPlayerInformation(tag) } throws serviceUnavailable()

        // when
        sut.renew(tag)

        // then
        val saved = renewalRepository.findByBrawlStarsTagAndType(tag, RenewalType.PLAYER)!!
        assertThat(saved.status).isEqualTo(RenewalStatus.FAILED)
        assertThat(brawlStarsApiMaintenanceRepository.count()).isEqualTo(1)
    }

    private fun notFound(): HttpClientErrorException =
        HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders(), ByteArray(0), null)

    private fun serviceUnavailable(): HttpServerErrorException =
        HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", HttpHeaders(), ByteArray(0), null)

    private fun createPlayerResponse(tag: String) =
        PlayerResponse(
            tag = tag,
            name = "Test",
            nameColor = "0xff0000ff",
            icon = PlayerIconResponse(id = 1L),
            trophies = 1000,
            highestTrophies = 1200,
            expLevel = 50,
            expPoints = 5000,
            isQualifiedFromChampionshipChallenge = false,
            victories3vs3 = 10,
            victoriesSolo = 5,
            victoriesDuo = 3,
            bestRoboRumbleTime = 0,
            bestTimeAsBigBrawler = 0,
            totalPrestigeLevel = 0,
            club = PlayerClubResponse(tag = "#CLUBTEST", name = "Test Club"),
            brawlers = emptyList<BrawlerStatResponse>(),
        )
}
