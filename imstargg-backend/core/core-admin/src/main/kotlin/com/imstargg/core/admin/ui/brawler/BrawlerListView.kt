package com.imstargg.core.admin.ui.brawler

import com.imstargg.core.admin.application.BrawlerAdminService
import com.imstargg.core.admin.response.BrawlerResponse
import com.imstargg.core.admin.ui.MainLayout
import com.imstargg.core.enums.BrawlerRarity
import com.imstargg.core.enums.BrawlerRole
import com.imstargg.core.enums.Language
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
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

        val addButton = Button("브롤러 등록") { openCreateDialog() }
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY)

        add(addButton, grid)
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

    private fun openCreateDialog() {
        val dialog = Dialog()
        dialog.headerTitle = "브롤러 등록"

        val brawlStarsIdField = TextField("BrawlStars ID")
        val rarityCombo =
            ComboBox<BrawlerRarity>("Rarity").apply {
                setItems(*BrawlerRarity.entries.toTypedArray())
            }
        val roleCombo =
            ComboBox<BrawlerRole>("Role").apply {
                setItems(*BrawlerRole.entries.toTypedArray())
            }

        val saveButton =
            Button("저장") {
                val id = brawlStarsIdField.value?.toLongOrNull()
                val rarity = rarityCombo.value
                val role = roleCombo.value
                if (id == null || rarity == null || role == null) {
                    Notification.show("필수 값을 입력하세요.").addThemeVariants(NotificationVariant.LUMO_ERROR)
                    return@Button
                }
                brawlerAdminService.create(id, rarity, role)
                Notification.show("브롤러가 등록되었습니다.")
                dialog.close()
                refreshGrid()
            }
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY)

        dialog.add(VerticalLayout(brawlStarsIdField, rarityCombo, roleCombo))
        dialog.footer.add(Button("취소") { dialog.close() }, saveButton)
        dialog.open()
    }

    private fun refreshGrid() {
        grid.setItems(brawlerAdminService.findAll())
    }
}
