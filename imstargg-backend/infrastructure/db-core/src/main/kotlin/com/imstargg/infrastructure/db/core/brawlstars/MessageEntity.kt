package com.imstargg.infrastructure.db.core.brawlstars

import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.BaseTimeEntity
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
        Index(name = "uk_message__type_brawlstarsid_lang", columnList = "type, brawlstars_id, lang", unique = true),
    ],
)
class MessageEntity(
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 45, updatable = false, nullable = false)
    val type: MessageType,
    @Column(name = "brawlstars_id", updatable = false, nullable = false)
    val brawlStarsId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "lang", length = 25, updatable = false, nullable = false)
    val lang: Language,
    @Column(name = "content", updatable = false, nullable = false)
    val content: String,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    val id: Long? = null
}
