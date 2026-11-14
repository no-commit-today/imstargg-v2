package com.imstargg.core.batch.domain.statistics

import com.imstargg.core.enums.BattleResult
import com.imstargg.core.enums.EnumCodeType
import com.imstargg.core.enums.StatsTierRange
import com.imstargg.infrastructure.db.core.statistics.battle.StatsResultBattleEntity

/**
 * 3대3 봇 전투 규칙이 맞게 돌고 있는지 지켜보는 누적기. 집계하면서 같은 전투로 지표를 센다.
 * 대상은 [StatsBotBattlePolicy.appliesTo] 를 만족하는 전투뿐이고, 적용 범위 밖 전투는 어느 지표에도 세지 않는다.
 *
 * 지표의 뜻(스펙 §3.3):
 * - 걸린 전투 승률이 문턱 미만이면 규칙이 실제 전투를 지우고 있다.
 * - 남은 전투 승률이 문턱 초과면 규칙이 봇을 놓치고 있다.
 * - 2000 이상 구간 승률이 문턱 초과면 2000 위에 봇이 생겼다.
 * - 걸리는 비중과 기본 이름 비율은 로그만 남긴다. 비중은 수집 시점에 따라 크게 움직이고, 기본 이름 비율이 떨어지면 봇 표기가 바뀐 것이다.
 *
 * 승률은 수집자 기준 `승 / (승 + 패)` 이고 무승부와 알 수 없는 결과는 뺀다.
 * 정책의 판정·단계 함수만 부르고 범위·창 산식을 다시 쓰지 않는다. [record] 는 예외를 던지지 않는다.
 * 스레드 안전하지 않다. 한 Task 실행 안에서 하나씩 만들어 쓴다.
 */
class StatsBotBattleMonitor : StatsBotMonitor<StatsResultBattleEntity> {
    /** 3대3 적용 범위 안의 전투 수. */
    var targetCount: Long = 0
        private set

    /** [targetCount] 중 [StatsBotBattlePolicy.isBotBattle] 인 전투 수. */
    var flaggedCount: Long = 0
        private set

    private var flaggedWins = 0L
    private var flaggedDecided = 0L
    private var remainingWins = 0L
    private var remainingDecided = 0L
    private var aboveLimitWins = 0L
    private var aboveLimitDecided = 0L
    private var ownRangeCount = 0L
    private var ownRangeFlagged = 0L
    private var defaultNameSlots = 0L
    private var flaggedOpponentSlots = 0L

    override fun record(battle: StatsResultBattleEntity) {
        if (!StatsBotBattlePolicy.appliesTo(battle)) return
        targetCount++

        val result = EnumCodeType.findByCode<BattleResult>(battle.result)
        val decided = result == BattleResult.VICTORY || result == BattleResult.DEFEAT
        val win = result == BattleResult.VICTORY
        val flagged = StatsBotBattlePolicy.isBotBattle(battle)
        val ownTrophies =
            battle
                .playerTeams()
                .flatten()
                .firstOrNull { it.brawlStarsTag == battle.playerBrawlStarsTag }
                ?.brawler
                ?.trophies
        val ownInRange =
            ownTrophies != null && ownTrophies >= StatsTierRange.MIN_TROPHY && ownTrophies < StatsBotBattlePolicy.BOT_TROPHY_LIMIT

        if (flagged) {
            flaggedCount++
            if (decided) {
                flaggedDecided++
                if (win) flaggedWins++
            }
            val opponents = battle.enemyTeams().flatten()
            flaggedOpponentSlots += opponents.size
            defaultNameSlots += opponents.count { it.name == DEFAULT_PLAYER_NAME }
        }
        if (ownInRange) {
            ownRangeCount++
            if (flagged) {
                ownRangeFlagged++
            } else if (decided) {
                remainingDecided++
                if (win) remainingWins++
            }
        }
        if (decided &&
            StatsBotBattlePolicy.opponentsInTeamAverageWindow(battle) &&
            !StatsBotBattlePolicy.opponentsBelowBotLimit(battle)
        ) {
            aboveLimitDecided++
            if (win) aboveLimitWins++
        }
    }

    /** 걸린 전투 승률, 남은 전투 승률, 2000 이상 구간 승률, 걸리는 비중, 기본 이름 비율 순이다. */
    override fun metrics(): List<StatsBotMonitorMetric> =
        listOf(
            StatsBotMonitorMetric(
                name = FLAGGED_WIN_RATE,
                count = flaggedWins,
                total = flaggedDecided,
                warnBelow = FLAGGED_WIN_RATE_WARN_BELOW,
                minSample = FLAGGED_WIN_RATE_MIN_SAMPLE,
            ),
            StatsBotMonitorMetric(
                name = REMAINING_WIN_RATE,
                count = remainingWins,
                total = remainingDecided,
                warnAbove = REMAINING_WIN_RATE_WARN_ABOVE,
                minSample = REMAINING_WIN_RATE_MIN_SAMPLE,
            ),
            StatsBotMonitorMetric(
                name = ABOVE_LIMIT_WIN_RATE,
                count = aboveLimitWins,
                total = aboveLimitDecided,
                warnAbove = ABOVE_LIMIT_WIN_RATE_WARN_ABOVE,
                minSample = ABOVE_LIMIT_WIN_RATE_MIN_SAMPLE,
            ),
            StatsBotMonitorMetric(name = FLAGGED_SHARE, count = ownRangeFlagged, total = ownRangeCount),
            StatsBotMonitorMetric(name = DEFAULT_NAME_RATE, count = defaultNameSlots, total = flaggedOpponentSlots),
        )

    /** 요약 로그용 한 줄. 본인 1000~1999 전투 수와 그중 걸린 수는 걸리는 비중 지표의 표본과 분자다. */
    override fun summary(): String =
        "대상 ${targetCount}건, 걸린 전투 ${flaggedCount}건, " +
            "본인 1000~1999 ${ownRangeCount}건 중 걸린 ${ownRangeFlagged}건, " +
            metrics().joinToString(", ") { it.describe() }

    companion object {
        private const val FLAGGED_WIN_RATE = "걸린 전투 승률"
        private const val REMAINING_WIN_RATE = "남은 전투 승률"
        private const val ABOVE_LIMIT_WIN_RATE = "2000 이상 구간 승률"
        private const val FLAGGED_SHARE = "걸리는 비중"
        private const val DEFAULT_NAME_RATE = "기본 이름 비율"

        /** 봇의 기본 표기. 브롤러 이름이 아니라 플레이어 이름(`name`)이다. 2026년에 표기가 두 번 바뀌었으므로 판정에 쓰지 않고 감시에만 쓴다. */
        private const val DEFAULT_PLAYER_NAME = "Brawler"

        /** 걸린 전투 승률 경고 문턱(미만). 기준선은 스펙 §3.3 의 92.7 ~ 95.0% 다. 이보다 낮으면 규칙이 실제 전투를 지우고 있다. */
        private const val FLAGGED_WIN_RATE_WARN_BELOW = 0.90

        /** 걸린 전투 승률을 판정하는 최소 결정전 수. */
        private const val FLAGGED_WIN_RATE_MIN_SAMPLE = 1000L

        /** 남은 전투 승률 경고 문턱(초과). 기준선은 51.8 ~ 52.5% 이고 새벽 정기 실행 시점에는 52.2 ~ 53.3% 다. 이보다 높으면 규칙이 봇을 놓치고 있다. */
        private const val REMAINING_WIN_RATE_WARN_ABOVE = 0.55

        /** 남은 전투 승률을 판정하는 최소 결정전 수. */
        private const val REMAINING_WIN_RATE_MIN_SAMPLE = 5000L

        /** 2000 이상 구간 승률 경고 문턱(초과). 기준선은 56 ~ 61% 다. 이보다 높으면 2000 위에 봇이 생겼다. */
        private const val ABOVE_LIMIT_WIN_RATE_WARN_ABOVE = 0.75

        /** 2000 이상 구간 승률을 판정하는 최소 결정전 수. */
        private const val ABOVE_LIMIT_WIN_RATE_MIN_SAMPLE = 300L
    }
}
