package com.example.record

import com.example.analysis.RepoUrl
import com.example.data.model.CodebaseFile

data class UrlCandidate(val url: String, val source: String)

/** Looks through the source for the URL where the app is deployed. */
object LiveUrlFinder {
    private val ENV_KEYS = listOf(
        "NEXT_PUBLIC_SITE_URL", "NEXT_PUBLIC_APP_URL", "NEXT_PUBLIC_BASE_URL", "NEXT_PUBLIC_URL", "VITE_APP_URL", "VITE_SITE_URL",
        "REACT_APP_URL", "APP_URL", "SITE_URL", "BASE_URL", "PUBLIC_URL", "FRONTEND_URL", "DOMAIN"
    )
    private val IGNORED_HOSTS = listOf(
        "github.com", "githubusercontent.com", "shields.io", "npmjs.com", "gitlab.com", "bitbucket.org", "localhost", "127.0.0.1",
        "example.com", "example.org", "twitter.com", "x.com", "linkedin.com", "discord.gg", "discord.com", "youtube.com", "youtu.be",
        "nodejs.org", "reactjs.org", "react.dev", "nextjs.org", "w3.org", "schema.org", "googleapis.com", "gstatic.com",
        "cloudflare.com", "medium.com", "stackoverflow.com", "wikipedia.org", "creativecommons.org", "vercel.com", "netlify.com"
    )

    fun fromSource(files: List<CodebaseFile>, repoUrl: String? = null): List<UrlCandidate> {
        val out = LinkedHashMap<String, UrlCandidate>()
        fun add(raw: String?, source: String) {
            val u = normalize(raw) ?: return
            out.putIfAbsent(u, UrlCandidate(u, source))
        }
        // package.json "homepage"
        files.filter { it.path.endsWith("package.json") }.forEach { f ->
            Regex(""""homepage"\s*:\s*"([^"]+)"""").find(f.content)?.let { add(it.groupValues[1], "${f.path} homepage") }
        }
        // GitHub Pages CNAME
        files.filter { it.path.substringAfterLast('/') == "CNAME" }.forEach { f -> add("https://" + f.content.trim(), f.path) }
        // env style files (.env.example is in the archive as text only if extension allowed; also scan any file mentioning keys)
        files.forEach { f ->
            ENV_KEYS.forEach { k ->
                Regex("""\b$k\s*[=:]\s*["']?(https?://[^\s"']+|[a-z0-9.-]+\.[a-z]{2,}[^\s"']*)""").find(f.content)?.let { add(it.groupValues[1], "${f.path} $k") }
            }
        }
        // README: first non-badge URLs, prefer ones next to words like demo/live/website
        files.filter { it.path.substringAfterLast('/').lowercase().startsWith("readme") }.forEach { f ->
            f.content.lines().take(200).forEach { line ->
                val hinted = Regex("""(?i)\b(live|demo|website|visit|production|app)\b""").containsMatchIn(line)
                Regex("""https?://[^\s)>\]"']+""").findAll(line).forEach { m ->
                    if (hinted || out.size < 3) add(m.value.trimEnd('.', ',', ';'), "${f.path}")
                }
            }
        }
        // GitHub Pages guess
        RepoUrl.parse(repoUrl ?: "")?.takeIf { it.host == "github.com" }?.let { add("https://${it.owner.lowercase()}.github.io/${it.repo}/", "GitHub Pages guess") }
        return out.values.toList()
    }

    fun normalize(raw: String?): String? {
        var u = raw?.trim()?.trim('"', '\'', '`') ?: return null
        if (u.isEmpty()) return null
        if (!u.startsWith("http://") && !u.startsWith("https://")) u = "https://$u"
        if (u.contains("\${") || u.contains("{{") || u.contains("<")) return null
        val host = Regex("""^https?://([^/:?#]+)""").find(u)?.groupValues?.get(1)?.lowercase() ?: return null
        if (!host.contains('.')) return null
        val pages = host.endsWith(".github.io")
        if (!pages && (host.startsWith("docs.") || IGNORED_HOSTS.any { host == it || host.endsWith(".$it") })) return null
        return u.removeSuffix("/")
    }

    /** Accepts only http(s) URLs with a host. */
    fun isValid(url: String): Boolean = Regex("""^https?://[^/\s]+\.[^/\s]+""").containsMatchIn(url.trim())
}
