package com.imstargg.core.admin.application

import com.imstargg.CoreAdminContextTest
import com.imstargg.core.admin.request.GearCreateRequest
import com.imstargg.core.enums.GearRarity
import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.brawlstars.MessageType
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.GearJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.repository.MessageJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class GearServiceTest
    @Autowired
    constructor(
        private val sut: GearService,
        private val gearJpaRepository: GearJpaRepository,
        private val messageJpaRepository: MessageJpaRepository,
        private val cleanUp: CleanUp,
    ) : CoreAdminContextTest() {
        @AfterEach
        fun tearDown() {
            cleanUp.all()
        }

        @Test
        fun `기어 등록 시 이름이 함께 저장된다`() {
            // given
            val request =
                GearCreateRequest(
                    brawlStarsId = 62000000L,
                    rarity = GearRarity.SUPER_RARE,
                    names = mapOf(Language.KOREAN to "속도", Language.ENGLISH to "Speed"),
                )

            // when
            sut.create(request)

            // then
            assertThat(gearJpaRepository.findAll().map { it.brawlStarsId }).contains(62000000L)
            val names =
                messageJpaRepository
                    .findAllByTypeAndBrawlStarsIdIn(MessageType.GEAR_NAME, listOf(62000000L))
                    .associate { it.lang to it.content }
            assertThat(names)
                .containsExactlyInAnyOrderEntriesOf(
                    mapOf(Language.KOREAN to "속도", Language.ENGLISH to "Speed"),
                )
        }
    }
