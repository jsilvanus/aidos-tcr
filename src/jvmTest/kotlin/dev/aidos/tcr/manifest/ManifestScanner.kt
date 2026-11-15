package dev.aidos.tcr.manifest

import kotlinx.serialization.Serializable

/**
 * Source scanner behind the TCR component manifest (`manifest/tcr-manifest.json`).
 *
 * The manifest is the registry's machine-readable index: every public composable and every public
 * data/enum contract type in `commonMain`, with a versioned signature. It is derived from source
 * (not hand-written) so it cannot drift: `ManifestSyncTest` fails when the source and the manifest
 * disagree, and the updater below bumps a contract version according to the kind of change.
 *
 * This is deliberately a text scanner, not a Kotlin parser. It only understands what this
 * repository actually writes: top-level declarations at column 0. If a construct is not
 * recognised it is *skipped*, and the scanner tests pin the recognised shapes.
 */

enum class Kind { COMPOSABLE, FUNCTION, DATA_CLASS, ENUM }

enum class Status { EXPERIMENTAL, STABLE, DEPRECATED }

@Serializable
data class ManifestEntry(
    val id: String,
    val kind: String,
    val domain: String,
    val file: String,
    val status: String,
    /** Semver of this component's contract (its signature). See [Semver] / [classify]. */
    val contract: String,
    val summary: String,
    /** Normalised signature: `name: Type` for required, `name: Type =` for optional parameters. */
    val signature: List<String>,
)

@Serializable
data class Manifest(
    val schemaVersion: Int,
    val library: String,
    val libraryVersion: String,
    val components: List<ManifestEntry>,
)

data class Scanned(
    val id: String,
    val kind: Kind,
    val packageName: String,
    val file: String,
    val summary: String,
    val signature: List<String>,
)

enum class Change { NONE, MINOR, MAJOR }

object ManifestScanner {
    const val ROOT = "dev.aidos.tcr"

    private val composableOrFun =
        Regex("""^((?:@\S+\s+)*)(?:public\s+)?fun\s+(?:<[^>]*>\s+)?(?:([\w.<>?, ]+)\.)?([A-Za-z]\w*)\s*\(""")
    private val objectLine = Regex("""^(?:public\s+)?object\s+(\w+)""")
    private val dataClass =
        Regex("""^(?:@\S+\s+)*(?:public\s+)?data\s+class\s+(\w+)(?:<[^>]*>)?\s*\(""")
    private val enumClass =
        Regex("""^(?:@\S+\s+)*(?:public\s+)?enum\s+class\s+(\w+)\s*(\(|\{)""")
    private val packageLine = Regex("""^package\s+([\w.]+)""", RegexOption.MULTILINE)

    fun scan(file: String, text: String): List<Scanned> {
        val pkg = packageLine.find(text)?.groupValues?.get(1) ?: return emptyList()
        val lines = text.split("\n")
        val offsets = IntArray(lines.size)
        var acc = 0
        for (i in lines.indices) { offsets[i] = acc; acc += lines[i].length + 1 }
        val out = mutableListOf<Scanned>()
        var currentObject: String? = null
        for ((i, line) in lines.withIndex()) {
            if (currentObject != null && line.startsWith("    ") && !line.startsWith("     ")) {
                composableOrFun.find(line.substring(4))?.let { m ->
                    addFunction(out, m, text, offsets[i] + 4, lines, i, 4, pkg, file, kdocSummary(lines, i), currentObject)
                }
                continue
            }
            if (line.isEmpty() || line[0].isWhitespace()) continue
            val summary = kdocSummary(lines, i)
            objectLine.find(line)?.let { currentObject = it.groupValues[1] }
            if (line == "}") currentObject = null
            composableOrFun.find(line)?.let { m -> addFunction(out, m, text, offsets[i], lines, i, 0, pkg, file, summary, null) }
            dataClass.find(line)?.let { m ->
                val open = offsets[i] + m.range.last
                val params = paramsOf(text, open) ?: return@let
                out += Scanned("$pkg.${m.groupValues[1]}", Kind.DATA_CLASS, pkg, file, summary, params.map(::normalizeParam))
            }
            enumClass.find(line)?.let { m ->
                val open = offsets[i] + m.range.last
                val entries = enumEntries(text, if (m.groupValues[2] == "(") skipParens(text, open) else open) ?: return@let
                out += Scanned("$pkg.${m.groupValues[1]}", Kind.ENUM, pkg, file, summary, entries)
            }
        }
        return disambiguate(out)
    }

    /** Overloads get `#2`, `#3`, … in source order (so inserting an overload above shifts ids). */
    private fun disambiguate(all: List<Scanned>): List<Scanned> {
        val seen = mutableMapOf<String, Int>()
        return all.map { s ->
            val n = seen.merge(s.id, 1, Int::plus)!!
            if (n == 1) s else s.copy(id = "${s.id}#$n")
        }
    }

    private fun addFunction(
        out: MutableList<Scanned>, m: MatchResult, text: String, lineOffset: Int, lines: List<String>, i: Int,
        indent: Int, pkg: String, file: String, summary: String, owner: String?,
    ) {
        val annotations = m.groupValues[1] + precedingAnnotations(lines, i, indent)
        val params = paramsOf(text, lineOffset + m.range.last) ?: return
        val receiver = m.groupValues[2].takeIf { it.isNotEmpty() }
        val signature = listOfNotNull(receiver?.let { "this: $it" }) + params.map(::normalizeParam)
        val kind = if (annotations.contains("@Composable")) Kind.COMPOSABLE else Kind.FUNCTION
        val name = (owner?.let { "$it." } ?: "") + m.groupValues[3]
        out += Scanned("$pkg.$name", kind, pkg, file, summary, signature)
    }

    private fun precedingAnnotations(lines: List<String>, i: Int, indent: Int = 0): String {
        val sb = StringBuilder()
        var j = i - 1
        val pad = " ".repeat(indent)
        while (j >= 0 && lines[j].startsWith("$pad@") && lines[j].drop(indent).startsWith("@")) { sb.append(lines[j]).append(' '); j-- }
        return sb.toString()
    }

    /** First sentence of the KDoc block that ends just above the declaration's annotations. */
    private fun kdocSummary(lines: List<String>, declLine: Int): String {
        var j = declLine - 1
        while (j >= 0 && lines[j].trim().startsWith("@")) j--
        if (j < 0 || !lines[j].trim().endsWith("*/")) return ""
        var k = j
        while (k >= 0 && !lines[k].trim().startsWith("/**")) k--
        if (k < 0) return ""
        val body = (k..j).joinToString(" ") { lines[it].trim().removePrefix("/**").removeSuffix("*/").removePrefix("*").trim() }
            .replace(Regex("""\s+"""), " ").trim()
        val end = Regex("""[.!?](\s|$)""").find(body)
        return (if (end != null) body.substring(0, end.range.first + 1) else body).take(200)
    }

    /** Index of the `)` matching the `(` at [open]; strings and comments are skipped. */
    private fun skipParens(text: String, open: Int): Int {
        val end = matching(text, open) ?: return text.length
        return end + 1
    }

    private fun paramsOf(text: String, open: Int): List<String>? {
        val end = matching(text, open) ?: return null
        return splitTopLevel(stripComments(text.substring(open + 1, end)))
    }

    private fun matching(text: String, open: Int): Int? {
        var depth = 0
        var i = open
        var inStr = false
        while (i < text.length) {
            val c = text[i]
            if (inStr) {
                if (c == '\\') i++ else if (c == '"') inStr = false
            } else when {
                c == '"' -> inStr = true
                text.startsWith("//", i) -> { while (i < text.length && text[i] != '\n') i++; continue }
                text.startsWith("/*", i) -> { i = text.indexOf("*/", i).let { if (it < 0) return null else it + 1 } }
                c == '(' || c == '{' || c == '[' -> depth++
                c == ')' || c == '}' || c == ']' -> { depth--; if (depth == 0) return i }
            }
            i++
        }
        return null
    }

    fun stripComments(s: String): String {
        val sb = StringBuilder()
        var i = 0
        var inStr = false
        while (i < s.length) {
            val c = s[i]
            if (inStr) {
                sb.append(c)
                if (c == '\\' && i + 1 < s.length) { sb.append(s[i + 1]); i++ } else if (c == '"') inStr = false
            } else if (c == '"') { inStr = true; sb.append(c) }
            else if (s.startsWith("//", i)) { while (i < s.length && s[i] != '\n') i++; continue }
            else if (s.startsWith("/*", i)) { i = s.indexOf("*/", i).let { if (it < 0) s.length else it + 1 } }
            else sb.append(c)
            i++
        }
        return sb.toString()
    }

    /** Split on commas outside (), [], {}, <> and strings. `->` is not a generic closer. */
    fun splitTopLevel(s: String): List<String> {
        val parts = mutableListOf<String>()
        val cur = StringBuilder()
        var depth = 0
        var inStr = false
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (inStr) {
                cur.append(c)
                if (c == '\\' && i + 1 < s.length) { cur.append(s[i + 1]); i++ } else if (c == '"') inStr = false
            } else when {
                c == '"' -> { inStr = true; cur.append(c) }
                c == '-' && s.startsWith("->", i) -> { cur.append("->"); i++ }
                c == '(' || c == '[' || c == '{' || c == '<' -> { depth++; cur.append(c) }
                c == ')' || c == ']' || c == '}' || c == '>' -> { depth--; cur.append(c) }
                c == ',' && depth == 0 -> { parts += cur.toString(); cur.clear() }
                else -> cur.append(c)
            }
            i++
        }
        if (cur.isNotBlank()) parts += cur.toString()
        return parts.map { it.trim() }.filter { it.isNotEmpty() }
    }

    /** `name: Type` (required) or `name: Type =` (has a default). Modifiers and annotations dropped. */
    fun normalizeParam(raw: String): String {
        var p = raw.replace(Regex("""\s+"""), " ").trim()
        p = p.replace(Regex("""^(@\w+(\([^)]*\))?\s+)+"""), "")
        p = p.replace(Regex("""^((vararg|noinline|crossinline|val|var|private|public|internal|override)\s+)+"""), "")
        val colon = topLevelIndex(p, ':')
        if (colon < 0) return p
        val name = p.substring(0, colon).trim()
        val rest = p.substring(colon + 1).trim()
        val eq = topLevelIndex(rest, '=')
        return if (eq < 0) "$name: $rest" else "$name: ${rest.substring(0, eq).trim()} ="
    }

    private fun topLevelIndex(s: String, target: Char): Int {
        var depth = 0
        var inStr = false
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (inStr) { if (c == '\\') i++ else if (c == '"') inStr = false }
            else when {
                c == '"' -> inStr = true
                c == '-' && s.startsWith("->", i) -> i++
                c == '(' || c == '[' || c == '{' || c == '<' -> depth++
                c == ')' || c == ']' || c == '}' || c == '>' -> depth--
                c == target && depth == 0 && !(target == '=' && s.startsWith("==", i)) -> return i
            }
            i++
        }
        return -1
    }

    /** Entry names of an enum whose body `{` is at [from] (or at the first `{` after it). */
    private fun enumEntries(text: String, from: Int): List<String>? {
        val open = text.indexOf('{', from)
        if (open < 0) return null
        var depth = 0
        var i = open + 1
        val sb = StringBuilder()
        var inStr = false
        loop@ while (i < text.length) {
            val c = text[i]
            if (inStr) { sb.append(c); if (c == '\\') { sb.append(text[i + 1]); i++ } else if (c == '"') inStr = false }
            else when {
                c == '"' -> { inStr = true; sb.append(c) }
                text.startsWith("//", i) -> { while (i < text.length && text[i] != '\n') i++; continue@loop }
                text.startsWith("/*", i) -> i = text.indexOf("*/", i).let { if (it < 0) text.length else it + 1 }
                c == '(' || c == '{' || c == '[' -> { depth++; sb.append(c) }
                c == ')' || c == ']' -> { depth--; sb.append(c) }
                c == '}' -> { if (depth == 0) break@loop; depth--; sb.append(c) }
                c == ';' && depth == 0 -> break@loop
                else -> sb.append(c)
            }
            i++
        }
        return splitTopLevel(sb.toString()).mapNotNull { Regex("""^\s*(?:@\w+\s+)*(\w+)""").find(it)?.groupValues?.get(1) }
    }

    /**
     * Classify a signature change.
     *  - MAJOR: anything that can break an existing caller (removed/renamed/retyped/reordered parameter,
     *    a new required parameter, a parameter that lost its default, a removed enum entry).
     *  - MINOR: purely additive — new optional parameters appended after all existing ones, new enum entries.
     */
    fun classify(kind: Kind, old: List<String>, new: List<String>): Change {
        if (old == new) return Change.NONE
        if (new.size >= old.size && new.take(old.size) == old) {
            val added = new.drop(old.size)
            val additive = when (kind) {
                Kind.ENUM -> true
                else -> added.all { it.endsWith(" =") }
            }
            return if (additive) Change.MINOR else Change.MAJOR
        }
        return Change.MAJOR
    }
}

object Semver {
    private val re = Regex("""^(\d+)\.(\d+)\.(\d+)$""")
    fun isValid(v: String) = re.matches(v)
    private fun parts(v: String) = re.find(v)!!.groupValues.drop(1).map { it.toInt() }

    /** Semver bump; for 0.y.z a breaking change bumps the minor and an additive one the patch. */
    fun bump(v: String, change: Change): String {
        if (change == Change.NONE) return v
        val (maj, min, pat) = parts(v)
        return if (maj == 0) {
            if (change == Change.MAJOR) "0.${min + 1}.0" else "0.$min.${pat + 1}"
        } else {
            if (change == Change.MAJOR) "${maj + 1}.0.0" else "$maj.${min + 1}.0"
        }
    }
}
