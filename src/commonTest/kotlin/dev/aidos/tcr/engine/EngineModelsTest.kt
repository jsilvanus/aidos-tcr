package dev.aidos.tcr.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EngineModelsTest {
    private val mb = 1024L * 1024L

    private fun model(
        sizeMb: Long?,
        verdict: ModelFitVerdict = ModelFitVerdict.RUNS_WELL,
        runnable: Boolean = true,
    ) = CookbookModel(
        id = "m",
        name = "m",
        kind = "LLM",
        quantization = "GGUF",
        sizeBytes = sizeMb?.times(mb),
        contextLength = 4096,
        fitVerdict = verdict,
        isRunnable = runnable,
    )

    @Test
    fun defaultFiltersAcceptEverythingIncludingUnknownSize() {
        val f = ModelSearchFilters()
        assertFalse(f.isActive)
        assertTrue(f.accepts(model(null)))
    }

    @Test
    fun sizeBoundsAreInclusiveAndUnknownSizeIsRejected() {
        val f = ModelSearchFilters(minSizeMb = 100, maxSizeMb = 500)
        assertTrue(f.isActive)
        assertTrue(f.accepts(model(100)))
        assertTrue(f.accepts(model(500)))
        assertFalse(f.accepts(model(99)))
        assertFalse(f.accepts(model(501)))
        assertFalse(f.accepts(model(null)))
    }

    @Test
    fun fitAndRunnableFilters() {
        val f = ModelSearchFilters(fitsDeviceOnly = true, runnableOnly = true)
        assertTrue(f.accepts(model(1)))
        assertFalse(f.accepts(model(1, verdict = ModelFitVerdict.WILL_NOT_FIT)))
        assertFalse(f.accepts(model(1, runnable = false)))
    }

    @Test
    fun testLoadingStateTransitions() {
        val initial = ModelLoadingState()
        assertEquals("", initial.modelId)
        assertEquals(ModelLoadingStatus.NOT_LOADED, initial.status)
        assertEquals(0, initial.loadProgress)
        assertNull(initial.error)
        assertNull(initial.loadTimeMs)

        val loading = initial.copy(
            modelId = "test-model",
            status = ModelLoadingStatus.LOADING,
            loadProgress = 45,
        )
        assertEquals(ModelLoadingStatus.LOADING, loading.status)
        assertEquals(45, loading.loadProgress)

        val loaded = loading.copy(
            status = ModelLoadingStatus.LOADED,
            loadProgress = 100,
            loadTimeMs = 3200L,
        )
        assertEquals(ModelLoadingStatus.LOADED, loaded.status)
        assertEquals(100, loaded.loadProgress)
        assertEquals(3200L, loaded.loadTimeMs)

        val error = loading.copy(
            status = ModelLoadingStatus.ERROR,
            error = "Out of memory",
        )
        assertEquals(ModelLoadingStatus.ERROR, error.status)
        assertEquals("Out of memory", error.error)
    }

    @Test
    fun selfTestTokensPerSecondCalculation() {
        val outcome = SelfTestOutcome(
            passed = true,
            summary = "ok",
            generatedTokens = 20,
            durationMs = 2000L,
        )
        assertEquals(10.0f, outcome.tokensPerSecond)
        assertEquals("ok", outcome.title)
        assertNull(SelfTestOutcome(passed = true, summary = "ok", durationMs = 2000L).tokensPerSecond)
        assertNull(SelfTestOutcome(passed = true, summary = "ok", generatedTokens = 0, durationMs = 2000L).tokensPerSecond)
        assertNull(SelfTestOutcome(passed = true, summary = "ok", generatedTokens = 5, durationMs = 0L).tokensPerSecond)
    }

    @Test
    fun formattingHelpers() {
        assertEquals("size unknown", formatSize(null))
        assertEquals("size unknown", formatSize(0))
        assertEquals("1 KB", formatSize(500))
        assertEquals("1 KB", formatSize(1024))
        assertEquals("1.5 MB", formatSize((1.5 * 1024 * 1024).toLong()))
        assertEquals("2 GB", formatSize(2L * 1024 * 1024 * 1024))

        assertEquals("just now", formatElapsed(500))
        assertEquals("1s ago", formatElapsed(1500))
        assertEquals("2m ago", formatElapsed(120_000))

        assertTrue(formatActionableErrorMessage("device has insufficient_memory to run").contains("Out of memory"))
        assertTrue(formatActionableErrorMessage("peer_busy right now").contains("busy"))
    }
}
