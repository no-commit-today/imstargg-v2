package com.imstargg.core.application.brawlstars

import com.imstargg.CoreApiContextTest
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.enums.Country
import com.imstargg.infrastructure.db.core.brawlstars.ranking.RankingEntityPlayer
import com.imstargg.infrastructure.db.core.test.TestDataManager
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
import java.time.OffsetDateTime

class RankingRepositoryTest(
    private val sut: RankingRepository,
    private val testDataManager: TestDataManager,
) : CoreApiContextTest() {
    @BeforeEach
    fun setUp() {
        testDataManager.cleanUp()
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
                    .withCollectedAt(OffsetDateTime.now())
                    .build()
            testDataManager.add(ranking)

            val item =
                aPlayerRankingItemEntity()
                    .withPlayerRankingId(ranking.id!!)
                    .withRank(1)
                    .withPlayer(createPlayer("#PLAYER1"))
                    .build()
            testDataManager.add(item)

            // when
            val result = sut.findPlayerRankings(Country.GLOBAL)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].rankChange).isNull()
        }

        @Test
        fun `이전 랭킹에 없던 플레이어의 rankChange는 null이다`() {
            // given
            val now = OffsetDateTime.now()
            val previousRanking =
                aPlayerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(now.minusHours(1))
                    .build()
            testDataManager.add(previousRanking)

            val previousItem =
                aPlayerRankingItemEntity()
                    .withPlayerRankingId(previousRanking.id!!)
                    .withRank(1)
                    .withPlayer(createPlayer("#PLAYER1"))
                    .build()
            testDataManager.add(previousItem)

            val latestRanking =
                aPlayerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(now)
                    .build()
            testDataManager.add(latestRanking)

            val newPlayerItem =
                aPlayerRankingItemEntity()
                    .withPlayerRankingId(latestRanking.id!!)
                    .withRank(2)
                    .withPlayer(createPlayer("#NEWPLAYER"))
                    .build()
            testDataManager.add(newPlayerItem)

            // when
            val result = sut.findPlayerRankings(Country.GLOBAL)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].rankChange).isNull()
        }

        @Test
        fun `이전 랭킹과 순위가 같으면 rankChange는 0이다`() {
            // given
            val now = OffsetDateTime.now()
            val previousRanking =
                aPlayerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(now.minusHours(1))
                    .build()
            testDataManager.add(previousRanking)

            val previousItem =
                aPlayerRankingItemEntity()
                    .withPlayerRankingId(previousRanking.id!!)
                    .withRank(3)
                    .withPlayer(createPlayer("#PLAYER1"))
                    .build()
            testDataManager.add(previousItem)

            val latestRanking =
                aPlayerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(now)
                    .build()
            testDataManager.add(latestRanking)

            val latestItem =
                aPlayerRankingItemEntity()
                    .withPlayerRankingId(latestRanking.id!!)
                    .withRank(3)
                    .withPlayer(createPlayer("#PLAYER1"))
                    .build()
            testDataManager.add(latestItem)

            // when
            val result = sut.findPlayerRankings(Country.GLOBAL)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].rankChange).isEqualTo(0)
        }

        @Test
        fun `이전 랭킹보다 순위가 올랐으면 rankChange는 양수이다`() {
            // given
            val now = OffsetDateTime.now()
            val previousRanking =
                aPlayerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(now.minusHours(1))
                    .build()
            testDataManager.add(previousRanking)

            val previousItem =
                aPlayerRankingItemEntity()
                    .withPlayerRankingId(previousRanking.id!!)
                    .withRank(5)
                    .withPlayer(createPlayer("#PLAYER1"))
                    .build()
            testDataManager.add(previousItem)

            val latestRanking =
                aPlayerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(now)
                    .build()
            testDataManager.add(latestRanking)

            val latestItem =
                aPlayerRankingItemEntity()
                    .withPlayerRankingId(latestRanking.id!!)
                    .withRank(3)
                    .withPlayer(createPlayer("#PLAYER1"))
                    .build()
            testDataManager.add(latestItem)

            // when
            val result = sut.findPlayerRankings(Country.GLOBAL)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].rankChange).isEqualTo(2)
        }

        @Test
        fun `이전 랭킹보다 순위가 내렸으면 rankChange는 음수이다`() {
            // given
            val now = OffsetDateTime.now()
            val previousRanking =
                aPlayerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(now.minusHours(1))
                    .build()
            testDataManager.add(previousRanking)

            val previousItem =
                aPlayerRankingItemEntity()
                    .withPlayerRankingId(previousRanking.id!!)
                    .withRank(3)
                    .withPlayer(createPlayer("#PLAYER1"))
                    .build()
            testDataManager.add(previousItem)

            val latestRanking =
                aPlayerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(now)
                    .build()
            testDataManager.add(latestRanking)

            val latestItem =
                aPlayerRankingItemEntity()
                    .withPlayerRankingId(latestRanking.id!!)
                    .withRank(5)
                    .withPlayer(createPlayer("#PLAYER1"))
                    .build()
            testDataManager.add(latestItem)

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
                    .withCollectedAt(OffsetDateTime.now())
                    .build()
            testDataManager.add(ranking)

            val item =
                aBrawlerRankingItemEntity()
                    .withBrawlerRankingId(ranking.id!!)
                    .withRank(1)
                    .withPlayer(createPlayer("#PLAYER1"))
                    .build()
            testDataManager.add(item)

            // when
            val result = sut.findBrawlerRankings(Country.GLOBAL, brawlerId)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].rankChange).isNull()
        }

        @Test
        fun `이전 랭킹에 없던 플레이어의 rankChange는 null이다`() {
            // given
            val now = OffsetDateTime.now()
            val previousRanking =
                aBrawlerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withBrawlerBrawlStarsId(brawlerId.value)
                    .withCollectedAt(now.minusHours(1))
                    .build()
            testDataManager.add(previousRanking)

            val previousItem =
                aBrawlerRankingItemEntity()
                    .withBrawlerRankingId(previousRanking.id!!)
                    .withRank(1)
                    .withPlayer(createPlayer("#PLAYER1"))
                    .build()
            testDataManager.add(previousItem)

            val latestRanking =
                aBrawlerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withBrawlerBrawlStarsId(brawlerId.value)
                    .withCollectedAt(now)
                    .build()
            testDataManager.add(latestRanking)

            val newPlayerItem =
                aBrawlerRankingItemEntity()
                    .withBrawlerRankingId(latestRanking.id!!)
                    .withRank(2)
                    .withPlayer(createPlayer("#NEWPLAYER"))
                    .build()
            testDataManager.add(newPlayerItem)

            // when
            val result = sut.findBrawlerRankings(Country.GLOBAL, brawlerId)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].rankChange).isNull()
        }

        @Test
        fun `이전 랭킹과 순위가 같으면 rankChange는 0이다`() {
            // given
            val now = OffsetDateTime.now()
            val previousRanking =
                aBrawlerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withBrawlerBrawlStarsId(brawlerId.value)
                    .withCollectedAt(now.minusHours(1))
                    .build()
            testDataManager.add(previousRanking)

            val previousItem =
                aBrawlerRankingItemEntity()
                    .withBrawlerRankingId(previousRanking.id!!)
                    .withRank(3)
                    .withPlayer(createPlayer("#PLAYER1"))
                    .build()
            testDataManager.add(previousItem)

            val latestRanking =
                aBrawlerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withBrawlerBrawlStarsId(brawlerId.value)
                    .withCollectedAt(now)
                    .build()
            testDataManager.add(latestRanking)

            val latestItem =
                aBrawlerRankingItemEntity()
                    .withBrawlerRankingId(latestRanking.id!!)
                    .withRank(3)
                    .withPlayer(createPlayer("#PLAYER1"))
                    .build()
            testDataManager.add(latestItem)

            // when
            val result = sut.findBrawlerRankings(Country.GLOBAL, brawlerId)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].rankChange).isEqualTo(0)
        }

        @Test
        fun `이전 랭킹보다 순위가 올랐으면 rankChange는 양수이다`() {
            // given
            val now = OffsetDateTime.now()
            val previousRanking =
                aBrawlerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withBrawlerBrawlStarsId(brawlerId.value)
                    .withCollectedAt(now.minusHours(1))
                    .build()
            testDataManager.add(previousRanking)

            val previousItem =
                aBrawlerRankingItemEntity()
                    .withBrawlerRankingId(previousRanking.id!!)
                    .withRank(5)
                    .withPlayer(createPlayer("#PLAYER1"))
                    .build()
            testDataManager.add(previousItem)

            val latestRanking =
                aBrawlerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withBrawlerBrawlStarsId(brawlerId.value)
                    .withCollectedAt(now)
                    .build()
            testDataManager.add(latestRanking)

            val latestItem =
                aBrawlerRankingItemEntity()
                    .withBrawlerRankingId(latestRanking.id!!)
                    .withRank(3)
                    .withPlayer(createPlayer("#PLAYER1"))
                    .build()
            testDataManager.add(latestItem)

            // when
            val result = sut.findBrawlerRankings(Country.GLOBAL, brawlerId)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].rankChange).isEqualTo(2)
        }

        @Test
        fun `이전 랭킹보다 순위가 내렸으면 rankChange는 음수이다`() {
            // given
            val now = OffsetDateTime.now()
            val previousRanking =
                aBrawlerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withBrawlerBrawlStarsId(brawlerId.value)
                    .withCollectedAt(now.minusHours(1))
                    .build()
            testDataManager.add(previousRanking)

            val previousItem =
                aBrawlerRankingItemEntity()
                    .withBrawlerRankingId(previousRanking.id!!)
                    .withRank(3)
                    .withPlayer(createPlayer("#PLAYER1"))
                    .build()
            testDataManager.add(previousItem)

            val latestRanking =
                aBrawlerRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withBrawlerBrawlStarsId(brawlerId.value)
                    .withCollectedAt(now)
                    .build()
            testDataManager.add(latestRanking)

            val latestItem =
                aBrawlerRankingItemEntity()
                    .withBrawlerRankingId(latestRanking.id!!)
                    .withRank(5)
                    .withPlayer(createPlayer("#PLAYER1"))
                    .build()
            testDataManager.add(latestItem)

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
                    .withCollectedAt(OffsetDateTime.now())
                    .build()
            testDataManager.add(ranking)

            val item =
                aClubRankingItemEntity()
                    .withClubRankingId(ranking.id!!)
                    .withRank(1)
                    .withBrawlStarsTag("#TESTCLUB1")
                    .build()
            testDataManager.add(item)

            // when
            val result = sut.findClubRankings(Country.GLOBAL)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].rankChange).isNull()
        }

        @Test
        fun `이전 랭킹에 없던 클럽의 rankChange는 null이다`() {
            // given
            val now = OffsetDateTime.now()
            val previousRanking =
                aClubRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(now.minusHours(1))
                    .build()
            testDataManager.add(previousRanking)

            val previousItem =
                aClubRankingItemEntity()
                    .withClubRankingId(previousRanking.id!!)
                    .withRank(1)
                    .withBrawlStarsTag("#TESTCLUB1")
                    .build()
            testDataManager.add(previousItem)

            val latestRanking =
                aClubRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(now)
                    .build()
            testDataManager.add(latestRanking)

            val newClubItem =
                aClubRankingItemEntity()
                    .withClubRankingId(latestRanking.id!!)
                    .withRank(2)
                    .withBrawlStarsTag("#NEWCLUB")
                    .build()
            testDataManager.add(newClubItem)

            // when
            val result = sut.findClubRankings(Country.GLOBAL)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].rankChange).isNull()
        }

        @Test
        fun `이전 랭킹과 순위가 같으면 rankChange는 0이다`() {
            // given
            val now = OffsetDateTime.now()
            val previousRanking =
                aClubRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(now.minusHours(1))
                    .build()
            testDataManager.add(previousRanking)

            val previousItem =
                aClubRankingItemEntity()
                    .withClubRankingId(previousRanking.id!!)
                    .withRank(3)
                    .withBrawlStarsTag("#TESTCLUB1")
                    .build()
            testDataManager.add(previousItem)

            val latestRanking =
                aClubRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(now)
                    .build()
            testDataManager.add(latestRanking)

            val latestItem =
                aClubRankingItemEntity()
                    .withClubRankingId(latestRanking.id!!)
                    .withRank(3)
                    .withBrawlStarsTag("#TESTCLUB1")
                    .build()
            testDataManager.add(latestItem)

            // when
            val result = sut.findClubRankings(Country.GLOBAL)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].rankChange).isEqualTo(0)
        }

        @Test
        fun `이전 랭킹보다 순위가 올랐으면 rankChange는 양수이다`() {
            // given
            val now = OffsetDateTime.now()
            val previousRanking =
                aClubRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(now.minusHours(1))
                    .build()
            testDataManager.add(previousRanking)

            val previousItem =
                aClubRankingItemEntity()
                    .withClubRankingId(previousRanking.id!!)
                    .withRank(5)
                    .withBrawlStarsTag("#TESTCLUB1")
                    .build()
            testDataManager.add(previousItem)

            val latestRanking =
                aClubRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(now)
                    .build()
            testDataManager.add(latestRanking)

            val latestItem =
                aClubRankingItemEntity()
                    .withClubRankingId(latestRanking.id!!)
                    .withRank(3)
                    .withBrawlStarsTag("#TESTCLUB1")
                    .build()
            testDataManager.add(latestItem)

            // when
            val result = sut.findClubRankings(Country.GLOBAL)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].rankChange).isEqualTo(2)
        }

        @Test
        fun `이전 랭킹보다 순위가 내렸으면 rankChange는 음수이다`() {
            // given
            val now = OffsetDateTime.now()
            val previousRanking =
                aClubRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(now.minusHours(1))
                    .build()
            testDataManager.add(previousRanking)

            val previousItem =
                aClubRankingItemEntity()
                    .withClubRankingId(previousRanking.id!!)
                    .withRank(3)
                    .withBrawlStarsTag("#TESTCLUB1")
                    .build()
            testDataManager.add(previousItem)

            val latestRanking =
                aClubRankingEntity()
                    .withCountry(Country.GLOBAL)
                    .withCollectedAt(now)
                    .build()
            testDataManager.add(latestRanking)

            val latestItem =
                aClubRankingItemEntity()
                    .withClubRankingId(latestRanking.id!!)
                    .withRank(5)
                    .withBrawlStarsTag("#TESTCLUB1")
                    .build()
            testDataManager.add(latestItem)

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
