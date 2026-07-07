package com.imstargg.core.worker.application

import com.imstargg.CoreWorkerContextTest
import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.client.brawlstars.response.BrawlerStatResponse
import com.imstargg.client.brawlstars.response.ListResponse
import com.imstargg.client.brawlstars.response.PlayerClubResponse
import com.imstargg.client.brawlstars.response.PlayerIconResponse
import com.imstargg.client.brawlstars.response.PlayerResponse
import com.imstargg.core.enums.RenewalStatus
import com.imstargg.core.enums.RenewalType
import com.imstargg.infrastructure.db.core.renewal.repository.RenewalJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.RenewalEntityFixture.Companion.aRenewalEntity
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class PlayerRenewalServiceTest(
    private val sut: PlayerRenewalService,
    private val brawlStarsClient: BrawlStarsClient,
    private val renewalRepository: RenewalJpaRepository,
    private val cleanUp: CleanUp,
) : CoreWorkerContextTest() {
    @MockkBean
    private lateinit var statsBattleAggregator: StatsBattleAggregator

    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `통계 집계가 실패해도 renewal 은 COMPLETED 를 유지한다`() {
        // given
        val tag = "#RENEW1"
        val renewal = aRenewalEntity().withBrawlStarsTag(tag).build()
        renewal.toPending()
        renewalRepository.save(renewal)

        every { brawlStarsClient.getPlayerInformation(tag) } returns createPlayerResponse(tag)
        every { brawlStarsClient.getPlayerRecentBattles(tag) } returns
            ListResponse(items = emptyList(), paging = null)
        every { statsBattleAggregator.aggregate(any()) } throws IllegalStateException("집계 실패")

        // when
        sut.renew(tag)

        // then
        val saved = renewalRepository.findByBrawlStarsTagAndType(tag, RenewalType.PLAYER)!!
        assertThat(saved.status).isEqualTo(RenewalStatus.COMPLETED)
    }

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
