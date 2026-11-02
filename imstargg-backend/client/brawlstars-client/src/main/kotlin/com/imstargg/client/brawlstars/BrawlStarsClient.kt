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
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

/**
 * 브롤스타즈 API 클라이언트. 예외는 RestClient 예외를 그대로 올린다 — 공용 예외 계층을 두지 않는다.
 *
 * - HTTP 오류 상태: `HttpClientErrorException`·`HttpServerErrorException`(상태별 하위 타입, 예: `NotFound`·`ServiceUnavailable`),
 *   그 밖의 상태는 `RestClientResponseException`
 * - 타임아웃·연결 실패: `ResourceAccessException`
 * - 응답 해석 실패: `RestClientException`(`RestClientResponseException` 아님)
 * - 레이트리밋 대기 초과: resilience4j `RequestNotPermitted`
 *
 * 재시도하지 않는다.
 *
 * @param batchConcurrency 배치 메서드가 클라이언트 전체에서 동시에 띄우는 요청 수 상한
 */
class BrawlStarsClient internal constructor(
    private val brawlStarsApi: BrawlStarsApi,
    batchConcurrency: Int,
) {
    // 배치 호출끼리 겹쳐도 상한을 넘지 않도록 호출마다가 아니라 클라이언트 전체가 나눠 쓴다
    private val batchPermits = Semaphore(batchConcurrency)

    fun getPlayerRecentBattles(playerTag: String): ListResponse<BattleResponse> = brawlStarsApi.getLogOfRecentBattlesForAPlayer(playerTag)

    /**
     * 여러 플레이어의 battlelog 를 가상 스레드에서 함께 조회한다. 동시에 떠 있는 요청은 클라이언트 전체에서 `batchConcurrency` 개까지다.
     *
     * 중복 태그는 한 번만 조회하고, 결과 맵은 입력 순서를 따른다. 태그별 `Exception` 은 [Result.failure] 로 담아
     * 나머지 태그를 막지 않고(예외 타입은 [getPlayerRecentBattles] 와 같다), `Error` 는 그대로 던진다.
     * 모든 태그가 끝날 때까지 기다리며 전체 제한 시간은 두지 않는다 — 건별 read-timeout 과 레이트리밋 대기가 상한이다.
     */
    fun getPlayersRecentBattles(playerTags: Collection<String>): Map<String, Result<ListResponse<BattleResponse>>> {
        val tags = playerTags.distinct()
        if (tags.isEmpty()) return emptyMap()
        return Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            tags
                .associateWith { tag -> executor.submit(Callable { withBatchPermit { getPlayerRecentBattles(tag) } }) }
                .mapValues { (_, future) ->
                    try {
                        Result.success(future.get())
                    } catch (ex: ExecutionException) {
                        // runCatching 은 Error 까지 Result 에 담으므로 쓰지 않는다
                        val cause = ex.cause ?: ex
                        if (cause is Error) throw cause
                        Result.failure(cause)
                    }
                }
        }
    }

    private inline fun <T> withBatchPermit(block: () -> T): T {
        batchPermits.acquire()
        return try {
            block()
        } finally {
            batchPermits.release()
        }
    }

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
