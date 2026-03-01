package com.unciv.logic.civilization

import com.unciv.logic.map.HexCoord
import com.unciv.logic.map.HexMath
import com.unciv.models.stats.Stat
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.max

object AiStatusExporter {
    /**
     * Generates a Markdown-formatted report of the empire's current layout, stats, and relations in
     * English. Note: Must be called on GL thread as it accesses Stats that may be updated
     * concurrently.
     */
    fun generateAiStatusReport(civ: Civilization): String {
        if (civ.isSpectator()) {
            return "Spectator mode - no civilization data available."
        }

        val sb = StringBuilder()
        sb.append("<unciv_export>\n")
        sb.append("<system_context>\n")
        sb.append(
                "You are an AI advisor for the game Unciv (an open-source Civilization V clone). "
        )
        sb.append("The following data represents the current state of the player's empire. ")
        sb.append(
                "Map coordinates are [x,y] on a hex grid. Tactical Radar sections summarize threats within 3 tiles of cities. "
        )
        sb.append("Use this data to provide tactical, economic, and diplomatic advice.\n")
        sb.append("</system_context>\n\n")

        sb.append("<global_status>\n")
        sb.append("# AI Status Report for ${civ.civName}\n\n")

        // Global Empire Status
        val turn = civ.gameInfo.turns
        val era = civ.getEra().name
        val happiness = civ.getHappiness()
        val totalGold = civ.gold
        val gpt = civ.stats.statsForNextTurn.gold
        val science = civ.stats.statsForNextTurn.science
        val culture = civ.stats.statsForNextTurn.culture
        val faith = civ.stats.statsForNextTurn.faith
        val currentResearch = civ.tech.currentTechnologyName()
        val researchStatus =
                if (currentResearch != null) {
                    val remainingCost = civ.tech.remainingScienceToTech(currentResearch).toDouble()
                    val turnsStr =
                            if (remainingCost <= 0f) "0"
                            else if (civ.stats.statsForNextTurn.science <= 0f) "∞"
                            else
                                    max(
                                                    1,
                                                    ceil(
                                                                    remainingCost /
                                                                            civ.stats
                                                                                    .statsForNextTurn
                                                                                    .science
                                                            )
                                                            .toInt()
                                            )
                                            .toString()
                    "$currentResearch ($turnsStr turns)"
                } else {
                    val availableTechs =
                            civ.gameInfo
                                    .ruleset
                                    .technologies
                                    .values
                                    .filter { civ.tech.canBeResearched(it.name) }
                                    .sortedBy { it.column?.columnNumber ?: 0 }
                                    .map { it.name }
                    if (availableTechs.isEmpty()) {
                        "None (All technologies researched)"
                    } else {
                        val availableStr = availableTechs.joinToString(", ")
                        "None (Available: $availableStr)"
                    }
                }

        val goldenAgeTurns = civ.goldenAges.turnsLeftForCurrentGoldenAge
        val goldenAgeProgress = "${civ.goldenAges.storedHappiness}/${civ.goldenAges.happinessRequiredForNextGoldenAge()}"
        val goldenAgeStr = if (civ.goldenAges.isGoldenAge()) "Active ($goldenAgeTurns turns remaining)" else "Inactive (Progress: $goldenAgeProgress)"

        sb.append("## Global Empire Status\n")
        sb.append("- **Turn:** $turn\n")
        sb.append("- **Era:** $era\n")
        sb.append("- **Global Happiness:** $happiness\n")
        sb.append("- **Golden Age:** $goldenAgeStr\n")
        sb.append("- **Total Gold:** $totalGold (GPT: $gpt)\n")
        sb.append("- **Science:** $science\n")
        sb.append("- **Culture:** $culture\n")
        sb.append("- **Faith:** $faith\n")
        sb.append("- **Current Research:** $researchStatus\n\n")

        // Active Social Policies
        sb.append("### Active Social Policies\n")
        var hasPolicies = false
        for (branch in civ.gameInfo.ruleset.policyBranches.values) {
            val adoptedInBranch = branch.policies.count { civ.policies.isAdopted(it.name) }
            val isBranchUnlocked = civ.policies.isAdopted(branch.name)
            if (isBranchUnlocked || adoptedInBranch > 0) {
                sb.append("- **${branch.name}:** Unlocked, $adoptedInBranch policies adopted\n")
                hasPolicies = true
            }
        }
        if (!hasPolicies) sb.append("- None\n")
        sb.append("</global_status>\n\n")

        // Religion & Beliefs
        val religion = civ.religionManager.religion
        val majorityReligion = civ.religionManager.getMajorityReligion()
        
        if (religion != null || majorityReligion != null) {
            sb.append("<religion_data>\n")
            sb.append("## Religion & Beliefs\n")
            if (religion != null) {
                val beliefsStr = religion.getAllBeliefsOrdered().map { it.name }.joinToString(", ")
                val type = if (religion.isPantheon()) "Pantheon" else "Religion"
                val beliefsFormatted = if (beliefsStr.isNotEmpty()) " ($beliefsStr)" else ""
                sb.append("- **Founded $type:** ${religion.getReligionDisplayName()}$beliefsFormatted\n")
            }
            if (majorityReligion != null && majorityReligion.name != religion?.name) {
                sb.append("- **Majority Religion:** ${majorityReligion.getReligionDisplayName()}\n")
            }
            sb.append("</religion_data>\n\n")
        }

        // Map Data
        sb.append("<map_data>\n")
        sb.append("<points_of_interest>\n")
        val poiList = mutableListOf<Pair<Int, String>>()
        for (tile in civ.viewableTiles) {
            val tileNotes = mutableListOf<String>()
            var priority = 5 // lower is higher priority

            val city = if (tile.isCityCenter()) tile.getCity() else null
            if (city != null) {
                tileNotes.add(
                        "City: ${city.name} (${city.civ.civName}, Pop ${city.population.population})"
                )
                priority = minOf(priority, if (city.civ != civ) 0 else 4)
            }

            val milUnit = tile.militaryUnit
            if (milUnit != null) {
                tileNotes.add("Military Unit: ${milUnit.name} (${milUnit.civ.civName})")
                priority = minOf(priority, if (milUnit.civ != civ) 1 else 4)
            }

            val civUnit = tile.civilianUnit
            if (civUnit != null) {
                tileNotes.add("Civilian Unit: ${civUnit.name} (${civUnit.civ.civName})")
                priority = minOf(priority, if (civUnit.civ != civ) 2 else 4)
            }

            if (tile.isNaturalWonder()) {
                tileNotes.add("Natural Wonder: ${tile.naturalWonder}")
                priority = minOf(priority, 2)
            }

            val resource = tile.tileResource
            if (resource != null && civ.canSeeResource(resource)) {
                tileNotes.add("Resource: ${resource.name}")
                priority = minOf(priority, if (resource.resourceType.name == "Bonus") 4 else 3)
            }

            if (tileNotes.isNotEmpty()) {
                poiList.add(
                        Pair(
                                priority,
                                "- `[${tile.position.x},${tile.position.y}]` ${tileNotes.joinToString(" | ")}"
                        )
                )
            }
        }

        poiList.sortBy { it.first }
        var poiCount = 0
        for ((_, text) in poiList) {
            if (poiCount < 50) {
                sb.append("$text\n")
                poiCount++
            } else if (poiCount == 50) {
                sb.append("- *(Output truncated... additional points of interest not shown)*\n")
                poiCount++
            }
        }
        sb.append("</points_of_interest>\n\n")

        sb.append("<tactical_radar>\n")
        if (civ.cities.isEmpty()) {
            sb.append("- No cities founded yet to generate radar.\n")
        } else {
            for (city in civ.cities) {
                sb.append("<city name=\"${city.name}\">\n")
                val cityTile = city.getCenterTile()
                for (tile in cityTile.getTilesInDistance(3)) {
                    if (tile == cityTile) continue

                    var notable = false
                    val radarNotes = mutableListOf<String>()

                    val milUnit = tile.militaryUnit
                    if (milUnit != null && milUnit.civ != civ) {
                        radarNotes.add("Enemy Unit: ${milUnit.name} (${milUnit.civ.civName})")
                        notable = true
                    }

                    if (tile.isImpassible()) {
                        radarNotes.add("Terrain: ${tile.lastTerrain.name}")
                        notable = true
                    }

                    val resource = tile.tileResource
                    if (resource != null && civ.canSeeResource(resource)) {
                        radarNotes.add("Resource: ${resource.name}")
                        notable = true
                    }

                    if (notable) {
                        val dx = tile.position.x - cityTile.position.x
                        val dy = tile.position.y - cityTile.position.y
                        val dist = tile.aerialDistanceTo(cityTile)

                        val worldDiff = HexMath.hex2WorldCoords(HexCoord.of(dx, dy))
                        val deg =
                                Math.toDegrees(
                                        atan2(worldDiff.x.toDouble(), worldDiff.y.toDouble())
                                )
                        val dir =
                                when {
                                    deg >= -30 && deg < 30 -> "N"
                                    deg >= 30 && deg < 90 -> "NE"
                                    deg >= 90 && deg < 150 -> "SE"
                                    deg >= 150 || deg < -150 -> "S"
                                    deg >= -150 && deg < -90 -> "SW"
                                    deg >= -90 && deg < -30 -> "NW"
                                    else -> "Near"
                                }

                        sb.append("- **$dir, Dist $dist**: ${radarNotes.joinToString(" | ")}\n")
                    }
                }
                sb.append("</city>\n")
            }
        }
        sb.append("</tactical_radar>\n")
        sb.append("</map_data>\n\n")

        // City Reports
        sb.append("<city_reports>\n")
        sb.append("## City Reports\n")
        if (civ.cities.isEmpty()) {
            sb.append("- No cities founded yet.\n")
        } else {
            for (city in civ.cities) {
                val pop = city.population.population
                val prod =
                        city.cityConstructions.currentConstructionName().takeIf { it.isNotEmpty() }
                                ?: "Nothing"
                val cityFood = city.cityStats.currentCityStats[Stat.Food]
                val cityProd = city.cityStats.currentCityStats[Stat.Production]
                val citySci = city.cityStats.currentCityStats[Stat.Science]
                sb.append(
                        "- **${city.name}:** Pop $pop | Building: $prod | Yields: $cityFood Food, $cityProd Prod, $citySci Science\n"
                )
            }
        }
        sb.append("</city_reports>\n\n")

        // Military & Civilians
        sb.append("<military_units>\n")
        sb.append("## Military & Civilians\n")
        if (civ.units.getCivUnitsSize() == 0) {
            sb.append("- No units.\n")
        } else {
            val unitCounts = HashMap<String, Int>()
            for (unit in civ.units.getCivUnits()) {
                val name = unit.name
                unitCounts[name] = (unitCounts[name] ?: 0) + 1
            }
            for ((name, count) in unitCounts) {
                sb.append("- $count $name\n")
            }
        }
        sb.append("</military_units>\n\n")

        // Diplomatic Situation
        sb.append("<diplomatic_relations>\n")
        sb.append("## Diplomatic Situation\n")
        var hasDiplomacy = false
        for (otherCiv in civ.getKnownCivs()) {
            if (otherCiv.isBarbarian) continue
            hasDiplomacy = true
            val type = if (otherCiv.isCityState) "City-State" else "Major"
            val status = if (civ.isAtWarWith(otherCiv)) "At War" else "Peace"
            sb.append("- **${otherCiv.civName}** ($type): $status\n")
        }
        if (!hasDiplomacy) sb.append("- No known civilizations.\n")
        sb.append("</diplomatic_relations>\n\n")

        sb.append("</unciv_export>")

        return sb.toString()
    }
}
