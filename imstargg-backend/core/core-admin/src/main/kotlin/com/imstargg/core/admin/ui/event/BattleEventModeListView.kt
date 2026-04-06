package com.imstargg.core.admin.ui.event

import com.imstargg.core.admin.application.BattleEventAdminService
import com.imstargg.core.admin.response.BattleEventModeResponse
import com.imstargg.core.admin.ui.MainLayout
import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageType
import com.imstargg.infrastructure.db.core.brawlstars.MessageType
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.component.upload.Upload
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.streams.UploadHandler

@Route(value = "event-mode", layout = MainLayout::class)
@PageTitle("이벤트 모드 관리")
class BattleEventModeListView(
    private val battleEventAdminService: BattleEventAdminService,
) : VerticalLayout() {
    private val grid = Grid(BattleEventModeResponse::class.java, false)

    init {
        setSizeFull()
        configureGrid()

        val addButton = Button("이벤트 모드 등록") { openAddModeDialog() }
        add(addButton, grid)
        refreshGrid()
    }

    private fun configureGrid() {
        grid.addColumn { it.mode.brawlStarsId }.setHeader("BrawlStars ID").setSortable(true)
        grid.addColumn { it.mode.brawlStarsName }.setHeader("BrawlStars Name")
        grid.addColumn { it.names[Language.KOREAN] ?: "" }.setHeader("이름(KO)")
        grid.addColumn { it.names[Language.ENGLISH] ?: "" }.setHeader("이름(EN)")
        grid.addColumn { if (it.icon != null) "O" else "X" }.setHeader("아이콘")

        grid
            .addComponentColumn { mode ->
                Button("편집") { openEditDialog(mode) }
            }.setHeader("편집")

        grid.setSizeFull()
    }

    private fun openAddModeDialog() {
        val dialog = Dialog()
        dialog.headerTitle = "이벤트 모드 등록"

        val idField = TextField("BrawlStars ID")
        val nameField = TextField("BrawlStars Name")

        val saveButton =
            Button("저장") {
                val id = idField.value?.toLongOrNull()
                if (id == null) {
                    Notification.show("ID를 입력하세요.").addThemeVariants(NotificationVariant.LUMO_ERROR)
                    return@Button
                }
                val name = nameField.value
                if (name.isNullOrBlank()) {
                    Notification.show("Name을 입력하세요.").addThemeVariants(NotificationVariant.LUMO_ERROR)
                    return@Button
                }
                battleEventAdminService.createMode(id, name)
                Notification.show("이벤트 모드가 등록되었습니다.")
                dialog.close()
                refreshGrid()
            }
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY)

        dialog.add(VerticalLayout(idField, nameField))
        dialog.footer.add(Button("취소") { dialog.close() }, saveButton)
        dialog.open()
    }

    private fun openEditDialog(mode: BattleEventModeResponse) {
        val dialog = Dialog()
        dialog.headerTitle = "이벤트 모드 편집 - ${mode.mode.brawlStarsName}"

        val koField = TextField("이름 (KO)").apply { value = mode.names[Language.KOREAN] ?: "" }
        val enField = TextField("이름 (EN)").apply { value = mode.names[Language.ENGLISH] ?: "" }

        val saveNameButton =
            Button("이름 저장") {
                battleEventAdminService.updateMessages(
                    MessageType.BATTLE_EVENT_MODE_NAME,
                    mode.mode.brawlStarsId,
                    mapOf(Language.KOREAN to koField.value, Language.ENGLISH to enField.value),
                )
                Notification.show("이름이 저장되었습니다.")
                refreshGrid()
            }
        saveNameButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY)

        val handler =
            UploadHandler.inMemory { metadata, data ->
                battleEventAdminService.updateImage(
                    BrawlStarsImageType.BATTLE_EVENT_MODE_ICON,
                    mode.mode.brawlStarsId,
                    data.inputStream(),
                    metadata.contentType(),
                )
                ui.ifPresent {
                    it.access {
                        Notification.show("아이콘이 업로드되었습니다.")
                        refreshGrid()
                    }
                }
            }
        val upload =
            Upload(handler).apply {
                setAcceptedFileTypes("image/*")
                maxFiles = 1
            }

        dialog.add(VerticalLayout(HorizontalLayout(koField, enField), saveNameButton, upload))
        dialog.footer.add(Button("닫기") { dialog.close() })
        dialog.open()
    }

    private fun refreshGrid() {
        grid.setItems(battleEventAdminService.findAllModes())
    }
}
