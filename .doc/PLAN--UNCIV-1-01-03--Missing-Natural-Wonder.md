# Fix Missing Natural Wonder 'Fountain of Youth' in AI Export (Localized Fix)
*Saved: 2026-03-01*

| Date | 2026-03-01 |
|---|---|

The 'Fountain of Youth' natural wonder is stored as the `baseTerrain` of a tile instead of in the `naturalWonder` field. Because this is an open source project and we want to keep the footprint as small as possible, we will avoid modifying core Engine classes like `Tile.kt`. Instead, we will address the issue directly where it causes the bug: the AI Status Exporter.

## Proposed Changes

### AI Status Exporter

#### [MODIFY] [AiStatusExporter.kt](file:///c:/Git/Unciv/core/src/com/unciv/logic/civilization/AiStatusExporter.kt)
We will add an import for `TerrainType` and update the natural wonder check logic natively inline in `AiStatusExporter.kt` to also scan `tile.allTerrains`.

```kotlin
// Import to add
import com.unciv.models.ruleset.tile.TerrainType

// ... inside generateAiStatusReport ...

            // Workaround: Some natural wonders (like Fountain of Youth) are stored as 
            // the baseTerrain rather than in the naturalWonder field directly.
            // tile.isNaturalWonder() only checks the naturalWonder field and will miss them.
           val naturalWonderName = tile.naturalWonder 
                ?: tile.allTerrains.firstOrNull { it.type == TerrainType.NaturalWonder }?.name

            if (naturalWonderName != null) {
                tileNotes.add("Natural Wonder: $naturalWonderName")
                priority = minOf(priority, 2)
            }
```

### Verification Plan

### Automated Tests
- **Reproduction Test**: We will add a small test case to `tests/src/com/unciv/testing/logic/civilization/AiStatusExporterTest.kt` to verify that our exporter can detect a wonder even when it acts as the base terrain.

> [!NOTE] 
> This localized approach safely fixes the export issue without modifying core `Tile.kt` logic, adhering to the principle of minimizing impact on the broader codebase. Issues elsewhere (like Wonder Overview UI also missing it) could be handled separately and locally if needed.
