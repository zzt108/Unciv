package com.unciv.ui.screens.overviewscreen.aiexport

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.civilization.aiexport.AiStatusExporter
import com.unciv.ui.components.UncivTooltip.Companion.addTooltip
import com.unciv.ui.components.extensions.toCheckBox
import com.unciv.ui.components.extensions.toTextButton
import com.unciv.ui.components.input.onClick
import com.unciv.ui.popups.ToastPopup

/**
 * Creates the header table for the Empire Overview screen, injecting the AI Status Export tools.
 * Gracefully degrades to just returning a table with the close button if the export feature fails to load.
 */
fun createAiExportHeader(viewingPlayer: Civilization, closeButton: Actor): Table {
    val headerTable = Table()

    try {
        val includeContextCheckbox = "Context".toCheckBox(AiExportPreferences.includeContext) {
            AiExportPreferences.includeContext = it
        }
        includeContextCheckbox.addTooltip("Check to include the Unciv ruleset and map instructions (Recommended for new AI chats)")

        val exportButton = "Copy Status".toTextButton()
        exportButton.onClick {
            val reportText = AiStatusExporter.generateAiStatusReport(viewingPlayer, includeContextCheckbox.isChecked)
            Gdx.app.clipboard.contents = reportText
            val stage = includeContextCheckbox.stage
            if (stage != null) {
                ToastPopup("Status for AI copied to clipboard!", stage)
            }
        }

        headerTable.add(includeContextCheckbox).padRight(10f)
        headerTable.add(exportButton).padRight(10f)
    } catch (e: Exception) {
        Gdx.app.error("AiExportUIInjector", "Failed to inject AI Export buttons", e)
    }

    headerTable.add(closeButton)
    return headerTable
}
