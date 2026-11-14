package com.imstargg.core.application.statistics

import com.imstargg.core.domain.statistics.StatsEventModeTypes
import org.springframework.stereotype.Service

@Service
class StatsEventModeService(
    private val statsEventModeRepository: StatsEventModeRepository,
) {
    fun getTypes(): StatsEventModeTypes = statsEventModeRepository.findAll()
}
