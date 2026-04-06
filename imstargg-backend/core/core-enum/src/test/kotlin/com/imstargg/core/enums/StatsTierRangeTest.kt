package com.imstargg.core.enums

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class StatsTierRangeTest {
    @Nested
    inner class ResolveAll {
        @Nested
        inner class `일반 전투` {
            @Test
            fun `트로피 999이면 빈 리스트를 반환한다`() {
                // when
                val result = StatsTierRange.resolveAll(BattleType.RANKED, 999)

                // then
                assertThat(result).isEmpty()
            }

            @Test
            fun `트로피 1000이면 TROPHY_1000_PLUS만 반환한다`() {
                // when
                val result = StatsTierRange.resolveAll(BattleType.RANKED, 1000)

                // then
                assertThat(result).containsExactly(StatsTierRange.TROPHY_1000_PLUS)
            }

            @Test
            fun `트로피 2000이면 TROPHY_1000_PLUS와 TROPHY_2000_PLUS를 반환한다`() {
                // when
                val result = StatsTierRange.resolveAll(BattleType.RANKED, 2000)

                // then
                assertThat(result).containsExactly(
                    StatsTierRange.TROPHY_1000_PLUS,
                    StatsTierRange.TROPHY_2000_PLUS,
                )
            }

            @Test
            fun `트로피 3000이면 모든 트로피 tierRange를 반환한다`() {
                // when
                val result = StatsTierRange.resolveAll(BattleType.RANKED, 3000)

                // then
                assertThat(result).containsExactly(
                    StatsTierRange.TROPHY_1000_PLUS,
                    StatsTierRange.TROPHY_2000_PLUS,
                    StatsTierRange.TROPHY_3000_PLUS,
                )
            }

            @Test
            fun `트로피 1500이면 TROPHY_1000_PLUS만 반환한다`() {
                // when
                val result = StatsTierRange.resolveAll(BattleType.CHALLENGE, 1500)

                // then
                assertThat(result).containsExactly(StatsTierRange.TROPHY_1000_PLUS)
            }
        }

        @Nested
        inner class `솔로 랭크 전투` {
            @Test
            fun `MYTHIC_1 미만이면 빈 리스트를 반환한다`() {
                // when
                val result = StatsTierRange.resolveAll(BattleType.SOLO_RANKED, SoloRankTier.DIAMOND_3.value)

                // then
                assertThat(result).isEmpty()
            }

            @Test
            fun `MYTHIC_1이면 SOLO_RANK_MYTHIC_PLUS만 반환한다`() {
                // when
                val result = StatsTierRange.resolveAll(BattleType.SOLO_RANKED, SoloRankTier.MYTHIC_1.value)

                // then
                assertThat(result).containsExactly(StatsTierRange.SOLO_RANK_MYTHIC_PLUS)
            }

            @Test
            fun `LEGENDARY_1이면 MYTHIC_PLUS와 LEGENDARY_PLUS를 반환한다`() {
                // when
                val result = StatsTierRange.resolveAll(BattleType.SOLO_RANKED, SoloRankTier.LEGENDARY_1.value)

                // then
                assertThat(result).containsExactly(
                    StatsTierRange.SOLO_RANK_MYTHIC_PLUS,
                    StatsTierRange.SOLO_RANK_LEGENDARY_PLUS,
                )
            }

            @Test
            fun `MASTER_1이면 모든 솔로랭크 tierRange를 반환한다`() {
                // when
                val result = StatsTierRange.resolveAll(BattleType.SOLO_RANKED, SoloRankTier.MASTER_1.value)

                // then
                assertThat(result).containsExactly(
                    StatsTierRange.SOLO_RANK_MYTHIC_PLUS,
                    StatsTierRange.SOLO_RANK_LEGENDARY_PLUS,
                    StatsTierRange.SOLO_RANK_MASTER_PLUS,
                )
            }

            @Test
            fun `PRO이면 모든 솔로랭크 tierRange를 반환한다`() {
                // when
                val result = StatsTierRange.resolveAll(BattleType.SOLO_RANKED, SoloRankTier.PRO.value)

                // then
                assertThat(result).containsExactly(
                    StatsTierRange.SOLO_RANK_MYTHIC_PLUS,
                    StatsTierRange.SOLO_RANK_LEGENDARY_PLUS,
                    StatsTierRange.SOLO_RANK_MASTER_PLUS,
                )
            }
        }
    }
}
