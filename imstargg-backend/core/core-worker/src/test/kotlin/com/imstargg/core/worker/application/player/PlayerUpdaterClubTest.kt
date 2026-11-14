package com.imstargg.core.worker.application.player

import com.imstargg.CoreWorkerContextTest
import com.imstargg.client.brawlstars.response.BrawlerStatResponse
import com.imstargg.client.brawlstars.response.PlayerClubResponse
import com.imstargg.client.brawlstars.response.PlayerIconResponse
import com.imstargg.client.brawlstars.response.PlayerResponse
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/**
 * 클럽이 없는 플레이어는 API 가 `"club": {}` 로 내려준다 — tag·name 이 없는 club 객체는 클럽 없음으로 저장한다.
 */
class PlayerUpdaterClubTest(
    private val sut: PlayerUpdater,
    private val cleanUp: CleanUp,
    private val playerRepository: PlayerJpaRepository,
) : CoreWorkerContextTest() {
    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `빈 club 객체면 클럽 없음으로 저장한다`() {
        // given
        val tag = "#CLUB1"
        val response = createPlayerResponse(tag, club = PlayerClubResponse(tag = null, name = null))

        // when
        sut.update(response, emptyList())

        // then
        assertThat(playerRepository.findByBrawlStarsTag(tag)!!.club).isNull()
    }

    @Test
    fun `빈 club 객체로 갱신하면 기존 클럽 소속을 지운다`() {
        // given
        val tag = "#CLUB2"
        sut.update(createPlayerResponse(tag, club = PlayerClubResponse(tag = "#CLUBTEST", name = "Test Club")), emptyList())
        assertThat(playerRepository.findByBrawlStarsTag(tag)!!.club!!.brawlStarsTag).isEqualTo("#CLUBTEST")

        // when
        sut.update(createPlayerResponse(tag, club = PlayerClubResponse(tag = null, name = null)), emptyList())

        // then
        assertThat(playerRepository.findByBrawlStarsTag(tag)!!.club).isNull()
    }

    private fun createPlayerResponse(
        tag: String,
        club: PlayerClubResponse?,
    ) = PlayerResponse(
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
        club = club,
        brawlers = emptyList<BrawlerStatResponse>(),
    )
}
