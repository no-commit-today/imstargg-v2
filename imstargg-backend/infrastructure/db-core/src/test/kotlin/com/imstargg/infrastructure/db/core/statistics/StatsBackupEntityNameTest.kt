package com.imstargg.infrastructure.db.core.statistics

import jakarta.persistence.GeneratedValue
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class StatsBackupEntityNameTest {
    @Test
    fun `1인 승률 옛 통계의 테이블, 인덱스, 유니크, 시퀀스 이름에 backup 이 붙는다`() {
        assertBackupNames(
            entity = BrawlerBattleResultStatsBackupEntity::class,
            table = "brawler_battle_result_stats_backup",
            unique = "uk_brawler_battle_result_stats_backup__1",
            indexes =
                listOf(
                    "ix_brawler_battle_result_stats_backup_1",
                    "ix_brawler_battle_result_stats_backup_2",
                ),
            sequence = "brawler_battle_result_stats_backup_seq",
            generator = "brawlerBattleResultStatsBackupSeq",
        )
    }

    @Test
    fun `1인 순위 옛 통계의 테이블, 인덱스, 유니크, 시퀀스 이름에 backup 이 붙는다`() {
        assertBackupNames(
            entity = BrawlerBattleRankStatsBackupEntity::class,
            table = "brawler_battle_rank_stats_backup",
            unique = "uk_brawler_battle_rank_stats_backup__1",
            indexes = listOf("ix_brawler_battle_rank_stats_backup"),
            sequence = "brawler_battle_rank_stats_backup_seq",
            generator = "brawlerBattleRankStatsBackupSeq",
        )
    }

    @Test
    fun `상대 옛 통계의 테이블, 유니크, 시퀀스 이름에 backup 이 붙는다`() {
        assertBackupNames(
            entity = BrawlerEnemyBattleResultStatsBackupEntity::class,
            table = "brawler_enemy_battle_result_stats_backup",
            unique = "uk_brawler_enemy_battle_result_stats_backup__1",
            indexes = emptyList(),
            sequence = "brawler_enemy_battle_result_stats_backup_seq",
            generator = "brawlerEnemyBattleResultStatsBackupSeq",
        )
    }

    @Test
    fun `2인 조합 옛 통계의 테이블, 인덱스, 유니크, 시퀀스 이름에 backup 이 붙는다`() {
        assertBackupNames(
            entity = BrawlerPairBattleResultStatsBackupEntity::class,
            table = "brawler_pair_battle_result_stats_backup",
            unique = "uk_brawler_pair_battle_result_stats_backup__1",
            indexes =
                listOf(
                    "ix_brawler_pair_battle_result_stats_backup__brawler1",
                    "ix_brawler_pair_battle_result_stats_backup__brawler2",
                ),
            sequence = "brawler_pair_battle_result_stats_backup_seq",
            generator = "brawlerPairBattleResultStatsBackupSeq",
        )
    }

    @Test
    fun `3인 조합 옛 통계의 테이블, 인덱스, 유니크, 시퀀스 이름에 backup 이 붙는다`() {
        assertBackupNames(
            entity = BrawlerTrioBattleResultStatsBackupEntity::class,
            table = "brawler_trio_battle_result_stats_backup",
            unique = "uk_brawler_trio_battle_result_stats_backup__1",
            indexes =
                listOf(
                    "ix_brawler_trio_battle_result_stats_backup__brawler1",
                    "ix_brawler_trio_battle_result_stats_backup__brawler2",
                    "ix_brawler_trio_battle_result_stats_backup__brawler3",
                ),
            sequence = "brawler_trio_battle_result_stats_backup_seq",
            generator = "brawlerTrioBattleResultStatsBackupSeq",
        )
    }

    private fun assertBackupNames(
        entity: KClass<*>,
        table: String,
        unique: String,
        indexes: List<String>,
        sequence: String,
        generator: String,
    ) {
        val tableAnnotation = entity.java.getAnnotation(Table::class.java)
        assertThat(tableAnnotation.name).isEqualTo(table)
        assertThat(tableAnnotation.uniqueConstraints.map { it.name }).containsExactly(unique)
        assertThat(tableAnnotation.indexes.map { it.name }).containsExactlyInAnyOrderElementsOf(indexes)

        val idField = entity.java.getDeclaredField("id")
        val sequenceGenerator = idField.getAnnotation(SequenceGenerator::class.java)
        assertThat(sequenceGenerator.sequenceName).isEqualTo(sequence)
        assertThat(sequenceGenerator.name).isEqualTo(generator)
        assertThat(idField.getAnnotation(GeneratedValue::class.java).generator).isEqualTo(generator)
    }
}
