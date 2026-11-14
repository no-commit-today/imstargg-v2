package com.imstargg.core.api.controller.v1

import com.imstargg.CoreApiContextTest
import com.imstargg.core.application.brawlstars.BattleEventService
import com.imstargg.core.application.statistics.StatsEventModeService
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.Messages
import com.imstargg.core.domain.brawlstars.event.BattleEvent
import com.imstargg.core.domain.brawlstars.event.BattleEventMode
import com.imstargg.core.domain.brawlstars.event.RotationBattleEvent
import com.imstargg.core.domain.statistics.StatsEventModeTypes
import com.imstargg.core.enums.StatsEventModeType.RESULT_3VS3
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.Instant

@AutoConfigureMockMvc
class BattleEventControllerTest(
    @Autowired private val mockMvc: MockMvc,
) : CoreApiContextTest() {
    @MockkBean(relaxed = true)
    private lateinit var battleEventService: BattleEventService

    @MockkBean(relaxed = true)
    private lateinit var statsEventModeService: StatsEventModeService

    @BeforeEach
    fun setUp() {
        every { statsEventModeService.getTypes() } returns
            StatsEventModeTypes(mapOf(BrawlStarsId(20L) to RESULT_3VS3))
    }

    @Test
    fun `미지원 모드 이벤트는 resultType null 을 내보내고 color 를 내보내지 않는다`() {
        // given
        every { battleEventService.get(any()) } returns
            BattleEvent(
                id = BrawlStarsId(15_000_001L),
                mode =
                    BattleEventMode(
                        id = BrawlStarsId(25L),
                        name = Messages.newDefault("Wipeout"),
                        iconImageUrl = null,
                    ),
                mapName = Messages.newDefault("Hard Rock Mine"),
            )

        // when // then
        mockMvc
            .get("/v1/events/{id}", 15_000_001L)
            .andExpect {
                status { isOk() }
                content {
                    string(containsString("\"resultType\":null"))
                    string(not(containsString("\"color\"")))
                }
            }
    }

    @Test
    fun `수집 모드 이벤트는 로테이션 응답에도 resultType 을 낸다`() {
        // given
        every { battleEventService.getCurrentRotationEvents() } returns
            listOf(
                RotationBattleEvent(
                    event =
                        BattleEvent(
                            id = BrawlStarsId(15_000_002L),
                            mode =
                                BattleEventMode(
                                    id = BrawlStarsId(20L),
                                    name = Messages.newDefault("Brawl Ball"),
                                    iconImageUrl = null,
                                ),
                            mapName = Messages.newDefault("Super Beach"),
                        ),
                    startTime = Instant.parse("2025-08-01T00:00:00Z"),
                    endTime = Instant.parse("2025-08-02T00:00:00Z"),
                ),
            )

        // when // then
        mockMvc
            .get("/v1/events/rotation")
            .andExpect {
                status { isOk() }
                content {
                    string(containsString("\"resultType\":\"RESULT\""))
                }
            }
    }
}
