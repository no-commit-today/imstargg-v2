package com.imstargg.core.worker.application

import com.imstargg.CoreWorkerContextTest
import com.imstargg.core.enums.RenewalStatus
import com.imstargg.core.enums.RenewalType
import com.imstargg.infrastructure.db.core.renewal.repository.RenewalJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.RenewalEntityFixture.Companion.aRenewalEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.orm.ObjectOptimisticLockingFailureException
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PlayerRenewalProcessorConcurrencyTest(
    private val sut: PlayerRenewalProcessor,
    private val renewalRepository: RenewalJpaRepository,
    private val cleanUp: CleanUp,
) : CoreWorkerContextTest() {
    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `동시에 같은 renewal을 executing하면 하나만 성공하고 나머지는 ObjectOptimisticLockingFailureException이 발생한다`() {
        // given
        val tag = "#PLAYER_CONC"
        val entity = aRenewalEntity().withBrawlStarsTag(tag).build()
        entity.toPending()
        renewalRepository.save(entity)

        val readBarrier = CyclicBarrier(3)
        val executeBarrier = CyclicBarrier(3)
        val executor = Executors.newFixedThreadPool(2)

        // when
        val futures =
            (1..2).map {
                executor.submit<Exception?> {
                    // 두 스레드 모두 같은 version의 엔티티를 조회
                    val renewal = renewalRepository.findByBrawlStarsTagAndType(tag, RenewalType.PLAYER)!!
                    readBarrier.await(10, TimeUnit.SECONDS)

                    // 두 스레드 모두 조회 완료 후 동시에 executing 호출
                    executeBarrier.await(10, TimeUnit.SECONDS)
                    try {
                        sut.executing(renewal)
                        null
                    } catch (e: Exception) {
                        e
                    }
                }
            }

        // 메인 스레드도 barrier에 참여하여 동기화 제어
        readBarrier.await(10, TimeUnit.SECONDS)
        executeBarrier.await(10, TimeUnit.SECONDS)

        val results = futures.map { it.get(10, TimeUnit.SECONDS) }
        executor.shutdown()

        // then
        val successes = results.filter { it == null }
        val failures = results.filterNotNull()

        assertThat(successes).hasSize(1)
        assertThat(failures).hasSize(1)
        assertThat(failures[0]).isInstanceOf(ObjectOptimisticLockingFailureException::class.java)

        val saved = renewalRepository.findByBrawlStarsTagAndType(tag, RenewalType.PLAYER)!!
        assertThat(saved.status).isEqualTo(RenewalStatus.EXECUTING)
        assertThat(saved.version).isEqualTo(1L)
    }
}
