package com.imstargg.core.batch.domain.statistics

import com.imstargg.core.enums.BattleEventModeResultType
import com.imstargg.core.enums.StatsEventModeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import com.imstargg.core.batch.domain.statistics.StatsBotBattlePolicy as P

class StatsBotBattlePolicyTest {
    @Nested
    inner class ThreeVsThree {
        private val bot = threeVsThree(listOf(1000, 1100, 1200), listOf(1050, 1080, 1090))

        @Test
        fun `상대 세 명이 모두 창 안이고 2000 미만이면 봇 전투다`() {
            assertThat(P.appliesTo(bot)).isTrue()
            assertThat(P.isBotBattle(bot)).isTrue()
        }

        @Test
        fun `창의 하한은 3t 가 S - 225 이상이다`() {
            // S = 3300 → 3·1025 = 3075 = S - 225
            assertThat(P.isBotBattle(threeVsThree(listOf(1000, 1100, 1200), listOf(1025, 1080, 1080)))).isTrue()
            // S = 3301(평균이 정수가 아니다) → 3·1025 = 3075 = S - 226
            assertThat(P.isBotBattle(threeVsThree(listOf(1000, 1100, 1201), listOf(1025, 1080, 1080)))).isFalse()
        }

        @Test
        fun `창의 상한은 3t 가 S + 75 미만이다`() {
            // S = 3301 → 3·1125 = 3375 = S + 74
            assertThat(P.isBotBattle(threeVsThree(listOf(1000, 1100, 1201), listOf(1125, 1080, 1080)))).isTrue()
            // S = 3300 → 3·1125 = 3375 = S + 75
            assertThat(P.isBotBattle(threeVsThree(listOf(1000, 1100, 1200), listOf(1125, 1080, 1080)))).isFalse()
        }

        @Test
        fun `상대 한 명이 2000 이면 창 조건만 참이다`() {
            // S = 5970 → 창은 1915 이상 2015 미만
            val below = threeVsThree(listOf(2000, 2000, 1970), listOf(1950, 1960, 1999))
            val atLimit = threeVsThree(listOf(2000, 2000, 1970), listOf(1950, 1960, 2000))
            assertThat(P.isBotBattle(below)).isTrue()
            assertThat(P.opponentsInTeamAverageWindow(atLimit)).isTrue()
            assertThat(P.opponentsBelowBotLimit(atLimit)).isFalse()
            assertThat(P.isBotBattle(atLimit)).isFalse()
        }

        @Test
        fun `상대 쪽만 본다`() {
            // 수집자 팀 세 명은 상대 평균(1040)의 창 안이지만 상대는 수집자 팀 창(925 이상 1025 미만) 밖이다
            assertThat(P.isBotBattle(threeVsThree(listOf(1000, 1000, 1000), listOf(1030, 1040, 1050)))).isFalse()
        }

        @Test
        fun `트로피전이 아니면 네 함수 모두 false 다`() {
            listOf("soloRanked", "challenge", "unknown").forEach { type ->
                val battle = threeVsThree(listOf(1000, 1100, 1200), listOf(1050, 1080, 1090), type = type)
                assertThat(P.appliesTo(battle)).isFalse()
                assertThat(P.opponentsBelowBotLimit(battle)).isFalse()
                assertThat(P.opponentsInTeamAverageWindow(battle)).isFalse()
                assertThat(P.isBotBattle(battle)).isFalse()
            }
        }

        @Test
        fun `경쟁전 전투는 랭크 티어 값이 창 안이어도 봇 전투가 아니다`() {
            assertThat(P.isBotBattle(threeVsThree(listOf(19, 19, 19), listOf(19, 19, 19), type = "soloRanked"))).isFalse()
        }

        @Test
        fun `2×3 결과형이면 모드 ID 와 관계없이 판정 범위에 든다`() {
            // 수집 모드 목록은 DB 에 있고 배치는 읽지 않는다. 원본에 들어온 전투는 팀 구성으로 판정한다
            listOf(0L, 25L, 999L).forEach { modeId ->
                val battle = threeVsThree(listOf(1000, 1100, 1200), listOf(1050, 1080, 1090), eventModeBrawlStarsId = modeId)
                assertThat(P.appliesTo(battle)).isTrue()
                assertThat(P.isBotBattle(battle)).isTrue()
            }
        }

        @Test
        fun `팀이 2×3 이 아니면 false 다`() {
            val twoByTwo = listOf(listOf(statsPlayer(1), statsPlayer(2)), listOf(statsPlayer(4, 1050), statsPlayer(5, 1050)))
            val threeTeams = listOf(1, 4, 7).map { first -> (first..first + 2).map { statsPlayer(it) } }
            val threeVsTwo = listOf((1..3).map { statsPlayer(it) }, listOf(statsPlayer(4), statsPlayer(5)))
            // 수집자 팀만 2명이다. 합 3000 → 창 925 이상 1025 미만 안에 상대 세 명(1000)이 든다
            val twoVsThree = listOf(listOf(statsPlayer(1, 1500), statsPlayer(2, 1500)), (4..6).map { statsPlayer(it) })
            listOf(twoByTwo, threeTeams, threeVsTwo, twoVsThree).forEach {
                assertThat(P.appliesTo(statsResultBattle(it))).isFalse()
                assertThat(P.isBotBattle(statsResultBattle(it))).isFalse()
            }
        }

        @Test
        fun `수집자 슬롯이 정확히 하나가 아니면 false 다`() {
            val collectorTeam = (1..3).map { statsPlayer(it) }
            val opponents = (4..6).map { statsPlayer(it) }
            val absent = statsResultBattle(listOf(collectorTeam, opponents), playerBrawlStarsTag = "#NOBODY")
            val twiceInTeam = statsResultBattle(listOf(listOf(statsPlayer(1), statsPlayer(2, tag = "#TAG1"), statsPlayer(3)), opponents))
            val inBothTeams =
                statsResultBattle(listOf(collectorTeam, listOf(statsPlayer(4, tag = "#TAG1"), statsPlayer(5), statsPlayer(6))))
            listOf(absent, twiceInTeam, inBothTeams).forEach { assertThat(P.isBotBattle(it)).isFalse() }
        }
    }

    @Nested
    inner class Showdown {
        private fun solo(vararg others: Int) = showdown(StatsEventModeType.SOLO_SHOWDOWN, listOf(listOf(1500)) + others.map { listOf(it) })

        private fun duo(vararg others: List<Int>) = showdown(StatsEventModeType.DUO_SHOWDOWN, listOf(listOf(1500, 1500)) + others)

        private fun trio(vararg others: List<Int>) = showdown(StatsEventModeType.TRIO_SHOWDOWN, listOf(listOf(1500, 1500, 1500)) + others)

        private val clustered = arrayOf(listOf(1000, 1010), listOf(1020, 1030), listOf(1400, 1410), listOf(1600, 1610))

        private val farDuos = arrayOf(listOf(1700, 1710), listOf(1900, 1910))

        @Test
        fun `순위형 유형은 모두 판정 범위에 들고 상대 전원이 한 창에 몰리면 봇 로비다`() {
            // 새 순위형 유형을 쇼다운 임계값 when 에 넣지 않으면 그 유형의 봇 로비가 조용히 집계에 남는다
            val lobbies =
                StatsEventModeType.entries
                    .filter { it.resultType == BattleEventModeResultType.RANK }
                    .associateWith { type -> showdown(type, List(type.teamCount) { List(type.teamSize) { 1000 } }) }

            assertThat(lobbies.filterValues { !P.appliesTo(it) }.keys).describedAs("판정 범위에서 빠진 순위형 유형").isEmpty()
            assertThat(lobbies.filterValues { !P.isBotLobby(it) }.keys).describedAs("몰린 로비를 봇 로비로 보지 않는 순위형 유형").isEmpty()
        }

        @Test
        fun `솔로는 몰린 팀이 5 이상이면 봇 로비다`() {
            assertThat(P.isBotLobby(solo(1000, 1010, 1020, 1030, 1200, 1400, 1600, 1800, 1990))).isFalse()
            assertThat(P.isBotLobby(solo(1000, 1010, 1020, 1030, 1040, 1200, 1400, 1600, 1800))).isTrue()
        }

        @Test
        fun `듀오는 몰린 팀이 2 이상이면 봇 로비다`() {
            assertThat(P.isBotLobby(duo(listOf(1000, 1010), listOf(1200, 1210), listOf(1400, 1410), listOf(1600, 1610)))).isFalse()
            assertThat(P.isBotLobby(duo(listOf(1000, 1010), listOf(1020, 1030), listOf(1400, 1410), listOf(1600, 1610)))).isTrue()
        }

        @Test
        fun `트리오는 몰린 팀이 2 이상이면 봇 로비다`() {
            assertThat(P.isBotLobby(trio(listOf(1000, 1010, 1020), listOf(1200, 1210, 1220), listOf(1400, 1410, 1420)))).isFalse()
            assertThat(P.isBotLobby(trio(listOf(1000, 1010, 1020), listOf(1030, 1040, 1050), listOf(1400, 1410, 1420)))).isTrue()
        }

        @Test
        fun `팀의 최댓값이 창 시작 + 75 미만이어야 그 창에 든다`() {
            assertThat(P.isBotLobby(duo(listOf(1000, 1010), listOf(1020, 1074), *farDuos))).isTrue()
            assertThat(P.isBotLobby(duo(listOf(1000, 1010), listOf(1020, 1075), *farDuos))).isFalse()
        }

        @Test
        fun `팀원 한 명만 창 안이면 그 팀은 세지 않는다`() {
            assertThat(P.isBotLobby(duo(listOf(1000, 1010), listOf(1050, 1300), *farDuos))).isFalse()
        }

        @Test
        fun `팀원 트로피 폭이 75 이상인 팀은 자기 창에서도 세지 않는다`() {
            // 스펙 §5 의 예: 몰린 팀 수 1
            assertThat(P.isBotLobby(duo(listOf(1000, 1100), listOf(1010, 1050), listOf(1500, 1700), listOf(2100, 2200)))).isFalse()
        }

        @Test
        fun `팀원 한 명이 2000 인 팀은 자격이 없다`() {
            assertThat(P.isBotLobby(duo(listOf(1990, 1995), listOf(1992, 1999), listOf(1000, 1010), listOf(1300, 1310)))).isTrue()
            assertThat(P.isBotLobby(duo(listOf(1990, 1995), listOf(1992, 2000), listOf(1000, 1010), listOf(1300, 1310)))).isFalse()
        }

        @Test
        fun `자격 있는 팀이 없으면 범위 안이어도 false 다`() {
            val lobby = duo(listOf(2000, 2010), listOf(2020, 2030), listOf(2040, 2050), listOf(2060, 2070))
            assertThat(P.appliesTo(lobby)).isTrue()
            assertThat(P.isBotLobby(lobby)).isFalse()
        }

        @Test
        fun `수집자 팀은 세지 않는다`() {
            val lobby =
                showdown(
                    StatsEventModeType.DUO_SHOWDOWN,
                    listOf(listOf(1000, 1010), listOf(1020, 1030), listOf(1300, 1310), listOf(1600, 1610), listOf(1900, 1910)),
                )
            assertThat(P.isBotLobby(lobby)).isFalse()
        }

        @Test
        fun `트로피전 쇼다운의 정해진 팀 구성이 아니면 false 다`() {
            val notRanked = showdown(StatsEventModeType.DUO_SHOWDOWN, listOf(listOf(1500, 1500)) + clustered, type = "soloRanked")
            val twentySolos = statsRankBattle(teams = (1..20).map { listOf(statsPlayer(it)) }, eventModeBrawlStarsId = 75)
            val tenDuos =
                statsRankBattle(
                    teams = (0 until 10).map { t -> listOf(statsPlayer(2 * t + 1), statsPlayer(2 * t + 2)) },
                    eventModeBrawlStarsId = 86,
                )
            val nineSolos = showdown(StatsEventModeType.SOLO_SHOWDOWN, listOf(listOf(1500)) + (0..7).map { listOf(1000 + it) })
            val shortTeam =
                showdown(
                    StatsEventModeType.DUO_SHOWDOWN,
                    listOf(listOf(1500, 1500), listOf(1000, 1010), listOf(1020, 1030), listOf(1400, 1410), listOf(1600)),
                )
            listOf(notRanked, twentySolos, tenDuos, nineSolos, shortTeam).forEach {
                assertThat(P.appliesTo(it)).isFalse()
                assertThat(P.isBotLobby(it)).isFalse()
            }
        }

        @Test
        fun `순위형 판정은 모드 ID 가 아니라 팀 구성으로 정한다`() {
            val lobby = statsRankBattle(duo(*clustered).teams, eventModeBrawlStarsId = 0L)
            assertThat(P.appliesTo(lobby)).isTrue()
            assertThat(P.isBotLobby(lobby)).isTrue()
        }

        @Test
        fun `수집자 태그가 두 팀에 있으면 false 다`() {
            val teams =
                listOf(
                    listOf(statsPlayer(1, 1500), statsPlayer(2, 1500)),
                    listOf(statsPlayer(3, 1000, tag = "#TAG1"), statsPlayer(4, 1010)),
                    listOf(statsPlayer(5, 1020), statsPlayer(6, 1030)),
                    listOf(statsPlayer(7, 1040), statsPlayer(8, 1050)),
                    listOf(statsPlayer(9, 1700), statsPlayer(10, 1710)),
                )
            val lobby = statsRankBattle(teams, eventModeBrawlStarsId = 9L)
            assertThat(P.isBotLobby(lobby)).isFalse()
        }

        @Test
        fun `수집자 태그가 수집자 팀 안에 두 번 나오면 false 다`() {
            // playerTeams() 는 한 팀이지만 수집자 슬롯이 둘이다. 상대의 몰린 팀 수는 2 다
            val teams =
                listOf(
                    listOf(statsPlayer(1, 1500), statsPlayer(2, 1500, tag = "#TAG1")),
                    listOf(statsPlayer(3, 1000), statsPlayer(4, 1010)),
                    listOf(statsPlayer(5, 1020), statsPlayer(6, 1030)),
                    listOf(statsPlayer(7, 1400), statsPlayer(8, 1410)),
                    listOf(statsPlayer(9, 1600), statsPlayer(10, 1610)),
                )
            val lobby = statsRankBattle(teams, eventModeBrawlStarsId = 9L)
            assertThat(P.appliesTo(lobby)).isFalse()
            assertThat(P.isBotLobby(lobby)).isFalse()
        }
    }
}
