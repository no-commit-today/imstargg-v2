package com.imstargg.client.brawlstars

import com.imstargg.client.brawlstars.request.PagingParam
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
import io.github.resilience4j.ratelimiter.annotation.RateLimiter
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.cloud.openfeign.SpringQueryMap
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@FeignClient(
    value = "brawlstars",
    url = "\${app.client.brawlstars.url}",
    configuration = [BrawlStarsApiConfig::class],
)
@RateLimiter(name = "brawlstars")
internal interface BrawlStarsApi {
    /**
     * Get list of recent battle results for a player.
     * NOTE: It may take up to 30 minutes for a new battle to appear in the battlelog.
     *
     * @param playerTag Tag of the player.
     */
    @GetMapping(value = ["/v1/players/{playerTag}/battlelog"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun getLogOfRecentBattlesForAPlayer(
        @PathVariable playerTag: String,
    ): ListResponse<BattleResponse>

    /**
     * Get information about a single player by player tag.
     * Player tags can be found either in game or by from clan member list.
     *
     * @param playerTag Tag of the player.
     */
    @GetMapping(value = ["/v1/players/{playerTag}"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun getPlayerInformation(
        @PathVariable playerTag: String,
    ): PlayerResponse

    /**
     * List club members.
     *
     * @param clubTag Tag of the club.
     */
    @GetMapping(value = ["/v1/clubs/{clubTag}/members"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun getListClubMembers(
        @PathVariable clubTag: String,
        @SpringQueryMap paging: PagingParam,
    ): ListResponse<ClubMemberResponse>

    /**
     * Get information about a single clan by club tag.
     * Club tags can be found in game.
     * Note that clan tags start with hash character '#' and that needs to be URL-encoded properly to work in URL,
     * so for example clan tag '#2ABC' would become '%232ABC' in the URL.
     *
     * @param clubTag Tag of the club.
     */
    @GetMapping(value = ["/v1/clubs/{clubTag}"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun getClubInformation(
        @PathVariable clubTag: String,
    ): ClubResponse

    /**
     * Get club rankings for a country or global rankings.
     *
     * @param countryCode Two letter country code, or 'global' for global rankings.
     */
    @GetMapping(value = ["/v1/rankings/{countryCode}/clubs"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun getClubRankingsForACountryOrGlobalRankings(
        @PathVariable countryCode: String,
        @SpringQueryMap paging: PagingParam,
    ): ListResponse<ClubRankingResponse>

    /**
     * Get brawler rankings for a country or global rankings.
     * Brawler identifiers can be found by using the /v1/brawlers API endpoint.
     *
     * @param countryCode Two letter country code, or 'global' for global rankings.
     * @param brawlerId Identifier of the brawler.
     */
    @GetMapping(value = ["/v1/rankings/{countryCode}/brawlers/{brawlerId}"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun getBrawlerRankingsForACountryOrGlobalRankings(
        @PathVariable countryCode: String,
        @PathVariable brawlerId: Long,
        @SpringQueryMap paging: PagingParam,
    ): ListResponse<PlayerRankingResponse>

    /**
     * Get player rankings for a country or global rankings.
     *
     * @param countryCode Two letter country code, or 'global' for global rankings.
     */
    @GetMapping(value = ["/v1/rankings/{countryCode}/players"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun getPlayerRankingsForACountryOrGlobalRankings(
        @PathVariable countryCode: String,
        @SpringQueryMap paging: PagingParam,
    ): ListResponse<PlayerRankingResponse>

    /**
     * Get list of available brawlers.
     */
    @GetMapping(value = ["/v1/brawlers"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun getListOfAvailableBrawlers(
        @SpringQueryMap paging: PagingParam,
    ): ListResponse<BrawlerResponse>

    /**
     * Get information about a brawler.
     */
    @GetMapping(value = ["/v1/brawlers/{brawlerId}"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun getInformationAboutABrawler(
        @PathVariable brawlerId: Long,
    ): BrawlerResponse

    /**
     * Get event rotation for ongoing events.
     */
    @GetMapping(value = ["/v1/events/rotation"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun getEventRotation(): List<ScheduledEventResponse>

    /**
     * Get list of available game modes.
     */
    @GetMapping(value = ["/v1/gamemodes"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun getGameModes(
        @SpringQueryMap paging: PagingParam,
    ): ListResponse<EventTypeResponse>

    /**
     * Get the API specification (swagger 2.0 YAML).
     * NOTE: The response content type is application/octet-stream.
     */
    @GetMapping(value = ["/v1"])
    fun getApiSpec(): String
}
