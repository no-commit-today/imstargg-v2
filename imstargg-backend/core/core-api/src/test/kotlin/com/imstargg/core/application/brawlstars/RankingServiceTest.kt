package com.imstargg.core.application.brawlstars

import com.imstargg.CoreApiContextTest
import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.brawlstars.ranking.RankingEntityPlayer
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.PlayerRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.PlayerRankingJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.PlayerRankingEntityFixture.Companion.aPlayerRankingEntity
import com.imstargg.infrastructure.db.core.test.fixture.PlayerRankingItemEntityFixture.Companion.aPlayerRankingItemEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class RankingServiceTest(
    private val sut: RankingService,
    private val cleanUp: CleanUp,
    private val playerRankingJpaRepository: PlayerRankingJpaRepository,
    private val playerRankingItemJpaRepository: PlayerRankingItemJpaRepository,
) : CoreApiContextTest() {
    @BeforeEach
    fun setUp() {
        cleanUp.all()
    }

    @Test
    fun `최신 랭킹 데이터가 없으면 빈 리스트가 반환된다`() {
        // when
        val result = sut.getPlayerRankings(Country.GLOBAL)

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `Player 랭킹은 rank 오름차순으로 정렬되어 반환된다`() {
        // given
        val ranking =
            aPlayerRankingEntity()
                .withCountry(Country.GLOBAL)
                .withCollectedAt(Instant.now())
                .build()
        playerRankingJpaRepository.save(ranking)

        // 임의 순서(3, 1, 2)로 저장
        listOf(3, 1, 2).forEach { rank ->
            playerRankingItemJpaRepository.save(
                aPlayerRankingItemEntity()
                    .withPlayerRankingId(ranking.id!!)
                    .withRank(rank)
                    .withPlayer(rankingPlayer("#R${"%03d".format(rank)}"))
                    .build(),
            )
        }

        // when
        val result = sut.getPlayerRankings(Country.GLOBAL)

        // then
        assertThat(result)
            .extracting<Int> { it.rank }
            .containsExactly(1, 2, 3)
    }

    private fun rankingPlayer(tag: String) =
        RankingEntityPlayer(
            brawlStarsTag = tag,
            name = "플레이어$tag",
            nameColor = "0xffffffff",
            iconBrawlStarsId = 28000000L,
            clubName = "테스트클럽",
        )
}
