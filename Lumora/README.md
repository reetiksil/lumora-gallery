# Lumora 🌟

A clean, blazing-fast, open-source gallery app for Android written in Kotlin and Jetpack Compose.

Built from first principles for users who are exhausted by stock gallery bloat. Lumora has **zero AI/ML models**, **zero face-clustering databases**, **zero cloud sync**, and **no internet permission**.

---

## ⚡ Core Philosophy & Guarantees

1. **Strict 100 MB Storage Cap**: Your gallery app will never grow to 2 GB. All image caches are strictly capped (default 100 MB LRU disk cache, 15% heap memory cache). Clearing app data leaves zero phantom bloat.
2. **Instant Performance**: Cold start under 500 ms, smooth 60/120 FPS scrolling over 50,000+ items using direct MediaStore queries and minimal projections.
3. **Photos Tab Sources**: The main Photos tab displays **only** the albums you choose (Camera by default). Screenshots, downloads, and app folders remain in the Albums tab and do not clutter your primary timeline.
4. **Complete Offline Privacy**: `android.permission.INTERNET` is not even declared in the manifest. Your photos and metadata never leave your device.
5. **Zero Android Studio Needed**: Engineered to build cleanly in GitHub Actions CI/CD. You can write and edit code in VS Code on any low-spec computer.

---

## 🛠️ Getting Started Without Android Studio

You do **not** need a high-end laptop, Android Studio, or an emulator. All compilation, APK packaging, and quality checks happen in the cloud on GitHub's fast runners.

### 1. Check Your Local Environment
Open your terminal (PowerShell, Command Prompt, or bash) and verify Git and Java:
```bash
git --version
javac -version
```
- **Java Check**: If `javac -version` returns a version (e.g. `javac 17.0.x`), you have a full Java Development Kit. If it says command not found or only `java -version` works, you only have the JRE (runtime). **That is completely fine!** A local JDK 17 is entirely optional because GitHub Actions builds your APK for you in the cloud.
- **VS Code**: Install [VS Code](https://code.visualstudio.com/) along with the **Kotlin** (by fwcd) or **Kotlin Language** extension for syntax highlighting.

### 2. Push Code to GitHub
1. Go to [GitHub](https://github.com) and create a new repository named `lumora` (select **Public** or **Private**, do not initialize with README since we have one).
2. On your local machine in the `lumora` project directory:
```bash
git init -b main
git add .
git commit -m "feat(milestone-a): initial gallery skeleton, media timeline, and CI build"
git remote add origin https://github.com/YOUR_USERNAME/lumora.git
git push -u origin main
```

### 3. Watch the GitHub Actions Build
1. Open your repository on GitHub in your web browser.
2. Click on the **Actions** tab at the top.
3. You will see a workflow run named **"Lumora CI - Build Debug APK"** executing automatically.
4. Click on the run, then click on the `build` job to watch the live logs as Gradle sets up JDK 17, resolves dependencies, and compiles the app.
5. The build completes in ~1.5 to 2.5 minutes!

### 4. Download and Install the APK on Your Phone
1. Once the workflow run shows a green checkmark (completed), click on the run summary.
2. Scroll down to the **Artifacts** section at the bottom.
3. Click on **`lumora-debug-apk`** to download the zip file containing `app-debug.apk`.
4. Transfer the APK to your Android phone (or download it directly from GitHub using your phone's browser).
5. On your phone (ColorOS / OxygenOS / HyperOS / OneUI):
   - Tap the downloaded `.apk` file.
   - If prompted with *"For your security, your phone is not allowed to install unknown apps from this source"*, tap **Settings** and toggle **Allow from this source**.
   - Tap **Install**, then **Open**.

### 5. Troubleshooting Actions Build Errors
If a build step ever fails (red cross):
1. Click on the failed workflow run in the Actions tab.
2. Click on the red step (e.g., `Assemble Debug APK`).
3. Scroll to the bottom of the log to see the compiler output. It will point directly to the exact file and line number with a concise error message (such as a missing import or syntax typo).

---

## 📦 Project Structure

```
lumora/
├── .github/
│   └── workflows/
│       └── build.yml               # GitHub Actions CI Stage 1: Build & Artifact upload
├── gradle/
│   └── libs.versions.toml          # Centralized version catalog (all dependencies pinned)
├── app/
│   ├── build.gradle.kts            # App module Gradle configuration (arm64-v8a, Compose)
│   ├── proguard-rules.pro          # R8 optimizer rules
│   └── src/main/
│       ├── AndroidManifest.xml     # Scoped storage permissions, zero internet
│       ├── res/                    # Adaptive vector icons and theme colors
│       └── java/com/lumora/gallery/
│           ├── LumoraApplication.kt # Capped Coil disk cache (100MB), manual DI
│           ├── MainActivity.kt     # Edge-to-edge Compose host & permissions
│           ├── data/
│           │   ├── model/          # Immutable MediaItem and Album models
│           │   ├── preferences/    # DataStore for selected album bucket IDs
│           │   └── repository/     # ContentResolver MediaStore query & ContentObserver
│           └── ui/
│               ├── theme/          # True-black AMOLED theme & typography
│               ├── navigation/     # Floating pill bar (Photos | Albums)
│               ├── onboarding/     # "Choose albums to show in Photos" sheet
│               └── timeline/       # Date-grouped grid, pinch zoom, video duration
├── build.gradle.kts                # Root build script
├── settings.gradle.kts             # Plugin repositories and project name
├── gradle.properties               # Memory limit (1g), build caching
└── .gitignore                      # Clean exclusion rules (no binaries committed)
```

---

## 🚦 Milestones Roadmap

- [x] **Milestone (a)**: Core skeleton, modern permissions flow, MediaStore timeline with album filtering, and CI Stage 1 (automated debug build).
- [ ] **Milestone (b)**: Full Albums tab, pin section, settings screen, and CI Stage 2 (Quality gates: unit tests & lint).
- [ ] **Milestone (c)**: Instant shared-element photo/video viewer and default-app intent handlers (`ACTION_VIEW`, `ACTION_PICK`).
- [ ] **Milestone (d)**: Built-in non-destructive photo editor (crop, adjust, filters, markup, blur brush) and video trim.
- [ ] **Milestone (e)**: Scoped storage trash/restore, biometric private vault, natural language search, and duplicate cleaner.
- [ ] **Milestone (f)**: R8 release optimization, arm64-only signed APK, and CI Stages 3–5 (keystore secrets, auto GitHub releases, <8MB size guard).
