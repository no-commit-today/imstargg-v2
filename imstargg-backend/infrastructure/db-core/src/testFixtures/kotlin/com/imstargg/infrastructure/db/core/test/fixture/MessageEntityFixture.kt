package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.brawlstars.MessageEntity
import com.imstargg.infrastructure.db.core.brawlstars.MessageType

class MessageEntityFixture {
    private var type: MessageType = MessageType.BRAWLER_NAME
    private var brawlStarsId: Long = 16000000L
    private var lang: Language = Language.KOREAN
    private var content: String = "쉘리"

    companion object {
        fun aMessageEntity() = MessageEntityFixture()
    }

    fun withType(value: MessageType) = apply { type = value }

    fun withBrawlStarsId(value: Long) = apply { brawlStarsId = value }

    fun withLang(value: Language) = apply { lang = value }

    fun withContent(value: String) = apply { content = value }

    fun build() =
        MessageEntity(
            type = type,
            brawlStarsId = brawlStarsId,
            lang = lang,
            content = content,
        )
}
