package com.imstargg.core.admin.ui.event

import com.imstargg.core.admin.application.BattleEventAdminService
import com.imstargg.core.admin.response.BattleEventModeResponse
import com.imstargg.core.admin.response.BattleEventResponse
import com.imstargg.core.admin.ui.MainLayout
import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.brawlstars.MessageType
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.combobox.ComboBox
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route

@Route(value = "event", layout = MainLayout::class)
@PageTitle("이벤트 관리")
class BattleEventListView(
    private val battleEventAdminService: BattleEventAdminService,
) : VerticalLayout() {
    private val grid = Grid(BattleEventResponse::class.java, false)

    init {
        setSizeFull()
        configureGrid()

        val addButton = Button("이벤트 등록") { openAddEventDialog() }
        add(addButton, grid)
        refreshGrid()
    }

    private fun configureGrid() {
        grid.addColumn { it.event.brawlStarsId }.setHeader("BrawlStars ID").setSortable(true)
        grid.addColumn { it.event.mode?.brawlStarsName ?: "" }.setHeader("모드")
        grid.addColumn { it.event.mapName ?: "" }.setHeader("맵(원본)")
        grid.addColumn { it.mapNames[Language.KOREAN] ?: "" }.setHeader("맵 이름(KO)")
        grid.addColumn { it.mapNames[Language.ENGLISH] ?: "" }.setHeader("맵 이름(EN)")

        grid
            .addComponentColumn { event ->
                if (event.event.mapName != null) {
                    Button("맵 이름 편집") { openMapNameEditDialog(event) }
                } else {
                    Button("편집 불가").apply { isEnabled = false }
                }
            }.setHeader("편집")

        grid.setSizeFull()
    }

    private fun openAddEventDialog() {
        val dialog = Dialog()
        dialog.headerTitle = "이벤트 등록"

        val idField = TextField("BrawlStars ID")
        val modeCombo =
            ComboBox<BattleEventModeResponse>("Mode").apply {
                setItems(battleEventAdminService.findAllModes())
                setItemLabelGenerator { "${it.mode.brawlStarsId} - ${it.mode.brawlStarsName}" }
                isClearButtonVisible = true
            }
        val mapNameField = TextField("Map Name")

        val saveButton =
            Button("저장") {
                val id = idField.value?.toLongOrNull()
                if (id == null) {
                    Notification.show("ID를 입력하세요.").addThemeVariants(NotificationVariant.LUMO_ERROR)
                    return@Button
                }
                val modeId = modeCombo.value?.mode?.brawlStarsId
                val mapName = mapNameField.value?.takeIf { it.isNotBlank() }
                battleEventAdminService.createEvent(id, modeId, mapName)
                Notification.show("이벤트가 등록되었습니다.")
                dialog.close()
                refreshGrid()
            }
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY)

        dialog.add(VerticalLayout(idField, modeCombo, mapNameField))
        dialog.footer.add(Button("취소") { dialog.close() }, saveButton)
        dialog.open()
    }

    private fun openMapNameEditDialog(event: BattleEventResponse) {
        if (event.event.mapName == null) return
        val dialog = Dialog()
        dialog.headerTitle = "맵 이름 편집 - ${event.event.mapName}"

        val koField = TextField("이름 (KO)").apply { value = event.mapNames[Language.KOREAN] ?: "" }
        val enField = TextField("이름 (EN)").apply { value = event.mapNames[Language.ENGLISH] ?: "" }

        val saveButton =
            Button("저장") {
                battleEventAdminService.updateMessages(
                    MessageType.BATTLE_EVENT_NAME, event.event.brawlStarsId,
                    mapOf(Language.KOREAN to koField.value, Language.ENGLISH to enField.value),
                )
                Notification.show("맵 이름이 저장되었습니다.")
                dialog.close()
                refreshGrid()
            }
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY)

        dialog.add(VerticalLayout(HorizontalLayout(koField, enField)))
        dialog.footer.add(Button("취소") { dialog.close() }, saveButton)
        dialog.open()
    }

    private fun refreshGrid() {
        grid.setItems(battleEventAdminService.findAllEvents())
    }
}
