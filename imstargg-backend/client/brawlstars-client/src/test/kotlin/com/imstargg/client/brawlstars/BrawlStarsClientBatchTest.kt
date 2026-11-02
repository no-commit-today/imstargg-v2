package com.imstargg.client.brawlstars

import com.imstargg.client.brawlstars.response.BattleResponse
import com.imstargg.client.brawlstars.response.ListResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class BrawlStarsClientBatchTest {
    private val api = mockk<BrawlStarsApi>()
    private val sut = BrawlStarsClient(api, batchConcurrency = 2)

    @Test
    fun `일부 태그가 실패해도 나머지 태그의 결과를 담아 돌려준다`() {
        // given
        every { api.getLogOfRecentBattlesForAPlayer(any()) } returns emptyBattles()
        every { api.getLogOfRecentBattlesForAPlayer("#B") } throws notFound()

        // when
        val result = sut.getPlayersRecentBattles(listOf("#A", "#B", "#C"))

        // then
        assertThat(result.getValue("#B").exceptionOrNull()).isInstanceOf(HttpClientErrorException.NotFound::class.java)
        assertThat(result.getValue("#A").isSuccess).isTrue()
        assertThat(result.getValue("#C").isSuccess).isTrue()
    }

    @Test
    fun `동시에 보내는 요청 수가 batchConcurrency 를 넘지 않는다`() {
        // given
        val inFlight = AtomicInteger()
        val maxInFlight = AtomicInteger()
        // 허가를 먼저 받은 두 요청이 서로를 기다리게 해 최대 진행 중 수가 batchConcurrency 에 반드시 닿게 한다
        val firstTwoInFlight = CountDownLatch(2)
        every { api.getLogOfRecentBattlesForAPlayer(any()) } answers {
            maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), ::maxOf)
            firstTwoInFlight.countDown()
            firstTwoInFlight.await(5, TimeUnit.SECONDS)
            Thread.sleep(100)
            inFlight.decrementAndGet()
            emptyBattles()
        }

        // when
        sut.getPlayersRecentBattles((1..6).map { "#P$it" })

        // then
        assertThat(maxInFlight.get()).isEqualTo(2)
    }

    @Test
    fun `두 배치 호출이 겹쳐도 클라이언트 전체 동시 요청 수는 batchConcurrency 를 넘지 않는다`() {
        // given
        val inFlight = AtomicInteger()
        val maxInFlight = AtomicInteger()
        every { api.getLogOfRecentBattlesForAPlayer(any()) } answers {
            maxInFlight.accumulateAndGet(inFlight.incrementAndGet(), ::maxOf)
            Thread.sleep(100)
            inFlight.decrementAndGet()
            emptyBattles()
        }
        val start = CountDownLatch(1)

        // when
        Executors.newFixedThreadPool(2).use { callers ->
            listOf(listOf("#A1", "#A2", "#A3"), listOf("#B1", "#B2", "#B3"))
                .map { tags ->
                    callers.submit(
                        Callable {
                            start.await()
                            sut.getPlayersRecentBattles(tags)
                        },
                    )
                }.also { start.countDown() }
                .forEach { it.get(5, TimeUnit.SECONDS) }
        }

        // then
        assertThat(maxInFlight.get()).isLessThanOrEqualTo(2)
    }

    @Test
    fun `중복 태그는 한 번만 호출하고 결과는 입력 순서를 따른다`() {
        // given
        every { api.getLogOfRecentBattlesForAPlayer(any()) } returns emptyBattles()

        // when
        val result = sut.getPlayersRecentBattles(listOf("#B", "#A", "#B"))

        // then
        assertThat(result.keys).containsExactly("#B", "#A")
        verify(exactly = 1) { api.getLogOfRecentBattlesForAPlayer("#B") }
    }

    @Test
    fun `Error 는 Result 에 담지 않고 그대로 던진다`() {
        // given
        every { api.getLogOfRecentBattlesForAPlayer(any()) } throws StackOverflowError()

        // when
        val thrown = catchThrowableOfType(StackOverflowError::class.java) { sut.getPlayersRecentBattles(listOf("#A", "#B")) }

        // then
        assertThat(thrown).isNotNull()
    }

    @Test
    fun `빈 목록이면 호출 없이 빈 맵을 돌려준다`() {
        // when
        val result = sut.getPlayersRecentBattles(emptyList())

        // then
        assertThat(result).isEmpty()
        verify(exactly = 0) { api.getLogOfRecentBattlesForAPlayer(any()) }
    }

    private fun emptyBattles(): ListResponse<BattleResponse> = ListResponse(items = emptyList(), paging = null)

    private fun notFound(): HttpClientErrorException =
        HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", HttpHeaders(), ByteArray(0), null)
}
