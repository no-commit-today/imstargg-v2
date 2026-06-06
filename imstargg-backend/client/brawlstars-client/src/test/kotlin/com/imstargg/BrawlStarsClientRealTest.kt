package com.imstargg

import com.imstargg.client.brawlstars.BrawlStarsClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.TestPropertySource

@TestPropertySource(
    properties = [
        "app.client.brawlstars.api-keys=",
    ],
)
class BrawlStarsClientRealTest(
    private val brawlStarsClient: BrawlStarsClient,
) : BrawlStarsClientDevelopTest() {
    companion object {
        const val PLAYER_TAG = "%2388J8Q9RPV"
        const val CLUB_TAG = "%232U8CVLQQ9"
        const val COUNTRY = "kr"
        const val BRAWLER_ID = 16000000L
    }

    @Test
    fun getPlayerRecentBattles() {
        val result = brawlStarsClient.getPlayerRecentBattles(PLAYER_TAG)
        println(result)
    }

    @Test
    fun getPlayerInformation() {
        val result = brawlStarsClient.getPlayerInformation(PLAYER_TAG)
        println(result)
    }

    @Test
    fun getBrawlers() {
        val result = brawlStarsClient.getBrawlers()
        println(result)
    }

    @Test
    fun getEventRotation() {
        val result = brawlStarsClient.getEventRotation()
        println(result)
    }

    @Test
    fun getPlayerRanking() {
        val result = brawlStarsClient.getPlayerRanking(COUNTRY)
        println(result)
    }

    @Test
    fun getBrawlerRanking() {
        val result = brawlStarsClient.getBrawlerRanking(COUNTRY, BRAWLER_ID)
        println(result)
    }

    @Test
    fun getClub() {
        val result = brawlStarsClient.getClub(CLUB_TAG)
        println(result)
    }

    @Test
    fun getClubRanking() {
        val result = brawlStarsClient.getClubRanking(COUNTRY)
        println(result)
    }

    @Test
    fun getApiSpec() {
        val result = brawlStarsClient.getApiSpec()
        println(result.take(500))
        assertThat(result).contains("swagger")
    }
}
