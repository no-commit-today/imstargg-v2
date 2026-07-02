package com.imstargg.core.admin.application

import com.imstargg.CoreAdminContextTest
import com.imstargg.core.admin.request.BrawlerCreateRequest
import com.imstargg.core.admin.request.BrawlerSubItemCreateRequest
import com.imstargg.core.enums.BrawlerRarity
import com.imstargg.core.enums.BrawlerRole
import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.brawlstars.MessageType
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.BrawlerGearJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.BrawlerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.GadgetJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.HyperChargeJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.StarPowerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.repository.MessageJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BrawlerServiceTest
    @Autowired
    constructor(
        private val sut: BrawlerService,
        private val brawlerJpaRepository: BrawlerJpaRepository,
        private val brawlerGearJpaRepository: BrawlerGearJpaRepository,
        private val starPowerJpaRepository: StarPowerJpaRepository,
        private val gadgetJpaRepository: GadgetJpaRepository,
        private val hyperChargeJpaRepository: HyperChargeJpaRepository,
        private val messageJpaRepository: MessageJpaRepository,
        private val cleanUp: CleanUp,
    ) : CoreAdminContextTest() {
        @AfterEach
        fun tearDown() {
            cleanUp.all()
        }

        @Test
        fun `브롤러 등록 시 이름과 기어가 함께 저장된다`() {
            // given
            val request =
                BrawlerCreateRequest(
                    brawlStarsId = 16000000L,
                    rarity = BrawlerRarity.MYTHIC,
                    role = BrawlerRole.DAMAGE_DEALER,
                    names = mapOf(Language.KOREAN to "셸리", Language.ENGLISH to "Shelly"),
                    gearBrawlStarsIds = setOf(62000000L, 62000001L),
                )

            // when
            sut.createBrawler(request)

            // then
            assertThat(brawlerJpaRepository.findByBrawlStarsId(16000000L)).isNotNull
            val names =
                messageJpaRepository
                    .findAllByTypeAndBrawlStarsIdIn(MessageType.BRAWLER_NAME, listOf(16000000L))
                    .associate { it.lang to it.content }
            assertThat(names)
                .containsExactlyInAnyOrderEntriesOf(
                    mapOf(Language.KOREAN to "셸리", Language.ENGLISH to "Shelly"),
                )
            val gearIds =
                brawlerGearJpaRepository.findAllByBrawlerBrawlStarsId(16000000L).map { it.gearBrawlStarsId }
            assertThat(gearIds).containsExactlyInAnyOrder(62000000L, 62000001L)
        }

        @Test
        fun `스타파워 등록 시 이름이 함께 저장된다`() {
            // given
            val request =
                BrawlerSubItemCreateRequest(
                    brawlStarsId = 23000100L,
                    names = mapOf(Language.KOREAN to "조개 충격", Language.ENGLISH to "Shell Shock"),
                )

            // when
            sut.createStarPower(brawlerBrawlStarsId = 16000000L, request = request)

            // then
            assertThat(starPowerJpaRepository.findAllByBrawlerBrawlStarsId(16000000L)).hasSize(1)
            val names =
                messageJpaRepository
                    .findAllByTypeAndBrawlStarsIdIn(MessageType.STARPOWER_NAME, listOf(23000100L))
                    .associate { it.lang to it.content }
            assertThat(names)
                .containsExactlyInAnyOrderEntriesOf(
                    mapOf(Language.KOREAN to "조개 충격", Language.ENGLISH to "Shell Shock"),
                )
        }

        @Test
        fun `가젯과 하이퍼차지 등록 시 이름이 함께 저장된다`() {
            // given
            val gadgetRequest =
                BrawlerSubItemCreateRequest(
                    brawlStarsId = 23000200L,
                    names = mapOf(Language.KOREAN to "빠른 장전"),
                )
            val hyperChargeRequest =
                BrawlerSubItemCreateRequest(
                    brawlStarsId = 23000300L,
                    names = mapOf(Language.ENGLISH to "Supercharged"),
                )

            // when
            sut.createGadget(brawlerBrawlStarsId = 16000000L, request = gadgetRequest)
            sut.createHyperCharge(brawlerBrawlStarsId = 16000000L, request = hyperChargeRequest)

            // then
            assertThat(gadgetJpaRepository.findAllByBrawlerBrawlStarsId(16000000L)).hasSize(1)
            assertThat(hyperChargeJpaRepository.findAllByBrawlerBrawlStarsId(16000000L)).hasSize(1)
            val gadgetNames =
                messageJpaRepository
                    .findAllByTypeAndBrawlStarsIdIn(MessageType.GADGET_NAME, listOf(23000200L))
                    .associate { it.lang to it.content }
            assertThat(gadgetNames).containsExactlyInAnyOrderEntriesOf(mapOf(Language.KOREAN to "빠른 장전"))
            val hyperChargeNames =
                messageJpaRepository
                    .findAllByTypeAndBrawlStarsIdIn(MessageType.HYPERCHARGE_NAME, listOf(23000300L))
                    .associate { it.lang to it.content }
            assertThat(hyperChargeNames).containsExactlyInAnyOrderEntriesOf(mapOf(Language.ENGLISH to "Supercharged"))
        }

        @Test
        fun `이름 없이 스타파워를 등록하면 메시지가 저장되지 않는다`() {
            // given
            val request = BrawlerSubItemCreateRequest(brawlStarsId = 23000400L)

            // when
            sut.createStarPower(brawlerBrawlStarsId = 16000000L, request = request)

            // then
            assertThat(starPowerJpaRepository.findAllByBrawlerBrawlStarsId(16000000L)).hasSize(1)
            assertThat(
                messageJpaRepository.findAllByTypeAndBrawlStarsIdIn(MessageType.STARPOWER_NAME, listOf(23000400L)),
            ).isEmpty()
        }
    }
