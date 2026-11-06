package com.imstargg.core.batch.domain.statistics

import com.imstargg.core.enums.StatsEventMode
import com.imstargg.infrastructure.db.core.statistics.battle.StatsRankBattleEntity

/**
 * 쇼다운 봇 로비 규칙이 맞게 돌고 있는지 지켜보는 누적기. 모드(솔로·듀오·트리오)별로 센다.
 * 대상은 [StatsBotBattlePolicy.appliesTo] 를 만족하는 전투뿐이고, 적용 범위 밖 전투는 어느 지표에도 세지 않는다.
 *
 * 지표의 뜻(스펙 §3.3):
 * - 걸린 전투의 1위 비율이 문턱 미만이면 규칙이 실제 로비를 지우고 있다.
 * - 남은 전투의 1위 비율이 문턱 초과면 규칙이 봇을 놓치고 있다.
 * - 걸리는 비중은 로그만 남긴다. 날짜에 따라 솔로 18%p, 듀오 21%p, 트리오 22%p 까지 움직인다.
 *
 * 최소 표본에 못 미친 지표는 경고를 판정하지 않고 요약에 "표본 부족" 으로 남는다.
 * 트리오의 남은 전투는 전투가 적은 날(기준선 아홉 날짜 중 10-28, 10-29) 최소 표본에 못 미쳐 판정을 건너뛴다.
 *
 * 정책의 판정 함수만 부른다. [record] 는 예외를 던지지 않는다. 스레드 안전하지 않다.
 */
class StatsBotLobbyMonitor {
    private val tallies = MODES.associateWith { Tally() }

    private class Tally {
        var flagged = 0L
        var flaggedFirst = 0L
        var remaining = 0L
        var remainingFirst = 0L
    }

    private class ModeSpec(
        val mode: StatsEventMode,
        val label: String,
        val flaggedWarnBelow: Double,
        val flaggedMinSample: Long,
        val remainingWarnAbove: Double,
        val remainingMinSample: Long,
    )

    fun record(battle: StatsRankBattleEntity) {
        if (!StatsBotBattlePolicy.appliesTo(battle)) return
        val mode = StatsEventMode.find(battle.eventModeBrawlStarsId) ?: return
        val tally = tallies[mode] ?: return
        val first = battle.battleRank == 1
        if (StatsBotBattlePolicy.isBotLobby(battle)) {
            tally.flagged++
            if (first) tally.flaggedFirst++
        } else {
            tally.remaining++
            if (first) tally.remainingFirst++
        }
    }

    /** 솔로·듀오·트리오 순으로 모드마다 걸린 전투 1위 비율, 남은 전투 1위 비율, 걸리는 비중 순이다. */
    fun metrics(): List<StatsBotMonitorMetric> =
        SPECS.flatMap { spec ->
            val tally = tallies.getValue(spec.mode)
            listOf(
                StatsBotMonitorMetric(
                    name = "${spec.label} 걸린 전투 1위 비율",
                    count = tally.flaggedFirst,
                    total = tally.flagged,
                    warnBelow = spec.flaggedWarnBelow,
                    minSample = spec.flaggedMinSample,
                ),
                StatsBotMonitorMetric(
                    name = "${spec.label} 남은 전투 1위 비율",
                    count = tally.remainingFirst,
                    total = tally.remaining,
                    warnAbove = spec.remainingWarnAbove,
                    minSample = spec.remainingMinSample,
                ),
                StatsBotMonitorMetric(
                    name = "${spec.label} 걸리는 비중",
                    count = tally.flagged,
                    total = tally.flagged + tally.remaining,
                ),
            )
        }

    /** 요약 로그용 한 줄. 모드별 대상 수는 걸리는 비중의 표본, 걸린 수는 걸린 전투 1위 비율의 표본으로 찍힌다. */
    fun summary(): String = metrics().joinToString(", ") { it.describe() }

    companion object {
        private val SPECS =
            listOf(
                // 걸린 전투 기준선 56.5 ~ 61.8%, 남은 전투 기준선 9.4 ~ 10.8%
                ModeSpec(
                    mode = StatsEventMode.SOLO_SHOWDOWN,
                    label = "솔로",
                    flaggedWarnBelow = 0.50,
                    flaggedMinSample = 1000,
                    remainingWarnAbove = 0.125,
                    remainingMinSample = 3000,
                ),
                // 걸린 전투 기준선 76.3 ~ 81.2%, 남은 전투 기준선 18.7 ~ 21.6%
                ModeSpec(
                    mode = StatsEventMode.DUO_SHOWDOWN,
                    label = "듀오",
                    flaggedWarnBelow = 0.70,
                    flaggedMinSample = 500,
                    remainingWarnAbove = 0.24,
                    remainingMinSample = 1000,
                ),
                // 걸린 전투 기준선 76.0 ~ 85.8%, 남은 전투 기준선 23.5 ~ 28.0%
                ModeSpec(
                    mode = StatsEventMode.TRIO_SHOWDOWN,
                    label = "트리오",
                    flaggedWarnBelow = 0.70,
                    flaggedMinSample = 300,
                    remainingWarnAbove = 0.31,
                    remainingMinSample = 1000,
                ),
            )

        private val MODES = SPECS.map { it.mode }
    }
}
