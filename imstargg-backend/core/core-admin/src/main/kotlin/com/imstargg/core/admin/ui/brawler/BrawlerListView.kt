package com.imstargg.core.admin.ui.brawler

import com.imstargg.core.admin.application.BrawlerAdminService
import com.imstargg.core.admin.response.BrawlerResponse
import com.imstargg.core.admin.ui.MainLayout
import com.imstargg.core.enums.Language
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route

@Route(value = "brawler", layout = MainLayout::class)
@PageTitle("브롤러 관리")
class BrawlerListView(
    private val brawlerAdminService: BrawlerAdminService,
) : VerticalLayout() {
    private val grid = Grid(BrawlerResponse::class.java, false)

    init {
        setSizeFull()
        configureGrid()

        add(grid)
        refreshGrid()
    }

    private fun configureGrid() {
        grid.addColumn { it.brawler.brawlStarsId }.setHeader("BrawlStars ID").setSortable(true)
        grid.addColumn { it.names[Language.KOREAN] ?: "" }.setHeader("이름(KO)")
        grid.addColumn { it.names[Language.ENGLISH] ?: "" }.setHeader("이름(EN)")
        grid.addColumn { it.brawler.rarity.name }.setHeader("Rarity")
        grid.addColumn { it.brawler.role.name }.setHeader("Role")
        grid.addColumn { if (it.image != null) "O" else "X" }.setHeader("이미지")

        grid
            .addComponentColumn { brawler ->
                Button("상세") {
                    ui.ifPresent { it.navigate("brawler/${brawler.brawler.brawlStarsId}") }
                }
            }.setHeader("상세")

        grid.setSizeFull()
    }

    private fun refreshGrid() {
        grid.setItems(brawlerAdminService.findAll())
    }
}
