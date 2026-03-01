package com.unciv.logic.civilization

import com.unciv.models.stats.Stat

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
        val currentResearch = civ.tech.currentTechnology() ?: "None"

        sb.append("## Global Empire Status\n")
        sb.append("- **Turn:** $turn\n")
        sb.append("- **Era:** $era\n")
        sb.append("- **Global Happiness:** $happiness\n")
        sb.append("- **Total Gold:** $totalGold (GPT: $gpt)\n")
        sb.append("- **Science:** $science\n")
        sb.append("- **Culture:** $culture\n")
        sb.append("- **Faith:** $faith\n")
        sb.append("- **Current Research:** $currentResearch\n\n")

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
        sb.append("\n")

        // City Reports
        sb.append("## City Reports\n")
        if (civ.cities.isEmpty()) {
            sb.append("- No cities founded yet.\n")
        } else {
            for (city in civ.cities) {
                val pop = city.population.population
                val prod =
                        city.cityConstructions.currentConstructionName().takeIf { it.isNotEmpty() }
                                ?: "Nothing"
                val cityFood = city.cityStats.currentCityStats[Stat.Food] ?: 0f
                val cityProd = city.cityStats.currentCityStats[Stat.Production] ?: 0f
                val citySci = city.cityStats.currentCityStats[Stat.Science] ?: 0f
                sb.append(
                        "- **${city.name}:** Pop $pop | Building: $prod | Yields: $cityFood Food, $cityProd Prod, $citySci Science\n"
                )
            }
        }
        sb.append("\n")

        // Military & Civilians
        sb.append("## Military & Civilians\n")
        if (civ.units.getCivUnitsSize() == 0) {
            sb.append("- No units.\n")
        } else {
            val unitCounts = HashMap<String, Int>()
            for (unit in civ.units.getCivUnits()) {
                val name = unit.type.name
                unitCounts[name] = (unitCounts[name] ?: 0) + 1
            }
            for ((name, count) in unitCounts) {
                sb.append("- $count $name\n")
            }
        }
        sb.append("\n")

        // Diplomatic Situation
        sb.append("## Diplomatic Situation\n")
        var hasDiplomacy = false
        for (otherCiv in civ.getKnownCivs()) {
            if (otherCiv.isBarbarian) continue
            hasDiplomacy = true
            val status = if (civ.isAtWarWith(otherCiv)) "At War" else "Peace"
            sb.append("- **${otherCiv.civName}:** $status\n")
        }
        if (!hasDiplomacy) sb.append("- No known civilizations.\n")

        return sb.toString()
    }
}
