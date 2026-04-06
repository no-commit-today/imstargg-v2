package com.imstargg.core.admin.ui

import com.imstargg.core.admin.ui.brawler.BrawlerListView
import com.imstargg.core.admin.ui.event.BattleEventListView
import com.imstargg.core.admin.ui.event.BattleEventModeListView
import com.imstargg.core.admin.ui.gear.GearListView
import com.imstargg.core.admin.ui.maintenance.BrawlStarsApiMaintenanceListView
import com.vaadin.flow.component.applayout.AppLayout
import com.vaadin.flow.component.applayout.DrawerToggle
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.orderedlayout.Scroller
import com.vaadin.flow.component.sidenav.SideNav
import com.vaadin.flow.component.sidenav.SideNavItem
import com.vaadin.flow.theme.lumo.LumoUtility

class MainLayout : AppLayout() {
    init {
        primarySection = Section.DRAWER
        addDrawerContent()
        addHeaderContent()
    }

    private fun addHeaderContent() {
        val toggle = DrawerToggle()
        val title =
            H1("imstargg Admin").apply {
                style.set("font-size", "var(--lumo-font-size-l)")
                style.set("margin", "0")
            }
        addToNavbar(true, toggle, title)
    }

    private fun addDrawerContent() {
        val nav =
            SideNav().apply {
                addItem(SideNavItem("대시보드", DashboardView::class.java))
                addItem(SideNavItem("브롤러 관리", BrawlerListView::class.java))
                addItem(SideNavItem("기어 관리", GearListView::class.java))
                addItem(SideNavItem("이벤트 관리", BattleEventListView::class.java))
                addItem(SideNavItem("이벤트 모드 관리", BattleEventModeListView::class.java))
                addItem(SideNavItem("API 점검 관리", BrawlStarsApiMaintenanceListView::class.java))
            }
        val scroller =
            Scroller(nav).apply {
                addClassNames(LumoUtility.Padding.SMALL)
            }
        addToDrawer(scroller)
    }
}
