🚀 [AI Status Export: Research Details] - Plan Version [UNCIV-1-01-02] - Phase [01]
| Plan Version | UNCIV-1-01-02 |
| Parent Plan | UNCIV-1-01-01 |
| App Version | 4.x |
| Date | 2026-03-01 |
| Status | ✅ |

# AI Status Export: Research Details (vUNCIV-1-01-02)

*Saved: 2026-03-01*

This plan covers adding current research, estimated turns to completion, or available research options if none are selected, to the AI Status Export report.

## Proposed Changes

### [core]

#### [MODIFY] [AiStatusExporter.kt](file:///c:/Git/Unciv/core/src/com/unciv/logic/civilization/AiStatusExporter.kt)

- Update the `Global Empire Status` section to include:
  - If researching: `Tech Name (X turns)`
  - If not researching: `None (Available: Tech1, Tech2, Tech3...)`

## Verification Plan

### Automated Tests

- New test case `testResearchExport` in `AiStatusExporterTest.kt` to verify:
  - If researching: `Tech Name (X turns)`
  - If not: `None (Available: Tech1, Tech2, Tech3...)`

### Manual Verification

- N/A (UI trigger already exists from previous tasks).
