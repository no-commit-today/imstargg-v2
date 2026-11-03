package com.imstargg.client.brawlstars

import com.imstargg.client.brawlstars.response.BattleResponse
import com.imstargg.client.brawlstars.response.BrawlerResponse
import com.imstargg.client.brawlstars.response.ClubMemberResponse
import com.imstargg.client.brawlstars.response.ClubRankingResponse
import com.imstargg.client.brawlstars.response.ClubResponse
import com.imstargg.client.brawlstars.response.EventTypeResponse
import com.imstargg.client.brawlstars.response.ListResponse
import com.imstargg.client.brawlstars.response.PlayerRankingResponse
import com.imstargg.client.brawlstars.response.PlayerResponse
import com.imstargg.client.brawlstars.response.ScheduledEventResponse
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

@HttpExchange
internal interface BrawlStarsApi {
    /**
     * Get list of recent battle results for a player.
     * NOTE: It may take up to 30 minutes for a new battle to appear in the battlelog.
     *
     * @param playerTag Tag of the player.
     */
    @GetExchange("/v1/players/{playerTag}/battlelog")
    fun getLogOfRecentBattlesForAPlayer(
        @PathVariable playerTag: String,
    ): ListResponse<BattleResponse>

    /**
     * Get information about a single player by player tag.
     * Player tags can be found either in game or by from clan member list.
     *
     * @param playerTag Tag of the player.
     */
    @GetExchange("/v1/players/{playerTag}")
    fun getPlayerInformation(
        @PathVariable playerTag: String,
    ): PlayerResponse

    /**
     * List club members.
     *
     * @param clubTag Tag of the club.
     */
    @GetExchange("/v1/clubs/{clubTag}/members")
    fun getListClubMembers(
        @PathVariable clubTag: String,
    ): ListResponse<ClubMemberResponse>

    /**
     * Get information about a single clan by club tag.
     * Club tags can be found in game.
     * Note that clan tags start with hash character '#' and that needs to be URL-encoded properly to work in URL,
     * so for example clan tag '#2ABC' would become '%232ABC' in the URL.
     *
     * @param clubTag Tag of the club.
     */
    @GetExchange("/v1/clubs/{clubTag}")
    fun getClubInformation(
        @PathVariable clubTag: String,
    ): ClubResponse

    /**
     * Get club rankings for a country or global rankings.
     *
     * @param countryCode Two letter country code, or 'global' for global rankings.
     */
    @GetExchange("/v1/rankings/{countryCode}/clubs")
    fun getClubRankingsForACountryOrGlobalRankings(
        @PathVariable countryCode: String,
    ): ListResponse<ClubRankingResponse>

    /**
     * Get brawler rankings for a country or global rankings.
     * Brawler identifiers can be found by using the /v1/brawlers API endpoint.
     *
     * @param countryCode Two letter country code, or 'global' for global rankings.
     * @param brawlerId Identifier of the brawler.
     */
    @GetExchange("/v1/rankings/{countryCode}/brawlers/{brawlerId}")
    fun getBrawlerRankingsForACountryOrGlobalRankings(
        @PathVariable countryCode: String,
        @PathVariable brawlerId: Long,
    ): ListResponse<PlayerRankingResponse>

    /**
     * Get player rankings for a country or global rankings.
     *
     * @param countryCode Two letter country code, or 'global' for global rankings.
     */
    @GetExchange("/v1/rankings/{countryCode}/players")
    fun getPlayerRankingsForACountryOrGlobalRankings(
        @PathVariable countryCode: String,
    ): ListResponse<PlayerRankingResponse>

    /**
     * Get list of available brawlers.
     */
    @GetExchange("/v1/brawlers")
    fun getListOfAvailableBrawlers(): ListResponse<BrawlerResponse>

    /**
     * Get information about a brawler.
     */
    @GetExchange("/v1/brawlers/{brawlerId}")
    fun getInformationAboutABrawler(
        @PathVariable brawlerId: Long,
    ): BrawlerResponse

    /**
     * Get event rotation for ongoing events.
     */
    @GetExchange("/v1/events/rotation")
    fun getEventRotation(): List<ScheduledEventResponse>

    /**
     * Get list of available game modes.
     */
    @GetExchange("/v1/gamemodes")
    fun getGameModes(): ListResponse<EventTypeResponse>

    /**
     * Get the API specification (swagger 2.0 YAML).
     * NOTE: The response content type is application/octet-stream.
     */
    @GetExchange("/v1")
    fun getApiSpec(): String
}
