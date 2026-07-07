package com.imstargg.core.batch.support.coreapi

enum class PlayerRenewalResult {
    /** 갱신 요청 접수됨 (2xx) */
    ACCEPTED,

    /** 이미 갱신 진행 중 — 스킵 (409) */
    ALREADY_IN_PROGRESS,

    /** 삭제된 플레이어 — 스킵 (410) */
    PLAYER_DELETED,

    /** 브롤스타즈 API 점검 중 — 남은 요청 중단 (503) */
    API_UNDER_MAINTENANCE,

    /** 그 외 오류 — 로그 후 다음 태그 진행 */
    FAILED,
}
