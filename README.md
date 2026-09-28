# CodeCast — Automated Codebase-to-Video SaaS Studio

CodeCast transforms application and website codebases into verified, production-quality video tutorials and interactive developer walkthroughs automatically.

---

## 🚀 GitHub Actions: Download & Share Android APK

This repository includes an automated GitHub Actions workflow to compile, package, and distribute the Android APK.

### 1. Download Latest APK from GitHub Actions Artifacts
Every commit or pull request automatically builds a fresh APK:
1. Navigate to the **Actions** tab on your GitHub repository.
2. Click on the latest run of **"Build and Release Android APK"**.
3. Scroll down to the **Artifacts** section at the bottom of the page.
4. Download **`CodeCast-Android-APK`** (contains `CodeCast-app-debug.apk`).
5. Transfer the APK to your Android device or emulator to install!

### 2. Create a Permanent Versioned Release & Shareable Link
To create a permanent, public download link for testers or users:
- **Option A (Git Tag):**
  Create and push a git tag (e.g., `v1.0.0`):
  ```bash
  git tag v1.0.0
  git push origin v1.0.0
  ```
  GitHub Actions will automatically generate a **GitHub Release** with the APK permanently attached. The download link will look like:
  ```
  https://github.com/<your-username>/<repo-name>/releases/download/v1.0.0/CodeCast-app-debug.apk
  ```

- **Option B (Manual Web Trigger):**
  1. Go to the **Actions** tab.
  2. Select **"Build and Release Android APK"** from the left sidebar.
  3. Click **"Run workflow"**.
  4. Check the box **"Publish as a permanent GitHub Release with APK attached"** and enter a version tag (e.g., `v1.0.0`).
  5. Click **"Run workflow"**.

---

## 🛠 Tech Stack
- **Architecture:** Jetpack Compose, Material 3, Clean Architecture & MVVM
- **Database:** Local Room Database for project knowledge indexing & tutorial plans
- **Async & Reactive:** Kotlin Coroutines & Flow
- **Image Assets:** Dynamic scene compositing & custom adaptive launcher icons
- **Target SDK:** Android 14+ (API 36 / 34)
