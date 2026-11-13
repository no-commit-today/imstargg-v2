package com.imstargg.core.admin.controller

import com.imstargg.CoreAdminContextTest
import com.imstargg.core.enums.StatsEventModeType
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventModeEntity
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventModeJpaRepository
import com.imstargg.infrastructure.db.core.statistics.StatsEventModeEntity
import com.imstargg.infrastructure.db.core.statistics.repository.StatsEventModeJpaRepository
import com.imstargg.infrastructure.db.core.test.CleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put

@AutoConfigureMockMvc
class BattleEventModeControllerTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val battleEventModeJpaRepository: BattleEventModeJpaRepository,
        private val statsEventModeJpaRepository: StatsEventModeJpaRepository,
        private val cleanUp: CleanUp,
    ) : CoreAdminContextTest() {
        @AfterEach
        fun tearDown() {
            cleanUp.all()
        }

        @Test
        fun `모드 목록에 통계 수집 유형을 붙인다`() {
            // given
            battleEventModeJpaRepository.save(BattleEventModeEntity(brawlStarsId = 0L, brawlStarsName = "gemGrab"))
            battleEventModeJpaRepository.save(BattleEventModeEntity(brawlStarsId = 6L, brawlStarsName = "soloShowdown"))
            battleEventModeJpaRepository.save(BattleEventModeEntity(brawlStarsId = 24L, brawlStarsName = "duels"))
            statsEventModeJpaRepository.save(StatsEventModeEntity(0L, StatsEventModeType.RESULT_3VS3))
            statsEventModeJpaRepository.save(StatsEventModeEntity(6L, StatsEventModeType.SOLO_SHOWDOWN))

            // when & then
            mockMvc
                .get("/event-modes")
                .andExpect {
                    status { isOk() }
                    jsonPath("$[?(@.brawlStarsId==0)].statsType") { value("RESULT_3VS3") }
                    jsonPath("$[?(@.brawlStarsId==6)].statsType") { value("SOLO_SHOWDOWN") }
                    jsonPath("$[?(@.brawlStarsId==24)].statsType") { value(null as String?) }
                }
        }

        @Test
        fun `유형 지정 API 는 본문의 유형으로 행을 바꾼다`() {
            // given
            battleEventModeJpaRepository.save(BattleEventModeEntity(brawlStarsId = 20L, brawlStarsName = "knockout"))

            // when & then
            mockMvc
                .put("/event-modes/20/stats-type") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"type":"RESULT_3VS3"}"""
                }.andExpect { status { isOk() } }
            assertThat(statsEventModeJpaRepository.findByEventModeBrawlStarsId(20L)?.type)
                .isEqualTo(StatsEventModeType.RESULT_3VS3)

            mockMvc
                .put("/event-modes/20/stats-type") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"type":null}"""
                }.andExpect { status { isOk() } }
            assertThat(statsEventModeJpaRepository.findByEventModeBrawlStarsId(20L)).isNull()
        }
    }
