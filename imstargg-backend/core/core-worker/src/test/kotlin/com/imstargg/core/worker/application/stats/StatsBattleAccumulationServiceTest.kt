package com.imstargg.core.worker.application.stats

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxy
import ch.qos.logback.core.read.ListAppender
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
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import java.net.SocketTimeoutException
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

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

    // 서비스 로거는 테스트 JVM 전체가 함께 쓰므로 테스트마다 붙이고 떼어 낸다
    private val serviceLogger = LoggerFactory.getLogger(StatsBattleAccumulationService::class.java) as Logger
    private val logAppender = ListAppender<ILoggingEvent>()

    @BeforeEach
    fun setUp() {
        logAppender.start()
        serviceLogger.addAppender(logAppender)
        // 서비스는 ID 를 [1, 최대 ID] 에서 뽑으므로 운영처럼 1부터 채운다 — CleanUp 은 ID 를 되돌리지 않아 앞 테스트만큼 밀린다
        jdbcTemplate.execute("TRUNCATE TABLE stats_player RESTART IDENTITY")
        idByTag =
            statsPlayerRepository
                .saveAll((1..200).map { aStatsPlayerEntity().withBrawlStarsTag("#P$it").build() })
                .associate { it.brawlStarsTag to it.id!! }
    }

    @AfterEach
    fun tearDown() {
        serviceLogger.detachAppender(logAppender)
        cleanUp.all()
    }

    @Test
    fun `플레이어마다 무작위 ID 를 따로 뽑아 연속 ID 묶음으로 수집하지 않는다`() {
        // given
        every { brawlStarsClient.getPlayerRecentBattles(any()) } returns emptyBattles()

        // when
        sut.accumulate()

        // then
        val tags = requestedTags()
        assertThat(tags)
            .isNotEmpty()
            .hasSizeLessThanOrEqualTo(properties.playerCount)
            .doesNotHaveDuplicates()
            .allMatch { it in idByTag }
        val fetchedIds = tags.map { idByTag.getValue(it) }
        // 연속 ID 묶음이면 범위가 playerCount - 1 이하 — 독립 추출(200명 중 playerCount회)이 우연히 그 범위에 몰릴 확률은 무시 가능
        assertThat(fetchedIds.max() - fetchedIds.min()).isGreaterThanOrEqualTo(properties.playerCount.toLong())
    }

    @Test
    fun `동시에 점검 응답을 받아도 점검은 한 번만 등록한다`() {
        // given
        every { brawlStarsClient.getPlayerRecentBattles(any()) } throws serviceUnavailable()

        // when
        sut.accumulate()

        // then
        assertThat(brawlStarsApiMaintenanceRepository.count()).isEqualTo(1)
    }

    @Test
    fun `타임아웃이 난 플레이어는 경고만 남기고 나머지 플레이어를 계속 집계한다`() {
        // given
        val failedTag = stubFailingFirstRequest(readTimeout())

        // when
        sut.accumulate()

        // then
        verify(exactly = requestedTags().size - 1) { statsBattleAggregator.aggregate(any()) }
        val warn = logAppender.list.single { it.level == Level.WARN }
        assertThat(warn.formattedMessage).isEqualTo("[배틀 통계 축적] API 통신 실패 tag=${failedTag.get()}")
        assertThat((warn.throwableProxy as ThrowableProxy).throwable).isInstanceOf(ResourceAccessException::class.java)
    }

    @Test
    fun `레이트리밋 허가를 못 받은 플레이어가 있어도 나머지 플레이어는 집계한다`() {
        // given
        val notPermitted = RequestNotPermitted.createRequestNotPermitted(RateLimiter.ofDefaults("brawlstars"))
        val failedTag = stubFailingFirstRequest(notPermitted)

        // when
        sut.accumulate()

        // then
        verify(exactly = requestedTags().size - 1) { statsBattleAggregator.aggregate(any()) }
        assertThat(statsPlayerRepository.findById(idByTag.getValue(failedTag.get())).get().status).isEqualTo(StatsPlayerStatus.ACTIVE)
        assertThat(brawlStarsApiMaintenanceRepository.count()).isEqualTo(0)
        val errors = logAppender.list.filter { it.level == Level.ERROR }
        assertThat(errors).hasSize(1)
        assertThat(errors.single().formattedMessage).isEqualTo("[배틀 통계 축적] 처리 실패 tag=${failedTag.get()}")
        assertThat((errors.single().throwableProxy as ThrowableProxy).throwable).isInstanceOf(RequestNotPermitted::class.java)
    }

    @Test
    fun `403 은 표시하지 않고 경고만 남긴다`() {
        // given
        val failedTag = stubFailingFirstRequest(forbidden())

        // when
        sut.accumulate()

        // then
        assertThat(statsPlayerRepository.findById(idByTag.getValue(failedTag.get())).get().status).isEqualTo(StatsPlayerStatus.ACTIVE)
        verify(exactly = requestedTags().size - 1) { statsBattleAggregator.aggregate(any()) }
        val warn = logAppender.list.single { it.level == Level.WARN }
        assertThat(warn.formattedMessage).isEqualTo("[배틀 통계 축적] API 오류 status=403 tag=${failedTag.get()}")
    }

    @Test
    fun `한 틱에 점검 응답이 섞여도 나머지 플레이어는 집계한다`() {
        // given
        stubFailingFirstRequest(serviceUnavailable())

        // when
        sut.accumulate()

        // then
        assertThat(brawlStarsApiMaintenanceRepository.count()).isEqualTo(1)
        verify(exactly = requestedTags().size - 1) { statsBattleAggregator.aggregate(any()) }
    }

    @Test
    fun `battlelog 404 면 stats_player 를 BATTLE_LOG_UNAVAILABLE 로 표시하고 행과 player 는 유지한다`() {
        // given
        playerRepository.saveAll((1..200).map { aPlayerEntity().withBrawlStarsTag("#P$it").build() })
        every { brawlStarsClient.getPlayerRecentBattles(any()) } throws notFound()

        // when
        sut.accumulate()

        // then
        val tags = requestedTags()
        assertThat(tags).isNotEmpty()
        tags.forEach { tag ->
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
        every { brawlStarsClient.getPlayerRecentBattles(any()) } returns emptyBattles()

        // when
        sut.accumulate()

        // then
        assertThat(requestedTags())
            .isNotEmpty()
            .allMatch { it in activeTags }
    }

    @Test
    fun `점검 응답이 섞여도 404 인 플레이어는 모두 표시한다`() {
        // given - 처음 도착한 요청만 점검(503), 나머지는 battlelog 404
        val maintenanceTag = AtomicReference<String>()
        every { brawlStarsClient.getPlayerRecentBattles(any()) } answers {
            if (maintenanceTag.compareAndSet(null, firstArg())) throw serviceUnavailable()
            throw notFound()
        }

        // when
        sut.accumulate()

        // then - 점검 응답에서 멈추지 않으므로 처리 순서상 그 뒤의 404 도 표시된다
        val notFoundTags = requestedTags() - maintenanceTag.get()
        assertThat(notFoundTags).isNotEmpty()
        notFoundTags.forEach { tag ->
            assertThat(statsPlayerRepository.findById(idByTag.getValue(tag)).get().status)
                .isEqualTo(StatsPlayerStatus.BATTLE_LOG_UNAVAILABLE)
        }
        assertThat(statsPlayerRepository.findById(idByTag.getValue(maintenanceTag.get())).get().status)
            .isEqualTo(StatsPlayerStatus.ACTIVE)
        assertThat(brawlStarsApiMaintenanceRepository.count()).isEqualTo(1)
    }

    @Test
    fun `뽑을 ACTIVE 플레이어가 없으면 API 를 부르지 않는다`() {
        // given
        statsPlayerRepository.saveAll(statsPlayerRepository.findAll().onEach { it.markBattleLogUnavailable() })

        // when
        sut.accumulate()

        // then
        verify(exactly = 0) { brawlStarsClient.getPlayerRecentBattles(any()) }
    }

    @Test
    fun `동시에 보내는 battlelog 요청 수가 concurrency 를 넘지 않는다`() {
        // given - 한 틱 인원이 상한보다 많아야 상한이 드러난다
        assertThat(properties.playerCount).isGreaterThan(properties.concurrency)
        val inFlight = AtomicInteger()
        val maxInFlight = AtomicInteger()
        // 허가를 먼저 받은 요청들이 서로를 기다리게 해 최대 진행 중 수가 concurrency 에 반드시 닿게 한다
        val firstWave = CountDownLatch(properties.concurrency)
        every { brawlStarsClient.getPlayerRecentBattles(any()) } answers {
            maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), ::maxOf)
            firstWave.countDown()
            firstWave.await(5, TimeUnit.SECONDS)
            Thread.sleep(100)
            inFlight.decrementAndGet()
            emptyBattles()
        }

        // when
        sut.accumulate()

        // then
        assertThat(maxInFlight.get()).isEqualTo(properties.concurrency)
    }

    @Test
    fun `틱이 겹쳐도 동시에 보내는 battlelog 요청 수는 concurrency 를 넘지 않는다`() {
        // given
        val inFlight = AtomicInteger()
        val maxInFlight = AtomicInteger()
        every { brawlStarsClient.getPlayerRecentBattles(any()) } answers {
            maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), ::maxOf)
            Thread.sleep(50)
            inFlight.decrementAndGet()
            emptyBattles()
        }
        val start = CountDownLatch(1)

        // when
        Executors.newFixedThreadPool(2).use { ticks ->
            List(2) {
                ticks.submit(
                    Callable {
                        start.await()
                        sut.accumulate()
                    },
                )
            }.also { start.countDown() }
                .forEach { it.get(10, TimeUnit.SECONDS) }
        }

        // then
        assertThat(maxInFlight.get()).isLessThanOrEqualTo(properties.concurrency)
    }

    @Test
    fun `Error 는 플레이어별 실패로 담지 않고 그대로 던진다`() {
        // given
        every { brawlStarsClient.getPlayerRecentBattles(any()) } throws StackOverflowError()

        // when
        val thrown = catchThrowableOfType(StackOverflowError::class.java) { sut.accumulate() }

        // then
        assertThat(thrown).isNotNull()
    }

    // 요청이 함께 나가 도착 순서가 정해지지 않으므로, 처음 도착한 요청 하나만 실패시키고 그 태그를 돌려준다. 나머지는 전투 없이 성공한다
    private fun stubFailingFirstRequest(failure: Throwable): AtomicReference<String> {
        val failedTag = AtomicReference<String>()
        every { brawlStarsClient.getPlayerRecentBattles(any()) } answers {
            if (failedTag.compareAndSet(null, firstArg())) throw failure
            emptyBattles()
        }
        return failedTag
    }

    // 호출이 끝난 뒤 검증 단계에서 모으므로 동시 호출과 겹치지 않는다
    private fun requestedTags(): List<String> {
        val tags = mutableListOf<String>()
        verify { brawlStarsClient.getPlayerRecentBattles(capture(tags)) }
        return tags
    }

    private fun emptyBattles(): ListResponse<BattleResponse> = ListResponse(emptyList(), null)

    private fun notFound(): HttpClientErrorException =
        HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders(), ByteArray(0), null)

    private fun forbidden(): HttpClientErrorException =
        HttpClientErrorException.create(HttpStatus.FORBIDDEN, "Forbidden", HttpHeaders(), ByteArray(0), null)

    private fun serviceUnavailable(): HttpServerErrorException =
        HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable", HttpHeaders(), ByteArray(0), null)

    private fun readTimeout(): ResourceAccessException =
        ResourceAccessException("I/O error on GET request: Read timed out", SocketTimeoutException("Read timed out"))
}
