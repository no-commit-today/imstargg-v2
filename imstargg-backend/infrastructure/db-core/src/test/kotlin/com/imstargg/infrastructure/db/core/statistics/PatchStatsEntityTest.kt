package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.DbCoreContextTest
import com.imstargg.core.enums.StatsTierRange
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional

@Transactional
class PatchStatsEntityTest(
    private val entityManager: EntityManager,
) : DbCoreContextTest() {
    private val tierRange = StatsTierRange.TROPHY_1000_PLUS

    @Test
    fun `2인 조합은 브롤러 id 가 오름차순이 아니면 만들 수 없다`() {
        // when & then
        assertThatThrownBy { aPair(brawler1BrawlStarsId = 20L, brawler2BrawlStarsId = 10L) }
            .isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { aPair(brawler1BrawlStarsId = 10L, brawler2BrawlStarsId = 10L) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `3인 조합은 브롤러 id 가 오름차순이 아니면 만들 수 없다`() {
        // when & then
        assertThatThrownBy { aTrio(10L, 30L, 20L) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { aTrio(10L, 20L, 20L) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThat(aTrio(10L, 20L, 30L).brawler3BrawlStarsId).isEqualTo(30L)
    }

    @Test
    fun `상대는 자기 자신일 수 없다`() {
        // when & then
        assertThatThrownBy {
            BrawlerEnemyBattleResultStatsEntity(
                brawlStarsPatchId = 1L,
                tierRange = tierRange,
                brawlerBrawlStarsId = 10L,
                enemyBrawlerBrawlStarsId = 10L,
                resultCount = ResultCountEmbedded.create(1L, 0L, 0L),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `1인 순위는 순위별 횟수가 10개가 아니면 만들 수 없다`() {
        // when & then
        assertThatThrownBy { aRank(listOf(1L, 2L, 3L)) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { aRank(List(11) { 1L }) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `1인 순위의 합은 순위별 횟수의 합이다`() {
        // given
        val sut = aRank(listOf(3L, 2L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L))

        // when
        entityManager.persist(sut)
        entityManager.flush()
        entityManager.clear()
        val found = entityManager.find(BrawlerBattleRankStatsEntity::class.java, sut.id)

        // then
        assertThat(sut.totalCount).isEqualTo(6L)
        assertThat(found.totalCount).isEqualTo(6L)
        assertThat(found.rankCounts()).containsExactly(3L, 2L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 1L)
    }

    @Test
    fun `같은 키의 줄을 두 번 넣으면 실패한다`() {
        // given
        entityManager.persist(aResult())
        entityManager.flush()

        // when & then
        assertThatThrownBy {
            entityManager.persist(aResult())
            entityManager.flush()
        }.isInstanceOfAny(DataIntegrityViolationException::class.java, org.hibernate.exception.ConstraintViolationException::class.java)
    }

    private fun aResult() =
        BrawlerBattleResultStatsEntity(
            brawlStarsPatchId = 1L,
            tierRange = tierRange,
            eventBrawlStarsId = 100L,
            brawlerBrawlStarsId = 10L,
            resultCount = ResultCountEmbedded.create(1L, 0L, 0L),
            starPlayerCount = 0L,
        )

    private fun aRank(rankCounts: List<Long>) =
        BrawlerBattleRankStatsEntity(
            brawlStarsPatchId = 1L,
            tierRange = tierRange,
            eventBrawlStarsId = 100L,
            brawlerBrawlStarsId = 10L,
            rankCounts = rankCounts,
        )

    private fun aPair(
        brawler1BrawlStarsId: Long,
        brawler2BrawlStarsId: Long,
    ) = BrawlerPairBattleResultStatsEntity(
        brawlStarsPatchId = 1L,
        tierRange = tierRange,
        brawler1BrawlStarsId = brawler1BrawlStarsId,
        brawler2BrawlStarsId = brawler2BrawlStarsId,
        resultCount = ResultCountEmbedded.create(1L, 0L, 0L),
    )

    private fun aTrio(
        brawler1BrawlStarsId: Long,
        brawler2BrawlStarsId: Long,
        brawler3BrawlStarsId: Long,
    ) = BrawlerTrioBattleResultStatsEntity(
        brawlStarsPatchId = 1L,
        tierRange = tierRange,
        brawler1BrawlStarsId = brawler1BrawlStarsId,
        brawler2BrawlStarsId = brawler2BrawlStarsId,
        brawler3BrawlStarsId = brawler3BrawlStarsId,
        resultCount = ResultCountEmbedded.create(1L, 0L, 0L),
    )
}
