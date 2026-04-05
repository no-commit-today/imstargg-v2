package com.imstargg.core.domain.brawlstars

import com.imstargg.core.enums.Language

data class Messages(
    val messages: Map<Language, Message>,
) {
    fun find(language: Language): Message? = messages[language] ?: messages[Language.DEFAULT]

    fun containsIgnoreCase(query: String): Boolean =
        messages.values.any { it.content.contains(query, ignoreCase = true) }

    companion object {
        fun create(messages: List<Message>): Messages = Messages(messages.associateBy { it.language })

        fun newDefault(content: String): Messages = create(listOf(Message(Language.DEFAULT, content)))
    }
}
