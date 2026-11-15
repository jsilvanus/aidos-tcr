package dev.aidos.tcr.manifest

import kotlinx.serialization.json.Json
import java.io.File

/** Reads sources, reads/writes `manifest/tcr-manifest.json`, and derives the next manifest. */
object ManifestIo {
    val json = Json { prettyPrint = true; prettyPrintIndent = "  " }
    val manifestFile = File("manifest/tcr-manifest.json")
    val sourceRoot = File("src/commonMain/kotlin")

    fun libraryVersion(): String =
        Regex("""(?m)^version\s*=\s*"([^"]+)"""").find(File("build.gradle.kts").readText())!!.groupValues[1]

    fun scanSources(root: File = sourceRoot): List<Scanned> =
        root.walkTopDown().filter { it.isFile && it.extension == "kt" }.sortedBy { it.path }
            .flatMap { f -> ManifestScanner.scan(f.path.replace('\\', '/'), f.readText()).asSequence() }
            .toList()

    fun domainOf(pkg: String) = pkg.removePrefix(ManifestScanner.ROOT).removePrefix(".").ifEmpty { "root" }

    fun read(): Manifest? = if (manifestFile.exists()) json.decodeFromString(Manifest.serializer(), manifestFile.readText()) else null

    fun write(m: Manifest) {
        manifestFile.parentFile.mkdirs()
        manifestFile.writeText(json.encodeToString(Manifest.serializer(), m) + "\n")
    }

    /**
     * The manifest the current sources imply, carrying over `status` and bumping `contract`
     * according to [ManifestScanner.classify] wherever a signature changed.
     */
    fun derive(scanned: List<Scanned>, old: Manifest?): Manifest {
        val byId = old?.components?.associateBy { it.id } ?: emptyMap()
        val entries = scanned.map { s ->
            val prev = byId[s.id]
            val contract = if (prev == null) "0.1.0"
            else Semver.bump(prev.contract, ManifestScanner.classify(s.kind, prev.signature, s.signature))
            ManifestEntry(
                id = s.id, kind = s.kind.name, domain = domainOf(s.packageName), file = s.file,
                status = prev?.status ?: Status.EXPERIMENTAL.name, contract = contract,
                summary = s.summary, signature = s.signature,
            )
        }.sortedBy { it.id }
        return Manifest(1, "dev.aidos.tcr:aidos-tcr", libraryVersion(), entries)
    }
}
