package com.imstargg.core.application.brawlstars

import com.imstargg.CoreApiContextTest
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.brawlstars.ranking.RankingEntityPlayer
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.BrawlerRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.BrawlerRankingJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.ClubRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.ClubRankingJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.PlayerRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.PlayerRankingJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.imstargg.infrastructure.db.core.test.fixture.BrawlerRankingEntityFixture.Companion.aBrawlerRankingEntity
import com.imstargg.infrastructure.db.core.test.fixture.BrawlerRankingItemEntityFixture.Companion.aBrawlerRankingItemEntity
import com.imstargg.infrastructure.db.core.test.fixture.ClubRankingEntityFixture.Companion.aClubRankingEntity
import com.imstargg.infrastructure.db.core.test.fixture.ClubRankingItemEntityFixture.Companion.aClubRankingItemEntity
import com.imstargg.infrastructure.db.core.test.fixture.PlayerRankingEntityFixture.Companion.aPlayerRankingEntity
import com.imstargg.infrastructure.db.core.test.fixture.PlayerRankingItemEntityFixture.Companion.aPlayerRankingItemEntity
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class RankingRepositoryTest(
    private val sut: RankingRepository,
    private val cleanUp: CleanUp,
    private val playerRankingRepository: PlayerRankingJpaRepository,
    private val playerRankingItemRepository: PlayerRankingItemJpaRepository,
    private val brawlerRankingRepository: BrawlerRankingJpaRepository,
    private val brawlerRankingItemRepository: BrawlerRankingItemJpaRepository,
    private val clubRankingRepository: ClubRankingJpaRepository,
    private val clubRankingItemRepository: ClubRankingItemJpaRepository,
) : CoreApiContextTest() {
    @BeforeEach
    fun setUp() {
        cleanUp.all()
    }

    @Nested
    inner class FindPlayerRankings {
        @Test
        fun `최신 랭킹 데이터가 없으면 빈 리스트를 반환한다`() {
            // given
            // no data

            // when
            val result = sut.findPlayerRankings(Country.GLOBAL)

            // then
            assertThat(result).isEmpty()
        }

        @Test
        fun `이전 랭킹 데이터가 없으면 모든 rankChange가 null이다`() {
            // given
            val ranking =
                aPlayerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(Instant.now())
                    .build()
            playerRankingRepository.save(ranking)

            val item =
                aPlayerRankingItemEntity()
                    .withPlayerRanking(ranking)
                    .withRank(1)
                    .withPlayer(createPlayer("#PLAYER1"))
                    .build()
            playerRankingItemRepository.save(item)

            // when
            val result = sut.findPlayerRankings(Country.GLOBAL)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].rankChange).isNull()
        }

        @Test
        fun `이전 랭킹에 없던 플레이어의 rankChange는 null이다`() {
            // given
            val now = Instant.now()
            val previousRanking =
                aPlayerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(now.minus(Duration.ofHours(1)))
                    .build()
            playerRankingRepository.save(previousRanking)

            val previousItem =
                aPlayerRankingItemEntity()
                    .withPlayerRanking(previousRanking)
                    .withRank(1)
                    .withPlayer(createPlayer("#PLAYER1"))
                    .build()
            playerRankingItemRepository.save(previousItem)

            val latestRanking =
                aPlayerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(now)
                    .build()
            playerRankingRepository.save(latestRanking)

            val newPlayerItem =
                aPlayerRankingItemEntity()
                    .withPlayerRanking(latestRanking)
                    .withRank(2)
                    .withPlayer(createPlayer("#NEWPLAYER"))
                    .build()
            playerRankingItemRepository.save(newPlayerItem)

            // when
            val result = sut.findPlayerRankings(Country.GLOBAL)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].rankChange).isNull()
        }

        @Test
        fun `이전 랭킹과 순위가 같으면 rankChange는 0이다`() {
            // given
            val now = Instant.now()
            val previousRanking =
                aPlayerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(now.minus(Duration.ofHours(1)))
                    .build()
            playerRankingRepository.save(previousRanking)

            val previousItem =
                aPlayerRankingItemEntity()
                    .withPlayerRanking(previousRanking)
                    .withRank(3)
                    .withPlayer(createPlayer("#PLAYER1"))
                    .build()
            playerRankingItemRepository.save(previousItem)

            val latestRanking =
                aPlayerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(now)
                    .build()
            playerRankingRepository.save(latestRanking)

            val latestItem =
                aPlayerRankingItemEntity()
                    .withPlayerRanking(latestRanking)
                    .withRank(3)
                    .withPlayer(createPlayer("#PLAYER1"))
                    .build()
            playerRankingItemRepository.save(latestItem)

            // when
            val result = sut.findPlayerRankings(Country.GLOBAL)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].rankChange).isEqualTo(0)
        }

        @Test
        fun `이전 랭킹보다 순위가 올랐으면 rankChange는 양수이다`() {
            // given
            val now = Instant.now()
            val previousRanking =
                aPlayerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(now.minus(Duration.ofHours(1)))
                    .build()
            playerRankingRepository.save(previousRanking)

            val previousItem =
                aPlayerRankingItemEntity()
                    .withPlayerRanking(previousRanking)
                    .withRank(5)
                    .withPlayer(createPlayer("#PLAYER1"))
                    .build()
            playerRankingItemRepository.save(previousItem)

            val latestRanking =
                aPlayerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(now)
                    .build()
            playerRankingRepository.save(latestRanking)

            val latestItem =
                aPlayerRankingItemEntity()
                    .withPlayerRanking(latestRanking)
                    .withRank(3)
                    .withPlayer(createPlayer("#PLAYER1"))
                    .build()
            playerRankingItemRepository.save(latestItem)

            // when
            val result = sut.findPlayerRankings(Country.GLOBAL)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].rankChange).isEqualTo(2)
        }

        @Test
        fun `이전 랭킹보다 순위가 내렸으면 rankChange는 음수이다`() {
            // given
            val now = Instant.now()
            val previousRanking =
                aPlayerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(now.minus(Duration.ofHours(1)))
                    .build()
            playerRankingRepository.save(previousRanking)

            val previousItem =
                aPlayerRankingItemEntity()
                    .withPlayerRanking(previousRanking)
                    .withRank(3)
                    .withPlayer(createPlayer("#PLAYER1"))
                    .build()
            playerRankingItemRepository.save(previousItem)

            val latestRanking =
                aPlayerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(now)
                    .build()
            playerRankingRepository.save(latestRanking)

            val latestItem =
                aPlayerRankingItemEntity()
                    .withPlayerRanking(latestRanking)
                    .withRank(5)
                    .withPlayer(createPlayer("#PLAYER1"))
                    .build()
            playerRankingItemRepository.save(latestItem)

            // when
            val result = sut.findPlayerRankings(Country.GLOBAL)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].rankChange).isEqualTo(-2)
        }
    }

    @Nested
    inner class FindBrawlerRankings {
        private val brawlerId = BrawlStarsId(16000000L)

        @Test
        fun `최신 랭킹 데이터가 없으면 빈 리스트를 반환한다`() {
            // given
            // no data

            // when
            val result = sut.findBrawlerRankings(Country.GLOBAL, brawlerId)

            // then
            assertThat(result).isEmpty()
        }

        @Test
        fun `이전 랭킹 데이터가 없으면 모든 rankChange가 null이다`() {
            // given
            val ranking =
                aBrawlerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withBrawlerBrawlStarsId(brawlerId.value)
                    .withCollectedAt(Instant.now())
                    .build()
            brawlerRankingRepository.save(ranking)

            val item =
                aBrawlerRankingItemEntity()
                    .withBrawlerRanking(ranking)
                    .withRank(1)
                    .withPlayer(createPlayer("#PLAYER1"))
                    .build()
            brawlerRankingItemRepository.save(item)

            // when
            val result = sut.findBrawlerRankings(Country.GLOBAL, brawlerId)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].rankChange).isNull()
        }

        @Test
        fun `이전 랭킹에 없던 플레이어의 rankChange는 null이다`() {
            // given
            val now = Instant.now()
            val previousRanking =
                aBrawlerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withBrawlerBrawlStarsId(brawlerId.value)
                    .withCollectedAt(now.minus(Duration.ofHours(1)))
                    .build()
            brawlerRankingRepository.save(previousRanking)

            val previousItem =
                aBrawlerRankingItemEntity()
                    .withBrawlerRanking(previousRanking)
                    .withRank(1)
                    .withPlayer(createPlayer("#PLAYER1"))
                    .build()
            brawlerRankingItemRepository.save(previousItem)

            val latestRanking =
                aBrawlerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withBrawlerBrawlStarsId(brawlerId.value)
                    .withCollectedAt(now)
                    .build()
            brawlerRankingRepository.save(latestRanking)

            val newPlayerItem =
                aBrawlerRankingItemEntity()
                    .withBrawlerRanking(latestRanking)
                    .withRank(2)
                    .withPlayer(createPlayer("#NEWPLAYER"))
                    .build()
            brawlerRankingItemRepository.save(newPlayerItem)

            // when
            val result = sut.findBrawlerRankings(Country.GLOBAL, brawlerId)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].rankChange).isNull()
        }

        @Test
        fun `이전 랭킹과 순위가 같으면 rankChange는 0이다`() {
            // given
            val now = Instant.now()
            val previousRanking =
                aBrawlerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withBrawlerBrawlStarsId(brawlerId.value)
                    .withCollectedAt(now.minus(Duration.ofHours(1)))
                    .build()
            brawlerRankingRepository.save(previousRanking)

            val previousItem =
                aBrawlerRankingItemEntity()
                    .withBrawlerRanking(previousRanking)
                    .withRank(3)
                    .withPlayer(createPlayer("#PLAYER1"))
                    .build()
            brawlerRankingItemRepository.save(previousItem)

            val latestRanking =
                aBrawlerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withBrawlerBrawlStarsId(brawlerId.value)
                    .withCollectedAt(now)
                    .build()
            brawlerRankingRepository.save(latestRanking)

            val latestItem =
                aBrawlerRankingItemEntity()
                    .withBrawlerRanking(latestRanking)
                    .withRank(3)
                    .withPlayer(createPlayer("#PLAYER1"))
                    .build()
            brawlerRankingItemRepository.save(latestItem)

            // when
            val result = sut.findBrawlerRankings(Country.GLOBAL, brawlerId)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].rankChange).isEqualTo(0)
        }

        @Test
        fun `이전 랭킹보다 순위가 올랐으면 rankChange는 양수이다`() {
            // given
            val now = Instant.now()
            val previousRanking =
                aBrawlerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withBrawlerBrawlStarsId(brawlerId.value)
                    .withCollectedAt(now.minus(Duration.ofHours(1)))
                    .build()
            brawlerRankingRepository.save(previousRanking)

            val previousItem =
                aBrawlerRankingItemEntity()
                    .withBrawlerRanking(previousRanking)
                    .withRank(5)
                    .withPlayer(createPlayer("#PLAYER1"))
                    .build()
            brawlerRankingItemRepository.save(previousItem)

            val latestRanking =
                aBrawlerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withBrawlerBrawlStarsId(brawlerId.value)
                    .withCollectedAt(now)
                    .build()
            brawlerRankingRepository.save(latestRanking)

            val latestItem =
                aBrawlerRankingItemEntity()
                    .withBrawlerRanking(latestRanking)
                    .withRank(3)
                    .withPlayer(createPlayer("#PLAYER1"))
                    .build()
            brawlerRankingItemRepository.save(latestItem)

            // when
            val result = sut.findBrawlerRankings(Country.GLOBAL, brawlerId)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].rankChange).isEqualTo(2)
        }

        @Test
        fun `이전 랭킹보다 순위가 내렸으면 rankChange는 음수이다`() {
            // given
            val now = Instant.now()
            val previousRanking =
                aBrawlerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withBrawlerBrawlStarsId(brawlerId.value)
                    .withCollectedAt(now.minus(Duration.ofHours(1)))
                    .build()
            brawlerRankingRepository.save(previousRanking)

            val previousItem =
                aBrawlerRankingItemEntity()
                    .withBrawlerRanking(previousRanking)
                    .withRank(3)
                    .withPlayer(createPlayer("#PLAYER1"))
                    .build()
            brawlerRankingItemRepository.save(previousItem)

            val latestRanking =
                aBrawlerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withBrawlerBrawlStarsId(brawlerId.value)
                    .withCollectedAt(now)
                    .build()
            brawlerRankingRepository.save(latestRanking)

            val latestItem =
                aBrawlerRankingItemEntity()
                    .withBrawlerRanking(latestRanking)
                    .withRank(5)
                    .withPlayer(createPlayer("#PLAYER1"))
                    .build()
            brawlerRankingItemRepository.save(latestItem)

            // when
            val result = sut.findBrawlerRankings(Country.GLOBAL, brawlerId)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].rankChange).isEqualTo(-2)
        }
    }

    @Nested
    inner class FindClubRankings {
        @Test
        fun `최신 랭킹 데이터가 없으면 빈 리스트를 반환한다`() {
            // given
            // no data

            // when
            val result = sut.findClubRankings(Country.GLOBAL)

            // then
            assertThat(result).isEmpty()
        }

        @Test
        fun `이전 랭킹 데이터가 없으면 모든 rankChange가 null이다`() {
            // given
            val ranking =
                aClubRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(Instant.now())
                    .build()
            clubRankingRepository.save(ranking)

            val item =
                aClubRankingItemEntity()
                    .withClubRanking(ranking)
                    .withRank(1)
                    .withBrawlStarsTag("#TESTCLUB1")
                    .build()
            clubRankingItemRepository.save(item)

            // when
            val result = sut.findClubRankings(Country.GLOBAL)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].rankChange).isNull()
        }

        @Test
        fun `이전 랭킹에 없던 클럽의 rankChange는 null이다`() {
            // given
            val now = Instant.now()
            val previousRanking =
                aClubRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(now.minus(Duration.ofHours(1)))
                    .build()
            clubRankingRepository.save(previousRanking)

            val previousItem =
                aClubRankingItemEntity()
                    .withClubRanking(previousRanking)
                    .withRank(1)
                    .withBrawlStarsTag("#TESTCLUB1")
                    .build()
            clubRankingItemRepository.save(previousItem)

            val latestRanking =
                aClubRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(now)
                    .build()
            clubRankingRepository.save(latestRanking)

            val newClubItem =
                aClubRankingItemEntity()
                    .withClubRanking(latestRanking)
                    .withRank(2)
                    .withBrawlStarsTag("#NEWCLUB")
                    .build()
            clubRankingItemRepository.save(newClubItem)

            // when
            val result = sut.findClubRankings(Country.GLOBAL)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].rankChange).isNull()
        }

        @Test
        fun `이전 랭킹과 순위가 같으면 rankChange는 0이다`() {
            // given
            val now = Instant.now()
            val previousRanking =
                aClubRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(now.minus(Duration.ofHours(1)))
                    .build()
            clubRankingRepository.save(previousRanking)

            val previousItem =
                aClubRankingItemEntity()
                    .withClubRanking(previousRanking)
                    .withRank(3)
                    .withBrawlStarsTag("#TESTCLUB1")
                    .build()
            clubRankingItemRepository.save(previousItem)

            val latestRanking =
                aClubRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(now)
                    .build()
            clubRankingRepository.save(latestRanking)

            val latestItem =
                aClubRankingItemEntity()
                    .withClubRanking(latestRanking)
                    .withRank(3)
                    .withBrawlStarsTag("#TESTCLUB1")
                    .build()
            clubRankingItemRepository.save(latestItem)

            // when
            val result = sut.findClubRankings(Country.GLOBAL)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].rankChange).isEqualTo(0)
        }

        @Test
        fun `이전 랭킹보다 순위가 올랐으면 rankChange는 양수이다`() {
            // given
            val now = Instant.now()
            val previousRanking =
                aClubRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(now.minus(Duration.ofHours(1)))
                    .build()
            clubRankingRepository.save(previousRanking)

            val previousItem =
                aClubRankingItemEntity()
                    .withClubRanking(previousRanking)
                    .withRank(5)
                    .withBrawlStarsTag("#TESTCLUB1")
                    .build()
            clubRankingItemRepository.save(previousItem)

            val latestRanking =
                aClubRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(now)
                    .build()
            clubRankingRepository.save(latestRanking)

            val latestItem =
                aClubRankingItemEntity()
                    .withClubRanking(latestRanking)
                    .withRank(3)
                    .withBrawlStarsTag("#TESTCLUB1")
                    .build()
            clubRankingItemRepository.save(latestItem)

            // when
            val result = sut.findClubRankings(Country.GLOBAL)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].rankChange).isEqualTo(2)
        }

        @Test
        fun `이전 랭킹보다 순위가 내렸으면 rankChange는 음수이다`() {
            // given
            val now = Instant.now()
            val previousRanking =
                aClubRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(now.minus(Duration.ofHours(1)))
                    .build()
            clubRankingRepository.save(previousRanking)

            val previousItem =
                aClubRankingItemEntity()
                    .withClubRanking(previousRanking)
                    .withRank(3)
                    .withBrawlStarsTag("#TESTCLUB1")
                    .build()
            clubRankingItemRepository.save(previousItem)

            val latestRanking =
                aClubRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(now)
                    .build()
            clubRankingRepository.save(latestRanking)

            val latestItem =
                aClubRankingItemEntity()
                    .withClubRanking(latestRanking)
                    .withRank(5)
                    .withBrawlStarsTag("#TESTCLUB1")
                    .build()
            clubRankingItemRepository.save(latestItem)

            // when
            val result = sut.findClubRankings(Country.GLOBAL)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].rankChange).isEqualTo(-2)
        }
    }

    private fun createPlayer(tag: String) =
        RankingEntityPlayer(
            brawlStarsTag = tag,
            name = "플레이어$tag",
            nameColor = "0xffffffff",
            iconBrawlStarsId = 28000000L,
            clubName = "테스트클럽",
        )
}
