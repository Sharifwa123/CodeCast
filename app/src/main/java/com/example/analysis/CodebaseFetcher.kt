package com.example.analysis

import java.net.HttpURLConnection
import java.net.URL

/** Downloads a public repository archive over HTTPS and reads it. */
object CodebaseFetcher {
    fun fetch(repoUrl: String): ZipReadResult {
        val urls = RepoUrl.archiveUrls(repoUrl)
        require(urls.isNotEmpty()) { "Unsupported repository URL. Use a github.com, gitlab.com or bitbucket.org link." }
        var last: Exception? = null
        for (u in urls) {
            try {
                val c = URL(u).openConnection() as HttpURLConnection
                c.connectTimeout = 15_000
                c.readTimeout = 60_000
                c.instanceFollowRedirects = true
                c.setRequestProperty("User-Agent", "CodeCast/1.0")
                if (c.responseCode == 200) return c.inputStream.use { ZipCodebaseReader.read(it) }
                last = IllegalStateException("HTTP ${c.responseCode} for $u" + if (c.responseCode == 404) " (repository private or not found)" else "")
            } catch (e: Exception) {
                last = e
            }
        }
        throw IllegalStateException("Could not download repository: ${last?.message}", last)
    }
}
