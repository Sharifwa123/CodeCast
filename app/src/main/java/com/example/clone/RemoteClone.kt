package com.example.clone

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Client for a self-hosted clone server (see tools/clone-server). Contract:
 *   GET  {base}/health                      -> 200
 *   POST {base}/tts           multipart: text, language, reference(file)      -> audio/wav
 *   POST {base}/talking-head  multipart: image(file), audio(file)             -> video/mp4
 * An optional bearer token is sent as "Authorization: Bearer <token>".
 */
object RemoteClone {
    class CloneException(message: String) : Exception(message)

    private class Part(val name: String, val filename: String?, val contentType: String?, val data: ByteArray)

    fun normalize(base: String): String = base.trim().removeSuffix("/")

    fun health(base: String, token: String = ""): String? = try {
        val c = open("${normalize(base)}/health", "GET", token, 15_000)
        if (c.responseCode in 200..299) null else "Server answered HTTP ${c.responseCode}"
    } catch (e: Exception) { "Can't reach the server: ${e.message}" }

    fun synthesize(base: String, token: String, text: String, language: String, reference: ByteArray, referenceName: String = "reference.wav"): ByteArray =
        post("${normalize(base)}/tts", token, listOf(
            Part("text", null, null, text.toByteArray()),
            Part("language", null, null, languageCode(language).toByteArray()),
            Part("reference", referenceName, mimeOf(referenceName), reference)
        ), 180_000)

    fun talkingHead(base: String, token: String, image: ByteArray, imageName: String, wav: ByteArray): ByteArray =
        post("${normalize(base)}/talking-head", token, listOf(
            Part("image", imageName, mimeOf(imageName), image),
            Part("audio", "audio.wav", "audio/wav", wav)
        ), 600_000)

    fun languageCode(language: String) = when (language.lowercase()) {
        "french" -> "fr"; "spanish" -> "es"; "german" -> "de"; "portuguese" -> "pt"; "arabic" -> "ar"; "twi" -> "ak"; else -> "en"
    }

    private fun mimeOf(name: String) = when (name.substringAfterLast('.', "").lowercase()) {
        "wav" -> "audio/wav"; "mp3" -> "audio/mpeg"; "m4a" -> "audio/mp4"; "ogg" -> "audio/ogg"; "flac" -> "audio/flac"
        "jpg", "jpeg" -> "image/jpeg"; "png" -> "image/png"; else -> "application/octet-stream"
    }

    private fun open(url: String, method: String, token: String, timeout: Int): HttpURLConnection {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = method; c.connectTimeout = 20_000; c.readTimeout = timeout
        c.setRequestProperty("User-Agent", "CodeCast/1.0")
        c.setRequestProperty("ngrok-skip-browser-warning", "1")
        if (token.isNotBlank()) c.setRequestProperty("Authorization", "Bearer ${token.trim()}")
        return c
    }

    private fun post(url: String, token: String, parts: List<Part>, timeout: Int): ByteArray {
        val boundary = "----CodeCast" + UUID.randomUUID().toString().replace("-", "")
        val body = ByteArrayOutputStream()
        parts.forEach { p ->
            body.write("--$boundary\r\n".toByteArray())
            body.write(("Content-Disposition: form-data; name=\"${p.name}\"" + (p.filename?.let { "; filename=\"$it\"" } ?: "") + "\r\n").toByteArray())
            p.contentType?.let { body.write("Content-Type: $it\r\n".toByteArray()) }
            body.write("\r\n".toByteArray()); body.write(p.data); body.write("\r\n".toByteArray())
        }
        body.write("--$boundary--\r\n".toByteArray())
        val c = open(url, "POST", token, timeout)
        c.doOutput = true
        c.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        c.setFixedLengthStreamingMode(body.size())
        try {
            c.outputStream.use { it.write(body.toByteArray()) }
            if (c.responseCode !in 200..299) {
                val err = try { c.errorStream?.bufferedReader()?.readText()?.take(300) } catch (_: Exception) { null }
                throw CloneException("Server answered HTTP ${c.responseCode}" + (err?.let { ": $it" } ?: ""))
            }
            return c.inputStream.use { it.readBytes() }
        } catch (e: CloneException) {
            throw e
        } catch (e: Exception) {
            throw CloneException("Clone server request failed: ${e.message}")
        }
    }
}
