package com.example.analysis

/** Turns a GitHub / GitLab / Bitbucket repository URL into archive download URLs. */
object RepoUrl {
    data class Parsed(val host: String, val owner: String, val repo: String, val ref: String?)

    fun parse(url: String): Parsed? {
        val cleaned = url.trim().removeSuffix("/").removeSuffix(".git")
        val m = Regex("""^(?:https?://)?(?:www\.)?(github\.com|gitlab\.com|bitbucket\.org)/([^/\s]+)/([^/\s?#]+)(?:/(?:-/)?tree/([^?#\s]+))?""")
            .find(cleaned) ?: return null
        val (host, owner, repo, ref) = m.destructured
        return Parsed(host, owner, repo, ref.ifEmpty { null })
    }

    fun archiveUrls(url: String): List<String> {
        val p = parse(url) ?: return emptyList()
        val refs = if (p.ref != null) listOf(p.ref) else listOf("HEAD", "main", "master")
        return refs.map { r ->
            when (p.host) {
                "github.com" -> "https://codeload.github.com/${p.owner}/${p.repo}/zip/$r"
                "gitlab.com" -> "https://gitlab.com/${p.owner}/${p.repo}/-/archive/$r/${p.repo}-${r.replace('/', '-')}.zip"
                else -> "https://bitbucket.org/${p.owner}/${p.repo}/get/$r.zip"
            }
        }
    }

    fun displayName(url: String): String = parse(url)?.repo?.replace('-', ' ')?.replace('_', ' ')
        ?.split(" ")?.joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } } ?: "Project"
}
