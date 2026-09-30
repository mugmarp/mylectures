# Developer Guide — Lectures

**Application Name**: **Lectures**  
**Author & Lead Engineer**: **MUGENDAWALA MARK PAUL** ([TiralLab](https://tirallab.page))  
**GitHub**: [@mugmarp](https://github.com/mugmarp) • **Email**: [markpaulmu@gmail.com](mailto:markpaulmu@gmail.com)

This guide assists software engineers and contributors in setting up, maintaining, and extending **Lectures**.

---

## 1. Environment Setup

### Required Tools
* **JDK**: OpenJDK 17 or Eclipse Adoptium Temurin 21
* **Android SDK**: Build tools 35.0.0, Compile SDK 35, Min SDK 26 (Android 8.0 Oreo)
* **Gradle**: Gradle 9.x using Kotlin DSL (`.gradle.kts`)
* **IDE**: Android Studio Ladybug (2024.2+) or higher with Jetpack Compose support

### Build Commands
```bash
# Clean and assemble debug APK
gradle assembleDebug

# Compile verification check
gradle compileDebugKotlin

# Run unit tests
gradle :app:testDebugUnitTest

# Run code style and Android lint checks
gradle check
```

---

## 2. Adding a New Academic Programme or Timetable Dataset

Timetable schedules are bundled inside the assets folder:
* Location: `app/src/main/assets/timetable_entries.json`

### JSON Structure:
```json
[
  {
    "courseCode": "BCS 2101",
    "courseTitle": "Data Structures & Algorithms",
    "dayOfWeek": "Monday",
    "startTime": "08:00",
    "endTime": "10:00",
    "room": "LT 2",
    "lecturer": "Dr. Sarah Namubiru",
    "sessionType": "Lecture",
    "programmeGroup": "Faculty of Computing - BCS Year 2 Sem 1"
  }
]
```

### Steps to Add:
1. Append new JSON entries with the corresponding `programmeGroup` string.
2. In `app/src/main/java/com/mustime/features/onboarding/UniversityDirectory.kt`, ensure the faculty, programme name, and years are registered in the directory maps.
3. Upon launch, `DataLoader.kt` automatically verifies hashes and updates SQLite via Room.

---

## 3. UI Styling & Material 3 Theming

* **Theme Definitions**: `app/src/main/java/com/mustime/ui/theme/Theme.kt`
* **Color System**:
  * Primary Blue: `Color(0xFF0052CC)`
  * Dark Surface Base: `Color(0xFF0F172A)`
  * Dark Card Base: `Color(0xFF1E293B)`
  * Light Surface Base: `Color(0xFFF8FAFC)`
* **Always respect dark mode**: Check `LocalAppTheme.current.isDark` or use `MaterialTheme.colorScheme` tokens.
* **Component Touch Targets**: All interactive icons, chips, and buttons must adhere to the 48dp minimum touch target size.

---

## 4. Performance Guidelines

When developing new features:
1. **Never use `IntrinsicSize.Min` or `IntrinsicSize.Max` inside `LazyColumn` items**:
   * Use `Box` with `Modifier.matchParentSize()` for decorative backgrounds and accent bars to preserve single-pass layout rendering.
2. **Avoid Full-Screen Pointer Gesture Interceptors**:
   * Do not place horizontal drag listeners over root containers holding vertical scrollers.
3. **Always supply stable keys to `items()` in Lazy lists**:
   * Example: `items(notes, key = { it.naturalKey }) { ... }`
4. **Wrap expensive calculations in `remember`**:
   * Date formatting, filtering, and regex operations should be memoized with their inputs.
