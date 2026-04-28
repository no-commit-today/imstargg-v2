package com.imstargg.core.config

import com.imstargg.CoreApiContextTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
@TestPropertySource(
    properties = [
        "app.cors.allowed-origins=https://imstargg.com,http://localhost:3000",
    ],
)
class WebMvcConfigTest(
    @Autowired private val mockMvc: MockMvc,
) : CoreApiContextTest() {
    @Test
    fun `허용된 origin 의 OPTIONS 요청에 CORS 헤더가 응답된다`() {
        mockMvc
            .perform(
                options("/v1/players/%2388J8Q9RPV")
                    .header(HttpHeaders.ORIGIN, "https://imstargg.com")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"),
            ).andExpect(status().isOk)
            .andExpect(header().string("Access-Control-Allow-Origin", "https://imstargg.com"))
    }
}
