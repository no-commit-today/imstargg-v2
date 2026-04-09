package com.imstargg.core.admin.controller.response

import com.imstargg.core.admin.response.DashboardSummaryResponse

data class DashboardSummaryAdminResponse(
    val brawlerTotal: Int,
    val brawlerWithName: Int,
    val brawlerWithImage: Int,
    val gearTotal: Int,
    val gearWithName: Int,
    val gearWithImage: Int,
    val eventModeTotal: Int,
    val eventModeWithName: Int,
    val eventModeWithIcon: Int,
    val eventMapTotal: Int,
    val eventMapWithName: Int,
) {
    companion object {
        fun from(response: DashboardSummaryResponse) =
            DashboardSummaryAdminResponse(
                brawlerTotal = response.brawlerTotal,
                brawlerWithName = response.brawlerWithName,
                brawlerWithImage = response.brawlerWithImage,
                gearTotal = response.gearTotal,
                gearWithName = response.gearWithName,
                gearWithImage = response.gearWithImage,
                eventModeTotal = response.eventModeTotal,
                eventModeWithName = response.eventModeWithName,
                eventModeWithIcon = response.eventModeWithIcon,
                eventMapTotal = response.eventMapTotal,
                eventMapWithName = response.eventMapWithName,
            )
    }
}
