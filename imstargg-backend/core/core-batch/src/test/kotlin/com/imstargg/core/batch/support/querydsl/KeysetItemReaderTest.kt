package com.imstargg.core.batch.support.querydsl

import com.imstargg.CoreBatchContextTest
import com.imstargg.infrastructure.db.core.statistics.battle.QStatsPlayerEntity.Companion.statsPlayerEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsPlayerEntity
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsPlayerJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import jakarta.persistence.EntityManagerFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.batch.infrastructure.item.ExecutionContext

class KeysetItemReaderTest(
    private val entityManagerFactory: EntityManagerFactory,
    private val statsPlayerJpaRepository: StatsPlayerJpaRepository,
    private val cleanUp: CleanUp,
) : CoreBatchContextTest() {
    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `키셋으로 전체 행을 id 오름차순으로 읽는다`() {
        // given - 페이지 크기(7)로 나누어떨어지지 않는 25건
        statsPlayerJpaRepository.saveAll((1..25).map { StatsPlayerEntity(brawlStarsTag = "#TAG$it") })
        val sut = keysetItemReader()

        // when
        sut.open(ExecutionContext())
        val read = generateSequence { sut.read() }.toList()
        sut.close()

        // then
        assertThat(read).hasSize(25)
        assertThat(read.map { it.id!! }).isSorted()
        assertThat(read.map { it.brawlStarsTag }).containsExactlyInAnyOrderElementsOf((1..25).map { "#TAG$it" })
    }

    @Test
    fun `ExecutionContext 로 재시작하면 마지막으로 읽은 항목 다음부터 읽는다`() {
        // given - 20건 저장 후 5건만 읽고 상태 저장
        statsPlayerJpaRepository.saveAll((1..20).map { StatsPlayerEntity(brawlStarsTag = "#TAG$it") })
        val executionContext = ExecutionContext()
        val first = keysetItemReader()
        first.open(executionContext)
        val firstRead = (1..5).map { first.read()!! }
        first.update(executionContext)
        first.close()

        // when - 같은 ExecutionContext 로 새 리더 open
        val second = keysetItemReader()
        second.open(executionContext)
        val secondRead = generateSequence { second.read() }.toList()
        second.close()

        // then - 중복·누락 없이 이어 읽는다
        assertThat(secondRead).hasSize(15)
        assertThat(firstRead.map { it.id!! } + secondRead.map { it.id!! })
            .isSorted()
            .doesNotHaveDuplicates()
    }

    private fun keysetItemReader(): KeysetItemReader<StatsPlayerEntity> =
        KeysetItemReader(
            name = "keysetItemReaderTest",
            entityManagerFactory = entityManagerFactory,
            pageSize = 7,
            idPath = statsPlayerEntity.id,
            idOf = { it.id!! },
        ) { queryFactory -> queryFactory.selectFrom(statsPlayerEntity) }
}
