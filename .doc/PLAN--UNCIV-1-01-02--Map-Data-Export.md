🚀 [Map Data Export] - Plan Version [UNCIV-1-01-02] - Phase [02]
| Plan Version | UNCIV-1-01-02 |
| Parent Plan | UNCIV-1-01 |
| App Version | 4.x |
| Date | 2026-03-01 |
| Status | 📋 |

# Map Data Export Implementation (vUNCIV-1-01-02)

*Saved: 2026-03-01*

This plan outlines the addition of map data (Points of Interest and Tactical Radar) to the "Export Status for AI" feature, utilizing a hybrid Markdown/XML format to make the data highly digestible for AI assistants.

## Goal Description

The feature will enhance the existing `AiStatusExporter.kt` to output a structured XML-like format enclosing Markdown lists. This semantic wrapping provides explicit boundaries for the AI. It will also introduce coordinate-based geography:

1. **Points of Interest (POI)**: A list of explicitly known tiles containing cities, units, or key terrain.
2. **Tactical Radar**: A relative radius around the player's cities to give the AI strategic context without needing to rebuild the entire map grid.

## Proposed Changes

---

### Logic Layer

#### [MODIFY] `core/src/com/unciv/logic/civilization/AiStatusExporter.kt`

- Refactor the `generateAiStatusReport(civ: Civilization)` function to enclose the entire output in `<unciv_export>` tags.
- Insert a hard-coded `<system_context>` tag as the very first child of `<unciv_export>`. This must contain a string explaining: "You are an AI advisor for the game Unciv (an open-source Civilization V clone)... Map coordinates are [x,y] on a hex grid. Tactical Radar sections summarize threats within 3 tiles of cities. Use this data to provide tactical, economic, and diplomatic advice."
- Wrap the existing "Global Empire Status" inside `<global_status>` tags.
- Wrap the existing "Diplomatic Situation" inside `<diplomatic_relations>` tags.
- Add a new section wrapped in `<map_data>` tags. Inside it, implement two sub-sections:
  
  **1. Points of Interest (`<points_of_interest>`)**
  - Iterate through tiles currently visible to the civilization (`civ.viewableTiles`).
  - To keep the export consistent with "what the player can see right now", only export data from these live viewable tiles:
    - For Units (`tile.militaryUnit != null` or `tile.civilianUnit != null`), emit coordinate `[x,y]`, Unit Name, and Owner.
    - For Cities (`tile.city != null`) and Important Terrain (e.g., Natural Wonders, active Resources), emit coordinate `[x,y]`, Name, Owner, and Population (for cities).
  - *Note:* It is acceptable for tiles near your cities to appear in both POI and Tactical Radar to help the AI cross-reference.
  - *Safety constraint:* if the POI list exceeds 50 entries, emit a warning note `(Output truncated... +N more tiles)` to prevent context overflow.

  **2. Tactical Radar (`<tactical_radar>`)**
  - Iterate through the player's cities (`civ.cities`).
  - For each city, generate a `<city name="${city.name}">` block.
  - Scan `city.getCenterTile().getTilesInDistance(3)` directly (bypassing `viewableTiles` filtering, since a player's city vision overlaps this anyway).
  - Group significant findings (Enemy units, Mountains, Coastlines, Resources) by compound compass direction (e.g., NE, NW, SE, SW, E, W) relative to the city on the hex grid. Distance can be calculated using tile distance functions.
  - Output these findings as a Markdown list within the city's XML block.

- Preserve the previous format of "Global Empire Status" inside `<global_status>`, "Diplomatic Situation" inside `<diplomatic_relations>`, "City Reports" inside `<city_reports>`, and "Military & Civilians" inside `<military_units>` to retain explicit macro-summaries.

## Verification Plan

### Automated Tests

- Command to run: `./gradlew test`
- Update the existing `tests/src/com/unciv/testing/logic/civilization/AiStatusExporterTest.kt`.
- Modify the assertions to ensure the new XML tags (`<unciv_export>`, `<map_data>`, `<points_of_interest>`, etc.) are present in the output string.
- Add a test case where a unit is placed exactly 2 tiles East of a City to verify the Tactical Radar correctly identifies it under the "E" bullet point with "Distance: 2".

### Manual Verification

- Start any Unciv game and found a city.
- Open the Empire Overview screen and click "Export Status for AI".
- Paste the clipboard contents into a text editor.
- Verify that the output is wrapped in `<unciv_export>` tags.
- Verify that `<points_of_interest>` correctly lists the coordinates of your units and city.
- Verify that the `<tactical_radar>` contains a `<city>` block for your capital and accurately describes the geography directly adjacent to it.
