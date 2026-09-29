package com.example.analysis

import com.example.data.model.CodebaseFile
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

data class ZipReadResult(val files: List<CodebaseFile>, val entryCount: Int, val totalBytes: Long)

/** Reads a real source archive. Never substitutes sample data: an archive without source is an error. */
object ZipCodebaseReader {
    private const val MAX_FILES = 600
    private const val MAX_FILE_BYTES = 80_000

    private val CODE_EXT = setOf(
        "ts", "tsx", "js", "jsx", "mjs", "vue", "svelte", "html", "py", "php", "rb", "go", "java", "kt", "dart",
        "prisma", "sql", "json", "yaml", "yml", "toml", "gradle", "kts", "md", "xml"
    )
    private val SKIP_DIRS = listOf(
        "node_modules/", ".git/", "/dist/", "/build/", "/.next/", "/vendor/", "/__pycache__/", "/.gradle/", "/coverage/", "/.dart_tool/"
    )
    private val SKIP_FILES = listOf("package-lock.json", "yarn.lock", "pnpm-lock.yaml", "composer.lock", ".min.js", "tsconfig.tsbuildinfo")

    fun read(input: InputStream): ZipReadResult {
        val raw = ArrayList<Triple<String, String, Long>>()
        var entries = 0
        var total = 0L
        ZipInputStream(input).use { zis ->
            var e = zis.nextEntry
            while (e != null) {
                if (!e.isDirectory) {
                    entries++
                    val name = e.name.replace('\\', '/')
                    val padded = "/" + name.lowercase()
                    val ext = name.substringAfterLast('.', "").lowercase()
                    val wanted = (ext in CODE_EXT || padded.endsWith("/requirements.txt")) && SKIP_DIRS.none { padded.contains(it) } &&
                        SKIP_FILES.none { padded.endsWith(it) } && raw.size < MAX_FILES
                    val buf = ByteArrayOutputStream()
                    val tmp = ByteArray(8192)
                    var n: Int
                    var size = 0L
                    while (zis.read(tmp).also { n = it } != -1) {
                        size += n
                        if (wanted && buf.size() < MAX_FILE_BYTES) buf.write(tmp, 0, n)
                    }
                    total += size
                    if (wanted) raw.add(Triple(name, buf.toString("UTF-8"), size))
                }
                e = zis.nextEntry
            }
        }
        if (raw.isEmpty()) throw IllegalArgumentException("No source files found in the archive (${entries} entries scanned).")
        val prefix = commonRoot(raw.map { it.first })
        val files = raw.map { (name, content, size) ->
            val path = name.removePrefix(prefix)
            val lines = content.lines()
            CodebaseFile(
                path = path,
                language = languageOf(path),
                lineCount = lines.size,
                sizeBytes = size,
                content = content,
                exportedSymbols = lines.filter { l ->
                    l.trimStart().let { it.startsWith("export ") || it.startsWith("def ") || it.startsWith("class ") || it.startsWith("function ") || it.startsWith("model ") || it.startsWith("fun ") }
                }.take(4).map { it.trim().take(48) },
                category = categoryOf(path)
            )
        }.sortedBy { it.path }
        return ZipReadResult(files, entries, total)
    }

    private fun commonRoot(names: List<String>): String {
        val first = names.first().substringBefore('/', "")
        if (first.isEmpty()) return ""
        return if (names.all { it.startsWith("$first/") }) "$first/" else ""
    }

    fun languageOf(path: String): String = when (path.substringAfterLast('.', "").lowercase()) {
        "ts", "tsx" -> "typescript"; "js", "jsx", "mjs" -> "javascript"; "py" -> "python"; "dart" -> "dart"
        "kt", "kts" -> "kotlin"; "json" -> "json"; "prisma" -> "prisma"; "sql" -> "sql"; "php" -> "php"
        "go" -> "go"; "java" -> "java"; "rb" -> "ruby"; "vue" -> "vue"; "html" -> "html"; else -> "code"
    }

    fun categoryOf(path: String): String {
        val l = path.lowercase()
        return when {
            l.endsWith(".json") || l.endsWith(".yaml") || l.endsWith(".yml") || l.endsWith(".toml") || l.contains("config") -> "Config"
            l.contains("schema") || l.contains("/models/") || l.contains("migration") || l.endsWith(".sql") || l.endsWith(".prisma") -> "Database"
            l.contains("/api/") || l.contains("route") || l.contains("controller") || l.contains("server") -> "Backend"
            else -> "Frontend"
        }
    }
}
