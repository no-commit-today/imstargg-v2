package com.imstargg.core.batch.job.statistics.patch

/**
 * 배치 갱신이 돌려준 줄별 결과.
 *
 * 키로 한 줄을 고치는 update 묶음의 결과만 해석한다. 드라이버가 줄 수를 알려 주지 않는 값
 * ([java.sql.Statement.SUCCESS_NO_INFO])이나 실패 값([java.sql.Statement.EXECUTE_FAILED])이 오면
 * 어느 키가 갱신됐는지 알 수 없으므로 실패시킨다. insert 묶음의 결과에는 쓰지 않는다
 * (운영의 `reWriteBatchedInserts=true` 에서는 SUCCESS_NO_INFO 가 정상이다).
 */
class BatchUpdateCounts(
    private val counts: IntArray,
) {
    /** 갱신된 줄이 없는 자리. 0 이나 1 이 아닌 값이 있으면 [IllegalStateException]. */
    fun zeroIndexes(): List<Int> {
        counts.forEachIndexed { index, count ->
            check(count == 0 || count == 1) { "줄별 결과가 0 이나 1 이 아니다. index=$index, count=$count" }
        }
        return counts.indices.filter { counts[it] == 0 }
    }

    /** 1 이 아닌 값이 있으면 [IllegalStateException]. */
    fun requireAllOne() {
        counts.forEachIndexed { index, count ->
            check(count == 1) { "줄별 결과가 1 이 아니다. index=$index, count=$count" }
        }
    }
}
