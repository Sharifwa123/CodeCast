package com.example.ai

import com.example.analysis.Translator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Translates narration/subtitles with the Gemini REST API. Returns null when unavailable or on any error. */
class GeminiTranslator(private val apiKey: String, private val protectedTerms: List<String>) : Translator {
    override suspend fun translate(texts: List<String>, targetLanguage: String): List<String>? {
        if (apiKey.isBlank() || texts.isEmpty()) return null
        return withContext(Dispatchers.IO) {
            try {
                val prompt = "Translate every string in this JSON array into $targetLanguage for a software tutorial. " +
                    "Keep these terms exactly unchanged: ${protectedTerms.joinToString(", ").ifEmpty { "(none)" }}. " +
                    "Keep file names, code identifiers and URLs unchanged. " +
                    "Return ONLY a JSON array of strings with exactly ${texts.size} items, in the same order.\n" + JSONArray(texts).toString()
                val body = JSONObject()
                    .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
                    .put("generationConfig", JSONObject().put("responseMimeType", "application/json"))
                val c = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent").openConnection() as HttpURLConnection
                c.requestMethod = "POST"
                c.connectTimeout = 15_000; c.readTimeout = 60_000
                c.setRequestProperty("Content-Type", "application/json")
                c.setRequestProperty("x-goog-api-key", apiKey)
                c.doOutput = true
                c.outputStream.use { it.write(body.toString().toByteArray()) }
                if (c.responseCode != 200) return@withContext null
                val resp = JSONObject(c.inputStream.bufferedReader().readText())
                val out = resp.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                val arr = JSONArray(out)
                if (arr.length() != texts.size) null else List(arr.length()) { arr.getString(it) }
            } catch (_: Exception) {
                null
            }
        }
    }
}
