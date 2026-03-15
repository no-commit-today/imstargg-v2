package com.imstargg.core.admin.application

import com.imstargg.core.admin.response.DashboardSummaryResponse
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.MessageJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.BrawlerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.GearJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.BattleEventModeJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class DashboardService(
    private val brawlerJpaRepository: BrawlerJpaRepository,
    private val gearJpaRepository: GearJpaRepository,
    private val battleEventJpaRepository: BattleEventJpaRepository,
    private val battleEventModeJpaRepository: BattleEventModeJpaRepository,
    private val messageJpaRepository: MessageJpaRepository,
    private val imageJpaRepository: BrawlStarsImageJpaRepository,
) {
    fun getSummary(): DashboardSummaryResponse {
        val brawlers = brawlerJpaRepository.findAll()
        val gears = gearJpaRepository.findAll()
        val events = battleEventJpaRepository.findAll()
        val modes = battleEventModeJpaRepository.findAll()

        val allMessageCodes =
            (
                brawlers.map { it.nameMessageCode() } +
                    gears.map { it.nameMessageCode() } +
                    modes.map { it.nameMessageCode() } +
                    events.mapNotNull { it.mapNameMessageCode() }
            ).toSet()
        val existingMessageCodes =
            if (allMessageCodes.isNotEmpty()) {
                messageJpaRepository.findAllByCodeIn(allMessageCodes).map { it.code }.toSet()
            } else {
                emptySet()
            }

        val allImageCodes =
            (
                brawlers.map { it.portraitImageCode() } +
                    gears.map { it.imageCode() } +
                    modes.map { it.iconImageCode() }
            ).toSet()
        val existingImageCodes =
            if (allImageCodes.isNotEmpty()) {
                imageJpaRepository.findAllByCodeIn(allImageCodes).map { it.code }.toSet()
            } else {
                emptySet()
            }

        return DashboardSummaryResponse(
            brawlerTotal = brawlers.size,
            brawlerWithName = brawlers.count { it.nameMessageCode() in existingMessageCodes },
            brawlerWithImage = brawlers.count { it.portraitImageCode() in existingImageCodes },
            gearTotal = gears.size,
            gearWithName = gears.count { it.nameMessageCode() in existingMessageCodes },
            gearWithImage = gears.count { it.imageCode() in existingImageCodes },
            eventModeTotal = modes.size,
            eventModeWithName = modes.count { it.nameMessageCode() in existingMessageCodes },
            eventModeWithIcon = modes.count { it.iconImageCode() in existingImageCodes },
            eventMapTotal = events.count { it.mapName != null },
            eventMapWithName =
                events.count {
                    val code = it.mapNameMessageCode()
                    code != null && code in existingMessageCodes
                },
        )
    }
}
