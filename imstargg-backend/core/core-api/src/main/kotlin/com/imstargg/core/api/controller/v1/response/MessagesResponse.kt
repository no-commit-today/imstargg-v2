package com.imstargg.core.api.controller.v1.response

import com.fasterxml.jackson.annotation.JsonValue
import com.imstargg.core.domain.brawlstars.Messages

/**
 * 다국어 메시지 응답.
 *
 * JSON 키는 `Language.code`(ISO 639-1 소문자 — `ko`, `en`) 를 사용한다.
 * `Language` enum 을 Map 키로 두면 Jackson 기본값이 enum `name`(`KOREAN`/`ENGLISH`)
 * 으로 직렬화하여 프론트엔드 `MessagesResponse = Record<"ko"|"en", string>` 계약과
 * 어긋나므로 `code` 문자열을 직접 키로 사용한다.
 */
data class MessagesResponse(
    @get:JsonValue
    val messages: Map<String, String>,
) {
    companion object {
        fun of(messages: Messages) =
            MessagesResponse(
                messages = messages.messages.map { (lang, msg) -> lang.code to msg.content }.toMap(),
            )
    }
}
