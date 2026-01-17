package com.imstargg.core.api.controller.v1.response

import com.fasterxml.jackson.annotation.JsonValue
import com.imstargg.core.domain.brawlstars.Messages
import com.imstargg.core.enums.Language

data class MessagesResponse(
    @get:JsonValue
    val messages: Map<Language, String>,
) {
    companion object {
        fun of(messages: Messages) =
            MessagesResponse(
                messages = messages.messages.map { (lang, msg) -> lang to msg.content }.toMap(),
            )
    }
}
