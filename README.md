# CodeCast — Automated Codebase-to-Video SaaS Studio

CodeCast transforms application and website codebases into verified, production-quality video tutorials and interactive developer walkthroughs automatically.

---

## 🚀 Download & Install the Android APK

### Option 1: Direct 1-Tap APK Install (Recommended — No ZIP Extraction Needed!)
1. In your GitHub repository, tap **"Releases"** (on mobile: tap the repo home page, scroll down to **Releases**, or open `https://github.com/<owner>/<repo>/releases`).
2. Under the latest release (e.g. **v1.0.0**), tap **`CodeCast.apk`** (or `CodeCast-app-debug.apk`).
3. Your phone downloads the raw `.apk` file directly.
4. Tap **Open** or tap the downloaded notification, then tap **Install**.
   *(No zip extractors, no file manager errors!)*

---

### Option 2: Download from GitHub Actions
1. Navigate to the **Actions** tab on your GitHub repository.
2. Tap the latest run of **"Build and Release Android APK"**.
3. Scroll down to the **Artifacts** section at the bottom.
4. Download **`CodeCast-Android-APK`**.
5. Once downloaded, open your Files app to extract the `.apk` and tap to install.

---

## 🛠 Tech Stack
- **Architecture:** Jetpack Compose, Material 3, Clean Architecture & MVVM
- **Database:** Local Room Database for project knowledge indexing & tutorial plans
- **Async & Reactive:** Kotlin Coroutines & Flow
- **Image Assets:** Dynamic scene compositing & custom adaptive launcher icons
- **Target SDK:** Android 14+ (API 36 / 34)
