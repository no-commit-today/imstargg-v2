package com.imstargg.core.worker.application.player

import com.imstargg.CoreWorkerContextTest
import com.imstargg.client.brawlstars.response.AccessoryResponse
import com.imstargg.client.brawlstars.response.BrawlerBuffiesResponse
import com.imstargg.client.brawlstars.response.BrawlerStatResponse
import com.imstargg.client.brawlstars.response.GearStatResponse
import com.imstargg.client.brawlstars.response.PlayerIconResponse
import com.imstargg.client.brawlstars.response.PlayerResponse
import com.imstargg.client.brawlstars.response.SkinResponse
import com.imstargg.core.enums.BuffyType
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerBuffyJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerGadgetJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerGearJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerHyperChargeJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.player.repository.PlayerBrawlerStarPowerJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/**
 * 액세서리 5종의 저장 동작을 고정한다 — 신규 플레이어는 전량 INSERT, 재갱신 시에는 기존 액세서리를
 * 중복 INSERT 하지 않고 추가분만 저장([Player.update] 의 기존 액세서리 필터). 액세서리는 SEQUENCE PK
 * 기반 배치 INSERT 경로(V260804a)라 이 필터가 깨지면 유니크 충돌로 갱신이 실패한다.
 */
class PlayerUpdaterAccessoryTest(
    private val sut: PlayerUpdater,
    private val cleanUp: CleanUp,
    private val playerBrawlerRepository: PlayerBrawlerJpaRepository,
    private val gearRepository: PlayerBrawlerGearJpaRepository,
    private val starPowerRepository: PlayerBrawlerStarPowerJpaRepository,
    private val gadgetRepository: PlayerBrawlerGadgetJpaRepository,
    private val hyperChargeRepository: PlayerBrawlerHyperChargeJpaRepository,
    private val buffyRepository: PlayerBrawlerBuffyJpaRepository,
) : CoreWorkerContextTest() {
    @AfterEach
    fun tearDown() {
        cleanUp.all()
    }

    @Test
    fun `신규_플레이어의_액세서리를_모두_저장한다`() {
        // given
        val tag = "#ACCESSORY1"
        val response =
            createPlayerResponse(
                tag = tag,
                brawlers =
                    listOf(
                        createBrawlerStatResponse(
                            brawlerId = 1L,
                            gearIds = listOf(10L, 11L),
                            starPowerIds = listOf(20L),
                            gadgetIds = listOf(30L, 31L),
                            hyperChargeIds = listOf(40L),
                            buffyTypes = setOf(BuffyType.GADGET),
                        ),
                        createBrawlerStatResponse(brawlerId = 2L, gearIds = listOf(12L)),
                    ),
            )

        // when
        sut.update(response, emptyList())

        // then
        assertThat(playerBrawlerRepository.count()).isEqualTo(2)
        assertThat(gearRepository.count()).isEqualTo(3)
        assertThat(starPowerRepository.count()).isEqualTo(1)
        assertThat(gadgetRepository.count()).isEqualTo(2)
        assertThat(hyperChargeRepository.count()).isEqualTo(1)
        assertThat(buffyRepository.count()).isEqualTo(1)

        val gear = gearRepository.findAll().first { it.gearBrawlStarsId == 10L }
        assertThat(gear.playerBrawler.id).isNotNull()
        assertThat(gear.createdAt).isNotNull()
        assertThat(buffyRepository.findAll().single().buffyType).isEqualTo(BuffyType.GADGET)
    }

    @Test
    fun `재갱신하면_기존_액세서리는_중복_저장하지_않고_신규만_추가한다`() {
        // given
        val tag = "#ACCESSORY2"
        sut.update(
            createPlayerResponse(
                tag = tag,
                brawlers =
                    listOf(
                        createBrawlerStatResponse(
                            brawlerId = 1L,
                            gearIds = listOf(10L),
                            starPowerIds = listOf(20L),
                            buffyTypes = setOf(BuffyType.GADGET),
                        ),
                    ),
            ),
            emptyList(),
        )

        // when
        sut.update(
            createPlayerResponse(
                tag = tag,
                brawlers =
                    listOf(
                        createBrawlerStatResponse(
                            brawlerId = 1L,
                            gearIds = listOf(10L, 11L),
                            starPowerIds = listOf(20L),
                            buffyTypes = setOf(BuffyType.GADGET, BuffyType.STAR_POWER),
                        ),
                    ),
            ),
            emptyList(),
        )

        // then
        assertThat(playerBrawlerRepository.count()).isEqualTo(1)
        assertThat(gearRepository.findAll().map { it.gearBrawlStarsId }).containsExactlyInAnyOrder(10L, 11L)
        assertThat(starPowerRepository.findAll().map { it.starPowerBrawlStarsId }).containsExactly(20L)
        assertThat(buffyRepository.findAll().map { it.buffyType })
            .containsExactlyInAnyOrder(BuffyType.GADGET, BuffyType.STAR_POWER)
    }

    private fun createPlayerResponse(
        tag: String,
        brawlers: List<BrawlerStatResponse>,
    ) = PlayerResponse(
        tag = tag,
        name = "Test",
        nameColor = "0xff0000ff",
        icon = PlayerIconResponse(id = 1L),
        trophies = 1000,
        highestTrophies = 1200,
        expLevel = 50,
        expPoints = 5000,
        isQualifiedFromChampionshipChallenge = false,
        victories3vs3 = 10,
        victoriesSolo = 5,
        victoriesDuo = 3,
        bestRoboRumbleTime = 0,
        bestTimeAsBigBrawler = 0,
        totalPrestigeLevel = 0,
        club = null,
        brawlers = brawlers,
    )

    private fun createBrawlerStatResponse(
        brawlerId: Long,
        gearIds: List<Long> = emptyList(),
        starPowerIds: List<Long> = emptyList(),
        gadgetIds: List<Long> = emptyList(),
        hyperChargeIds: List<Long> = emptyList(),
        buffyTypes: Set<BuffyType> = emptySet(),
    ) = BrawlerStatResponse(
        id = brawlerId,
        name = "Brawler$brawlerId",
        power = 11,
        rank = 20,
        trophies = 500,
        highestTrophies = 600,
        prestigeLevel = 0,
        maxWinStreak = 5,
        currentWinStreak = 0,
        buffies =
            BrawlerBuffiesResponse(
                hyperCharge = BuffyType.HYPER_CHARGE in buffyTypes,
                gadget = BuffyType.GADGET in buffyTypes,
                starPower = BuffyType.STAR_POWER in buffyTypes,
            ),
        skin = SkinResponse(id = 1L, name = "Default"),
        gears = gearIds.map { GearStatResponse(id = it, name = "Gear$it", level = 1) },
        starPowers = starPowerIds.map { AccessoryResponse(id = it, name = "StarPower$it") },
        gadgets = gadgetIds.map { AccessoryResponse(id = it, name = "Gadget$it") },
        hyperCharges = hyperChargeIds.map { AccessoryResponse(id = it, name = "HyperCharge$it") },
    )
}
