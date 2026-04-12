package com.imstargg.core.admin.application

import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.brawlstars.MessageEntity

fun toNameMap(entities: List<MessageEntity>?): Map<Language, String> {
    if (entities.isNullOrEmpty()) return emptyMap()
    return entities.associate { it.lang to it.content }
}
