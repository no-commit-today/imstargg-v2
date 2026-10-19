package com.imstargg.core.batch.job.brawlstars.task

import com.imstargg.CoreBatchContextTest
import com.imstargg.client.brawlstars.BrawlStarsClient
import com.imstargg.client.brawlstars.response.AccessoryResponse
import com.imstargg.client.brawlstars.response.BrawlerResponse
import com.imstargg.client.brawlstars.response.GearStatResponse
import com.imstargg.client.brawlstars.response.HyperChargeResponse
import com.imstargg.client.brawlstars.response.ListResponse
import com.imstargg.client.brawlstars.response.StarPowerResponse
import com.imstargg.core.batch.support.alert.AlertCommand
import com.imstargg.core.batch.support.alert.AlertManager
import com.imstargg.core.enums.BrawlerRarity
import com.imstargg.core.enums.BrawlerRole
import com.imstargg.infrastructure.db.core.brawlstars.brawler.BrawlerEntity
import com.imstargg.infrastructure.db.core.brawlstars.brawler.BrawlerGearEntity
import com.imstargg.infrastructure.db.core.brawlstars.brawler.StarPowerEntity
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.BrawlerGearJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.BrawlerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.StarPowerJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Clock

class BrawlerSyncTaskTest
    @Autowired
    constructor(
        private val sut: BrawlerSyncTask,
        private val brawlStarsClient: BrawlStarsClient,
        private val brawlerJpaRepository: BrawlerJpaRepository,
        private val starPowerJpaRepository: StarPowerJpaRepository,
        private val brawlerGearJpaRepository: BrawlerGearJpaRepository,
        private val cleanUp: CleanUp,
    ) : CoreBatchContextTest() {
        @MockkBean(relaxUnitFun = true)
        private lateinit var alertManager: AlertManager

        @AfterEach
        fun tearDown() {
            cleanUp.all()
        }

        @Test
        fun `새로운 브롤러를 감지하면 신규 알림을 발송한다`() {
            // given
            every { brawlStarsClient.getBrawlers() } returns
                ListResponse(items = listOf(shelly()), paging = null)

            // when
            sut.sync()

            // then
            val alerts = mutableListOf<AlertCommand>()
            verify(exactly = 1) { alertManager.alert(capture(alerts)) }
            assertThat(alerts[0].title).contains("새로운 브롤러 발견")
            assertThat(alerts[0].content).contains("Shelly")
        }

        @Test
        fun `삭제된 브롤러를 감지하면 삭제 알림을 발송한다`() {
            // given
            brawlerJpaRepository.save(
                BrawlerEntity(brawlStarsId = 16000000, rarity = BrawlerRarity.STARTING_BRAWLER, role = BrawlerRole.DAMAGE_DEALER),
            )
            every { brawlStarsClient.getBrawlers() } returns
                ListResponse(items = emptyList(), paging = null)

            // when
            sut.sync()

            // then
            val alerts = mutableListOf<AlertCommand>()
            verify(exactly = 1) { alertManager.alert(capture(alerts)) }
            assertThat(alerts[0].title).contains("브롤러 삭제 감지")
            assertThat(alerts[0].content).contains("brawlStarsId=16000000")
        }

        @Test
        fun `기존 브롤러의 항목 추가와 삭제를 감지하면 변경 알림을 발송한다`() {
            // given
            brawlerJpaRepository.save(
                BrawlerEntity(brawlStarsId = 16000000, rarity = BrawlerRarity.STARTING_BRAWLER, role = BrawlerRole.DAMAGE_DEALER),
            )
            starPowerJpaRepository.save(StarPowerEntity(brawlStarsId = 23000076, brawlerBrawlStarsId = 16000000))
            brawlerGearJpaRepository.save(BrawlerGearEntity(brawlerBrawlStarsId = 16000000, gearBrawlStarsId = 62000001))
            brawlerGearJpaRepository.save(BrawlerGearEntity(brawlerBrawlStarsId = 16000000, gearBrawlStarsId = 62000002))
            every { brawlStarsClient.getBrawlers() } returns
                ListResponse(
                    items =
                        listOf(
                            shelly(
                                starPowers =
                                    listOf(
                                        StarPowerResponse(id = 23000076, name = "Shell Shock"),
                                        StarPowerResponse(id = 23000077, name = "Band-Aid"),
                                    ),
                                gears = listOf(GearStatResponse(id = 62000001, name = "Speed", level = 1)),
                            ),
                        ),
                    paging = null,
                )

            // when
            sut.sync()

            // then
            val alerts = mutableListOf<AlertCommand>()
            verify(exactly = 1) { alertManager.alert(capture(alerts)) }
            assertThat(alerts[0].title).contains("브롤러 변경 감지")
            assertThat(alerts[0].content).contains("Shelly(16000000) 스타파워 추가 Band-Aid(brawlStarsId=23000077)")
            assertThat(alerts[0].content).contains("Shelly(16000000) 기어 제거 brawlStarsId=62000002")
        }

        @Test
        fun `변경이 없으면 알림을 발송하지 않는다`() {
            // given
            brawlerJpaRepository.save(
                BrawlerEntity(brawlStarsId = 16000000, rarity = BrawlerRarity.STARTING_BRAWLER, role = BrawlerRole.DAMAGE_DEALER),
            )
            starPowerJpaRepository.save(StarPowerEntity(brawlStarsId = 23000076, brawlerBrawlStarsId = 16000000))
            every { brawlStarsClient.getBrawlers() } returns
                ListResponse(
                    items =
                        listOf(
                            shelly(starPowers = listOf(StarPowerResponse(id = 23000076, name = "Shell Shock"))),
                        ),
                    paging = null,
                )

            // when
            sut.sync()

            // then
            verify(exactly = 0) { alertManager.alert(any()) }
        }

        @Test
        fun `soft-delete 된 기어가 API 에 남아 있으면 알림을 발송하지 않는다`() {
            // given
            brawlerJpaRepository.save(
                BrawlerEntity(brawlStarsId = 16000000, rarity = BrawlerRarity.STARTING_BRAWLER, role = BrawlerRole.DAMAGE_DEALER),
            )
            brawlerGearJpaRepository.save(
                BrawlerGearEntity(brawlerBrawlStarsId = 16000000, gearBrawlStarsId = 62000005).apply { delete(Clock.systemUTC()) },
            )
            every { brawlStarsClient.getBrawlers() } returns
                ListResponse(
                    items = listOf(shelly(gears = listOf(GearStatResponse(id = 62000005, name = "RELOAD SPEED", level = 1)))),
                    paging = null,
                )

            // when
            sut.sync()

            // then
            verify(exactly = 0) { alertManager.alert(any()) }
        }

        private fun shelly(
            starPowers: List<StarPowerResponse> = emptyList(),
            gadgets: List<AccessoryResponse> = emptyList(),
            hyperCharges: List<HyperChargeResponse> = emptyList(),
            gears: List<GearStatResponse> = emptyList(),
        ): BrawlerResponse =
            BrawlerResponse(
                id = 16000000,
                name = "Shelly",
                starPowers = starPowers,
                gadgets = gadgets,
                hyperCharges = hyperCharges,
                gears = gears,
            )
    }
