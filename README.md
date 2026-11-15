# Trusted Component Registry (TCR) · `aidos-tcr`

**Status:** Canonical Ecosystem Standard  
**Authority:** Aidos Architecture Council (Decision D46, RFC-0051, RFC-0103)  
**Artifact:** `dev.aidos.tcr:aidos-tcr:0.1.0`  
**Consuming Projects:** `aidos`, `dictator`, `agentic-messenger`

---

## 1. Vision & Purpose

`aidos-tcr` is the **single source of truth for all shared Compose Multiplatform UI components** across the Aidos ecosystem.

The ultimate target for Aidos is **Desktop Generative UI** (RFC-0051, RFC-0106). In a generative UI paradigm, autonomous AI agents and spatial layouts orchestrate user interfaces dynamically. **Generative UI must not generate raw, unstandardized controls.** Instead, agents and applications assemble, configure, and bind human-tested, secure components from the **Trusted Component Registry (TCR)**.

### Cardinal Rules of UI Work:
1. **Never duplicate shared components in consumer apps**:
   - Any component shared across apps (`aidos`, `dictator`, `agentic-messenger`) lives in `aidos-tcr`.
   - Consumer apps are **pure consumers** (`dev.aidos.tcr.*`).
2. **All shared code in `commonMain`**:
   - Models, presenters, state holders, and composables consumed by Android and Desktop must live in `commonMain`.
   - Never put shared Android-facing code in `jvmMain`.
3. **Use Canonical TCR Tokens**:
   - All colors, shapes, typography, and cadence springs come from `dev.aidos.tcr.theme.*`.

For the complete architectural guide, see [UI_MANUAL.md](UI_MANUAL.md) or Aidos's [docs/TCR_UI_DEVELOPMENT_MANUAL.md](file:///C:/Users/jsilv/Code/aidos/docs/TCR_UI_DEVELOPMENT_MANUAL.md).

---

## 2. Package Map

| Domain / Package | Contents | Consumer Roles |
|---|---|---|
| `dev.aidos.tcr.theme` | `TcrTheme`, `TcrColors`, `ProvenanceColors`, `TcrShapes`, `TcrTypography` | Theme wrapper & canonical color tokens for all apps |
| `dev.aidos.tcr.actor`<br>`dev.aidos.tcr.pill` | `PersistentRedPill`, `FloatingActorPill`, `RadialCountdownRing`, `HappeningsHUD`, `CadenceEngine`, `PillConstellationOverlay` | Caret presence, dictation HUD, action staging (Dictator, Amp, Aidos) |
| `dev.aidos.tcr.document` | `DocumentChatFlow`, `DocumentInputSection` | Collaborative text editing & document generation (Dictator, Aidos) |
| `dev.aidos.tcr.agent` | `AgentsPane`, `AgentRunCard`, `AgentPlacementBadge`, `AgentEgressSummary`, `AgentBudgetMeter`, `AgentTaskGraphView`, `WorkerOutcomeCard` | Agent telemetry, placement chips, egress audit, task hierarchy |
| `dev.aidos.tcr.engine` | `ConnectedAppsPane`, `ModelsPanes`, `ModelDetailContent`, `SettingsContent`, `StatusPane`, `EngineModels` | Aidos Engine control, model catalog, connected app approvals |
| `dev.aidos.tcr.mcp` | `McpServersPane`, `McpModels` | MCP server discovery, catalog inspect, tool adoption |
| `dev.aidos.tcr.mesh` | `ConversationInputBar`, `MessageBubble`, `ContactRow`, `PrivacySealBadge`, `ApprovalCard`, `WaveformVisualizer` | 4-party messaging & E2EE provenance (Agentic Messenger, Aidos) |
| `dev.aidos.tcr.generative` | `GenerativeUiRegistry` | Dynamic component resolution & spatial canvas binding |

---

## 3. Design Tokens (TcrColors)

```kotlin
object TcrColors {
    val EmberRed      = Color(0xFFEF4444) // Voice dictation, Red Pill, immediate cancel actions
    val ElectricBlue  = Color(0xFF3B82F6) // Deterministic text editing, selection shelf
    val EmeraldGreen  = Color(0xFF10B981) // Generative AI streaming, local model placement
    val AmberWarning  = Color(0xFFF59E0B) // Progressive disclosure, pending human approvals
    val VioletProtocol = Color(0xFF8B5CF6) // Cryptographic assertions, remote models, protocol stamps
}
```

---

## 4. Manifest & Versioned Contracts

[`manifest/tcr-manifest.json`](manifest/tcr-manifest.json) is the machine-readable registry: every public
composable, public function, `data class` and `enum` in `commonMain`, with its domain, `status`
(`EXPERIMENTAL` / `STABLE` / `DEPRECATED`), a **semver `contract`** version and its normalised
**signature** (`name: Type`, or `name: Type =` when the parameter has a default). It is derived from
the source, never hand-written, and `ManifestSyncTest` (part of `jvmTest`, so part of CI) fails when
source and manifest disagree.

When you add, change or remove a component, regenerate and commit the diff:

```bash
./gradlew jvmTest --tests '*ManifestSyncTest*' -Dtcr.updateManifest=true
```

The tool bumps each changed component's `contract` by what the change does to existing callers:

| Change | Bump (≥ 1.0) | Bump (0.x) |
|---|---|---|
| New optional parameter appended after all existing ones; new enum entry | minor | patch |
| Removed / renamed / retyped / reordered parameter, new *required* parameter, parameter lost its default, removed enum entry | major | minor |

`status` is the one field you edit by hand (promote to `STABLE` at contract `1.0.0`; `STABLE` needs a
KDoc summary). Consumers can read the manifest to find what exists before writing UI, and the
generative layer (RFC-0051) can validate against it. The scanner understands top-level declarations
and members of top-level `object`s; overloads get `#2`, `#3` suffixes.

## 5. Verification

Before pushing changes to `aidos-tcr`:
```bash
./gradlew jvmTest
```
`jvmTest` runs the unit tests (`commonTest`), the manifest/contract checks and the headless Compose UI
tests (`jvmTest/.../ui`, `runComposeUiTest`), and is exactly what CI runs.

Verify composite consumer builds:
```bash
# In C:\Users\jsilv\Code\aidos:
./gradlew :engine:engineui:assemble
./gradlew :engine:desktopapp:assemble
./gradlew :engine:androidapp:assembleDebug
```
