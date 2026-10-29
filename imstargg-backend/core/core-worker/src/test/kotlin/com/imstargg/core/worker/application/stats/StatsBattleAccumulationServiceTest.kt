package com.imstargg.core.worker.application.stats

import com.imstargg.CoreWorkerContextTest
import com.imstargg.client.brawlstars.BrawlStarsApiServerException
import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.client.brawlstars.response.ListResponse
import com.imstargg.core.worker.config.StatsBattleScheduleProperties
import com.imstargg.infrastructure.db.core.brawlstars.system.repository.BrawlStarsApiMaintenanceJpaRepository
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsPlayerJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.StatsPlayerEntityFixture.Companion.aStatsPlayerEntity
import feign.FeignException
import feign.Request
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class StatsBattleAccumulationServiceTest(
    private val sut: StatsBattleAccumulationService,
    private val properties: StatsBattleScheduleProperties,
    private val brawlStarsClient: BrawlStarsClient,
    private val statsPlayerRepository: StatsPlayerJpaRepository,
    private val brawlStarsApiMaintenanceRepository: BrawlStarsApiMaintenanceJpaRepository,
    private val cleanUp: CleanUp,
) : CoreWorkerContextTest() {
    private lateinit var idByTag: Map<String, Long>

    @BeforeEach
    fun setUp() {
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
        val fetchedTags = Collections.synchronizedList(mutableListOf<String>())
        every { brawlStarsClient.getPlayerRecentBattles(capture(fetchedTags)) } returns
            ListResponse(items = emptyList(), paging = null)

        // when
        sut.accumulate()

        // then
        assertThat(fetchedTags)
            .isNotEmpty()
            .hasSizeLessThanOrEqualTo(properties.playerCount)
            .doesNotHaveDuplicates()
            .allMatch { it in idByTag }
        val fetchedIds = fetchedTags.map { idByTag.getValue(it) }
        // 연속 ID 묶음이면 범위가 playerCount - 1 이하 — 독립 추출(200명 중 playerCount회)이 우연히 그 범위에 몰릴 확률은 무시 가능
        assertThat(fetchedIds.max() - fetchedIds.min()).isGreaterThanOrEqualTo(properties.playerCount.toLong())
    }

    @Test
    fun `한 틱의 플레이어를 동시에 수집한다`() {
        // given
        val inFlight = AtomicInteger()
        val maxInFlight = AtomicInteger()
        every { brawlStarsClient.getPlayerRecentBattles(any()) } answers {
            maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), ::maxOf)
            Thread.sleep(200)
            inFlight.decrementAndGet()
            ListResponse(items = emptyList(), paging = null)
        }

        // when
        sut.accumulate()

        // then
        assertThat(maxInFlight.get()).isGreaterThan(1)
    }

    @Test
    fun `동시에 점검 응답을 받아도 점검은 한 번만 등록한다`() {
        // given
        val inFlight = CountDownLatch(2)
        every { brawlStarsClient.getPlayerRecentBattles(any()) } answers {
            // 최소 두 요청이 동시에 점검 응답을 받도록 맞춘다
            inFlight.countDown()
            inFlight.await(5, TimeUnit.SECONDS)
            throw serviceUnavailable()
        }

        // when
        sut.accumulate()

        // then
        assertThat(brawlStarsApiMaintenanceRepository.count()).isEqualTo(1)
    }

    private fun serviceUnavailable(): BrawlStarsApiServerException.ServiceUnavailable =
        BrawlStarsApiServerException.ServiceUnavailable(
            "maintenance",
            FeignException.ServiceUnavailable(
                "maintenance",
                Request.create(Request.HttpMethod.GET, "https://api.brawlstars.com", emptyMap(), null, null, null),
                byteArrayOf(),
                emptyMap(),
            ),
        )
}
