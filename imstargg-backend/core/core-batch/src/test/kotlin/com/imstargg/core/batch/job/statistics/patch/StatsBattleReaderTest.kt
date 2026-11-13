package com.imstargg.core.batch.job.statistics.patch

import com.imstargg.CoreBatchContextTest
import com.imstargg.core.batch.domain.statistics.patch.PatchDateRange
import com.imstargg.core.batch.domain.statistics.showdown
import com.imstargg.core.batch.domain.statistics.threeVsThree
import com.imstargg.core.enums.StatsEventModeType
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsRankBattleJpaRepository
import com.imstargg.infrastructure.db.core.statistics.battle.repository.StatsResultBattleJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import jakarta.persistence.EntityManagerFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

class StatsBattleReaderTest(
    entityManagerFactory: EntityManagerFactory,
    jdbcTemplate: JdbcTemplate,
    resultBattleJpaRepository: StatsResultBattleJpaRepository,
    rankBattleJpaRepository: StatsRankBattleJpaRepository,
    private val cleanUp: CleanUp,
) : CoreBatchContextTest() {
    private val support = PatchStatsTestSupport(jdbcTemplate, resultBattleJpaRepository, rankBattleJpaRepository)
    private val resultReader = StatsResultBattleReader(entityManagerFactory, pageSize = 2)
    private val rankReader = StatsRankBattleReader(entityManagerFactory, pageSize = 2)
    private val createdAt = Instant.parse("2026-11-10T00:00:00Z")

    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `저장된 지 오래된 전투 중 가장 큰 id 가 상한이다`() {
        // given
        val date = LocalDate.of(2026, 11, 5)
        val old1 = saveResult(date, "k1", createdAt.minusSeconds(900))
        val old2 = saveResult(date, "k2", createdAt.minusSeconds(700))
        saveResult(date, "k3", createdAt.plusSeconds(60))

        // when
        val cap = resultReader.findBattleIdCap(createdAt, date)

        // then
        assertThat(cap).isEqualTo(maxOf(old1.id!!, old2.id!!))
        assertThat(cap).isEqualTo(old2.id)
    }

    @Test
    fun `기준 날짜보다 앞선 전투만 있으면 상한이 없다`() {
        // given
        saveResult(LocalDate.of(2026, 11, 1), "k1", createdAt.minusSeconds(900))

        // when
        val cap = resultReader.findBattleIdCap(createdAt, LocalDate.of(2026, 11, 2))

        // then
        assertThat(cap).isNull()
    }

    @Test
    fun `남아 있는 가장 이른 전투 날짜를 준다`() {
        // given
        saveResult(LocalDate.of(2026, 11, 5), "k1", createdAt)
        saveResult(LocalDate.of(2026, 11, 3), "k2", createdAt)
        saveResult(LocalDate.of(2026, 11, 8), "k3", createdAt)

        // when
        val earliest = resultReader.findEarliestBattleDate()

        // then
        assertThat(earliest).isEqualTo(LocalDate.of(2026, 11, 3))
    }

    @Test
    fun `원본이 없으면 가장 이른 날짜가 없다`() {
        assertThat(resultReader.findEarliestBattleDate()).isNull()
    }

    @Test
    fun `날짜 범위 안에서 체크포인트 뒤부터 상한까지 id 순으로 읽는다`() {
        // given
        val before = saveResult(LocalDate.of(2026, 11, 2), "k0", createdAt)
        val atCheckpoint = saveResult(LocalDate.of(2026, 11, 4), "k1", createdAt)
        val inRange1 = saveResult(LocalDate.of(2026, 11, 5), "k2", createdAt)
        val outOfRange = saveResult(LocalDate.of(2026, 11, 9), "k3", createdAt)
        val inRange2 = saveResult(LocalDate.of(2026, 11, 6), "k4", createdAt)
        val atCap = saveResult(LocalDate.of(2026, 11, 8), "k5", createdAt)
        val overCap = saveResult(LocalDate.of(2026, 11, 7), "k6", createdAt)
        val range = PatchDateRange(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 8))

        // when
        val ids = readIds(range, afterBattleId = atCheckpoint.id!!, battleIdCap = atCap.id!!)

        // then
        assertThat(ids).containsExactly(inRange1.id, inRange2.id, atCap.id)
        assertThat(ids).doesNotContain(before.id, outOfRange.id, overCap.id)
    }

    @Test
    fun `끝이 없는 범위는 시작 날짜 뒤를 모두 읽는다`() {
        // given
        saveResult(LocalDate.of(2026, 11, 2), "k0", createdAt)
        val a = saveResult(LocalDate.of(2026, 11, 3), "k1", createdAt)
        val b = saveResult(LocalDate.of(2026, 12, 25), "k2", createdAt)
        val range = PatchDateRange(LocalDate.of(2026, 11, 3), null)

        // when
        val ids = readIds(range, afterBattleId = 0L, battleIdCap = b.id!!)

        // then
        assertThat(ids).containsExactly(a.id, b.id)
    }

    @Test
    fun `쪽 경계를 넘겨도 빠지거나 겹치지 않는다`() {
        // given
        val saved = (1..5).map { saveResult(LocalDate.of(2026, 11, 5), "k$it", createdAt) }
        val range = PatchDateRange(LocalDate.of(2026, 11, 3), null)
        val pages = mutableListOf<List<Long>>()

        // when
        resultReader.read(range, 0L, saved.last().id!!) { battles -> pages.add(battles.map { it.id!! }) }

        // then
        assertThat(pages).hasSize(3)
        assertThat(pages.flatten()).containsExactlyElementsOf(saved.map { it.id })
    }

    @Test
    fun `순위형 원본도 같은 규칙으로 읽는다`() {
        // given
        val before = saveRank(LocalDate.of(2026, 11, 2), "k0", createdAt.minusSeconds(900))
        val checkpoint = saveRank(LocalDate.of(2026, 11, 4), "k1", createdAt.minusSeconds(800))
        val inRange = saveRank(LocalDate.of(2026, 11, 5), "k2", createdAt.minusSeconds(700))
        val cap = saveRank(LocalDate.of(2026, 11, 6), "k3", createdAt.minusSeconds(600))
        saveRank(LocalDate.of(2026, 11, 6), "k4", createdAt.plusSeconds(60))
        val range = PatchDateRange(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 8))

        // when
        val foundCap = rankReader.findBattleIdCap(createdAt, LocalDate.of(2026, 11, 3))
        val ids = mutableListOf<Long>()
        rankReader.read(range, checkpoint.id!!, cap.id!!) { battles -> ids.addAll(battles.map { it.id!! }) }

        // then
        assertThat(foundCap).isEqualTo(cap.id)
        assertThat(rankReader.findEarliestBattleDate()).isEqualTo(LocalDate.of(2026, 11, 2))
        assertThat(ids).containsExactly(inRange.id, cap.id)
        assertThat(ids).doesNotContain(before.id)
    }

    private fun readIds(
        range: PatchDateRange,
        afterBattleId: Long,
        battleIdCap: Long,
    ): List<Long> {
        val ids = mutableListOf<Long>()
        resultReader.read(range, afterBattleId, battleIdCap) { battles -> ids.addAll(battles.map { it.id!! }) }
        return ids
    }

    private fun saveResult(
        date: LocalDate,
        key: String,
        createdAt: Instant,
    ): StatsResultBattleEntity =
        support.saveResultBattle(
            threeVsThree(
                collectorTeam = listOf(1000, 1000, 1000),
                opponents = listOf(1000, 1000, 1000),
                battleKey = key,
                battleAt = noonOf(date),
            ),
            createdAt,
        )

    private fun saveRank(
        date: LocalDate,
        key: String,
        createdAt: Instant,
    ): StatsRankBattleEntity =
        support.saveRankBattle(
            showdown(
                modeType = StatsEventModeType.SOLO_SHOWDOWN,
                teamTrophies = listOf(listOf(1000), listOf(1000)),
                battleKey = key,
                battleAt = noonOf(date),
            ),
            createdAt,
        )

    private fun noonOf(date: LocalDate): Instant = OffsetDateTime.parse("${date}T12:00:00+09:00").toInstant()
}
