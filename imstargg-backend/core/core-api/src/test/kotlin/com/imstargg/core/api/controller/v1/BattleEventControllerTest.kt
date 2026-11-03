package com.imstargg.core.api.controller.v1

import com.imstargg.CoreApiContextTest
import com.imstargg.core.application.brawlstars.BattleEventService
import com.imstargg.core.domain.brawlstars.BrawlStarsId
import com.imstargg.core.domain.brawlstars.Messages
import com.imstargg.core.domain.brawlstars.event.BattleEvent
import com.imstargg.core.domain.brawlstars.event.BattleEventMode
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@AutoConfigureMockMvc
class BattleEventControllerTest(
    @Autowired private val mockMvc: MockMvc,
) : CoreApiContextTest() {
    @MockkBean(relaxed = true)
    private lateinit var battleEventService: BattleEventService

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
                        resultType = null,
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
}
