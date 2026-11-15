package dev.aidos.tcr.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SharingModelsTest {
    @Test
    fun hostPortAcceptsOnlyValidAddresses() {
        assertEquals("100.64.0.7" to 47100, parseHostPort(" 100.64.0.7:47100 "))
        assertEquals("home.example.org" to 22, parseHostPort("home.example.org:22"))
        assertNull(parseHostPort("nohost"))
        assertNull(parseHostPort(":80"))
        assertNull(parseHostPort("host:"))
        assertNull(parseHostPort("host:0"))
        assertNull(parseHostPort("host:70000"))
        assertNull(parseHostPort("two words:80"))
        assertNull(parseHostPort("host:abc"))
    }

    @Test
    fun toolNamesAreSplitTrimmedAndDeduplicated() {
        assertEquals(listOf("search", "read_note", "write"), parseToolNames("search, read_note\nwrite  search"))
        assertEquals(emptyList(), parseToolNames("  , ,\n"))
    }

    @Test
    fun peerDefaultsAreOffAndAllowlistsEmpty() {
        val peer = RemotePeerUi("id", "Laptop", connected = false, route = null, hostsAgentLoops = false, preferredRank = null, remoteAccessAllowed = false, hints = emptyList(), toolAllowlists = emptyList())
        assertEquals(false, peer.hostsAgentLoops)
        assertEquals(false, peer.remoteAccessAllowed)
        assertEquals(false, RemoteSharingUiState().aidinRunning)
        assertEquals(false, LanSharingUiState().enabled)
    }
}

class ShellScopeTest {
    @Test
    fun scopeStringRoundTrips() {
        assertEquals("shell:.", shellScopeString("", null))
        assertEquals("shell:sub|git,gradle", shellScopeString(" sub ", listOf("git", "gradle")))
        assertEquals("sub" to listOf("git", "gradle"), parseShellScope("shell:sub|git,gradle"))
        assertEquals("." to null, parseShellScope("shell:."))
        assertNull(parseShellScope("fs:/x"))
        assertNull(parseShellScope(null))
    }
}
