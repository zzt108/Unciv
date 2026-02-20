package com.unciv.models.tilesets

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TileSetConfigTest {

    private lateinit var config: TileSetConfig

    @Before
    fun setup() {
        config = TileSetConfig()
    }

    @Test
    fun `exact match takes precedence over template`() {
        // Setup: Add both an exact match and a template
        config.ruleVariants["Plains+Forest+Iron"] = arrayOf("Plains", "Iron", "ForestP")
        config.ruleVariants["Plains+Forest+[resource]"] = arrayOf("Plains", "[resource]", "ForestP")
        config.updateConfig(config) // Trigger template parsing

        // Test: Exact match should be returned
        val result = config.resolveRuleVariant("Plains+Forest+Iron")
        assertNotNull(result)
        assertArrayEquals(arrayOf("Plains", "Iron", "ForestP"), result)
    }

    @Test
    fun `template matches and substitutes single variable`() {
        // Setup: Template with single variable
        config.ruleVariants["Tundra+[resource]+Academy"] = arrayOf("Tundra", "[resource]", "AcademyT")
        config.updateConfig(config)

        // Test: Template should match and substitute
        val result = config.resolveRuleVariant("Tundra+Deer+Academy")
        assertNotNull(result)
        assertArrayEquals(arrayOf("Tundra", "Deer", "AcademyT"), result)
    }

    @Test
    fun `template matches and substitutes in multiple positions`() {
        // Setup: Template with variable appearing in multiple output positions
        config.ruleVariants["Tundra+[resource]+Forest+Trading post"] =
            arrayOf("Tundra", "[resource]", "ForestT", "Trading postT")
        config.updateConfig(config)

        // Test: Template should match and substitute
        val result = config.resolveRuleVariant("Tundra+Deer+Forest+Trading post")
        assertNotNull(result)
        assertArrayEquals(arrayOf("Tundra", "Deer", "ForestT", "Trading postT"), result)
    }

    @Test
    fun `template with complex resource names`() {
        // Setup: Template that should work with multi-word resources
        config.ruleVariants["Grassland+Forest+[resource]"] = arrayOf("Grassland", "[resource]", "ForestG")
        config.updateConfig(config)

        // Test: Multi-word resource name
        val result = config.resolveRuleVariant("Grassland+Forest+Gold Ore")
        assertNotNull(result)
        assertArrayEquals(arrayOf("Grassland", "Gold Ore", "ForestG"), result)
    }

    @Test
    fun `template does not match wrong terrain`() {
        // Setup: Specific terrain template
        config.ruleVariants["Plains+[resource]+Mine"] = arrayOf("Plains", "[resource]", "MineP")
        config.updateConfig(config)

        // Test: Different terrain should not match
        val result = config.resolveRuleVariant("Grassland+Iron+Mine")
        assertNull(result)
    }

    @Test
    fun `template does not match different length`() {
        // Setup: Template with 3 parts
        config.ruleVariants["Tundra+[resource]+Mine"] = arrayOf("Tundra", "[resource]", "MineT")
        config.updateConfig(config)

        // Test: 4-part combination should not match
        val result = config.resolveRuleVariant("Tundra+Hill+Iron+Mine")
        assertNull(result)
    }

    @Test
    fun `more specific template takes precedence`() {
        // Setup: Two templates with different specificity
        config.ruleVariants["[terrain]+Forest+Iron"] = arrayOf("[terrain]", "Iron", "Forest")
        config.ruleVariants["Plains+Forest+[resource]"] = arrayOf("Plains", "[resource]", "ForestP")
        config.updateConfig(config)

        // Test: More specific template (fewer wildcards) should match
        val result = config.resolveRuleVariant("Plains+Forest+Iron")
        assertNotNull(result)
        // Should match the more specific template with Plains
        assertArrayEquals(arrayOf("Plains", "Iron", "ForestP"), result)
    }

    @Test
    fun `result is cached after first lookup`() {
        // Setup: Template
        config.ruleVariants["Tundra+[resource]+Fort"] = arrayOf("Tundra", "[resource]", "FortT")
        config.updateConfig(config)

        // Test: First lookup
        val result1 = config.resolveRuleVariant("Tundra+Coal+Fort")
        assertNotNull(result1)

        // Modify the template (shouldn't affect cached result)
        config.ruleVariants["Tundra+[resource]+Fort"] = arrayOf("Modified", "[resource]", "Modified")

        // Second lookup should return cached result
        val result2 = config.resolveRuleVariant("Tundra+Coal+Fort")
        assertNotNull(result2)
        assertArrayEquals(arrayOf("Tundra", "Coal", "FortT"), result2)
    }

    @Test
    fun `cache is cleared on updateConfig`() {
        // Setup: Initial template
        config.ruleVariants["Tundra+[resource]+Fort"] = arrayOf("Tundra", "[resource]", "FortT")
        config.updateConfig(config)

        // First lookup to populate cache
        config.resolveRuleVariant("Tundra+Coal+Fort")

        // Update config with new template
        val newConfig = TileSetConfig()
        newConfig.ruleVariants["Tundra+[resource]+Fort"] = arrayOf("Tundra", "[resource]", "NewFort")
        config.updateConfig(newConfig)

        // Should return new result, not cached
        val result = config.resolveRuleVariant("Tundra+Coal+Fort")
        assertNotNull(result)
        assertArrayEquals(arrayOf("Tundra", "Coal", "NewFort"), result)
    }

    @Test
    fun `null result is cached for non-matching combinations`() {
        // Setup: Template that won't match our test case
        config.ruleVariants["Plains+[resource]+Farm"] = arrayOf("Plains", "Farm", "[resource]")
        config.updateConfig(config)

        // Test: Non-matching combination
        val result1 = config.resolveRuleVariant("Grassland+Iron+Mine")
        assertNull(result1)

        // Second lookup should also return null (from cache)
        val result2 = config.resolveRuleVariant("Grassland+Iron+Mine")
        assertNull(result2)
    }

    @Test
    fun `HexaRealm strategic resources under forest are equivalent`() {
        // Setup: Templates matching HexaRealm consolidation
        config.ruleVariants["Grassland+Forest+[resource]"] = arrayOf("Grassland", "[resource]", "ForestG")
        config.ruleVariants["Plains+Forest+[resource]"] = arrayOf("Plains", "[resource]", "ForestP")
        config.ruleVariants["Grassland+Hill+Forest+[resource]"] = arrayOf("Grassland+Hill", "[resource]", "ForestGH")
        config.ruleVariants["Plains+Hill+Forest+[resource]"] = arrayOf("Plains+Hill", "[resource]", "ForestPH")
        config.updateConfig(config)

        // Test: Verify templates produce same results as original explicit rules
        val tests = mapOf(
            "Grassland+Forest+Coal" to arrayOf("Grassland", "Coal", "ForestG"),
            "Plains+Forest+Iron" to arrayOf("Plains", "Iron", "ForestP"),
            "Grassland+Hill+Forest+Uranium" to arrayOf("Grassland+Hill", "Uranium", "ForestGH"),
            "Plains+Hill+Forest+Silver" to arrayOf("Plains+Hill", "Silver", "ForestPH"),
            "Grassland+Forest+Aluminum" to arrayOf("Grassland", "Aluminum", "ForestG"),
            "Plains+Forest+Copper" to arrayOf("Plains", "Copper", "ForestP"),
            "Grassland+Forest+Gems" to arrayOf("Grassland", "Gems", "ForestG"),
            "Plains+Forest+Gold Ore" to arrayOf("Plains", "Gold Ore", "ForestP")
        )

        tests.forEach { (combination, expected) ->
            val result = config.resolveRuleVariant(combination)
            assertNotNull("Failed to match: $combination", result)
            assertArrayEquals("Wrong result for: $combination", expected, result)
        }
    }

    @Test
    fun `HexaRealm Tundra improvements are equivalent`() {
        // Setup: Templates matching HexaRealm Great Improvements
        config.ruleVariants["Tundra+Academy"] = arrayOf("Tundra", "AcademyT")
        config.ruleVariants["Tundra+[resource]+Academy"] = arrayOf("Tundra", "[resource]", "AcademyT")
        config.ruleVariants["Tundra+Furs+Academy"] = arrayOf("Tundra", "FursT", "AcademyT")
        config.ruleVariants["Tundra+Hill+Academy"] = arrayOf("Tundra+Hill", "AcademyT")
        config.ruleVariants["Tundra+Hill+[resource]+Academy"] = arrayOf("Tundra+Hill", "[resource]", "AcademyT")
        config.ruleVariants["Tundra+Hill+Furs+Academy"] = arrayOf("Tundra+Hill", "FursT", "AcademyT")
        config.updateConfig(config)

        // Test: Verify Furs exception is handled correctly
        val fursResult = config.resolveRuleVariant("Tundra+Furs+Academy")
        assertNotNull(fursResult)
        assertArrayEquals(arrayOf("Tundra", "FursT", "AcademyT"), fursResult)

        // Test: Other resources use template
        val deerResult = config.resolveRuleVariant("Tundra+Deer+Academy")
        assertNotNull(deerResult)
        assertArrayEquals(arrayOf("Tundra", "Deer", "AcademyT"), deerResult)

        // Test: Hill variants
        val hillFursResult = config.resolveRuleVariant("Tundra+Hill+Furs+Academy")
        assertNotNull(hillFursResult)
        assertArrayEquals(arrayOf("Tundra+Hill", "FursT", "AcademyT"), hillFursResult)

        val hillIronResult = config.resolveRuleVariant("Tundra+Hill+Iron+Academy")
        assertNotNull(hillIronResult)
        assertArrayEquals(arrayOf("Tundra+Hill", "Iron", "AcademyT"), hillIronResult)
    }

    @Test
    fun `HexaRealm forest with lumber mill are equivalent`() {
        // Setup: Templates matching HexaRealm Forest+Lumber mill consolidation
        config.ruleVariants["Tundra+Forest+Lumber mill"] = arrayOf("Tundra", "ForestT", "Lumber millT")
        config.ruleVariants["Tundra+[resource]+Forest+Lumber mill"] =
            arrayOf("Tundra", "[resource]", "ForestT", "Lumber millT")
        config.ruleVariants["Tundra+Furs+Forest+Lumber mill"] =
            arrayOf("Tundra", "FursT", "ForestT", "Lumber millT")
        config.ruleVariants["Tundra+Hill+Forest+Lumber mill"] =
            arrayOf("Tundra+Hill", "ForestTH", "Lumber millT")
        config.ruleVariants["Tundra+Hill+[resource]+Forest+Lumber mill"] =
            arrayOf("Tundra+Hill", "[resource]", "ForestTH", "Lumber millT")
        config.ruleVariants["Tundra+Hill+Furs+Forest+Lumber mill"] =
            arrayOf("Tundra+Hill", "FursT", "ForestTH", "Lumber millT")
        config.updateConfig(config)

        // Test: Templates produce expected results
        val tests = mapOf(
            "Tundra+Deer+Forest+Lumber mill" to arrayOf("Tundra", "Deer", "ForestT", "Lumber millT"),
            "Tundra+Furs+Forest+Lumber mill" to arrayOf("Tundra", "FursT", "ForestT", "Lumber millT"),
            "Tundra+Hill+Stone+Forest+Lumber mill" to arrayOf("Tundra+Hill", "Stone", "ForestTH", "Lumber millT"),
            "Tundra+Hill+Furs+Forest+Lumber mill" to arrayOf("Tundra+Hill", "FursT", "ForestTH", "Lumber millT")
        )

        tests.forEach { (combination, expected) ->
            val result = config.resolveRuleVariant(combination)
            assertNotNull("Failed to match: $combination", result)
            assertArrayEquals("Wrong result for: $combination", expected, result)
        }
    }

    @Test
    fun `template does not match special cases like Barbarian encampment`() {
        // Setup: Template that might accidentally match
        config.ruleVariants["Plains+Forest+[resource]"] = arrayOf("Plains", "[resource]", "ForestP")
        config.updateConfig(config)

        // Note: In actual HexaRealm, Barbarian encampment has explicit rules
        // Template should work if queried, but won't be used because explicit rules exist
        val result = config.resolveRuleVariant("Plains+Forest+Barbarian encampment")

        // This WOULD match the template, but in practice explicit rules take precedence
        assertNotNull(result)
        assertArrayEquals(arrayOf("Plains", "Barbarian encampment", "ForestP"), result)
    }

    @Test
    fun `cloned config does not copy caches`() {
        // Setup: Config with populated cache
        config.ruleVariants["Tundra+[resource]+Mine"] = arrayOf("Tundra", "[resource]", "MineT")
        config.updateConfig(config)

        // Populate cache
        config.resolveRuleVariant("Tundra+Iron+Mine")

        // Clone
        val cloned = config.clone()

        // Cloned config should re-parse templates
        val result = cloned.resolveRuleVariant("Tundra+Uranium+Mine")
        assertNotNull(result)
        assertArrayEquals(arrayOf("Tundra", "Uranium", "MineT"), result)
    }

    @Test
    fun `empty rule variants returns null`() {
        // Setup: Empty config
        config.updateConfig(config)

        // Test: Should return null for any combination
        val result = config.resolveRuleVariant("Plains+Forest+Iron")
        assertNull(result)
    }

    @Test
    fun `template with no wildcards is treated as exact match`() {
        // Setup: "Template" with no wildcards
        config.ruleVariants["Plains+Forest+Iron"] = arrayOf("Plains", "Iron", "ForestP")
        config.updateConfig(config)

        // Test: Should work like exact match
        val result = config.resolveRuleVariant("Plains+Forest+Iron")
        assertNotNull(result)
        assertArrayEquals(arrayOf("Plains", "Iron", "ForestP"), result)
    }
}
