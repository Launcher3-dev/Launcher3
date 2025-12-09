# CLAUDE.md - Launcher3 AOSP 13+ Development Guide for AI Assistants

## Project Overview

**Project Name**: Launcher3 for AOSP (Android Open Source Project)
**Base Version**: android16-s2-release (based on Android 16, but supports Android 13+)
**Repository**: https://android.googlesource.com/platform/packages/apps/Launcher3/
**Language**: Mixed Java and Kotlin
**Build System**: Gradle (8.13.1) + Android.bp (Soong) for AOSP integration
**Package**: `com.android.launcher3`

### Purpose
Launcher3 is the default home screen and app launcher for AOSP-based Android devices. It provides:
- Home screen with app icons, widgets, and folders
- App drawer (all apps list)
- Quickstep: Recent apps/multitasking interface
- Search functionality
- Desktop mode support
- Taskbar support

## Codebase Structure

### Root Directory Organization

```
Launcher3Aosp13/
├── src/                          # Main launcher source (Java/Kotlin)
├── quickstep/                    # Quickstep (recents) implementation
├── src_no_quickstep/            # Fallback when Quickstep is disabled
├── src_plugins/                 # Plugin interfaces
├── src_build_config/            # Build configuration sources
├── src_ui_overrides/            # UI override sources (without Quickstep)
├── res/                         # Main resources
├── tests/                       # Test suite
│   ├── src/                     # Test sources
│   ├── tapl/                    # Test Automation Platform Library
│   └── multivalentTests/        # Multi-variant tests
├── go/                          # Launcher3Go variant (Android Go)
├── animationlib/                # Animation library module
├── iconloaderlib/              # Icon loading library module
├── shared/                      # Shared utilities module
├── wm_shared/                   # Window Manager shared code
├── msdllib/                     # MSDL (Multi-Sensory Design Language) module
├── flagslib/                    # Feature flags module
├── compose/                     # Jetpack Compose integration
├── protos/                      # Protocol buffer definitions
├── prebuilts/                   # Prebuilt JAR dependencies
├── tools/                       # Build and development tools
├── checks/                      # Lint checks
├── Android.bp                   # AOSP build configuration
├── build.gradle                 # Gradle build configuration
└── settings.gradle              # Gradle module settings
```

### Key Source Directories

#### Main Launcher (`src/com/android/launcher3/`)
Core launcher components:
- `Launcher.java` - Main activity (entry point)
- `LauncherApplication.java` - Application class
- `Workspace.java` - Home screen workspace
- `CellLayout.java` - Grid layout for icons
- `Hotseat.java` - Bottom dock
- `DeviceProfile.java` - Device-specific UI configuration
- `BubbleTextView.java` - App icon with label
- `Folder.java` - Folder implementation
- `model/` - Data models and loading
- `graphics/` - Graphics and icon rendering
- `allapps/` - App drawer implementation
- `touch/` - Touch handling and gestures
- `widget/` - Widget support
- `anim/` - Animations
- `util/` - Utility classes
- `pm/` - Package manager integration
- `logging/` - Analytics and logging

#### Quickstep (`quickstep/src/com/android/launcher3/`)
Recent apps and multitasking:
- `RecentsActivity.java` - Recents container
- `TaskView.java` - Individual task card
- `TaskbarActivity.java` - Taskbar implementation
- `taskbar/` - Taskbar controllers and views
- `uioverrides/` - Quickstep UI overrides
- `touch/` - Gesture navigation
- `dagger/` - Dependency injection modules
- Desktop mode support
- Split screen support
- Bubble bar support

### Build Variants

The project supports multiple build flavors:

#### Flavor Dimensions
1. **App Dimension**:
   - `aosp` - Standard AOSP launcher
   - `l3go` - Launcher3Go (Android Go edition)

2. **Recents Dimension**:
   - `withQuickstep` - Full Quickstep recent apps UI
   - `withoutQuickstep` - Legacy/simple recents

#### Build Variants
- `aospWithQuickstep` - Standard launcher with Quickstep
- `aospWithoutQuickstep` - Standard launcher without Quickstep
- `l3goWithQuickstep` - Go launcher with Quickstep
- `l3goWithoutQuickstep` - Go launcher without Quickstep

### Module Dependencies

The project is organized into multiple Gradle modules:

```
Main App
├── IconLoader (:IconLoader → iconloaderlib/)
├── Animation (:Animation → animationlib/)
├── Shared (:Shared → shared/)
├── WMShared (:WMShared → wm_shared/)
├── msdl (:msdl → msdllib/)
└── flags (:flags → flagslib/)
```

## Technology Stack

### Languages
- **Java** (targetCompatibility: Java 21)
- **Kotlin** (2.2.21) with coroutines
- JVM default compatibility: `-Xjvm-default=all`

### Build Tools
- Gradle: 8.13.1
- Android Gradle Plugin: 8.13.1
- Kotlin Gradle Plugin: 2.2.21
- KSP (Kotlin Symbol Processing): 2.3.3
- Protobuf Gradle Plugin: 0.9.5

### Key Dependencies

#### AndroidX Libraries
- AppCompat
- Core KTX
- RecyclerView
- DynamicAnimation
- Preference
- Slice Builders
- Graphics Shapes
- Window API
- Material3 (Jetpack Compose)
- Lifecycle (common-java8, extensions, runtime-ktx)

#### Dependency Injection
- Dagger 2 (2.56.2) with KAPT

#### Async/Concurrency
- Kotlin Coroutines (kotlinx-coroutines-android)

#### Serialization
- Protobuf JavaLite (4.33.1)

#### UI/Animation
- Lottie (animations)
- Material Design Components

#### Testing
- JUnit
- Mockito + Dexmaker
- AndroidX Test (runner, rules, uiautomator)

#### System Libraries (Prebuilt)
- SystemUI Shared Library
- WindowManager Shell Shared
- Plugin Core Library
- Platform Animation Library
- View Capture Library
- Feature Flags Libraries (launcher3, wm_shell, systemui)

## Development Workflows

### Building the Project

#### Standard Gradle Build
```bash
# Build all variants
./gradlew build

# Build specific variant
./gradlew assembleAospWithQuickstepDebug

# Clean build
./gradlew clean build

# Run tests
./gradlew test
./gradlew connectedAndroidTest
```

#### AOSP Build (using Android.bp)
```bash
# From AOSP root
m Launcher3QuickStep      # Standard Quickstep launcher
m Launcher3               # Basic launcher
m Launcher3Go             # Go launcher
```

### Build Configuration

#### SDK Versions
- `minSdkVersion`: 33 (Android 13)
- `targetSdkVersion`: 36 (Android 16)
- `compileSdkVersion`: android-36

#### Build Config Fields
Located in `build.gradle`, these flags control features:
- `IS_STUDIO_BUILD`: false
- `QSB_ON_FIRST_SCREEN`: true (Quick Search Box)
- `IS_DEBUG_DEVICE`: false
- `WIDGET_ON_FIRST_SCREEN`: true
- `WIDGETS_ENABLED`: true
- `NOTIFICATION_DOTS_ENABLED`: true

### Code Organization Conventions

#### Package Structure
```
com.android.launcher3/
├── (root) - Core launcher classes
├── model/ - Data models and loading
├── graphics/ - Graphics and rendering
├── allapps/ - App drawer
├── folder/ - Folder implementation
├── widget/ - Widgets
├── touch/ - Touch and gestures
├── anim/ - Animations
├── util/ - Utilities
├── logging/ - Analytics
├── pm/ - Package manager
├── config/ - Configuration
├── states/ - State management
└── views/ - Custom views
```

#### File Naming
- Activities: `*Activity.java` (e.g., `Launcher.java`, `RecentsActivity.java`)
- Views: `*View.java` (e.g., `BubbleTextView.java`, `TaskView.java`)
- Controllers: `*Controller.java` (e.g., `TaskbarController.java`)
- Models: `*Info.java` for data classes (e.g., `ItemInfo.java`, `AppInfo.java`)
- Utilities: `*Util.java` or `*Utils.java`
- Kotlin files: Use same conventions with `.kt` extension

#### Code Style
- Java: Follow AOSP code style
- Kotlin: Use Kotlin coding conventions
- Indentation: 4 spaces (no tabs)
- Line length: Prefer 100 characters
- Use `@Override` annotations
- Add Javadoc/KDoc for public APIs

### Protobuf Usage

The project uses Protocol Buffers for data serialization:

#### Proto Files
- `protos/*.proto` - Main proto definitions
- `protos_overrides/*.proto` - Override definitions
- `quickstep/protos_overrides/*.proto` - Quickstep-specific protos

#### Generated Code
- Build task: `generateProto`
- Output: `build/generated/source/proto/`
- Type: Lite (mobile-optimized)

### Testing

#### Test Structure
- `tests/src/` - Instrumentation tests
- `tests/tapl/` - Test Automation Platform Library (UI testing framework)
- `tests/multivalentTests/` - Tests across variants
- Unit tests: In module directories (e.g., `animationlib/tests/`)

#### Running Tests
```bash
# All tests
./gradlew test connectedAndroidTest

# Specific test class
./gradlew test --tests com.android.launcher3.ClassName

# TAPL tests (UI automation)
adb shell am instrument -w com.android.launcher3.tests/androidx.test.runner.AndroidJUnitRunner
```

### Dependency Management

#### Framework JARs
Located in `prebuilts/libs/`:
- `framework-16.jar` - Android framework (compile-only)
- `SystemUI-core.jar` - SystemUI core components
- `SystemUI-statsd.jar` - StatsD integration
- `sysui_shared.jar` - SystemUI shared library
- `WindowManager-Shell-shared.jar` - WM Shell shared
- Various feature flag libraries

#### Adding Dependencies
1. Check if available in Maven/Google repositories
2. Add to `build.gradle` dependencies block
3. Sync Gradle
4. For framework JARs, place in `prebuilts/libs/` and reference

### Feature Flags

The project uses multiple feature flag systems:

#### Flag Libraries
- `com_android_launcher3_flags` - Launcher3 flags
- `com_android_wm_shell_flags` - WindowManager Shell flags
- `com_android_systemui_flags` - SystemUI flags
- `com_android_systemui_shared_flags` - SystemUI shared flags

#### Usage
```kotlin
// Check feature flag
if (FeatureFlags.ENABLE_NEW_FEATURE.get()) {
    // Feature-specific code
}
```

### Git Workflow

#### Branches
- `main` - android16-s2-release (Android 16)
- `Launcher3-15-qc` - Qualcomm Android 15
- `Launcher3-15-s1-release` - Android 15
- `Launcher3-14-s2-release` - Android 14
- `Launcher3-13` - Android 13 (base for this repo)
- Various version-specific branches

#### Current Branch
- Working on: `claude/claude-md-mixzf7lcxsd8twfo-015j4Q48NKcmqLRRwAJ5Zxba`

#### Committing Changes
```bash
# Stage changes
git add .

# Commit with descriptive message
git commit -m "Brief description of changes"

# Push to branch
git push -u origin claude/claude-md-mixzf7lcxsd8twfo-015j4Q48NKcmqLRRwAJ5Zxba
```

## Key Development Patterns

### 1. Dependency Injection (Dagger 2)

The project uses Dagger 2 extensively in Quickstep:

```kotlin
// Module definition
@Module
class AppModule {
    @Provides
    @Singleton
    fun provideContext(app: Application): Context = app
}

// Component
@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {
    fun inject(activity: LauncherActivity)
}
```

### 2. State Management

Launcher uses a state machine pattern:

```java
// LauncherState defines different UI states
// - NORMAL: Home screen
// - ALL_APPS: App drawer
// - OVERVIEW: Recent apps
// - HINT_STATE: Gesture hints

// Transition between states
launcher.getStateManager().goToState(LauncherState.ALL_APPS);
```

### 3. Model-View-Controller Pattern

- **Model**: `model/` package - Data loading and management
- **View**: Custom view classes extending Android views
- **Controller**: Controller classes managing view behavior

### 4. Touch Handling

Complex gesture system in `touch/` package:
- `AbstractStateChangeTouchController` - Base for touch controllers
- `PortraitStatesTouchController` - Portrait mode gestures
- `LandscapeStatesTouchController` - Landscape gestures

### 5. Animation Framework

Custom animation system:
- `AnimatorSet` for complex animations
- `StateAnimationConfig` for state transitions
- `PendingAnimation` for collecting animations
- Interpolators in `anim/Interpolators.java`

### 6. Device Profile System

`DeviceProfile.java` and `InvariantDeviceProfile.java`:
- Calculates UI dimensions based on device
- Handles different form factors (phone, tablet, foldable)
- Grid size configuration
- Icon size calculations

### 7. Plugin System

Located in `src_plugins/`:
- Define plugin interfaces
- Launcher listens for plugin implementations
- Allows customization without modifying core code

### 8. Compose Integration

Optional Jetpack Compose support:
- Enabled via `release_enable_compose_in_launcher` flag
- Facades in `compose/facade/`
- Features in `compose/features/`
- Disabled by default for stability

## Important Files to Know

### Configuration Files
- `Android.bp` - AOSP build configuration (Soong)
- `build.gradle` - Gradle build configuration
- `settings.gradle` - Module configuration
- `gradle.properties` - Gradle properties and versions
- `proguard.flags` - ProGuard/R8 rules
- `local.properties` - Local SDK paths (auto-generated)

### Manifest Files
- `AndroidManifest.xml` - Base manifest (without Quickstep)
- `AndroidManifest-common.xml` - Common manifest entries
- `quickstep/AndroidManifest.xml` - Quickstep additions
- `quickstep/AndroidManifest-launcher.xml` - Launcher-specific Quickstep
- `go/AndroidManifest.xml` - Android Go additions

### Resource Files
- `res/` - Main resources (layouts, drawables, values)
- `quickstep/res/` - Quickstep-specific resources
- `go/res/` - Android Go resources

### Entry Points
- `src/com/android/launcher3/Launcher.java` - Main launcher activity
- `quickstep/src/com/android/launcher3/RecentsActivity.java` - Recents
- `quickstep/src/com/android/launcher3/uioverrides/QuickstepLauncher.java` - Quickstep launcher
- `src/com/android/launcher3/LauncherApplication.java` - Application class

## AI Assistant Guidelines

### When Making Changes

1. **Understand the Build Variant**: Know which variant you're modifying (aosp vs l3go, with/without Quickstep)

2. **Respect the Architecture**:
   - Don't mix UI logic with model code
   - Use controllers for complex UI behavior
   - Follow MVC patterns

3. **Handle Both Java and Kotlin**:
   - New code can be Kotlin (preferred for new features)
   - Maintain existing Java code in Java unless refactoring
   - Use appropriate null-safety (`@Nullable`, `@NonNull` in Java; `?` in Kotlin)

4. **Test Thoroughly**:
   - Run unit tests: `./gradlew test`
   - Run instrumentation tests: `./gradlew connectedAndroidTest`
   - Test on different build variants
   - Test with Quickstep enabled and disabled

5. **Consider Performance**:
   - Launcher is performance-critical (home screen)
   - Avoid heavy operations on main thread
   - Use background threads for loading
   - Optimize animations (60 FPS target)

6. **Follow AOSP Guidelines**:
   - Check AOSP code style: https://source.android.com/docs/setup/contribute/code-style
   - Add license headers to new files
   - Use proper visibility modifiers

7. **Handle Configuration Changes**:
   - Launcher handles orientation, screen size, fold/unfold
   - Test rotation and configuration changes
   - Use `DeviceProfile` for layout calculations

8. **Accessibility**:
   - Add content descriptions to views
   - Support TalkBack
   - Keyboard navigation support

9. **Localization**:
   - Use string resources (no hardcoded strings)
   - Support RTL (right-to-left) languages
   - Test with different locales

10. **Dependency Updates**:
    - Update `gradle.properties` for version changes
    - Sync prebuilt JARs with AOSP versions
    - Test after dependency updates

### Common Tasks

#### Adding a New Feature
1. Create feature flag in `flagslib/` if needed
2. Implement in appropriate package
3. Add resources to `res/`
4. Update manifest if needed
5. Add tests in `tests/`
6. Document in comments

#### Fixing a Bug
1. Identify the build variant affected
2. Write a failing test first (TDD)
3. Implement fix
4. Verify test passes
5. Test related functionality
6. Check for regressions

#### Refactoring Code
1. Ensure tests exist and pass
2. Make incremental changes
3. Run tests after each change
4. Keep backward compatibility
5. Update documentation

#### Adding Resources
1. Add to appropriate `res/` directory
2. Consider variant-specific resources (e.g., `quickstep/res/`)
3. Support different densities (drawable-mdpi, -hdpi, etc.)
4. Add translations for strings

### Debugging Tips

1. **Enable Debug Logging**:
   - Use `Log.d()`, `Log.v()` for debug logs
   - Check ProtoLog for Quickstep logs

2. **Layout Inspector**: Use Android Studio's Layout Inspector

3. **Device Profile Info**: Check `DeviceProfile.dump()` for layout info

4. **State Machine**: Log state transitions to understand flow

5. **Touch Events**: Enable touch event logging in `MotionEventsUtils`

### Code Review Checklist

- [ ] Code follows AOSP style guidelines
- [ ] No hardcoded strings (use resources)
- [ ] Null safety annotations/checks
- [ ] Performance considerations (no main thread blocking)
- [ ] Accessibility support (content descriptions)
- [ ] Tests added/updated
- [ ] Works on different build variants
- [ ] Configuration change handling (rotation)
- [ ] RTL support
- [ ] License header on new files
- [ ] Comments for complex logic
- [ ] No compiler warnings

## External Resources

### Official Documentation
- AOSP Launcher3: https://cs.android.com/android/platform/superproject/+/master:packages/apps/Launcher3/
- Android Source: https://source.android.com/
- Android Developer Docs: https://developer.android.com/

### Tutorials (Chinese)
Referenced in README.md - series by 墨香 on Launcher3 (based on Android 6.0):
1. Overview: http://www.codemx.cn/2016/07/30/Launcher01/
2. Data Loading: http://www.codemx.cn/2016/08/05/Launcher02/
3. Binding: http://www.codemx.cn/2016/08/14/Launcher03/
4. App Install/Update: http://www.codemx.cn/2016/08/21/Launcher04/
5. Workspace Sliding: http://www.codemx.cn/2016/10/16/Launcher05/
6. Drag and Drop: http://www.codemx.cn/2016/11/21/Launcher06/
7. Widgets: http://www.codemx.cn/2016/12/18/Launcher07/
8. Icons/Wallpapers: http://www.codemx.cn/2017/05/19/Launcher08/

Note: These tutorials are based on Android 6.0 Launcher3. Modern versions differ significantly but core concepts remain.

### Dependencies
- Dagger 2: https://dagger.dev/
- Kotlin Coroutines: https://kotlinlang.org/docs/coroutines-overview.html
- Protobuf: https://github.com/protocolbuffers/protobuf
- Lottie: https://airbnb.io/lottie/

## Recent Updates

**Latest Update**: 2025-10-14
- Base: android16-s2-release
- Source: https://android.googlesource.com/platform/packages/apps/Launcher3/+/refs/heads/android16-s2-release

**Recent Commits**:
- 946c706: update depence
- 654821d: 添加手势注释 (Add gesture annotations)
- bbf3dbb: update libs
- 12a6c41/aa1088d: Update README.md

## Notes for AI Assistants

1. **This is a large, complex project**: Take time to understand the module you're working on before making changes.

2. **Multiple entry points**: Understand whether you're modifying Launcher (home screen) or Quickstep (recents).

3. **Performance matters**: This runs on the home screen - every millisecond counts.

4. **AOSP integration**: Changes should work both with Gradle (development) and Android.bp (AOSP build).

5. **Backward compatibility**: Support multiple Android versions and form factors.

6. **Chinese documentation**: Some comments and the README are in Chinese. Use translation if needed.

7. **Prebuilt dependencies**: Many system libraries are prebuilt JARs in `prebuilts/libs/` - these come from AOSP framework.

8. **Feature flags**: Check if new features should be behind feature flags.

9. **Variant-specific code**: Use source sets for variant-specific implementations.

10. **Documentation**: Update this CLAUDE.md when project structure or conventions change significantly.

---

**Last Updated**: 2025-12-09
**Generated by**: Claude AI Assistant
**Purpose**: Guide AI assistants in understanding and modifying this codebase effectively
