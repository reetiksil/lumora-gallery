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

You do **not** need a high-end laptop, Android Studio, or an emulator. All compilation, unit tests, lint analysis, and APK packaging happen in the cloud on GitHub's fast runners.

### 1. Check Your Local Environment
Open your terminal (PowerShell, Command Prompt, or bash) and verify Git and Java:
```bash
git --version
javac -version
```
- **Java Check**: If `javac -version` returns a version (e.g. `javac 17` or `javac 25`), you have a full Java compiler. Even if you only have a JRE or a different JDK version, **GitHub Actions explicitly provisions JDK 17 for all builds**, ensuring 100% reproducible environments.
- **VS Code**: Edit code in [VS Code](https://code.visualstudio.com/) with syntax highlighting.

### 2. Push Code to GitHub
On your local machine in your `lumora` project directory:
```bash
git add .
git commit -m "feat(milestone-b): albums tab, settings, unit tests, and Stage 2 CI quality gates"
git push origin main
```

### 3. Watch the Stage 2 Quality Gates & Build
1. Open your repository on GitHub and click the **Actions** tab.
2. Click on the active workflow **"Lumora CI - Quality Gates & Build"**.
3. Watch the sequential execution:
   - **Quality Gate 1 (Unit Tests)**: Executes JVM unit tests in `app/src/test/` verifying filtering, sorting, date grouping, and cache formatting.
   - **Quality Gate 2 (Android Lint)**: Performs static code analysis checking for security pitfalls, deprecations, and resource leaks.
   - **Assemble Debug APK**: Packages `lumora-debug-apk` **only after both quality gates pass**.

### 4. Download Reports and the APK
In the workflow run summary under **Artifacts**:
- **`lumora-debug-apk`**: The installable APK for your phone.
- **`unit-test-report`**: Interactive HTML report detailing test pass/fail status and execution times.
- **`lint-report`**: Comprehensive HTML report of Android Lint code analysis.

---

## 📦 Project Structure

```
lumora/
├── .github/
│   └── workflows/
│       └── build.yml               # GitHub Actions CI Stage 2: Quality Gates (Tests, Lint, APK)
├── gradle/
│   └── libs.versions.toml          # Centralized version catalog (all dependencies pinned)
├── app/
│   ├── build.gradle.kts            # App module config (arm64-v8a, Compose, testOptions, lint)
│   ├── proguard-rules.pro          # R8 optimizer rules
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml # Scoped storage permissions, zero internet
│   │   │   ├── res/                # Adaptive vector icons and theme colors
│   │   │   └── java/com/lumora/gallery/
│   │   │       ├── LumoraApplication.kt # Capped Coil disk cache (100MB), manual DI
│   │   │       ├── MainActivity.kt      # Screen router (Photos, Albums, Detail, Settings)
│   │   │       ├── data/
│   │   │       │   ├── model/           # MediaItem, Album, SortOrder, ThemeMode
│   │   │       │   ├── preferences/     # DataStore repository (bucket IDs, settings)
│   │   │       │   ├── repository/      # ContentResolver queries & cache manager
│   │   │       │   └── util/            # Pure media filtering, sorting & date grouping
│   │   │       └── ui/
│   │   │           ├── theme/           # True-black AMOLED theme & typography
│   │   │           ├── navigation/      # Floating pill bar (Photos | Albums)
│   │   │           ├── onboarding/      # First-launch album selection sheet
│   │   │           ├── timeline/        # Date-grouped grid, pinch zoom, duration badges
│   │   │           ├── albums/          # Pinned cards, All Albums list, Album Detail grid
│   │   │           └── settings/        # Photos sources, Cache manager, Sort order
│   │   └── test/java/com/lumora/gallery/
│   │       └── MediaGroupingTest.kt     # Unit tests for album filtering, sorting, date headers
│   ├── build.gradle.kts                 # Root build script
│   ├── settings.gradle.kts              # Plugin repositories and project name
│   ├── gradle.properties                # Memory limit (1g), build caching
│   └── .gitignore                       # Clean exclusion rules (no binaries committed)
```

---

## 🚦 Milestones Roadmap

- [x] **Milestone (a)**: Core skeleton, modern permissions flow, MediaStore timeline with album filtering, and CI Stage 1 (automated debug build).
- [x] **Milestone (b)**: Full Albums tab (pinned cards, long-press actions, album detail grid), Settings screen (Photos tab sources, cache limits, clear cache), and CI Stage 2 (Quality gates: unit tests & lint).
- [ ] **Milestone (c)**: Instant shared-element photo/video viewer and default-app intent handlers (`ACTION_VIEW`, `ACTION_PICK`).
- [ ] **Milestone (d)**: Built-in non-destructive photo editor (crop, adjust, filters, markup, blur brush) and video trim.
- [ ] **Milestone (e)**: Scoped storage trash/restore, biometric private vault, natural language search, and duplicate cleaner.
- [ ] **Milestone (f)**: R8 release optimization, arm64-only signed APK, and CI Stages 3–5 (keystore secrets, auto GitHub releases, <8MB size guard).
