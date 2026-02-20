package com.unciv.models.tilesets

import com.badlogic.gdx.graphics.Color
import com.unciv.Constants
import com.unciv.ui.images.ImageGetter

class TileSetConfig {
    var useColorAsBaseTerrain = false
    var useSummaryImages = false
    var unexploredTileColor: Color = Color.DARK_GRAY
    var fogOfWarColor: Color = ImageGetter.CHARCOAL
    /** Name of the tileset to use when this one is missing images. Null to disable. */
    var fallbackTileSet: String? = Constants.defaultFallbackTileset
    /** Scale factor for hex images, with hex center as origin. */
    var tileScale: Float = 1f
    var tileScales: HashMap<String, Float> = HashMap()
    /** Maps terrain+resource+improvement combinations to rendering order. Supports [variable] templates for wildcards. 
     *
     * Example: "Plains+Hill+Forest" -> ["Plains+Hill","ForestPHUp","ForestPHLow"]
     * Template example: "Plains+Hill+Forest+[resource]" -> ["Plains+Hill","ForestPHUp","[resource]","ForestPHLow"]
     * */
    var ruleVariants: HashMap<String, Array<String>> = HashMap()

    // Template support for ruleVariants
    private data class TemplateRule(val patternParts: List<String>, val outputTemplate: Array<String>, val wildcardCount: Int)

    /** Parsed template rules from ruleVariants entries containing [variable] patterns, sorted by specificity. */
    @Transient private var templateRuleVariants: List<TemplateRule> = emptyList()
    /** Cache of resolved template matches to avoid repeated pattern matching. 
     * These are variants that do not exist directly in the json file, but are derived from a template.
     * Example: "Plains+Hill+Forest+Deer" -> ["Plains+Hill","ForestPHUp","Deer","ForestPHLow"] */
    @Transient private val resolvedVariantsCache: HashMap<String, Array<String>?> = HashMap()

    private fun parseTemplateRuleVariants() {
        templateRuleVariants = ruleVariants
            .filter { it.key.contains('[') }
            .map { (pattern, output) ->
                val parts = pattern.split('+')
                val wildcardCount = parts.count { it.contains('[') }
                TemplateRule(parts, output, wildcardCount)
            }
            .sortedBy { it.wildcardCount } // More specific (fewer wildcards) first
    }

    /**
     * Resolves a rule variant, supporting both exact matches and template patterns.
     * Templates use [name] syntax to match any value in that position.
     * Example: "Plains+Hill+Forest+[resource]" matches "Plains+Hill+Forest+Deer"
     * and returns the output with [resource] replaced by "Deer".
     */
    fun resolveRuleVariant(combination: String): Array<String>? {
        // Exact match in json
        ruleVariants[combination]?.let { return it }

        // Check cache for resolved templates
        resolvedVariantsCache[combination]?.let { return it }

        // Try template matching
        val combinationParts = combination.split('+')

        for (template in templateRuleVariants) {
            // Length must match
            if (template.patternParts.size != combinationParts.size) continue

            // Try to match and capture variables
            val captures = HashMap<String, String>()
            var matches = true

            for (i in template.patternParts.indices) {
                val patternPart = template.patternParts[i]
                val combinationPart = combinationParts[i]

                if (patternPart.startsWith('[') && patternPart.endsWith(']')) {
                    // This is a template variable - capture it
                    val varName = patternPart
                    captures[varName] = combinationPart
                } else if (patternPart != combinationPart) {
                    // Literal part doesn't match
                    matches = false
                    break
                }
            }

            if (matches) {
                // Substitute captured values into output
                val result = template.outputTemplate.map { outputPart ->
                    var substituted = outputPart
                    for ((varName, value) in captures) {
                        substituted = substituted.replace(varName, value)
                    }
                    substituted
                }.toTypedArray()

                // Cache and return
                resolvedVariantsCache[combination] = result
                return result
            }
        }

        resolvedVariantsCache[combination] = null // No match found
        return null
    }

    fun clone(): TileSetConfig {
        val toReturn = TileSetConfig()
        toReturn.useColorAsBaseTerrain = useColorAsBaseTerrain
        toReturn.useSummaryImages = useSummaryImages
        toReturn.unexploredTileColor = unexploredTileColor
        toReturn.fogOfWarColor = fogOfWarColor
        toReturn.fallbackTileSet = fallbackTileSet
        toReturn.tileScale = tileScale
        toReturn.tileScales = tileScales
        toReturn.ruleVariants.putAll(ruleVariants.map { Pair(it.key, it.value.clone()) })
        return toReturn
    }

    fun updateConfig(other: TileSetConfig) {
        useColorAsBaseTerrain = other.useColorAsBaseTerrain
        useSummaryImages = other.useSummaryImages
        unexploredTileColor = other.unexploredTileColor
        fogOfWarColor = other.fogOfWarColor
        fallbackTileSet = other.fallbackTileSet
        tileScale = other.tileScale
        for ((tileString, scale) in other.tileScales) {
            tileScales[tileString] = scale
        }
        for ((tileSetString, renderOrder) in other.ruleVariants) {
            ruleVariants[tileSetString] = renderOrder
        }
        
        parseTemplateRuleVariants()
        resolvedVariantsCache.clear()
    }
}
