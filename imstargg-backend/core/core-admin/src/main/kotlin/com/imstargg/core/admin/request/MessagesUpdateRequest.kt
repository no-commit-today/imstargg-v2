package com.imstargg.core.admin.request

import com.imstargg.core.enums.Language

data class MessagesUpdateRequest(
    val messages: Map<Language, String>,
)
