package com.imstargg.infrastructure.db.core.brawlstars.battle.repository

import java.time.Instant

interface BattleJpaRepositoryCustom {
    /**
     * 한 플레이어가 [startAt, endAt) 기간에 치른 [battleTypeCode] 배틀을, 본인이 사용한 브롤러와 결과별로 집계한다.
     *
     * 시즌 판정을 LocalDate 자정이 아닌 시각 경계로 거르는 이유: 과거 PlayerBrawlerStatsAccumulator 가
     * 배틀마다 BrawlPassSeason.of(battleAt) 로 시즌을 판정했으므로, 동일 결과를 내려면 시즌의 실제 시각
     * (Instant startAt/endAt) 으로 필터해야 한다.
     *
     * 본인 브롤러는 battle_team_player.brawlstars_tag == playerTag 조인으로 격리하고,
     * 배틀 소유는 battle.player_id == playerId 로 한정한다.
     *
     * 경계는 반열림 [startAt, endAt). RANKED 는 구 accumulator 와 완전 일치하나, SOLO_RANKED 는
     * endAt 포함 + 시즌 간 ~24h 공백이라, 구 SoloRankSeason.of() 가 그 경계 배틀(공백 구간·정확히
     * endAt 순간)까지 귀속시켰던 것과 달리 경계 배틀을 제외한다 — 의도된 결정(#38, 솔로랭크는 공백
     * 구간 플레이가 없어 실질 차이 없음).
     */
    fun aggregatePlayerBrawlerResults(
        playerId: Long,
        playerTag: String,
        battleTypeCode: String,
        startAt: Instant,
        endAt: Instant,
    ): List<PlayerBrawlerResultProjection>
}
