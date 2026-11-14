package com.imstargg.core.batch.domain.statistics

/**
 * 봇 전투 규칙이 맞게 돌고 있는지 지켜보는 누적기의 공통 모양. 전투를 하나씩 [record] 하고 끝에 한 번 판정한다.
 * [record] 는 예외를 던지지 않고, 스레드 안전하지 않다. 한 실행 안에서 하나씩 만들어 쓴다.
 */
interface StatsBotMonitor<TBattle> {
    fun record(battle: TBattle)

    fun metrics(): List<StatsBotMonitorMetric>

    /** 요약 로그용 한 줄. */
    fun summary(): String
}
