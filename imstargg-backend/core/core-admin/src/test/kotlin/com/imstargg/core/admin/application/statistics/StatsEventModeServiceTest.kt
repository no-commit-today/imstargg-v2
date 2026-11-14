package com.imstargg.core.admin.application.statistics

import com.imstargg.CoreAdminContextTest
import com.imstargg.core.enums.StatsEventModeType
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventModeEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventModeJpaRepository
import com.imstargg.infrastructure.db.core.statistics.repository.StatsEventModeJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class StatsEventModeServiceTest
    @Autowired
    constructor(
        private val sut: StatsEventModeService,
        private val battleEventModeJpaRepository: BattleEventModeJpaRepository,
        private val statsEventModeJpaRepository: StatsEventModeJpaRepository,
        private val cleanUp: CleanUp,
    ) : CoreAdminContextTest() {
        @BeforeEach
        fun setUp() {
            battleEventModeJpaRepository.save(BattleEventModeEntity(brawlStarsId = 20L, brawlStarsName = "knockout"))
        }

        @AfterEach
        fun tearDown() {
            cleanUp.all()
        }

        @Test
        fun `유형을 지정하면 행이 생긴다`() {
            // when
            sut.updateType(20L, StatsEventModeType.RESULT_3VS3)

            // then
            assertThat(sut.findAllTypes()).isEqualTo(mapOf(20L to StatsEventModeType.RESULT_3VS3))
        }

        @Test
        fun `같은 모드에 다시 지정하면 행은 하나이고 마지막 유형이다`() {
            // given
            sut.updateType(20L, StatsEventModeType.RESULT_3VS3)

            // when
            sut.updateType(20L, StatsEventModeType.SOLO_SHOWDOWN)

            // then
            assertThat(statsEventModeJpaRepository.count()).isEqualTo(1)
            assertThat(sut.findAllTypes()[20L]).isEqualTo(StatsEventModeType.SOLO_SHOWDOWN)
        }

        @Test
        fun `null 로 지정하면 행을 지운다`() {
            // given
            sut.updateType(20L, StatsEventModeType.RESULT_3VS3)

            // when
            sut.updateType(20L, null)

            // then
            assertThat(sut.findAllTypes()).isEmpty()
        }

        @Test
        fun `행이 없을 때 null 로 지정해도 오류가 없다`() {
            // when
            sut.updateType(20L, null)

            // then
            assertThat(sut.findAllTypes()).isEmpty()
        }

        @Test
        fun `등록되지 않은 모드면 404 다`() {
            // when
            val exception = assertThrows<ResponseStatusException> { sut.updateType(999L, StatsEventModeType.RESULT_3VS3) }

            // then
            assertThat(exception.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }
