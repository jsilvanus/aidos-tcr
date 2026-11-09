package dev.aidos.tcr.engine

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

/**
 * Model kind representation for local and cloud inference.
 */
@Serializable
enum class ModelKind {
    LLM,
    EMBEDDING,
    STT,
    TTS,
    VISION,
    OCR,
    RERANKER,
    TRANSLATION,
    DECISION;

    fun engineEndpoint(): String? = when (this) {
        LLM -> "/v1/chat/completions"
        EMBEDDING -> "/v1/embeddings"
        STT -> "/v1/audio/transcriptions"
        DECISION -> "/v1/systemone"
        else -> null
    }

    fun displayName(): String = when (this) {
        LLM -> "Chat / LLM"
        EMBEDDING -> "Embedding"
        STT -> "Speech to Text"
        TTS -> "Text to Speech"
        VISION -> "Vision"
        OCR -> "OCR"
        RERANKER -> "Reranker"
        TRANSLATION -> "Translation"
        DECISION -> "System One Decisions"
    }
}

/**
 * The test a model card offers, chosen by what the model is (RFC-0103, RFC-0022).
 */
enum class ModelTestKind(
    val buttonLabel: String,
    val inputLabel: String,
    val defaultText: String,
    val needsFile: Boolean = false,
    val fileLabel: String = "",
) {
    CHAT("Test Chat", "Prompt", "Say hello in one short sentence."),
    DECISION("Test Decisions", "Text to classify", "poista edellinen lause"),
    TRANSCRIPTION("Test Transcription", "Language (optional, e.g. fi)", "", needsFile = true, fileLabel = "Path to a WAV file"),
    VISION("Test Vision", "Question about the picture", "What is in this picture?", needsFile = true, fileLabel = "Path to an image file"),
}

fun ModelKind.selfTestKind(): ModelTestKind? = when (this) {
    ModelKind.LLM -> ModelTestKind.CHAT
    ModelKind.DECISION -> ModelTestKind.DECISION
    ModelKind.STT -> ModelTestKind.TRANSCRIPTION
    ModelKind.VISION -> ModelTestKind.VISION
    else -> null
}

fun ModelTestKind.defaultTextFor(modelId: String?): String = defaultText


/**
 * Fit verdict indicating whether a model fits in device RAM / context window.
 */
@Serializable
enum class ModelFitVerdict {
    RUNS_WELL,
    RUNS_TIGHT,
    EXCEEDS_CONTEXT,
    WILL_NOT_FIT;

    val label: String get() = when (this) {
        RUNS_WELL -> "Perfect"
        RUNS_TIGHT -> "Tight"
        EXCEEDS_CONTEXT -> "Exceeds CTX"
        WILL_NOT_FIT -> "Won't Fit"
    }
}

/**
 * Returns the theme status color for a given model fit verdict.
 */
fun statusColorForVerdict(verdict: ModelFitVerdict): Color = when (verdict) {
    ModelFitVerdict.RUNS_WELL -> Color(0xFF22C55E)      // Green
    ModelFitVerdict.RUNS_TIGHT -> Color(0xFFEAB308)     // Amber
    ModelFitVerdict.EXCEEDS_CONTEXT -> Color(0xFFF97316)// Orange
    ModelFitVerdict.WILL_NOT_FIT -> Color(0xFFEF4444)   // Red
}

/**
 * Lifecycle state of an Engine host.
 */
@Serializable
enum class EngineState {
    STARTING,
    READY,
    FAILED,
}

/**
 * Model resident in memory.
 */
@Serializable
data class ResidentModel(
    val id: String,
    val displayName: String,
    val quantization: String,
    val loadedAgoMs: Long,
)

/**
 * Device RAM budget in megabytes.
 */
@Serializable
data class MemoryBudget(
    val usedMB: Int,
    val totalMB: Int,
) {
    val percentUsed: Float = if (totalMB > 0) usedMB / totalMB.toFloat() else 0f
}

/**
 * Real-time download progress for a model artifact.
 */
@Serializable
data class DownloadProgress(
    val modelId: String,
    val modelName: String,
    val progressPercent: Int,
    val speedMBps: Float? = null,
    val etaSeconds: Int? = null,
)

/**
 * Model candidate in the cookbook or Hugging Face search results.
 */
@Serializable
data class CookbookModel(
    val id: String,
    val name: String,
    val kind: String,
    val quantization: String,
    val sizeBytes: Long?,
    val contextLength: Int,
    val fitVerdict: ModelFitVerdict,
    val isRunnable: Boolean = true,
    val tokensPerSecond: Float? = null,
    val estimatedVramMB: Int? = null,
)

/**
 * Client-side filters for model searching.
 */
@Serializable
data class ModelSearchFilters(
    val minSizeMb: Int? = null,
    val maxSizeMb: Int? = null,
    val fitsDeviceOnly: Boolean = false,
    val runnableOnly: Boolean = false,
) {
    val isActive: Boolean get() = minSizeMb != null || maxSizeMb != null || fitsDeviceOnly || runnableOnly

    fun accepts(model: CookbookModel): Boolean {
        if (minSizeMb != null || maxSizeMb != null) {
            val size = model.sizeBytes
            if (size == null || size <= 0) return false
            if (minSizeMb != null && size < minSizeMb * 1024L * 1024L) return false
            if (maxSizeMb != null && size > maxSizeMb * 1024L * 1024L) return false
        }
        if (fitsDeviceOnly && model.fitVerdict == ModelFitVerdict.WILL_NOT_FIT) return false
        if (runnableOnly && !model.isRunnable) return false
        return true
    }
}

/**
 * Suggested curated model.
 */
@Serializable
data class SuggestedModel(
    val id: String,
    val name: String,
    val description: String,
    val repoId: String,
    val filename: String,
    val format: String,
    val backend: String,
    val approxSizeBytes: Long,
    val runnable: Boolean,
) {
    val downloadUrl: String get() = "https://huggingface.co/$repoId/resolve/main/$filename"
}

/**
 * Suggested model with its live installation state.
 */
@Serializable
data class SuggestionUi(
    val model: SuggestedModel,
    val isInstalled: Boolean,
    val isInstalling: Boolean,
    val progressPercent: Int,
    val error: String?,
)

/**
 * Row in the context fit matrix.
 */
@Serializable
data class ContextFitRow(
    val contextLength: Int,
    val verdict: ModelFitVerdict,
    val estimatedMemoryMB: Int,
    val baselineVerdict: ModelFitVerdict? = null,
)

/**
 * Full detailed specification and diagnostics for an inspected model.
 */
@Serializable
data class ModelDetail(
    val id: String,
    val name: String,
    val description: String = "",
    val providerName: String,
    val kind: ModelKind = ModelKind.LLM,
    val licenseName: String? = null,
    val modelUrl: String? = null,
    val sizeMB: Int,
    val contextFitTable: List<ContextFitRow> = emptyList(),
    val isRunnable: Boolean = true,
    val format: String = "",
    val deviceTotalRamMB: Int = 0,
    val deviceFreeRamMB: Int = 0,
    val baselineFreeRamMB: Int? = null,
)

/**
 * Runtime execution settings for a loaded model.
 */
@Serializable
data class RunSettingsUi(
    val contextLength: Int? = null,
    val threads: Int? = null,
    val batchSize: Int? = null,
    val gpuLayers: Int? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
) {
    val isDefault: Boolean get() = this == RunSettingsUi()
}

/**
 * Self-test diagnostic outcome for a loaded model.
 */
@Serializable
data class SelfTestOutcome(
    val passed: Boolean,
    val summary: String,
    val lines: List<String> = emptyList(),
    val durationMs: Long? = null,
    val generatedTokens: Int? = null,
) {
    val title: String get() = summary
    val tokensPerSecond: Float?
        get() = if (durationMs != null && durationMs > 0 && generatedTokens != null && generatedTokens > 0) {
            (generatedTokens.toFloat() / durationMs) * 1000f
        } else null
}

/**
 * Active model loading progress status.
 */
enum class ModelLoadingStatus {
    NOT_LOADED,
    LOADING,
    LOADED,
    ERROR,
    UNLOADING,
}

@Serializable
data class ModelLoadingState(
    val modelId: String = "",
    val status: ModelLoadingStatus = ModelLoadingStatus.NOT_LOADED,
    val loadProgress: Int = 0,
    val estimatedMemoryMB: Int = 0,
    val error: String? = null,
    val loadTimeMs: Long? = null,
)

/**
 * Complete UI state for the Model Detail view.
 */
@Serializable
data class ModelDetailState(
    val model: ModelDetail? = null,
    val isInstalled: Boolean = false,
    val installedPath: String? = null,
    val installedSizeBytes: Long? = null,
    val installedDigest: String? = null,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
    val downloadError: String? = null,
    val embeddingInput: String = "",
    val embeddingVector: List<Float> = emptyList(),
    val isEmbeddingTesting: Boolean = false,
    val embeddingError: String? = null,
    val selfTestText: String = "",
    val selfTestFile: String = "",
    val isSelfTesting: Boolean = false,
    val selfTestOutcome: SelfTestOutcome? = null,
    val runSettings: RunSettingsUi = RunSettingsUi(),
    val loadedRunSettings: RunSettingsUi? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

/**
 * User actions supported in the Model Detail view.
 */
class ModelDetailActions(
    val onPreferredContextChange: (Int) -> Unit = {},
    val onOpenUrl: (String) -> Unit = {},
    val onStartDownload: () -> Unit = {},
    val onCancelDownload: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onEmbeddingInputChange: (String) -> Unit = {},
    val onTestEmbeddings: () -> Unit = {},
    val onToggleLoad: () -> Unit = {},
    val onTestChat: (() -> Unit)? = null,
    val onRunSelfTest: (() -> Unit)? = null,
    val onSelfTestTextChange: (String) -> Unit = {},
    val onSelfTestFileChange: (String) -> Unit = {},
    val onRunSettingsChange: ((RunSettingsUi) -> Unit)? = null,
    val onReload: (() -> Unit)? = null,
)

/**
 * Provider summary card representation.
 */
@Serializable
data class ProviderSummary(
    val id: String,
    val name: String,
    val statusText: String,
    val isEnabled: Boolean,
    val isConfigured: Boolean,
    val modelCount: Int,
)

/**
 * State for the Models screen tabs.
 */
@Serializable
data class ModelsPaneState(
    val engineState: EngineState = EngineState.STARTING,
    val localModels: List<CookbookModel> = emptyList(),
    val isLoading: Boolean = false,
    val cookbookModels: List<CookbookModel> = emptyList(),
    val isSearching: Boolean = false,
    val suggestions: List<SuggestionUi> = emptyList(),
    val providers: List<ProviderSummary> = emptyList(),
)

/**
 * User actions for the Models screen tabs.
 */
class ModelsPaneActions(
    val onModelSelected: (String) -> Unit = {},
    val onDeleteModel: (String) -> Unit = {},
    val onSearch: (query: String, kind: ModelKind?, minContext: Int?, filters: ModelSearchFilters) -> Unit = { _, _, _, _ -> },
    val onInstallSuggestion: (String) -> Unit = {},
    val onDismissSuggestion: (String) -> Unit = {},
    val onProviderSelected: (String) -> Unit = {},
)

/**
 * Status pane summary telemetry.
 */
@Serializable
data class StatusUiState(
    val isEngineRunning: Boolean = false,
    val engineStateName: String = "OFF",
    val residentModels: List<ResidentModel> = emptyList(),
    val memory: MemoryBudget = MemoryBudget(0, 0),
    val freeStorageGb: Int? = null,
    val approvedApps: List<String> = emptyList(),
    val pendingAppCount: Int = 0,
    val linkedPeerCount: Int = 0,
)

/**
 * Connected app handshake approval status.
 */
@Serializable
enum class AppApprovalStatus {
    APPROVED,
    DENIED,
    PENDING
}

/**
 * Connected app approval record presentation model.
 */
@Serializable
data class AppApprovalRecordUi(
    val packageName: String,
    val displayName: String,
    val status: AppApprovalStatus,
    val decidedAt: String? = null,
    val firstSeenAt: String = "",
    val lastSeenAt: String = "",
    val attemptCount: Int = 1,
    val requestCount: Int = 0,
    val sdkVersion: String? = null,
    val apiVersion: Int? = null,
)

/**
 * Settings configuration state.
 */
@Serializable
data class HfTokenStatus(
    val isConfigured: Boolean,
    val lastValidatedMs: Long? = null,
)

@Serializable
data class RamBaselineState(
    val baselineUsedMB: Int? = null,
    val currentUsedMB: Int = 0,
    val totalMB: Int = 0,
) {
    val baselineFreeMB: Int? get() = baselineUsedMB?.let { (totalMB - it).coerceAtLeast(0) }
}

@Serializable
data class SettingsState(
    val hfTokenStatus: HfTokenStatus = HfTokenStatus(false),
    val showTokenInput: Boolean = false,
    val tokenInput: String = "",
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
)

/**
 * Test chat message and state.
 */
@Serializable
data class UiChatMessage(
    val id: String = "",
    val role: String,
    val content: String,
    val tokensUsed: Int? = null,
    val generationTimeMs: Long? = null,
    val stopReason: String? = null,
    val route: String? = null,
)

@Serializable
data class TestChatState(
    val modelId: String = "",
    val modelName: String = "",
    val messages: List<UiChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalTokensUsed: Int = 0,
    val averageTokensPerSecond: Float = 0f,
)

/**
 * Human-readable artifact size formatter.
 */
fun formatSize(bytes: Long?): String = when {
    bytes == null || bytes <= 0 -> "size unknown"
    bytes < 1024L * 1024 -> "${(bytes + 1023) / 1024} KB"
    bytes < 1024L * 1024 * 1024 -> {
        val mb = bytes / (1024.0 * 1024)
        val tenths = kotlin.math.round(mb * 10).toLong()
        if (tenths % 10 == 0L) "${tenths / 10} MB" else "${tenths / 10}.${tenths % 10} MB"
    }
    else -> {
        val gb = bytes / (1024.0 * 1024 * 1024)
        val tenths = kotlin.math.round(gb * 10).toLong()
        if (tenths % 10 == 0L) "${tenths / 10} GB" else "${tenths / 10}.${tenths % 10} GB"
    }
}

/**
 * Elapsed duration formatter.
 */
fun formatElapsed(elapsedMs: Long): String = when {
    elapsedMs < 1_000 -> "just now"
    elapsedMs < 60_000 -> "${elapsedMs / 1_000}s ago"
    elapsedMs < 3_600_000 -> "${elapsedMs / 60_000}m ago"
    elapsedMs < 86_400_000 -> "${elapsedMs / 3_600_000}h ago"
    else -> "${elapsedMs / 86_400_000}d ago"
}

/**
 * Actionable error message translator.
 */
fun formatActionableErrorMessage(error: String?): String {
    if (error.isNullOrBlank()) return "Inference failed"
    val lower = error.lowercase()
    return when {
        lower.contains("model_unavailable") || lower.contains("no candidate") ->
            "No candidate models available in inference group. Check connected devices in Groups or install a local model."
        lower.contains("peer_busy") ->
            "Group peers are busy with other requests. Try again in a moment."
        lower.contains("plan_changed") || lower.contains("split plan") ->
            "Inference group topology changed (a peer joined, left, or disconnected). Please retry."
        lower.contains("peer_lost") ->
            "A peer disconnected during generation. Retrying on local fallback."
        lower.contains("split_failed") || lower.contains("helper process") || lower.contains("supervisor") ->
            "Split layer execution failed across peers. Check device resources and peer connectivity."
        lower.contains("out of memory") || lower.contains("insufficient_memory") || lower.contains("failed to allocate") ->
            "Out of memory: device RAM is full. Close background apps or choose a smaller context window."
        lower.contains("locality") || lower.contains("locality_violation") ->
            "Placement restricted: model cannot be executed under current locality policy."
        lower.contains("queue_saturated") || lower.contains("saturated") ->
            "Engine is busy with too many queued requests. Try again in a moment."
        lower.contains("timeout") || lower.contains("request_timeout") ->
            "Generation timed out before completion."
        lower.contains("busy") || lower.contains("model_busy") ->
            "Model is busy processing another request. Please retry shortly."
        lower.contains("integrity") || lower.contains("corrupt") || lower.contains("checksum") ->
            "Model file integrity check failed. The file may be damaged; please delete and re-download."
        lower.contains("incompatible") || lower.contains("unsupported") ->
            "Model format or architecture is incompatible with this device."
        lower.contains("not installed") || lower.contains("not found") ->
            "Model is not installed on this device. Download it first."
        lower.contains("too large") || lower.contains("request_too_large") ->
            "Request payload exceeds allowed limit (10 MB)."
        lower.contains("generation_limit") ->
            "Requested generation length exceeds allowed limit."
        else -> error
    }
}
