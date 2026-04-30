package com.imstargg.core.batch.domain

import com.imstargg.core.enums.BattleResult
import com.imstargg.infrastructure.db.core.statistics.ResultCountEmbedded

class ResultCountAccumulator<K> {
    private val counts = mutableMapOf<K, ResultCount>()

    fun add(
        key: K,
        result: BattleResult,
    ) {
        counts.getOrPut(key) { ResultCount() }.add(result)
    }

    fun <R> map(transform: (K, ResultCountEmbedded) -> R): List<R> = counts.map { (key, count) -> transform(key, count.toEmbedded()) }

    fun forEach(consumer: (K, ResultCountEmbedded) -> Unit) {
        counts.forEach { (key, count) ->
            consumer(key, count.toEmbedded())
        }
    }

    private class ResultCount {
        var victoryCount: Long = 0
        var defeatCount: Long = 0
        var drawCount: Long = 0

        fun add(result: BattleResult) {
            when (result) {
                BattleResult.VICTORY -> victoryCount++
                BattleResult.DEFEAT -> defeatCount++
                BattleResult.DRAW -> drawCount++
            }
        }

        fun toEmbedded(): ResultCountEmbedded =
            ResultCountEmbedded.create(
                victoryCount = victoryCount,
                defeatCount = defeatCount,
                drawCount = drawCount,
            )
    }
}
