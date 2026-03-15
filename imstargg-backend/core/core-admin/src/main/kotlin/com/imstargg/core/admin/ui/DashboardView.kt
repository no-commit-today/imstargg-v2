package com.imstargg.core.admin.ui

import com.imstargg.core.admin.application.DashboardService
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H2
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route

@Route(value = "", layout = MainLayout::class)
@PageTitle("대시보드 - imstargg Admin")
class DashboardView(
    private val dashboardService: DashboardService,
) : VerticalLayout() {
    init {
        val summary = dashboardService.getSummary()

        add(H2("데이터 완성도"))
        add(
            HorizontalLayout(
                createCard(
                    "브롤러",
                    listOf(
                        "전체: ${summary.brawlerTotal}",
                        "이름 등록: ${summary.brawlerWithName}/${summary.brawlerTotal}",
                        "이미지 등록: ${summary.brawlerWithImage}/${summary.brawlerTotal}",
                    ),
                    "brawler",
                ),
                createCard(
                    "기어",
                    listOf(
                        "전체: ${summary.gearTotal}",
                        "이름 등록: ${summary.gearWithName}/${summary.gearTotal}",
                        "이미지 등록: ${summary.gearWithImage}/${summary.gearTotal}",
                    ),
                    "gear",
                ),
                createCard(
                    "이벤트 모드",
                    listOf(
                        "전체: ${summary.eventModeTotal}",
                        "이름 등록: ${summary.eventModeWithName}/${summary.eventModeTotal}",
                        "아이콘 등록: ${summary.eventModeWithIcon}/${summary.eventModeTotal}",
                    ),
                    "event-mode",
                ),
                createCard(
                    "이벤트 맵",
                    listOf(
                        "전체: ${summary.eventMapTotal}",
                        "맵 이름 등록: ${summary.eventMapWithName}/${summary.eventMapTotal}",
                    ),
                    "event",
                ),
            ).apply { setWidthFull() },
        )
    }

    private fun createCard(
        title: String,
        lines: List<String>,
        linkTarget: String,
    ): Div {
        val card =
            Div().apply {
                style.set("border", "1px solid var(--lumo-contrast-20pct)")
                style.set("border-radius", "var(--lumo-border-radius-m)")
                style.set("padding", "var(--lumo-space-m)")
                style.set("flex", "1")
            }
        val content =
            VerticalLayout(H3(title)).apply {
                isPadding = false
                isSpacing = false
            }
        lines.filter { it.isNotBlank() }.forEach { content.add(Span(it)) }
        content.add(Anchor(linkTarget, "관리 페이지로 이동 →"))
        card.add(content)
        return card
    }
}
