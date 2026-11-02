package com.imstargg.core.worker.application.stats

import com.imstargg.CoreWorkerContextTest
import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.client.brawlstars.response.BattleResponse
import com.imstargg.client.brawlstars.response.ListResponse
import com.imstargg.core.enums.PlayerStatus
import com.imstargg.core.enums.StatsPlayerStatus
import com.imstargg.core.worker.config.StatsBattleScheduleProperties
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.system.repository.BrawlStarsApiMaintenanceJpaRepository
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsPlayerJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.PlayerEntityFixture.Companion.aPlayerEntity
import com.imstargg.infrastructure.db.core.test.fixture.StatsPlayerEntityFixture.Companion.aStatsPlayerEntity
import com.ninjasquad.springmockk.MockkBean
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RequestNotPermitted
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import java.net.SocketTimeoutException

class StatsBattleAccumulationServiceTest(
    private val sut: StatsBattleAccumulationService,
    private val properties: StatsBattleScheduleProperties,
    private val brawlStarsClient: BrawlStarsClient,
    private val statsPlayerRepository: StatsPlayerJpaRepository,
    private val playerRepository: PlayerJpaRepository,
    private val brawlStarsApiMaintenanceRepository: BrawlStarsApiMaintenanceJpaRepository,
    private val cleanUp: CleanUp,
    private val jdbcTemplate: JdbcTemplate,
) : CoreWorkerContextTest() {
    @MockkBean(relaxUnitFun = true)
    private lateinit var statsBattleAggregator: StatsBattleAggregator

    private lateinit var idByTag: Map<String, Long>

    @BeforeEach
    fun setUp() {
        // 서비스는 ID 를 [1, 최대 ID] 에서 뽑으므로 운영처럼 1부터 채운다 — CleanUp 은 ID 를 되돌리지 않아 앞 테스트만큼 밀린다
        jdbcTemplate.execute("TRUNCATE TABLE stats_player RESTART IDENTITY")
        idByTag =
            statsPlayerRepository
                .saveAll((1..200).map { aStatsPlayerEntity().withBrawlStarsTag("#P$it").build() })
                .associate { it.brawlStarsTag to it.id!! }
    }

    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `플레이어마다 무작위 ID 를 따로 뽑아 연속 ID 묶음으로 수집하지 않는다`() {
        // given
        val tags = slot<Collection<String>>()
        every { brawlStarsClient.getPlayersRecentBattles(capture(tags)) } answers {
            firstArg<Collection<String>>().associateWith { Result.success(ListResponse<BattleResponse>(emptyList(), null)) }
        }

        // when
        sut.accumulate()

        // then
        assertThat(tags.captured)
            .isNotEmpty()
            .hasSizeLessThanOrEqualTo(properties.playerCount)
            .doesNotHaveDuplicates()
            .allMatch { it in idByTag }
        val fetchedIds = tags.captured.map { idByTag.getValue(it) }
        // 연속 ID 묶음이면 범위가 playerCount - 1 이하 — 독립 추출(200명 중 playerCount회)이 우연히 그 범위에 몰릴 확률은 무시 가능
        assertThat(fetchedIds.max() - fetchedIds.min()).isGreaterThanOrEqualTo(properties.playerCount.toLong())
    }

    @Test
    fun `동시에 점검 응답을 받아도 점검은 한 번만 등록한다`() {
        // given
        every { brawlStarsClient.getPlayersRecentBattles(any()) } answers {
            firstArg<Collection<String>>().associateWith { Result.failure<ListResponse<BattleResponse>>(serviceUnavailable()) }
        }

        // when
        sut.accumulate()

        // then
        assertThat(brawlStarsApiMaintenanceRepository.count()).isEqualTo(1)
    }

    @Test
    fun `타임아웃이 난 플레이어는 경고만 남기고 나머지 플레이어를 계속 집계한다`() {
        // given
        val tags = slot<Collection<String>>()
        every { brawlStarsClient.getPlayersRecentBattles(capture(tags)) } answers { resultsFailingFirstTag(firstArg(), readTimeout()) }

        // when
        sut.accumulate()

        // then
        verify(exactly = tags.captured.size - 1) { statsBattleAggregator.aggregate(any()) }
    }

    @Test
    fun `레이트리밋 허가를 못 받은 플레이어가 있어도 나머지 플레이어는 집계한다`() {
        // given
        val tags = slot<Collection<String>>()
        val notPermitted = RequestNotPermitted.createRequestNotPermitted(RateLimiter.ofDefaults("brawlstars"))
        every { brawlStarsClient.getPlayersRecentBattles(capture(tags)) } answers { resultsFailingFirstTag(firstArg(), notPermitted) }

        // when
        sut.accumulate()

        // then
        verify(exactly = tags.captured.size - 1) { statsBattleAggregator.aggregate(any()) }
    }

    @Test
    fun `한 틱에 점검 응답이 섞여도 나머지 플레이어는 집계한다`() {
        // given
        val tags = slot<Collection<String>>()
        every { brawlStarsClient.getPlayersRecentBattles(capture(tags)) } answers {
            resultsFailingFirstTag(firstArg(), serviceUnavailable())
        }

        // when
        sut.accumulate()

        // then
        assertThat(brawlStarsApiMaintenanceRepository.count()).isEqualTo(1)
        verify(exactly = tags.captured.size - 1) { statsBattleAggregator.aggregate(any()) }
    }

    @Test
    fun `battlelog 404 면 stats_player 를 BATTLE_LOG_UNAVAILABLE 로 표시하고 행과 player 는 유지한다`() {
        // given
        playerRepository.saveAll((1..200).map { aPlayerEntity().withBrawlStarsTag("#P$it").build() })
        val tags = slot<Collection<String>>()
        every { brawlStarsClient.getPlayersRecentBattles(capture(tags)) } answers {
            firstArg<Collection<String>>().associateWith { Result.failure<ListResponse<BattleResponse>>(notFound()) }
        }

        // when
        sut.accumulate()

        // then
        assertThat(tags.captured).isNotEmpty()
        tags.captured.forEach { tag ->
            assertThat(statsPlayerRepository.findById(idByTag.getValue(tag)).get().status)
                .isEqualTo(StatsPlayerStatus.BATTLE_LOG_UNAVAILABLE)
            assertThat(playerRepository.findByBrawlStarsTag(tag)!!.status).isEqualTo(PlayerStatus.ACTIVE)
        }
        assertThat(statsPlayerRepository.count()).isEqualTo(200)
    }

    @Test
    fun `ACTIVE 인 stats_player 만 뽑는다`() {
        // given
        // 10의 배수 태그만 ACTIVE 로 남긴다 — 최대 ID 행(#P200)이 ACTIVE 라 어떤 무작위 ID 로도 ACTIVE 행이 잡힌다
        val activeTags = (10..200 step 10).map { "#P$it" }.toSet()
        statsPlayerRepository.saveAll(
            statsPlayerRepository
                .findAll()
                .filter { it.brawlStarsTag !in activeTags }
                .onEach { it.markBattleLogUnavailable() },
        )
        val tags = slot<Collection<String>>()
        every { brawlStarsClient.getPlayersRecentBattles(capture(tags)) } answers {
            firstArg<Collection<String>>().associateWith { Result.success(ListResponse<BattleResponse>(emptyList(), null)) }
        }

        // when
        sut.accumulate()

        // then
        assertThat(tags.captured)
            .isNotEmpty()
            .allMatch { it in activeTags }
    }

    @Test
    fun `점검 응답 뒤 순서의 404 도 표시한다`() {
        // given
        val tags = slot<Collection<String>>()
        every { brawlStarsClient.getPlayersRecentBattles(capture(tags)) } answers { resultsWithMaintenanceThenNotFound(firstArg()) }

        // when
        sut.accumulate()

        // then
        assertThat(tags.captured).hasSizeGreaterThanOrEqualTo(2)
        val maintenanceTag = tags.captured.first()
        val notFoundTag = tags.captured.elementAt(1)
        assertThat(statsPlayerRepository.findById(idByTag.getValue(notFoundTag)).get().status)
            .isEqualTo(StatsPlayerStatus.BATTLE_LOG_UNAVAILABLE)
        assertThat(statsPlayerRepository.findById(idByTag.getValue(maintenanceTag)).get().status)
            .isEqualTo(StatsPlayerStatus.ACTIVE)
    }

    @Test
    fun `뽑을 ACTIVE 플레이어가 없으면 API 를 부르지 않는다`() {
        // given
        statsPlayerRepository.saveAll(statsPlayerRepository.findAll().onEach { it.markBattleLogUnavailable() })

        // when
        sut.accumulate()

        // then
        verify(exactly = 0) { brawlStarsClient.getPlayersRecentBattles(any()) }
    }

    // 요청 순서의 첫 태그만 실패하고 나머지는 전투 없이 성공한다
    private fun resultsFailingFirstTag(
        tags: Collection<String>,
        failure: Throwable,
    ): Map<String, Result<ListResponse<BattleResponse>>> =
        tags.associateWith { tag ->
            if (tag == tags.first()) Result.failure(failure) else Result.success(ListResponse(emptyList(), null))
        }

    // 요청 순서의 첫 태그는 점검(503), 둘째 태그는 battlelog 404, 나머지는 전투 없이 성공한다
    private fun resultsWithMaintenanceThenNotFound(tags: Collection<String>): Map<String, Result<ListResponse<BattleResponse>>> =
        tags.associateWith { tag ->
            when (tag) {
                tags.first() -> Result.failure(serviceUnavailable())
                tags.elementAt(1) -> Result.failure(notFound())
                else -> Result.success(ListResponse(emptyList(), null))
            }
        }

    private fun notFound(): HttpClientErrorException =
        HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders(), ByteArray(0), null)

    private fun serviceUnavailable(): HttpServerErrorException =
        HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", HttpHeaders(), ByteArray(0), null)

    private fun readTimeout(): ResourceAccessException =
        ResourceAccessException("I/O error on GET request: Read timed out", SocketTimeoutException("Read timed out"))
}
