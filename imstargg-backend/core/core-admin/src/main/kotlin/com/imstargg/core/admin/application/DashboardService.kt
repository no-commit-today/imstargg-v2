package com.imstargg.core.admin.application

import com.imstargg.core.admin.controller.response.DashboardSummaryResponse
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageType
import com.imstargg.infrastructure.db.core.brawlstars.MessageType
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.BrawlerJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.brawler.repository.GearJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.event.repository.BattleEventModeJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.repository.BrawlStarsImageJpaRepository
import com.imstargg.infrastructure.db.core.brawlstars.repository.MessageJpaRepository
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

        val existingMessages =
            messageJpaRepository
                .findAll()
                .map { it.type to it.brawlStarsId }
                .toSet()

        val existingImages =
            imageJpaRepository
                .findAll()
                .map { it.type to it.brawlStarsId }
                .toSet()

        return DashboardSummaryResponse(
            brawlerTotal = brawlers.size,
            brawlerWithName = brawlers.count { (MessageType.BRAWLER_NAME to it.brawlStarsId) in existingMessages },
            brawlerWithImage = brawlers.count { (BrawlStarsImageType.BRAWLER_PORTRAIT to it.brawlStarsId) in existingImages },
            gearTotal = gears.size,
            gearWithName = gears.count { (MessageType.GEAR_NAME to it.brawlStarsId) in existingMessages },
            gearWithImage = gears.count { (BrawlStarsImageType.GEAR to it.brawlStarsId) in existingImages },
            eventModeTotal = modes.size,
            eventModeWithName = modes.count { (MessageType.BATTLE_EVENT_MODE_NAME to it.brawlStarsId) in existingMessages },
            eventModeWithIcon = modes.count { (BrawlStarsImageType.BATTLE_EVENT_MODE_ICON to it.brawlStarsId) in existingImages },
            eventMapTotal = events.count { it.mapName != null },
            eventMapWithName =
                events.count {
                    it.mapName != null && (MessageType.BATTLE_EVENT_NAME to it.brawlStarsId) in existingMessages
                },
        )
    }
}
