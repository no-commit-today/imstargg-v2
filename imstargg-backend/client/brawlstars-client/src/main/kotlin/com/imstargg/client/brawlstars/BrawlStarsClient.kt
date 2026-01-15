package com.imstargg.client.brawlstars

import com.imstargg.client.brawlstars.request.PagingParam
import com.imstargg.client.brawlstars.response.BattleResponse
import com.imstargg.client.brawlstars.response.BrawlerResponse
import com.imstargg.client.brawlstars.response.ClubRankingResponse
import com.imstargg.client.brawlstars.response.ClubResponse
import com.imstargg.client.brawlstars.response.ListResponse
import com.imstargg.client.brawlstars.response.PlayerRankingResponse
import com.imstargg.client.brawlstars.response.PlayerResponse
import com.imstargg.client.brawlstars.response.ScheduledEventResponse
import feign.FeignException

class BrawlStarsClient internal constructor(
    private val brawlStarsApi: BrawlStarsApi,
) {
    fun getPlayerRecentBattles(playerTag: String): ListResponse<BattleResponse> =
        execute("playerTag=$playerTag") { brawlStarsApi.getLogOfRecentBattlesForAPlayer(playerTag) }

    fun getPlayerInformation(playerTag: String): PlayerResponse =
        execute("playerTag=$playerTag") {
            brawlStarsApi.getPlayerInformation(playerTag)
        }

    fun getBrawlers(): ListResponse<BrawlerResponse> =
        execute("getBrawlers") { brawlStarsApi.getListOfAvailableBrawlers(PagingParam.DEFAULT) }

    fun getEventRotation(): List<ScheduledEventResponse> = execute("getEventRotation") { brawlStarsApi.getEventRotation() }

    fun getPlayerRanking(country: String): ListResponse<PlayerRankingResponse> =
        execute("country=$country") {
            brawlStarsApi.getPlayerRankingsForACountryOrGlobalRankings(country, PagingParam.DEFAULT)
        }

    fun getBrawlerRanking(
        country: String,
        brawlerId: Long,
    ): ListResponse<PlayerRankingResponse> =
        execute("country=$country, brawlerId=$brawlerId") {
            brawlStarsApi.getBrawlerRankingsForACountryOrGlobalRankings(country, brawlerId, PagingParam.DEFAULT)
        }

    fun getClub(clubTag: String): ClubResponse = execute("clubTag=$clubTag") { brawlStarsApi.getClubInformation(clubTag) }

    fun getClubRanking(country: String): ListResponse<ClubRankingResponse> =
        execute("country=$country") {
            brawlStarsApi.getClubRankingsForACountryOrGlobalRankings(country, PagingParam.DEFAULT)
        }

    private fun <T> execute(
        message: String,
        block: () -> T,
    ): T {
        try {
            return block()
        } catch (ex: FeignException) {
            throw BrawlStarsApiException.from(message, ex)
        }
    }
}
