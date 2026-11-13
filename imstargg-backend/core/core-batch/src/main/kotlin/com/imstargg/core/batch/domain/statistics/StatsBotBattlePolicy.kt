package com.imstargg.core.batch.domain.statistics

import com.imstargg.core.enums.BattleEventModeResultType
import com.imstargg.core.enums.BattleType
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.core.enums.StatsEventModeType
import com.imstargg.infrastructure.db.core.statistics.battle.BaseStatsBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity

/**
 * 통계에서 뺄 매치메이킹 봇 전투의 판정 규칙. 집계기는 [isBotBattle]·[isBotLobby] 만 부르고, 감시 누적기는 단계 함수를 함께 쓴다.
 *
 * - 3대3(결과형): 트로피전의 2×3 결과형 구성 전투에서 상대 세 명의 트로피가 모두 2000 미만이고 모두 수집자 팀 평균의 -75 이상 +25 미만이면 봇 전투다.
 * - 쇼다운(순위형): 솔로·듀오·트리오 쇼다운 유형의 팀 구성인 트로피전에서 수집자 팀을 뺀 팀 중 팀원 전원이 2000 미만인 팀에서, 폭 75 인 트로피 창 하나에 팀째로 드는 팀이
 *   솔로 5, 듀오 2, 트리오 2 이상이면 봇 로비다.
 *
 * 모두 순수 함수이고 예외를 던지지 않는다. 조건을 하나라도 만족하지 않으면 `false` 다.
 *
 * ## 근거 (2026-11 스펙 C1 §1, 부록 C.1 ~ C.3)
 * - 공식 릴리스 노트의 매치메이킹 표는 봇 매치를 브롤러 트로피 0 ~ 1999 에서 켜고 2000 이상에서 끈다. 수집 승률의 치우침도 그 구간에만 있다.
 * - 3대3: 규칙에 걸린 전투의 수집자 팀 승률은 93 ~ 95% 이고, 남은 전투는 약 52% 다(10-28 ~ 11-05, 걸린 전투 승률 92.7 ~ 95.1%,
 *   남은 전투 승률 51.8 ~ 52.5%). 확인된 실제 상대와 싸운 전투는 48 ~ 50% 다. 봇 슬롯의 99.9% 가 수집자 팀 평균 대비 -75 ~ +24 에 있고
 *   실제 플레이어는 약 9% 다.
 * - 창 [-75, +25): 변형 비교(부록 C.2)에서 [-60, +10) 과 [-50, 0) 은 남은 전투 승률이 53.0%, 55.2% 로 봇이 남고 진영 차가 3.3pp, 7.8pp 로
 *   벌어진다. [-100, +50) 은 오탐 지표가 0.29% 에서 0.63% 로 늘고, 새로 걸리는 전투는 승률 75.7% 에 약 31% 가 실제 전투다.
 *   [-75, +25) 까지는 새로 걸리는 전투의 약 95% 가 봇이다.
 * - 쇼다운 임계값(부록 C.3): 봇은 팀째로 들어와 트로피가 무리에 몰린다. 임계값은 부록 C.3 의 비교표에서 고른 값이다.
 *   솔로는 4 이면 본인 2000 이상에서 3.67% 가 걸려 5 로 하고(본인 2000 이상에서 걸리는 비중 0.55%, 남은 1위 비율 10.12%),
 *   듀오는 1 이면 30.94% 가 걸려 2 로 하며(본인 2000 이상에서 걸리는 비중 0.91%, 남은 1위 비율 20.83%), 트리오는 2 다(0.03%, 26.01%. 같은 기준). 폭 75 는 봇 무리의 최대 - 최소가 75 미만인 비율이
 *   96.6 ~ 99.4% 라서다. 슬롯 수가 아닌 팀 수로 세는 까닭은 듀오·트리오 봇이 팀째로 들어와서다(무리에 걸친 팀의 94 ~ 96% 가 전원 무리 안).
 *
 * ## 결정 이력
 * - 상대 쪽만 본다. 수집자 팀 세 명이 상대 평균의 창 안인 전투까지 빼면 0.29% 가 더 빠지지만 승률 52.1% 라 진영 차는 그대로이고
 *   약 40% 가 실제 전투다. 쇼다운에서도 수집자 팀과 수집자 본인 트로피는 조건에 넣지 않는다.
 * - 이름·태그·파워를 판정에 쓰지 않는다. 봇의 표기가 2026년에 두 번(04-22, 06-30) 바뀌었다. 이름은 감시 로그에서만 쓴다.
 * - 2000 미만 조건은 상대 세 명(쇼다운은 팀원 전원)에 건다. 10-29 이전에는 수집자 본인이 2000 이상이어도 팀 평균이 2000 아래면 봇이 붙었고, 이 방식은 그때와 지금
 *   모두 맞는다. 조건을 없애면 2000 이상 전투의 1.09%(실제 팀, 승률 58%)를 지운다.
 * - 모드 ID 가 아니라 팀 구성으로 유형을 정한다. 수집 모드 목록은 DB 에 있고 배치는 읽지 않는다(2026-11-13 통계 수집 모드 DB 관리 설계 §3.5).
 * - 원본 전투는 그대로 두고 집계 단계에서만 뺀다. 규칙이 추정이라 고친 뒤 다시 집계할 수 있어야 한다.
 * - 로비 단위로 맞추지 않는다. 같은 로비를 다른 수집자가 수집한 행은 경계에서 판정이 갈릴 수 있으나 행 기준 0.2 ~ 0.4% 다.
 *
 * 전체 설계와 측정은 `docs/superpowers/specs/2026-11-06-stats-bot-battle-exclusion-design.md` 에 있다.
 */
object StatsBotBattlePolicy {
    /** 봇 슬롯의 트로피 상한(미만). 공식 규칙의 봇 매치 구간이다. */
    const val BOT_TROPHY_LIMIT = 2000

    /** 3대3 창의 하한(포함). 수집자 팀 평균 대비다. */
    const val TEAM_AVERAGE_WINDOW_LOWER = -75

    /** 3대3 창의 상한(제외). 수집자 팀 평균 대비다. */
    const val TEAM_AVERAGE_WINDOW_UPPER = 25

    /** 쇼다운 몰림 창의 폭. */
    const val CLUSTER_WIDTH = 75

    /** 솔로 쇼다운에서 몰린 팀 수가 이 값 이상이면 봇 로비다. */
    const val SOLO_SHOWDOWN_CLUSTER_THRESHOLD = 5

    /** 듀오 쇼다운에서 몰린 팀 수가 이 값 이상이면 봇 로비다. */
    const val DUO_SHOWDOWN_CLUSTER_THRESHOLD = 2

    /** 트리오 쇼다운에서 몰린 팀 수가 이 값 이상이면 봇 로비다. */
    const val TRIO_SHOWDOWN_CLUSTER_THRESHOLD = 2

    private const val RESULT_TEAM_SIZE = 3

    /** 3대3 적용 범위: 트로피전, 2×3 결과형 팀 구성, 수집자 슬롯 정확히 하나. */
    fun appliesTo(battle: StatsResultBattleEntity): Boolean = resultTeams(battle) != null

    /** 3대3 판정의 상대 세 명 모두 2000 미만. 적용 범위 밖이면 `false` 다. */
    fun opponentsBelowBotLimit(battle: StatsResultBattleEntity): Boolean {
        val teams = resultTeams(battle) ?: return false
        return teams.opponents.all { it.brawler.trophies < BOT_TROPHY_LIMIT }
    }

    /**
     * 3대3 판정의 상대 세 명 모두 수집자 팀 평균 창 안. [opponentsBelowBotLimit] 과 무관하게 판정하고, 적용 범위 밖이면 `false` 다.
     *
     * 수집자 팀 트로피 합 S 와 상대 트로피 t 에 대해 `3·t ≥ S + 3·하한` 이고 `3·t < S + 3·상한` 이다. 평균을 정수 나눗셈으로 내면
     * 경계에서 측정과 달라지므로 정수 비교식으로 고정한다.
     */
    fun opponentsInTeamAverageWindow(battle: StatsResultBattleEntity): Boolean {
        val teams = resultTeams(battle) ?: return false
        val collectorSum = teams.collector.sumOf { it.brawler.trophies }
        return teams.opponents.all {
            val tripled = RESULT_TEAM_SIZE * it.brawler.trophies
            tripled >= collectorSum + RESULT_TEAM_SIZE * TEAM_AVERAGE_WINDOW_LOWER &&
                tripled < collectorSum + RESULT_TEAM_SIZE * TEAM_AVERAGE_WINDOW_UPPER
        }
    }

    /** 3대3 봇 전투 여부. 적용 범위와 두 판정을 모두 만족해야 한다. */
    fun isBotBattle(battle: StatsResultBattleEntity): Boolean =
        appliesTo(battle) && opponentsBelowBotLimit(battle) && opponentsInTeamAverageWindow(battle)

    /** 쇼다운 적용 범위: 트로피전, 솔로·듀오·트리오 쇼다운의 정해진 팀 구성, 수집자 슬롯 정확히 하나. */
    fun appliesTo(battle: StatsRankBattleEntity): Boolean = rankLobby(battle) != null

    /** 쇼다운 봇 로비 여부. 적용 범위를 만족하고 몰린 팀 수가 그 모드의 임계값 이상이어야 한다. */
    fun isBotLobby(battle: StatsRankBattleEntity): Boolean {
        val lobby = rankLobby(battle) ?: return false
        val qualified = lobby.opponentTeams.filter { team -> team.all { it.brawler.trophies < BOT_TROPHY_LIMIT } }
        val clusteredTeams =
            qualified.maxOfOrNull { anchor ->
                val windowStart = anchor.minOf { it.brawler.trophies }
                qualified.count { team ->
                    team.minOf { it.brawler.trophies } >= windowStart &&
                        team.maxOf { it.brawler.trophies } < windowStart + CLUSTER_WIDTH
                }
            } ?: 0
        return clusteredTeams >= lobby.threshold
    }

    private class ResultTeams(
        val collector: List<StatsBattleEntityTeamPlayer>,
        val opponents: List<StatsBattleEntityTeamPlayer>,
    )

    private class RankLobby(
        val opponentTeams: List<List<StatsBattleEntityTeamPlayer>>,
        val threshold: Int,
    )

    private fun resultTeams(battle: StatsResultBattleEntity): ResultTeams? {
        if (!isTrophyBattle(battle)) return null
        StatsEventModeType.find(BattleEventModeResultType.RESULT, battle.teams.map { it.size }) ?: return null
        if (!hasSingleCollectorSlot(battle)) return null
        return ResultTeams(
            collector = battle.playerTeams().single(),
            opponents = battle.enemyTeams().single(),
        )
    }

    private fun rankLobby(battle: StatsRankBattleEntity): RankLobby? {
        if (!isTrophyBattle(battle)) return null
        val type = StatsEventModeType.find(BattleEventModeResultType.RANK, battle.teams.map { it.size }) ?: return null
        val threshold =
            when (type) {
                StatsEventModeType.SOLO_SHOWDOWN -> SOLO_SHOWDOWN_CLUSTER_THRESHOLD
                StatsEventModeType.DUO_SHOWDOWN -> DUO_SHOWDOWN_CLUSTER_THRESHOLD
                StatsEventModeType.TRIO_SHOWDOWN -> TRIO_SHOWDOWN_CLUSTER_THRESHOLD
                StatsEventModeType.RESULT_3VS3 -> return null
            }
        if (!hasSingleCollectorSlot(battle)) return null
        return RankLobby(opponentTeams = battle.enemyTeams(), threshold = threshold)
    }

    private fun isTrophyBattle(battle: BaseStatsBattleEntity): Boolean =
        EnumCodeType.findByCode<BattleType>(battle.type) == BattleType.RANKED

    /** 수집자 팀이 정확히 한 팀이고, 전 슬롯 중 수집자 태그가 정확히 하나다. */
    private fun hasSingleCollectorSlot(battle: BaseStatsBattleEntity): Boolean =
        battle.playerTeams().size == 1 &&
            battle.teams.flatten().count { it.brawlStarsTag == battle.playerBrawlStarsTag } == 1
}
