package com.imstargg.core.batch.domain

import com.imstargg.infrastructure.db.core.statistics.battle.StatsBattleEntityTeamPlayer

class PlayerCombinator {
    fun combine(
        players: List<StatsBattleEntityTeamPlayer>,
        size: Int,
    ): List<List<StatsBattleEntityTeamPlayer>> {
        require(size > 0) {
            "size($size)는 0보다 커야 합니다."
        }
        require(size <= players.size) {
            "size($size)는 팀 크기(${players.size})보다 클 수 없습니다."
        }
        return combinations(players, size)
    }

    private fun <T> combinations(
        list: List<T>,
        size: Int,
    ): List<List<T>> {
        if (size == 0) return listOf(emptyList())
        if (size == list.size) return listOf(list)
        if (list.isEmpty()) return emptyList()
        val first = list.first()
        val rest = list.subList(1, list.size)
        val withFirst = combinations(rest, size - 1).map { listOf(first) + it }
        val withoutFirst = combinations(rest, size)
        return withFirst + withoutFirst
    }
}
