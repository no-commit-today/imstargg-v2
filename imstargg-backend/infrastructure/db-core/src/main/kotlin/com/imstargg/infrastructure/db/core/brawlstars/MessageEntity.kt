package com.imstargg.infrastructure.db.core.brawlstars

import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "message",
    indexes = [
        Index(name = "uk_message__code_lang", columnList = "code, lang", unique = true),
    ],
)
class MessageEntity(
    @Column(name = "code", length = 65, updatable = false, nullable = false)
    val code: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "lang", length = 25, updatable = false, nullable = false)
    val lang: Language,
    @Column(name = "content", updatable = false, nullable = false)
    val content: String,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    val id: Long? = null
}
