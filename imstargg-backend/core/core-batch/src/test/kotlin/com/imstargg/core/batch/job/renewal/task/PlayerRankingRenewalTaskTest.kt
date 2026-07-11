package com.imstargg.core.batch.job.renewal.task

import com.imstargg.CoreBatchContextTest
import com.imstargg.core.batch.support.coreapi.CoreApiClient
import com.imstargg.core.batch.support.coreapi.PlayerRenewalResult
import com.imstargg.core.enums.Country
import com.imstargg.core.enums.RenewalType
import com.imstargg.infrastructure.db.core.brawlstars.ranking.PlayerRankingEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.PlayerRankingItemEntity
import com.imstargg.infrastructure.db.core.brawlstars.ranking.RankingEntityPlayer
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.PlayerRankingItemJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.ranking.repository.PlayerRankingJpaRepository
import com.imstargg.infrastructure.db.core.renewal.RenewalEntity
import com.imstargg.infrastructure.db.core.renewal.repository.RenewalJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Clock
import java.time.Duration
import java.time.ZoneOffset

class PlayerRankingRenewalTaskTest
    @Autowired
    constructor(
        private val sut: PlayerRankingRenewalTask,
        private val playerRankingJpaRepository: PlayerRankingJpaRepository,
        private val playerRankingItemJpaRepository: PlayerRankingItemJpaRepository,
        private val renewalJpaRepository: RenewalJpaRepository,
        private val clock: Clock,
        private val cleanUp: CleanUp,
    ) : CoreBatchContextTest() {
        @MockkBean
        private lateinit var coreApiClient: CoreApiClient

        @AfterEach
        fun tearDown() {
            cleanUp.all()
        }

        private fun saveRanking(
            country: Country,
            vararg tags: String,
            collectedAtOffset: Duration = Duration.ZERO,
        ): PlayerRankingEntity {
            val ranking =
                playerRankingJpaRepository.save(
                    PlayerRankingEntity(country = country, collectedAt = clock.instant().plus(collectedAtOffset)),
                )
            playerRankingItemJpaRepository.saveAll(
                tags.mapIndexed { index, tag ->
                    PlayerRankingItemEntity(
                        playerRanking = ranking,
                        rank = index + 1,
                        player =
                            RankingEntityPlayer(
                                brawlStarsTag = tag,
                                name = "name$index",
                                nameColor = null,
                                iconBrawlStarsId = 28000000,
                                clubName = "",
                            ),
                        trophies = 10000,
                    )
                },
            )
            return ranking
        }

        private fun saveCompletedRenewal(
            tag: String,
            completedAgo: Duration,
        ) {
            val entity = RenewalEntity(brawlStarsTag = tag, type = RenewalType.PLAYER, requestedAt = clock.instant())
            entity.toPending()
            entity.toExecuting(clock)
            entity.toCompleted(Clock.fixed(clock.instant().minus(completedAgo), ZoneOffset.UTC))
            renewalJpaRepository.save(entity)
        }

        @Test
        fun `여러 국가 랭킹의 태그를 중복 제거해 한 번씩만 갱신 요청한다`() {
            // given
            saveRanking(Country.GLOBAL, "#AAA", "#BBB")
            saveRanking(Country.KOREA, "#BBB", "#CCC")
            every { coreApiClient.renewPlayer(any()) } returns PlayerRenewalResult.ACCEPTED

            // when
            sut.renewAll()

            // then
            verify(exactly = 1) { coreApiClient.renewPlayer("#AAA") }
            verify(exactly = 1) { coreApiClient.renewPlayer("#BBB") }
            verify(exactly = 1) { coreApiClient.renewPlayer("#CCC") }
        }

        @Test
        fun `24시간 이내 갱신 완료된 태그는 요청하지 않는다`() {
            // given
            saveRanking(Country.GLOBAL, "#FRESH", "#STALE", "#NEVER")
            saveCompletedRenewal("#FRESH", completedAgo = Duration.ofHours(1))
            saveCompletedRenewal("#STALE", completedAgo = Duration.ofHours(25))
            every { coreApiClient.renewPlayer(any()) } returns PlayerRenewalResult.ACCEPTED

            // when
            sut.renewAll()

            // then
            verify(exactly = 0) { coreApiClient.renewPlayer("#FRESH") }
            verify(exactly = 1) { coreApiClient.renewPlayer("#STALE") }
            verify(exactly = 1) { coreApiClient.renewPlayer("#NEVER") }
        }

        @Test
        fun `점검 응답을 받으면 남은 요청을 중단한다`() {
            // given
            saveRanking(Country.GLOBAL, "#AAA", "#BBB", "#CCC")
            every { coreApiClient.renewPlayer(any()) } returns PlayerRenewalResult.API_UNDER_MAINTENANCE

            // when
            sut.renewAll()

            // then
            verify(exactly = 1) { coreApiClient.renewPlayer(any()) }
        }

        @Test
        fun `국가별 최신 스냅샷만 사용한다`() {
            // given
            saveRanking(Country.GLOBAL, "#OLD", collectedAtOffset = Duration.ofHours(-1))
            saveRanking(Country.GLOBAL, "#NEW")
            every { coreApiClient.renewPlayer(any()) } returns PlayerRenewalResult.ACCEPTED

            // when
            sut.renewAll()

            // then
            verify(exactly = 0) { coreApiClient.renewPlayer("#OLD") }
            verify(exactly = 1) { coreApiClient.renewPlayer("#NEW") }
        }
    }
