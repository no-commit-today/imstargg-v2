package com.imstargg.client.brawlstars

import com.imstargg.client.brawlstars.response.BattleResponse
import com.imstargg.client.brawlstars.response.BrawlerResponse
import com.imstargg.client.brawlstars.response.ClubRankingResponse
import com.imstargg.client.brawlstars.response.ClubResponse
import com.imstargg.client.brawlstars.response.EventTypeResponse
import com.imstargg.client.brawlstars.response.ListResponse
import com.imstargg.client.brawlstars.response.PlayerRankingResponse
import com.imstargg.client.brawlstars.response.PlayerResponse
import com.imstargg.client.brawlstars.response.ScheduledEventResponse

/**
 * 브롤스타즈 API 클라이언트. 예외는 RestClient 예외를 그대로 올린다 — 공용 예외 계층을 두지 않는다.
 *
 * - HTTP 오류 상태: `HttpClientErrorException`·`HttpServerErrorException`(상태별 하위 타입, 예: `NotFound`·`ServiceUnavailable`),
 *   그 밖의 상태는 `RestClientResponseException`
 * - 타임아웃·연결 실패: `ResourceAccessException`
 * - 응답 해석 실패: `RestClientException`(`RestClientResponseException` 아님)
 * - 레이트리밋 대기 초과: resilience4j `RequestNotPermitted`
 *
 * 재시도하지 않는다. 여러 요청을 함께 보내는 일은 호출부가 맡는다.
 */
class BrawlStarsClient internal constructor(
    private val brawlStarsApi: BrawlStarsApi,
) {
    fun getPlayerRecentBattles(playerTag: String): ListResponse<BattleResponse> = brawlStarsApi.getLogOfRecentBattlesForAPlayer(playerTag)

    fun getPlayerInformation(playerTag: String): PlayerResponse = brawlStarsApi.getPlayerInformation(playerTag)

    fun getBrawlers(): ListResponse<BrawlerResponse> = brawlStarsApi.getListOfAvailableBrawlers()

    fun getGameModes(): ListResponse<EventTypeResponse> = brawlStarsApi.getGameModes()

    fun getEventRotation(): List<ScheduledEventResponse> = brawlStarsApi.getEventRotation()

    fun getPlayerRanking(country: String): ListResponse<PlayerRankingResponse> =
        brawlStarsApi.getPlayerRankingsForACountryOrGlobalRankings(country)

    fun getBrawlerRanking(
        country: String,
        brawlerId: Long,
    ): ListResponse<PlayerRankingResponse> = brawlStarsApi.getBrawlerRankingsForACountryOrGlobalRankings(country, brawlerId)

    fun getClub(clubTag: String): ClubResponse = brawlStarsApi.getClubInformation(clubTag)

    fun getClubRanking(country: String): ListResponse<ClubRankingResponse> =
        brawlStarsApi.getClubRankingsForACountryOrGlobalRankings(country)

    fun getApiSpec(): String = brawlStarsApi.getApiSpec()
}
