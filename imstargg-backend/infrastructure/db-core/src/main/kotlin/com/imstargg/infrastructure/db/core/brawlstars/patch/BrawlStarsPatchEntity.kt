package com.imstargg.infrastructure.db.core.brawlstars.patch

import com.imstargg.infrastructure.db.core.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDate

/** 게임 패치일. 같은 날짜는 하나만 둔다. */
@Entity
@Table(
    name = "brawlstars_patch",
    indexes = [
        Index(name = "uk_brawlstars_patch__patch_date", columnList = "patch_date", unique = true),
    ],
)
class BrawlStarsPatchEntity(
    patchDate: LocalDate,
    memo: String?,
) : BaseTimeEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brawlstars_patch_id")
    val id: Long? = null

    @Column(name = "patch_date", nullable = false)
    var patchDate: LocalDate = patchDate
        protected set

    @Column(name = "memo", length = 255)
    var memo: String? = memo
        protected set

    fun update(
        patchDate: LocalDate,
        memo: String?,
    ) {
        this.patchDate = patchDate
        this.memo = memo
    }
}
