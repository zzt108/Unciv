package com.unciv.testing.logic.civilization

import com.unciv.logic.civilization.AiStatusExporter
import com.unciv.models.ruleset.nation.Nation
import com.unciv.testing.GdxTestRunner
import com.unciv.testing.TestGame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(GdxTestRunner::class)
class AiStatusExporterTest {

    @Test
    fun testSpectatorExport() {
        val testGame = TestGame()
        val nation = Nation()
        nation.name = com.unciv.Constants.spectator
        val civ = testGame.addCiv(nation)

        val result = AiStatusExporter.generateAiStatusReport(civ)
        assertTrue(result.contains("Spectator mode"))
    }

    @Test
    fun testBasicExport() {
        val testGame = TestGame()
        testGame.makeHexagonalMap(1)

        val nation = Nation()
        nation.name = "Rome"
        val civ = testGame.addCiv(nation)

        val city = testGame.addCity(civ, testGame.getTile(0, 0))
        city.name = "Roma"

        val result = AiStatusExporter.generateAiStatusReport(civ)

        assertTrue(result.contains("AI Status Report for Rome"))
        assertTrue(result.contains("Global Empire Status"))
        assertTrue(result.contains("City Reports"))
        assertTrue(result.contains("Roma"))
        assertTrue(result.contains("Pop 1"))
    }
}
