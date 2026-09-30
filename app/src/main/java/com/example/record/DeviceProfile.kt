package com.example.record

/** Target device for the recording: viewport behaviour, user agent, output video size and window chrome. */
enum class DeviceProfile(
    val label: String,
    val family: String,          // "android" | "ios" | "windows" | "macos" | "linux"
    val isMobile: Boolean,
    val cssWidth: Int?,          // forced CSS viewport width (null = the phone's own width)
    val outWidth: Int,
    val outHeight: Int,
    val userAgent: String
) {
    ANDROID_PHONE("Android phone", "android", true, null, 720, 1280,
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"),
    IPHONE("iPhone", "ios", true, null, 720, 1280,
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1"),
    ANDROID_TABLET("Android tablet", "android", true, 1024, 1024, 768,
        "Mozilla/5.0 (Linux; Android 14; SM-X710) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"),
    IPAD("iPad", "ios", true, 1024, 1024, 768,
        "Mozilla/5.0 (iPad; CPU OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Mobile/15E148 Safari/604.1"),
    WINDOWS("Windows", "windows", false, 1280, 1280, 720,
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Edg/124.0.0.0"),
    MACOS("macOS", "macos", false, 1280, 1280, 720,
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15"),
    LINUX("Linux", "linux", false, 1280, 1280, 720,
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");

    /** Height of the window chrome drawn above / below the page, in output pixels. */
    val chromeTop: Int get() = when (family) {
        "macos" -> 84; "windows" -> 92; "linux" -> 88
        "ios" -> if (cssWidth == null) 96 else 84
        else -> if (cssWidth == null) 132 else 120 // android: status bar + browser bar
    }
    val chromeBottom: Int get() = when {
        family == "ios" && cssWidth == null -> 120
        family == "android" && cssWidth == null -> 56
        isMobile -> 40
        else -> 0
    }
    val contentWidth: Int get() = outWidth
    val contentHeight: Int get() = outHeight - chromeTop - chromeBottom
    /** height / width of the region the page is rendered into. */
    val contentAspect: Float get() = contentHeight.toFloat() / contentWidth

    companion object {
        fun fromId(id: String?) = values().firstOrNull { it.name == id } ?: ANDROID_PHONE
    }
}
