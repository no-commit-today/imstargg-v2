package com.imstargg.storage.db.core.brawlstars

import com.imstargg.core.enums.Language
import com.imstargg.storage.db.core.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "message",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_message__code_lang", columnNames = ["code", "lang"]),
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
