package com.imstargg.core.admin.controller.request

import com.imstargg.core.enums.Language

data class MessagesUpdateRequest(
    val messages: Map<Language, String>,
)
