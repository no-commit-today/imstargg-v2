package com.imstargg.core.admin.ui.gear

import com.imstargg.core.admin.application.GearAdminService
import com.imstargg.core.admin.response.GearResponse
import com.imstargg.core.admin.ui.MainLayout
import com.imstargg.core.enums.GearRarity
import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageType
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
import com.vaadin.flow.component.upload.Upload
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.streams.UploadHandler

@Route(value = "gear", layout = MainLayout::class)
@PageTitle("기어 관리")
class GearListView(
    private val gearAdminService: GearAdminService,
) : VerticalLayout() {
    private val grid = Grid(GearResponse::class.java, false)

    init {
        setSizeFull()
        configureGrid()

        val addButton = Button("기어 등록") { openCreateDialog() }
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY)

        add(addButton, grid)
        refreshGrid()
    }

    private fun configureGrid() {
        grid.addColumn { it.gear.brawlStarsId }.setHeader("BrawlStars ID").setSortable(true)
        grid.addColumn { it.names[Language.KOREAN] ?: "" }.setHeader("이름(KO)")
        grid.addColumn { it.names[Language.ENGLISH] ?: "" }.setHeader("이름(EN)")
        grid.addColumn { it.gear.rarity.name }.setHeader("Rarity")
        grid.addColumn { if (it.image != null) "O" else "X" }.setHeader("이미지")

        grid.addComponentColumn { gear -> createEditButton(gear) }.setHeader("편집")

        grid.setSizeFull()
    }

    private fun createEditButton(gear: GearResponse): Button = Button("편집") { openEditDialog(gear) }

    private fun openCreateDialog() {
        val dialog = Dialog()
        dialog.headerTitle = "기어 등록"

        val brawlStarsIdField = TextField("BrawlStars ID")
        val rarityCombo =
            ComboBox<GearRarity>("Rarity").apply {
                setItems(*GearRarity.entries.toTypedArray())
            }

        val saveButton =
            Button("저장") {
                val id = brawlStarsIdField.value?.toLongOrNull()
                val rarity = rarityCombo.value
                if (id == null || rarity == null) {
                    Notification.show("필수 값을 입력하세요.").addThemeVariants(NotificationVariant.LUMO_ERROR)
                    return@Button
                }
                gearAdminService.create(id, rarity)
                Notification.show("기어가 등록되었습니다.")
                dialog.close()
                refreshGrid()
            }
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY)

        dialog.add(VerticalLayout(brawlStarsIdField, rarityCombo))
        dialog.footer.add(Button("취소") { dialog.close() }, saveButton)
        dialog.open()
    }

    private fun openEditDialog(gear: GearResponse) {
        val dialog = Dialog()
        dialog.headerTitle = "기어 편집 - ${gear.gear.brawlStarsId}"

        val koField = TextField("이름 (KO)").apply { value = gear.names[Language.KOREAN] ?: "" }
        val enField = TextField("이름 (EN)").apply { value = gear.names[Language.ENGLISH] ?: "" }

        val saveNameButton =
            Button("이름 저장") {
                gearAdminService.updateMessages(
                    MessageType.GEAR_NAME,
                    gear.gear.brawlStarsId,
                    mapOf(Language.KOREAN to koField.value, Language.ENGLISH to enField.value),
                )
                Notification.show("이름이 저장되었습니다.")
                refreshGrid()
            }
        saveNameButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY)

        val handler =
            UploadHandler.inMemory { metadata, data ->
                gearAdminService.updateImage(
                    BrawlStarsImageType.GEAR,
                    gear.gear.brawlStarsId,
                    data.inputStream(),
                    metadata.contentType(),
                )
                ui.ifPresent {
                    it.access {
                        Notification.show("이미지가 업로드되었습니다.")
                        refreshGrid()
                    }
                }
            }
        val upload =
            Upload(handler).apply {
                setAcceptedFileTypes("image/*")
                maxFiles = 1
            }

        dialog.add(
            VerticalLayout(
                HorizontalLayout(koField, enField),
                saveNameButton,
                upload,
            ),
        )
        dialog.footer.add(Button("닫기") { dialog.close() })
        dialog.open()
    }

    private fun refreshGrid() {
        grid.setItems(gearAdminService.findAll())
    }
}
