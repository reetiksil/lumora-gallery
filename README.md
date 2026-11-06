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

## 📱 How to Set Lumora as Your Default Gallery

On ColorOS / OxygenOS / HyperOS / Stock Android:
1. Open phone **Settings** → **Apps** (or **App Management**).
2. Tap **Default Apps**.
3. Tap **Gallery** (or **Photos**).
4. Select **Lumora**.

Now, when you take a photo in your stock camera app and tap the circular preview thumbnail, Android instantly launches Lumora's high-speed viewer with swipe access to your recent camera photos!

---

## 🛠️ Getting Started Without Android Studio

You do **not** need a high-end laptop, Android Studio, or an emulator. All compilation, unit tests, lint analysis, and APK packaging happen in the cloud on GitHub's fast runners.

### 1. Check Your Local Environment
Open your terminal (PowerShell, Command Prompt, or bash) and verify Git and Java:
```bash
git --version
javac -version
```
- **Java Check**: If `javac -version` returns a version (e.g. `javac 17` or `javac 21`), you have a full Java compiler. Even if you only have a JRE or a different JDK version, **GitHub Actions explicitly provisions JDK 17 for all builds**, ensuring 100% reproducible environments.
- **VS Code**: Edit code in [VS Code](https://code.visualstudio.com/) with syntax highlighting.

### 2. Enable GitHub Actions on Your Repository
If you just created the repository or Actions has not run yet:
1. Go to your repository on GitHub: `https://github.com/reetiksil/lumora-gallery/actions`
2. If GitHub shows **"Workflows aren't being run on this repository"**, click the green button: **"I understand my workflows, go ahead and enable them"**.
3. Now every `git push` to `main` will automatically build your app and test it!

### 3. Push Code to GitHub
On your local machine in your project directory:
```bash
git add .
git commit -m "feat(milestone-c): media viewer, video player, EXIF sheet, and default gallery intents"
git push origin main
```

### 4. Watch the Stage 2 Quality Gates & Download APK
1. Open your repository on GitHub and click the **Actions** tab.
2. Click on the active workflow **"Lumora CI - Quality Gates & Build"**.
3. Once completed, scroll to **Artifacts** at the bottom of the summary page to download:
   - **`lumora-debug-apk`**: The installable APK for your phone.
   - **`unit-test-report`**: Interactive HTML report detailing test pass/fail status and execution times.
   - **`lint-report`**: Comprehensive HTML report of Android Lint code analysis.

---

## 📦 Project Structure

```
.
├── .github/
│   └── workflows/
│       └── build.yml               # GitHub Actions CI Stage 2: Quality Gates (Tests, Lint, APK)
├── gradle/
│   └── libs.versions.toml          # Centralized version catalog (all dependencies pinned)
├── app/
│   ├── build.gradle.kts            # App module config (arm64-v8a, resource minification, Compose)
│   ├── proguard-rules.pro          # R8 full-mode optimizer rules
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml # Scoped storage permissions, default gallery intent filters
│   │   │   ├── res/                # Adaptive vector icons and theme colors
│   │   │   └── java/com/lumora/gallery/
│   │   │       ├── LumoraApplication.kt # Capped Coil disk cache (100MB), manual DI
│   │   │       ├── MainActivity.kt      # Screen router, Intent handler, PiP, scoped delete
│   │   │       ├── data/
│   │   │       │   ├── model/           # MediaItem, Album, ExifMetadata, SortOrder, ThemeMode
│   │   │       │   ├── preferences/     # DataStore repository (bucket IDs, settings)
│   │   │       │   ├── repository/      # ContentResolver queries, single-URI resolver, cache manager
│   │   │       │   └── util/            # ExifReader, pure media filtering, sorting & date grouping
│   │   │       └── ui/
│   │   │           ├── theme/           # True-black AMOLED theme & typography
│   │   │           ├── navigation/      # Floating pill bar (Photos | Albums)
│   │   │           ├── onboarding/      # First-launch album selection sheet
│   │   │           ├── timeline/        # Date-grouped grid, pinch zoom, duration badges
│   │   │           ├── albums/          # Pinned cards, All Albums list, Album Detail grid
│   │   │           ├── viewer/          # MediaViewerScreen, ZoomableImage, VideoPlayer, MediaInfoSheet
│   │   │           ├── editor/          # PhotoEditorScreen, ColorMatrixGenerator, ImageSaverUtil, ImageDecoderUtil
│   │   │           └── settings/        # Photos sources, Cache manager, Sort order
│   │   └── test/java/com/lumora/gallery/
│   │       ├── MediaGroupingTest.kt     # Unit tests for album filtering, sorting, date headers
│   │       ├── MediaViewerTest.kt       # Unit tests for video duration, EXIF formatting & metadata
│   │       └── PhotoEditorTest.kt       # Unit tests for color matrix math, filters, undo/redo, ratios
│   ├── build.gradle.kts                 # Root build script
│   ├── settings.gradle.kts              # Plugin repositories and project name
│   ├── gradle.properties                # Memory limit (1g), build caching, R8 full mode
│   └── .gitignore                       # Clean exclusion rules (no binaries committed)
```

---

## 🚦 Milestones Roadmap

- [x] **Milestone (a)**: Core skeleton, modern permissions flow, MediaStore timeline with album filtering, and CI Stage 1 (automated debug build).
- [x] **Milestone (b)**: Full Albums tab (pinned cards, long-press actions, album detail grid), Settings screen (Photos tab sources, cache limits, clear cache), and CI Stage 2 (Quality gates: unit tests & lint).
- [x] **Milestone (c)**: Interactive Media Viewer (pinch/double-tap zoom, swipe down to dismiss, swipe between items, EXIF metadata sheet), Media3 video playback with transport controls & PiP, and Default Gallery intent filters (`ACTION_VIEW`, `ACTION_PICK`, `ACTION_GET_CONTENT`, `ACTION_SET_WALLPAPER`).
- [x] **Milestone (d)**: Built-in non-destructive photo editor (free/fixed aspect ratio crops, 90° rotation, horizontal/vertical flips, 17 ColorMatrix filters with live intensity slider, brightness/contrast/saturation/warmth/exposure/vignette sliders, freehand markup brush with privacy blur/highlighter/eraser, text overlays with styling, downscaled preview decoding with zero OOM risk, and high-resolution export dialog with JPEG/PNG/WebP format & quality control).
- [x] **Milestone (e)**: Soothing HyperOS / AMOLED UI polish:
  - **Fluid Gestures**: Horizontal swipe navigation between Photos and Albums tabs via `HorizontalPager`.
  - **Pinch & Zoom Engine**: Butter-smooth pinch-to-zoom with spring boundaries and animated double-tap zoom (1x to 2.5x); zero choppiness.
  - **Media Viewer Swiping**: Seamless left/right horizontal swipe between photos/videos, swipe UP to display EXIF details bottom sheet, swipe DOWN to dismiss.
  - **Album Section Customization**: Dedicated bottom-left button in Albums tab opening the HyperOS-style "Custom" arrange & hide albums bottom sheet (reorder & toggle More albums, Featured, Media type, More).
  - **Smart Search with Suggestions**: Top search bar with Cancel action, instant suggestion chips for Times (with photo preview cards), Smart Categories (Cards & documents, QR codes, Electronics, Transport, Screenshots, Videos), and Places (with location pins), plus multi-field filtering on date, time of day, location, and content.
  - **Theme System**: Full theme personalization in Settings supporting AMOLED Dark (Pure black), Light (HyperOS clean light), and Follow System.
  - **Reliable 30-Day Trash Vault**: Fix for deletion crash and fully functional Recently Deleted screen with 30-day countdown timers, one-tap Restoration back to gallery, and permanent Delete Forever.
- [x] **Milestone (f)**: Release engineering & distribution:
  - **R8 Full-Mode Optimizer**: Bytecode minification, resource shrinking, dead-code elimination, and ARM64 single-architecture targets.
  - **Size Regression CI Guard**: Automated CI gate verifying that release APK size is strictly `< 8 MB`.
  - **Release Signing**: Production-ready signing configuration in Gradle and CI.
  - **Automated GitHub Releases**: Pushing git tags (e.g. `v0.6.0`) automatically creates GitHub Releases with release & debug APK binaries attached.
