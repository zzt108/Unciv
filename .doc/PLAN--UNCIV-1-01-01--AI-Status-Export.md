🚀 [AI Status Export] - Plan Version [UNCIV-1-01] - Phase [01]
| Plan Version | UNCIV-1-01-01 |
| Parent Plan | UNCIV-1-01 |
| App Version | 4.x |
| Date | 2026-03-01 |
| Status | 📋 |

# AI Status Export Implementation (vUNCIV-1-01-01)

*Saved: 2026-03-01*

This plan outlines the addition of an "Export Status for AI" feature to Unciv, enabling players to easily retrieve and share their game state with AI assistants (like Gemini).

## Goal Description

The feature will generate a Markdown-formatted report of the empire's current layout, stats, and relations in English, saving it directly to the system clipboard to be easily pasted into an AI chat as requested by the AI in the provided `Gemini-UnCiv export for AI.md` doc.

## Proposed Changes

---

### Logic Layer

#### [NEW] `core/src/com/unciv/logic/civilization/AiStatusExporter.kt`

- Create an `object AiStatusExporter` with a function `fun generateAiStatusReport(civ: Civilization): String`. This function must have a KDoc `// Must be called on GL thread`.
- If `civ.isSpectator()` is true, the function should return `"Spectator mode - no civilization data available."`.
- The function will construct the AI requested Markdown string:
  - **Global Empire Status:** Current Turn, Era, Global Happiness, Total Gold, GPT, Science, Culture, Faith, Current Research, and Active Social Policies (iterate `civ.gameInfo.ruleset.policyBranches`, filter by `civ.policies.isAdopted(branch.name)`, emit branch name + count).
  - **City Reports:** Names, Population, Current Production, Yields (specifically accessing `city.cityStats.currentCityStats[Stat.Food]`, `[Stat.Production]`, `[Stat.Science]`).
  - **Military & Civilians:** Aggregated count of unit types (e.g. `4 Crossbowmen, 2 Knights`).
  - **Diplomatic Situation:** Known Civilizations, War/Peace Status, and City-State Relations.

---

### UI Layer

### [MODIFY] `core/src/com/unciv/ui/screens/overviewscreen/EmpireOverviewScreen.kt`

- In `EmpireOverviewScreen.kt`, the existing header decoration is just the `closeButton`. We need to wrap both the export and close button in a row:
  - Create a new `Table()`.
  - Add the new "Export Status for AI" `.toTextButton()`.
  - Add the existing `closeButton`.
  - Pass the entire `Table` to `tabbedPager.decorateHeader()`.
- On click of the new export button, call `AiStatusExporter.generateAiStatusReport(viewingPlayer)`.
- Write the result to the clipboard using `Gdx.app.clipboard.contents = reportText`.
- Show a confirmation toast using `ToastPopup("Status exported to clipboard for AI!", this)`.

## Verification Plan

### Automated Tests

- Command to run: `./gradlew test`
- Create `tests/src/com/unciv/testing/logic/civilization/AiStatusExporterTest.kt` using JUnit4.
- Use the existing test bootstrap pattern (`RulesetCache.loadRulesets()`) as seen in `BasicTests.kt`.
- Test initialization of a basic `GameInfo`, `Civilization`, and `City`, calling `AiStatusExporter.generateAiStatusReport(civ)`.
- Assert the returned string contains the expected Markdown sections and accurately formats the unit aggregations and yields.

### Manual Verification

- Start any Unciv game.
- Open the Empire Overview screen.
- Click the "Export Status for AI" button in the upper right.
- Verify the "Status exported to clipboard for AI!" toast appears.
- Paste the clipboard contents into a text editor and ensure it matches the AI's requested overview.
