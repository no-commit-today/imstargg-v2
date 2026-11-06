package com.imstargg.core.batch.domain.statistics

import java.util.Locale

/**
 * 감시 지표 하나. 값은 [count] / [total] 이고 [total] 이 표본 수다.
 * [warnBelow]·[warnAbove] 가 모두 null 이면 로그만 남기는 지표다.
 *
 * @property warnBelow 값이 이 값 미만이면 경고
 * @property warnAbove 값이 이 값 초과면 경고
 * @property minSample [total] 이 이 값 미만이면 경고를 판정하지 않는다
 */
data class StatsBotMonitorMetric(
    val name: String,
    val count: Long,
    val total: Long,
    val warnBelow: Double? = null,
    val warnAbove: Double? = null,
    val minSample: Long = 0,
) {
    /** [total] 이 0 이면 null 이다. */
    val rate: Double?
        get() = if (total == 0L) null else count.toDouble() / total

    /** 문턱이 있고 [total] 이 [minSample] 미만이다. 문턱이 없는 지표는 표본이 부족해도 `false` 다. */
    val isSampleInsufficient: Boolean
        get() = hasThreshold && total < minSample

    /** 문턱이 있고 표본이 충분하고 값이 문턱을 넘었다. 값이 없으면(표본 0) `false` 다. */
    val isWarning: Boolean
        get() {
            if (!hasThreshold || isSampleInsufficient) return false
            val rate = rate ?: return false
            return (warnBelow != null && rate < warnBelow) || (warnAbove != null && rate > warnAbove)
        }

    private val hasThreshold: Boolean
        get() = warnBelow != null || warnAbove != null

    /** 예: `걸린 전투 승률 88.2% (표본 1234건, 문턱 90.0% 미만)`. 표본이 부족하면 끝에 `, 표본 부족` 이 붙는다. */
    fun describe(): String {
        val details =
            buildList {
                add("표본 ${total}건")
                warnBelow?.let { add("문턱 ${percent(it)} 미만") }
                warnAbove?.let { add("문턱 ${percent(it)} 초과") }
                if (isSampleInsufficient) add("표본 부족")
            }
        return "$name ${rate?.let { percent(it) } ?: "-"} (${details.joinToString(", ")})"
    }

    private fun percent(value: Double): String = String.format(Locale.ROOT, "%.1f%%", value * 100)
}
