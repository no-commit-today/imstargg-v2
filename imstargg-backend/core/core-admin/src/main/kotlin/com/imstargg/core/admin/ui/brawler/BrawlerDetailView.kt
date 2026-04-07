package com.imstargg.core.admin.ui.brawler

import com.imstargg.core.admin.application.BrawlerAdminService
import com.imstargg.core.admin.config.ImageProperties
import com.imstargg.core.admin.response.BrawlerResponse
import com.imstargg.core.admin.response.GadgetResponse
import com.imstargg.core.admin.response.GearResponse
import com.imstargg.core.admin.response.HyperChargeResponse
import com.imstargg.core.admin.response.StarPowerResponse
import com.imstargg.core.admin.ui.MainLayout
import com.imstargg.core.enums.Language
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsImageType
import com.imstargg.infrastructure.db.core.brawlstars.MessageType
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.combobox.MultiSelectComboBox
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.component.html.Image
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.component.upload.Upload
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.streams.UploadHandler

@Route(value = "brawler/:brawlStarsId", layout = MainLayout::class)
@PageTitle("브롤러 상세")
class BrawlerDetailView(
    private val brawlerAdminService: BrawlerAdminService,
    private val imageProperties: ImageProperties,
) : VerticalLayout(),
    BeforeEnterObserver {
    private var brawlStarsId: Long = 0

    override fun beforeEnter(event: BeforeEnterEvent) {
        brawlStarsId = event.routeParameters["brawlStarsId"].orElse("0").toLong()
        buildContent()
    }

    private fun buildContent() {
        removeAll()
        val detail = brawlerAdminService.findDetail(brawlStarsId)
        if (detail == null) {
            add(Span("브롤러를 찾을 수 없습니다."))
            return
        }

        add(buildInfoSection(detail))
        add(buildNameSection(detail))
        add(buildImageSection(detail))
        add(buildBuffyImageSection(detail))
        add(buildStarPowerSection(detail))
        add(buildGadgetSection(detail))
        add(buildHyperChargeSection(detail))
        add(buildGearSection(detail))
    }

    private fun buildInfoSection(detail: BrawlerResponse): VerticalLayout =
        VerticalLayout(
            H3("기본 정보"),
            Span("BrawlStars ID: ${detail.brawler.brawlStarsId}"),
            Span("Rarity: ${detail.brawler.rarity}"),
            Span("Role: ${detail.brawler.role}"),
        ).apply { isPadding = false }

    private fun buildNameSection(detail: BrawlerResponse): VerticalLayout {
        val koField = TextField("이름 (KO)").apply { value = detail.names[Language.KOREAN] ?: "" }
        val enField = TextField("이름 (EN)").apply { value = detail.names[Language.ENGLISH] ?: "" }
        val saveButton =
            Button("이름 저장") {
                brawlerAdminService.updateMessages(
                    MessageType.BRAWLER_NAME,
                    brawlStarsId,
                    mapOf(Language.KOREAN to koField.value, Language.ENGLISH to enField.value),
                )
                Notification.show("이름이 저장되었습니다.")
            }
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY)
        return VerticalLayout(H3("이름"), HorizontalLayout(koField, enField), saveButton).apply { isPadding = false }
    }

    private fun buildImageSection(detail: BrawlerResponse): VerticalLayout {
        val imageStatus =
            if (detail.image != null) {
                Image("${imageProperties.baseUrl}/${detail.image.storedName}", "브롤러 이미지").apply {
                    maxWidth = "128px"
                    maxHeight = "128px"
                }
            } else {
                Span("이미지 없음")
            }
        val handler =
            UploadHandler.inMemory { metadata, data ->
                brawlerAdminService.updateImage(
                    BrawlStarsImageType.BRAWLER_PORTRAIT,
                    brawlStarsId,
                    data.inputStream(),
                    metadata.contentType(),
                )
                ui.ifPresent {
                    it.access {
                        Notification.show("이미지가 업로드되었습니다.")
                        buildContent()
                    }
                }
            }
        val upload =
            Upload(handler).apply {
                setAcceptedFileTypes("image/*")
                maxFiles = 1
            }
        return VerticalLayout(H3("이미지"), imageStatus, upload).apply { isPadding = false }
    }

    private fun buildBuffyImageSection(detail: BrawlerResponse): VerticalLayout {
        val buffyTypes =
            listOf(
                Triple("가젯 버피", BrawlStarsImageType.BUFFY_GADGET, detail.buffyImages[BrawlStarsImageType.BUFFY_GADGET]),
                Triple("스타파워 버피", BrawlStarsImageType.BUFFY_STAR_POWER, detail.buffyImages[BrawlStarsImageType.BUFFY_STAR_POWER]),
                Triple("하이퍼차지 버피", BrawlStarsImageType.BUFFY_HYPER_CHARGE, detail.buffyImages[BrawlStarsImageType.BUFFY_HYPER_CHARGE]),
            )

        val content = VerticalLayout().apply { isPadding = false }
        content.add(H3("버피 이미지"))

        buffyTypes.forEach { (label, imageType, imageEntity) ->
            val imageStatus =
                if (imageEntity != null) {
                    Image("${imageProperties.baseUrl}/${imageEntity.storedName}", "$label 이미지").apply {
                        maxWidth = "128px"
                        maxHeight = "128px"
                    }
                } else {
                    Span("$label: 이미지 없음")
                }
            val handler =
                UploadHandler.inMemory { metadata, data ->
                    brawlerAdminService.updateImage(imageType, brawlStarsId, data.inputStream(), metadata.contentType())
                    ui.ifPresent {
                        it.access {
                            Notification.show("$label 이미지가 업로드되었습니다.")
                            buildContent()
                        }
                    }
                }
            val upload =
                Upload(handler).apply {
                    setAcceptedFileTypes("image/*")
                    maxFiles = 1
                }
            content.add(imageStatus, upload)
        }

        return content
    }

    private fun buildStarPowerSection(detail: BrawlerResponse): VerticalLayout {
        val grid =
            Grid(StarPowerResponse::class.java, false).apply {
                addColumn { it.starPower.brawlStarsId }.setHeader("BrawlStars ID")
                addColumn { it.names[Language.KOREAN] ?: "" }.setHeader("이름(KO)")
                addColumn { it.names[Language.ENGLISH] ?: "" }.setHeader("이름(EN)")
                addColumn { if (it.image != null) "O" else "X" }.setHeader("이미지")
                addComponentColumn { sp -> Button("편집") { openStarPowerEditDialog(sp) } }.setHeader("편집")
                setItems(detail.starPowers)
            }

        val addButton = Button("스타파워 등록") { openAddStarPowerDialog() }
        return VerticalLayout(H3("스타파워"), addButton, grid).apply { isPadding = false }
    }

    private fun buildGadgetSection(detail: BrawlerResponse): VerticalLayout {
        val grid =
            Grid(GadgetResponse::class.java, false).apply {
                addColumn { it.gadget.brawlStarsId }.setHeader("BrawlStars ID")
                addColumn { it.names[Language.KOREAN] ?: "" }.setHeader("이름(KO)")
                addColumn { it.names[Language.ENGLISH] ?: "" }.setHeader("이름(EN)")
                addColumn { if (it.image != null) "O" else "X" }.setHeader("이미지")
                addComponentColumn { gd -> Button("편집") { openGadgetEditDialog(gd) } }.setHeader("편집")
                setItems(detail.gadgets)
            }

        val addButton = Button("가젯 등록") { openAddGadgetDialog() }
        return VerticalLayout(H3("가젯"), addButton, grid).apply { isPadding = false }
    }

    private fun buildHyperChargeSection(detail: BrawlerResponse): VerticalLayout {
        val grid =
            Grid(HyperChargeResponse::class.java, false).apply {
                addColumn { it.hyperCharge.brawlStarsId }.setHeader("BrawlStars ID")
                addColumn { it.names[Language.KOREAN] ?: "" }.setHeader("이름(KO)")
                addColumn { it.names[Language.ENGLISH] ?: "" }.setHeader("이름(EN)")
                addColumn { if (it.image != null) "O" else "X" }.setHeader("이미지")
                addComponentColumn { hc -> Button("편집") { openHyperChargeEditDialog(hc) } }.setHeader("편집")
                setItems(detail.hyperCharges)
            }

        val addButton = Button("하이퍼차지 등록") { openAddHyperChargeDialog() }
        return VerticalLayout(H3("하이퍼차지"), addButton, grid).apply { isPadding = false }
    }

    private fun buildGearSection(detail: BrawlerResponse): VerticalLayout {
        val comboBox =
            MultiSelectComboBox<GearResponse>("연결된 기어").apply {
                setItems(detail.allGears)
                setItemLabelGenerator { "${it.gear.brawlStarsId} (${it.gear.rarity})" }
                value = detail.allGears.filter { it.gear.brawlStarsId in detail.linkedGearIds }.toSet()
            }

        val saveButton =
            Button("기어 연결 저장") {
                brawlerAdminService.updateBrawlerGears(
                    brawlStarsId,
                    comboBox.value.map { it.gear.brawlStarsId }.toSet(),
                )
                Notification.show("기어 연결이 저장되었습니다.")
            }
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY)

        return VerticalLayout(H3("기어 연결"), comboBox, saveButton).apply { isPadding = false }
    }

    private fun openAddStarPowerDialog() {
        val dialog = Dialog()
        dialog.headerTitle = "스타파워 등록"
        val idField = TextField("BrawlStars ID")
        val saveButton =
            Button("저장") {
                val id = idField.value?.toLongOrNull()
                if (id == null) {
                    Notification.show("ID를 입력하세요.").addThemeVariants(NotificationVariant.LUMO_ERROR)
                    return@Button
                }
                brawlerAdminService.createStarPower(id, brawlStarsId)
                Notification.show("스타파워가 등록되었습니다.")
                dialog.close()
                buildContent()
            }
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY)
        dialog.add(idField)
        dialog.footer.add(Button("취소") { dialog.close() }, saveButton)
        dialog.open()
    }

    private fun openAddGadgetDialog() {
        val dialog = Dialog()
        dialog.headerTitle = "가젯 등록"
        val idField = TextField("BrawlStars ID")
        val saveButton =
            Button("저장") {
                val id = idField.value?.toLongOrNull()
                if (id == null) {
                    Notification.show("ID를 입력하세요.").addThemeVariants(NotificationVariant.LUMO_ERROR)
                    return@Button
                }
                brawlerAdminService.createGadget(id, brawlStarsId)
                Notification.show("가젯이 등록되었습니다.")
                dialog.close()
                buildContent()
            }
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY)
        dialog.add(idField)
        dialog.footer.add(Button("취소") { dialog.close() }, saveButton)
        dialog.open()
    }

    private fun openAddHyperChargeDialog() {
        val dialog = Dialog()
        dialog.headerTitle = "하이퍼차지 등록"
        val idField = TextField("BrawlStars ID")
        val saveButton =
            Button("저장") {
                val id = idField.value?.toLongOrNull()
                if (id == null) {
                    Notification.show("ID를 입력하세요.").addThemeVariants(NotificationVariant.LUMO_ERROR)
                    return@Button
                }
                brawlerAdminService.createHyperCharge(id, brawlStarsId)
                Notification.show("하이퍼차지가 등록되었습니다.")
                dialog.close()
                buildContent()
            }
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY)
        dialog.add(idField)
        dialog.footer.add(Button("취소") { dialog.close() }, saveButton)
        dialog.open()
    }

    private fun openHyperChargeEditDialog(hc: HyperChargeResponse) {
        openSubItemEditDialog(
            title = "하이퍼차지 편집 - ${hc.hyperCharge.brawlStarsId}",
            names = hc.names,
            messageType = MessageType.HYPERCHARGE_NAME,
            imageType = BrawlStarsImageType.HYPERCHARGE,
            brawlStarsId = hc.hyperCharge.brawlStarsId,
        )
    }

    private fun openStarPowerEditDialog(sp: StarPowerResponse) {
        openSubItemEditDialog(
            title = "스타파워 편집 - ${sp.starPower.brawlStarsId}",
            names = sp.names,
            messageType = MessageType.STARPOWER_NAME,
            imageType = BrawlStarsImageType.STARPOWER,
            brawlStarsId = sp.starPower.brawlStarsId,
        )
    }

    private fun openGadgetEditDialog(gd: GadgetResponse) {
        openSubItemEditDialog(
            title = "가젯 편집 - ${gd.gadget.brawlStarsId}",
            names = gd.names,
            messageType = MessageType.GADGET_NAME,
            imageType = BrawlStarsImageType.GADGET,
            brawlStarsId = gd.gadget.brawlStarsId,
        )
    }

    private fun openSubItemEditDialog(
        title: String,
        names: Map<Language, String>,
        messageType: MessageType,
        imageType: BrawlStarsImageType,
        brawlStarsId: Long,
    ) {
        val dialog = Dialog()
        dialog.headerTitle = title

        val koField = TextField("이름 (KO)").apply { value = names[Language.KOREAN] ?: "" }
        val enField = TextField("이름 (EN)").apply { value = names[Language.ENGLISH] ?: "" }
        val saveNameButton =
            Button("이름 저장") {
                brawlerAdminService.updateMessages(
                    messageType,
                    brawlStarsId,
                    mapOf(Language.KOREAN to koField.value, Language.ENGLISH to enField.value),
                )
                Notification.show("이름이 저장되었습니다.")
                buildContent()
            }
        saveNameButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY)

        val handler =
            UploadHandler.inMemory { metadata, data ->
                brawlerAdminService.updateImage(imageType, brawlStarsId, data.inputStream(), metadata.contentType())
                ui.ifPresent {
                    it.access {
                        Notification.show("이미지가 업로드되었습니다.")
                        buildContent()
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
}
