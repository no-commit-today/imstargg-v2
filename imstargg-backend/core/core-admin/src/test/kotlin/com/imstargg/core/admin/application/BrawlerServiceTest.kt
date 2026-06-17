package com.imstargg.core.admin.application

import com.imstargg.CoreAdminContextTest
import com.imstargg.core.admin.request.BrawlerCreateRequest
import com.imstargg.core.enums.BrawlerRarity
import com.imstargg.core.enums.BrawlerRole
import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.brawlstars.MessageType
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.BrawlerGearJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.BrawlerJpaRepository
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
    }
