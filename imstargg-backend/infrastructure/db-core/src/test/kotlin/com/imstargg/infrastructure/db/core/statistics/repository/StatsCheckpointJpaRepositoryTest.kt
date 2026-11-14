package com.imstargg.infrastructure.db.core.statistics.repository

import com.imstargg.DbCoreContextTest
import com.imstargg.core.enums.StatsType
import com.imstargg.infrastructure.db.core.statistics.StatsCheckpointEntity
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import java.time.Instant
import java.time.LocalDate

class StatsCheckpointJpaRepositoryTest(
    private val sut: StatsCheckpointJpaRepository,
) : DbCoreContextTest() {
    @Test
    fun `통계 유형으로 찾는다`() {
        // given
        sut.saveAll(
            listOf(
                aCheckpoint(StatsType.BRAWLER_BATTLE_RESULT, patchId = 1L),
                aCheckpoint(StatsType.BRAWLER_BATTLE_RESULT, patchId = 2L),
                aCheckpoint(StatsType.BRAWLER_BATTLE_RANK, patchId = 1L),
            ),
        )

        // when
        val result = sut.findAllByStatsType(StatsType.BRAWLER_BATTLE_RESULT)

        // then
        assertThat(result).hasSize(2)
        assertThat(result).allMatch { it.statsType == StatsType.BRAWLER_BATTLE_RESULT }
    }

    @Test
    fun `끝 날짜와 최근 전투 일시가 없어도 저장한다`() {
        // given
        val saved =
            sut.saveAndFlush(
                aCheckpoint(StatsType.BRAWLER_PAIR_BATTLE_RESULT, patchId = 1L, appliedEndDate = null, lastBattleAt = null),
            )

        // when
        val found = sut.findById(saved.id!!).get()

        // then
        assertThat(found.appliedEndDate).isNull()
        assertThat(found.lastBattleAt).isNull()
        assertThat(found.version).isEqualTo(0L)
    }

    @Test
    fun `같은 유형과 패치의 줄을 두 번 넣으면 실패한다`() {
        // given
        sut.saveAndFlush(aCheckpoint(StatsType.BRAWLER_TRIO_BATTLE_RESULT, patchId = 1L))

        // when & then
        assertThatThrownBy {
            sut.saveAndFlush(aCheckpoint(StatsType.BRAWLER_TRIO_BATTLE_RESULT, patchId = 1L))
        }.isInstanceOf(DataIntegrityViolationException::class.java)
    }

    private fun aCheckpoint(
        statsType: StatsType,
        patchId: Long,
        appliedEndDate: LocalDate? = LocalDate.of(2026, 11, 30),
        lastBattleAt: Instant? = Instant.parse("2026-11-10T00:00:00Z"),
    ) = StatsCheckpointEntity(
        statsType = statsType,
        brawlStarsPatchId = patchId,
        lastReadBattleId = 100L,
        appliedStartDate = LocalDate.of(2026, 11, 1),
        appliedEndDate = appliedEndDate,
        lastBattleAt = lastBattleAt,
        version = 0L,
    )
}
