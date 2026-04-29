package com.imstargg.core.api.controller.v1.response

import com.fasterxml.jackson.databind.ObjectMapper
import com.imstargg.core.domain.brawlstars.Message
import com.imstargg.core.domain.brawlstars.Messages
import com.imstargg.core.enums.Language
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MessagesResponseTest {
    private val objectMapper = ObjectMapper()

    @Test
    fun `Language code 를 JSON 키로 직렬화한다`() {
        // given
        val messages =
            Messages.create(
                listOf(
                    Message(Language.KOREAN, "쉘리"),
                    Message(Language.ENGLISH, "SHELLY"),
                ),
            )

        // when
        val sut = MessagesResponse.of(messages)
        val json = objectMapper.writeValueAsString(sut)

        // then
        assertThat(json).contains("\"ko\":\"쉘리\"")
        assertThat(json).contains("\"en\":\"SHELLY\"")
        assertThat(json).doesNotContain("KOREAN")
        assertThat(json).doesNotContain("ENGLISH")
    }

    @Test
    fun `한글만 있어도 ko 키로 직렬화한다`() {
        // given
        val messages = Messages.create(listOf(Message(Language.KOREAN, "쉘리")))

        // when
        val json = objectMapper.writeValueAsString(MessagesResponse.of(messages))

        // then
        assertThat(json).isEqualTo("{\"ko\":\"쉘리\"}")
    }
}
