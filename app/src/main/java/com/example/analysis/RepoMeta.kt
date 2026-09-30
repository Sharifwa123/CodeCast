package com.example.analysis

import java.net.HttpURLConnection
import java.net.URL

/** Reads the "homepage" a maintainer set on a public GitHub repository (often the live site). */
object RepoMeta {
    fun githubHomepage(repoUrl: String): String? {
        val p = RepoUrl.parse(repoUrl)?.takeIf { it.host == "github.com" } ?: return null
        return try {
            val c = URL("https://api.github.com/repos/${p.owner}/${p.repo}").openConnection() as HttpURLConnection
            c.connectTimeout = 8_000; c.readTimeout = 8_000
            c.setRequestProperty("User-Agent", "CodeCast/1.0"); c.setRequestProperty("Accept", "application/vnd.github+json")
            if (c.responseCode != 200) null
            else Regex(""""homepage"\s*:\s*"([^"]+)"""").find(c.inputStream.bufferedReader().readText())?.groupValues?.get(1)
        } catch (_: Exception) { null }
    }

    /** True when [url] answers with a non-error status. */
    fun reachable(url: String): String? = try {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000; c.readTimeout = 10_000; c.instanceFollowRedirects = true
        c.setRequestProperty("User-Agent", "Mozilla/5.0 CodeCast")
        if (c.responseCode < 400) null else "The site answered HTTP ${c.responseCode}"
    } catch (e: Exception) { "Can't reach the site: ${e.message}" }
}
