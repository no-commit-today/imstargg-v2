package com.imstargg.core.admin.controller.response

data class DashboardSummaryResponse(
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
)
