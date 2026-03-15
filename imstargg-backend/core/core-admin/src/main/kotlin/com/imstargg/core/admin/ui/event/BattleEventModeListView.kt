package com.imstargg.core.admin.ui.event

import com.imstargg.core.admin.application.EventAdminService
import com.imstargg.core.admin.response.BattleEventModeResponse
import com.imstargg.core.admin.ui.MainLayout
import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageCode
import com.imstargg.infrastructure.db.core.brawlstars.MessageCode
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.notification.Notification
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
    private val eventAdminService: EventAdminService,
) : VerticalLayout() {
    private val grid = Grid(BattleEventModeResponse::class.java, false)

    init {
        setSizeFull()
        configureGrid()
        add(grid)
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

    private fun openEditDialog(mode: BattleEventModeResponse) {
        val dialog = Dialog()
        dialog.headerTitle = "이벤트 모드 편집 - ${mode.mode.brawlStarsName}"

        val koField = TextField("이름 (KO)").apply { value = mode.names[Language.KOREAN] ?: "" }
        val enField = TextField("이름 (EN)").apply { value = mode.names[Language.ENGLISH] ?: "" }

        val saveNameButton =
            Button("이름 저장") {
                val code = MessageCode.of("event", "mode", mode.mode.brawlStarsId, "name")
                eventAdminService.updateMessages(
                    code,
                    mapOf(Language.KOREAN to koField.value, Language.ENGLISH to enField.value),
                )
                Notification.show("이름이 저장되었습니다.")
                refreshGrid()
            }
        saveNameButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY)

        val handler =
            UploadHandler.inMemory { metadata, data ->
                val code = BrawlStarsImageCode.of("event", "mode", mode.mode.brawlStarsId, "icon")
                eventAdminService.updateImage(code, data.inputStream(), metadata.contentType())
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
        grid.setItems(eventAdminService.findAllModes())
    }
}
