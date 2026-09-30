package com.example.analysis

import com.example.clone.RemoteClone
import com.example.media.WavData
import com.example.media.WavWriter
import com.sun.net.httpserver.HttpServer
import org.junit.Assert.*
import org.junit.Test
import java.net.InetSocketAddress

class CloneClientTest {
    private fun server(handler: (String, String, ByteArray, String?) -> Pair<Int, ByteArray>): HttpServer {
        val s = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        s.createContext("/") { ex ->
            val body = ex.requestBody.readBytes()
            val (code, out) = handler(ex.requestMethod, ex.requestURI.path, body, ex.requestHeaders.getFirst("Authorization"))
            ex.sendResponseHeaders(code, out.size.toLong()); ex.responseBody.use { it.write(out) }
        }
        s.start(); return s
    }
    private fun url(s: HttpServer) = "http://127.0.0.1:${s.address.port}/"

    @Test fun wavWriterRoundTripsThroughTheReader() {
        val pcm = ShortArray(16000) { (it % 50).toShort() }
        val w = WavData.parse(WavWriter.write(pcm, 16000))!!
        assertEquals(1.0, w.seconds, 0.001); assertEquals(16000, w.sampleRate)
    }

    @Test fun ttsSendsTextLanguageReferenceAndTokenAndReturnsAudio() {
        var seen = ""; var auth: String? = null; var path = ""
        val reply = WavWriter.write(ShortArray(8000) { 100 }, 16000)
        val s = server { _, p, body, a -> path = p; auth = a; seen = String(body, Charsets.ISO_8859_1); 200 to reply }
        try {
            val out = RemoteClone.synthesize(url(s), "secret", "Hello there", "French", ByteArray(2000) { 7 }, "me.wav")
            assertEquals("/tts", path); assertEquals("Bearer secret", auth)
            assertTrue(seen.contains("name=\"text\"") && seen.contains("Hello there"))
            assertTrue(seen.contains("name=\"language\"") && seen.contains("\r\n\r\nfr\r\n"))
            assertTrue(seen.contains("name=\"reference\"; filename=\"me.wav\"") && seen.contains("Content-Type: audio/wav"))
            assertEquals(0.5, WavData.parse(out)!!.seconds, 0.001)
        } finally { s.stop(0) }
    }

    @Test fun talkingHeadUploadsImageAndAudio() {
        var path = ""; var seen = ""
        val s = server { _, p, body, _ -> path = p; seen = String(body, Charsets.ISO_8859_1); 200 to ByteArray(10) { 1 } }
        try {
            assertEquals(10, RemoteClone.talkingHead(url(s), "", ByteArray(100), "me.jpg", ByteArray(100)).size)
            assertEquals("/talking-head", path)
            assertTrue(seen.contains("name=\"image\"; filename=\"me.jpg\"") && seen.contains("image/jpeg") && seen.contains("name=\"audio\""))
        } finally { s.stop(0) }
    }

    @Test fun serverErrorsAndDeadServersBecomeReadableMessages() {
        val s = server { _, _, _, _ -> 500 to "boom: out of memory".toByteArray() }
        val u = url(s)
        try {
            val e = try { RemoteClone.synthesize(u, "", "x", "English", ByteArray(1)); null } catch (e: RemoteClone.CloneException) { e }
            assertTrue(e!!.message!!, e.message!!.contains("500") && e.message!!.contains("boom"))
            assertNotNull(RemoteClone.health(u))            // 500 is not healthy
        } finally { s.stop(0) }
        assertTrue(RemoteClone.health(u)!!.startsWith("Can't reach"))
        val ok = server { m, p, _, _ -> if (m == "GET" && p == "/health") 200 to "ok".toByteArray() else 404 to ByteArray(0) }
        try { assertNull(RemoteClone.health(url(ok))) } finally { ok.stop(0) }
    }
}
