package com.imstargg.core.batch.domain.brawler

/**
 * 브롤러 동기화에서 감지된 변경.
 *
 * DB 는 API 원본(이름 등)을 보존하지 않아 ID 존재 기반의 생성/삭제만 감지한다 —
 * 수정(이름 변경 등) 감지는 비교 기준이 없어 다루지 않는다.
 * sealed 하위 타입들은 변경 모델 응집을 위해 한 파일에 둔다 (one-class-per-file 의 승인된 예외).
 */
sealed interface BrawlerChange {
    data class BrawlerAdded(
        val brawlerId: Long,
        val brawlerName: String,
    ) : BrawlerChange

    /** DB 는 브롤러 이름을 보존하지 않으므로 ID 만 갖는다. */
    data class BrawlerRemoved(
        val brawlerId: Long,
    ) : BrawlerChange

    data class ItemAdded(
        val brawlerId: Long,
        val brawlerName: String,
        val itemType: BrawlerItemType,
        val itemId: Long,
        val itemName: String,
    ) : BrawlerChange

    /** 제거된 항목은 API 에 더 이상 없으므로 항목 이름을 알 수 없다. */
    data class ItemRemoved(
        val brawlerId: Long,
        val brawlerName: String,
        val itemType: BrawlerItemType,
        val itemId: Long,
    ) : BrawlerChange
}
