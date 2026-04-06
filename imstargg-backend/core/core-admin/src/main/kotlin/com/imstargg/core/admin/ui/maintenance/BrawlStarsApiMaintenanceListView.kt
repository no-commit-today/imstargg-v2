package com.imstargg.core.admin.ui.maintenance

import com.imstargg.core.admin.application.BrawlStarsApiMaintenanceAdminService
import com.imstargg.core.admin.ui.MainLayout
import com.imstargg.infrastructure.db.core.brawlstars.BrawlStarsApiMaintenanceEntity
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.datetimepicker.DateTimePicker
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.grid.Grid
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import java.time.ZoneOffset

@Route(value = "maintenance", layout = MainLayout::class)
@PageTitle("API 점검 관리")
class BrawlStarsApiMaintenanceListView(
    private val maintenanceAdminService: BrawlStarsApiMaintenanceAdminService,
) : VerticalLayout() {
    private val grid = Grid(BrawlStarsApiMaintenanceEntity::class.java, false)

    init {
        setSizeFull()
        configureGrid()

        val addButton = Button("점검 등록") { openCreateDialog() }
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY)

        add(addButton, grid)
        refreshGrid()
    }

    private fun configureGrid() {
        grid.addColumn { it.id }.setHeader("ID").setSortable(true)
        grid.addColumn { it.startTime }.setHeader("시작 시간").setSortable(true)
        grid.addColumn { it.endTime }.setHeader("종료 시간").setSortable(true)
        grid.addColumn { it.createdAt }.setHeader("생성일")

        grid
            .addComponentColumn { entity ->
                Button("삭제") {
                    entity.id?.let { id ->
                        maintenanceAdminService.delete(id)
                        refreshGrid()
                        Notification.show("삭제되었습니다.")
                    }
                }
            }.setHeader("관리")

        grid.setSizeFull()
    }

    private fun openCreateDialog() {
        val dialog = Dialog()
        dialog.headerTitle = "점검 등록"

        val startTimePicker = DateTimePicker("시작 시간")
        val endTimePicker = DateTimePicker("종료 시간")

        val saveButton =
            Button("저장") {
                val startLocal = startTimePicker.value
                val endLocal = endTimePicker.value
                if (startLocal == null || endLocal == null) {
                    Notification.show("시작 시간과 종료 시간을 입력하세요.").addThemeVariants(NotificationVariant.LUMO_ERROR)
                    return@Button
                }
                if (!endLocal.isAfter(startLocal)) {
                    Notification.show("종료 시간은 시작 시간 이후여야 합니다.").addThemeVariants(NotificationVariant.LUMO_ERROR)
                    return@Button
                }
                maintenanceAdminService.register(
                    startLocal.atOffset(ZoneOffset.UTC),
                    endLocal.atOffset(ZoneOffset.UTC),
                )
                Notification.show("점검이 등록되었습니다.")
                dialog.close()
                refreshGrid()
            }
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY)

        dialog.add(VerticalLayout(startTimePicker, endTimePicker))
        dialog.footer.add(Button("취소") { dialog.close() }, saveButton)
        dialog.open()
    }

    private fun refreshGrid() {
        grid.setItems(maintenanceAdminService.findAll())
    }
}
