package com.imstargg.infrastructure.db.core.brawlstars.battle.repository

import java.time.OffsetDateTime

interface BattleJpaRepositoryCustom {
    /**
     * 한 플레이어가 [startAt, endAt) 기간에 치른 [battleTypeCode] 배틀을, 본인이 사용한 브롤러와 결과별로 집계한다.
     *
     * 시즌 판정을 LocalDate 자정이 아닌 시각 경계로 거르는 이유: 과거 PlayerBrawlerStatsAccumulator 가
     * 배틀마다 BrawlPassSeason.of(battleAt) 로 시즌을 판정했으므로, 동일 결과를 내려면 시즌의 실제 시각
     * (OffsetDateTime startAt/endAt) 으로 필터해야 한다.
     *
     * 본인 브롤러는 battle_team_player.brawlstars_tag == playerTag 조인으로 격리하고,
     * 배틀 소유는 battle.player_id == playerId 로 한정한다.
     */
    fun aggregatePlayerBrawlerResults(
        playerId: Long,
        playerTag: String,
        battleTypeCode: String,
        startAt: OffsetDateTime,
        endAt: OffsetDateTime,
    ): List<PlayerBrawlerResultProjection>
}
