package com.unciv.ui.screens.overviewscreen.aiexport

import com.badlogic.gdx.Gdx

/**
 * Manages the persistent settings for the AI Status Exporter feature.
 * Uses LibGDX Preferences to store settings independently of the main Unciv save files,
 * ensuring minimal intrusion into core game state.
 */
object AiExportPreferences {
    private const val PREFS_NAME = "AiExportSettings"
    private const val KEY_INCLUDE_CONTEXT = "includeContextForAiExport"
    private const val DEFAULT_INCLUDE_CONTEXT = true

    private val prefs by lazy { Gdx.app.getPreferences(PREFS_NAME) }

    var includeContext: Boolean
        get() = prefs.getBoolean(KEY_INCLUDE_CONTEXT, DEFAULT_INCLUDE_CONTEXT)
        set(value) {
            prefs.putBoolean(KEY_INCLUDE_CONTEXT, value)
            prefs.flush()
        }
}
