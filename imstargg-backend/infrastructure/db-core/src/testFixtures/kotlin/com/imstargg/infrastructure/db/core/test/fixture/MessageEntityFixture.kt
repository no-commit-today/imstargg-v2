package com.imstargg.infrastructure.db.core.test.fixture

import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.brawlstars.MessageCode
import com.imstargg.infrastructure.db.core.brawlstars.MessageEntity

class MessageEntityFixture {
    private var code: MessageCode = MessageCode("brawler:shelly:name")
    private var lang: Language = Language.KOREAN
    private var content: String = "쉘리"

    companion object {
        fun aMessageEntity() = MessageEntityFixture()
    }

    fun withCode(value: String) = apply { code = MessageCode(value) }

    fun withCode(value: MessageCode) = apply { code = value }

    fun withLang(value: Language) = apply { lang = value }

    fun withContent(value: String) = apply { content = value }

    fun build() =
        MessageEntity(
            code = code,
            lang = lang,
            content = content,
        )
}
