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

        var result = AiStatusExporter.generateAiStatusReport(civ)

        assertTrue(result.contains("AI Status Report for Rome"))
        assertTrue(result.contains("Global Empire Status"))
        assertTrue(result.contains("Ruleset:"))
        assertTrue(result.contains("City Reports"))
        assertTrue(result.contains("Roma"))
        assertTrue(result.contains("Pop 1"))

        result = AiStatusExporter.generateAiStatusReport(civ, includeSystemContext = false)
        assertTrue(result.contains("AI Status Report for Rome"))
        assertTrue(!result.contains("<system_context>"))
        assertTrue(!result.contains("Ruleset:"))
    }

    @Test
    fun testHexDirectionLogic() {
        val testGame = TestGame()
        testGame.makeHexagonalMap(2)

        val nation = Nation()
        nation.name = "Rome"
        val civ = testGame.addCiv(nation)

        val enemyNation = Nation()
        enemyNation.name = "Barbarians"
        val enemyCiv = testGame.addCiv(enemyNation)

        val cityTile = testGame.getTile(0, 0)
        val city = testGame.addCity(civ, cityTile)
        city.name = "Roma"

        // Based on clockPositionToHexcoordMap in HexMath:
        // 12 o'clock is (1, 1) -> N
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(1, 1)).apply { name = "Warrior_N" }
        // 2 o'clock is (0, 1) -> NE
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(0, 1)).apply { name = "Warrior_NE" }
        // 4 o'clock is (-1, 0) -> SE
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(-1, 0)).apply { name = "Warrior_SE" }
        // 6 o'clock is (-1, -1) -> S
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(-1, -1)).apply { name = "Warrior_S" }
        // 8 o'clock is (0, -1) -> SW
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(0, -1)).apply { name = "Warrior_SW" }
        // 10 o'clock is (1, 0) -> NW
        testGame.addUnit("Warrior", enemyCiv, testGame.getTile(1, 0)).apply { name = "Warrior_NW" }

        val result = AiStatusExporter.generateAiStatusReport(civ)

        assertTrue(result.contains("**N, Dist 1**: Enemy Unit: Warrior_N"))
        assertTrue(result.contains("**NE, Dist 1**: Enemy Unit: Warrior_NE"))
        assertTrue(result.contains("**SE, Dist 1**: Enemy Unit: Warrior_SE"))
        assertTrue(result.contains("**S, Dist 1**: Enemy Unit: Warrior_S"))
        assertTrue(result.contains("**SW, Dist 1**: Enemy Unit: Warrior_SW"))
        assertTrue(result.contains("**NW, Dist 1**: Enemy Unit: Warrior_NW"))
    }

    @Test
    fun testResearchExport() {
        val testGame = TestGame()
        testGame.makeHexagonalMap(1)
        val nation = Nation()
        nation.name = "Rome"
        val civ = testGame.addCiv(nation)

        // Case 1: No research selected
        civ.tech.techsToResearch.clear()
        var result = AiStatusExporter.generateAiStatusReport(civ)
        assertTrue(
                "Should show available techs",
                result.contains("**Current Research:** None (Available:")
        )

        // Case 2: Research selected
        val techName =
                civ.gameInfo
                        .ruleset
                        .technologies
                        .values
                        .filter { civ.tech.canBeResearched(it.name) }
                        .first()
                        .name
        civ.tech.techsToResearch.add(techName)
        result = AiStatusExporter.generateAiStatusReport(civ)
        assertTrue(
                "Should show current tech and turns",
                result.contains("**Current Research:** $techName")
        )
        assertTrue("Should show turns-to-completion", result.contains("turns)"))
    }

    @Test
    fun testTechAndReligionExport() {
        val testGame = TestGame()
        testGame.makeHexagonalMap(1)
        val nation = Nation()
        nation.name = "Rome"
        val civ = testGame.addCiv(nation)

        // Setup Tech
        civ.tech.techsResearched.add("Agriculture")
        civ.tech.techsResearched.add("Pottery")

        // Setup Golden Age
        civ.goldenAges.addHappiness(1000)
        civ.goldenAges.endTurn(0)

        // Generate Report
        val result = AiStatusExporter.generateAiStatusReport(civ)

        assertTrue("Should show golden age", result.contains("**Golden Age:** Active"))
    }
}
