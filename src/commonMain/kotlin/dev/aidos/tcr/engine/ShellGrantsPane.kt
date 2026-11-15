package dev.aidos.tcr.engine

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aidos.tcr.theme.TcrColors

/** A project a shell grant can be made in. */
data class ShellProjectUi(val id: String, val name: String, val path: String)

/** A session that holds (or could hold) shell grants. Runs act with their session's authority. */
data class ShellSessionUi(val id: String, val name: String)

/** One active `shell:exec` grant. [commands] null means any program; otherwise only these, run without a shell. */
data class ShellGrantUi(
    val id: String,
    val workingDirectory: String,
    val commands: List<String>?,
    val expiresAt: String?,
)

data class ShellGrantsUiState(
    val projects: List<ShellProjectUi> = emptyList(),
    val selectedProjectId: String? = null,
    val sessions: List<ShellSessionUi> = emptyList(),
    val selectedSessionId: String? = null,
    val grants: List<ShellGrantUi> = emptyList(),
    val error: String? = null,
)

/** `shell:<dir>` or `shell:<dir>|cmd1,cmd2`: the scope string the runtime API takes for a shell grant (RFC-0033). */
fun shellScopeString(workingDirectory: String, commands: List<String>?): String =
    "shell:" + workingDirectory.trim().ifEmpty { "." } + (commands?.takeIf { it.isNotEmpty() }?.let { "|" + it.joinToString(",") } ?: "")

/** Parses [shellScopeString]; null when [scope] is not a shell scope. */
fun parseShellScope(scope: String?): Pair<String, List<String>?>? {
    if (scope == null || !scope.startsWith("shell:")) return null
    val spec = scope.removePrefix("shell:")
    val dir = spec.substringBefore('|').ifBlank { "." }
    val cmds = if ('|' in spec) spec.substringAfter('|').split(',').map { it.trim() }.filter { it.isNotEmpty() } else null
    return dir to cmds
}

/**
 * Organism TCR Component: ShellGrantsPane (RFC-0033).
 *
 * Shell is never available by default: a run can only use it through a grant made here. Pick a project
 * and a session, then say which directory it may work in and, optionally, which programs it may start.
 * With a program list the command runs without a shell, so `;` and `&&` cannot add another program;
 * with none, anything the daemon user can run is allowed, so prefer the list. Shell exists only on
 * desktop and servers.
 */
@Composable
fun ShellGrantsPane(
    state: ShellGrantsUiState,
    onSelectProject: (String) -> Unit,
    onSelectSession: (String) -> Unit,
    onGrant: (workingDirectory: String, commands: List<String>?, lifetimeHours: Long?) -> Unit,
    onRevoke: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dir by remember { mutableStateOf(".") }
    var commands by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("8") }
    val lifetime = hours.toLongOrNull()?.coerceIn(1, 24 * 365)

    LazyColumn(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        state.error?.let { item { Text(it, color = TcrColors.EmberRed, fontSize = 12.sp) } }

        item { Text("Projects", fontWeight = FontWeight.SemiBold) }
        if (state.projects.isEmpty()) {
            item { Text("No project yet. Create one first; shell grants belong to a project's sessions.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(state.projects, key = { it.id }) { p ->
            val selected = p.id == state.selectedProjectId
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(p.name, fontWeight = FontWeight.Medium)
                        Text(p.path, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { onSelectProject(p.id) }, enabled = !selected) { Text(if (selected) "Selected" else "Select") }
                }
            }
        }

        if (state.selectedProjectId != null) {
            item { Text("Sessions", fontWeight = FontWeight.SemiBold) }
            if (state.sessions.isEmpty()) {
                item { Text("This project has no session yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(state.sessions, key = { it.id }) { s ->
                val selected = s.id == state.selectedSessionId
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.name, modifier = Modifier.weight(1f), fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                    TextButton(onClick = { onSelectSession(s.id) }, enabled = !selected) { Text(if (selected) "Selected" else "Select") }
                }
            }
        }

        if (state.selectedSessionId != null) {
            item { Text("Shell grants for this session", fontWeight = FontWeight.SemiBold) }
            if (state.grants.isEmpty()) {
                item { Text("None. Runs in this session cannot run commands.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(state.grants, key = { it.id }) { g ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("In ${g.workingDirectory}", fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                            Text(
                                g.commands?.let { "Only: ${it.joinToString(", ")}" } ?: "Any program",
                                fontSize = 12.sp,
                                color = if (g.commands == null) TcrColors.AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(g.expiresAt?.let { "Expires $it" } ?: "No expiry", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(onClick = { onRevoke(g.id) }) { Text("Revoke", color = TcrColors.EmberRed) }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Allow commands", fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(dir, { dir = it }, label = { Text("Working directory (inside the project)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(
                            commands, { commands = it }, label = { Text("Programs allowed (comma separated; empty = any)") },
                            placeholder = { Text("git, gradle, java") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(hours, { hours = it.filter(Char::isDigit) }, label = { Text("Valid for (hours)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        if (parseToolNames(commands).isEmpty()) {
                            Text("With no program list, anything this computer's user can run is allowed. Prefer a list.", fontSize = 11.sp, color = TcrColors.AmberWarning)
                        }
                        Button(
                            onClick = { onGrant(dir, parseToolNames(commands).takeIf { it.isNotEmpty() }, lifetime) },
                            enabled = dir.isNotBlank() && lifetime != null,
                        ) { Text("Grant") }
                    }
                }
            }
        }
    }
}
