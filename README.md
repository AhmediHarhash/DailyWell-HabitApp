# DailyWell - Habit-Based Health App

A minimalist health app focused on building 7 core habits with simple Yes/No tracking.

## Philosophy

**Consistency beats perfection. Simple beats complex.**

We don't track calories, sleep stages, or step counts. We ask one question: "Did you do it? Yes or No."

## The 7 Core Habits

| Habit | What Counts |
|-------|-------------|
| 😴 Sleep | 7+ hours |
| 💧 Water | 8+ glasses |
| 🏃 Move | 30+ minutes |
| 🥬 Vegetables | Ate some today |
| 🧘 Calm | Any stress relief practice |
| 💬 Connect | Talked to someone |
| 📵 Unplug | Screen-free before bed |

## Tech Stack

- **Language:** Kotlin
- **UI:** Compose Multiplatform
- **Architecture:** MVVM with Koin DI
- **Storage:** DataStore Preferences
- **Platforms:** Android (iOS ready)

## Project Structure

```
habit-health/
├── shared/                    # Shared Kotlin Multiplatform code
│   └── src/
│       ├── commonMain/        # Common code (UI, models, logic)
│       ├── androidMain/       # Android-specific implementations
│       └── iosMain/           # iOS-specific implementations
├── androidApp/                # Android application
└── iosApp/                    # iOS application (Xcode project)
```

## Building

### Prerequisites

- Android Studio Arctic Fox or later
- JDK 17+
- Android SDK 26+

### Build Android App

```bash
# Debug build
./gradlew :androidApp:assembleDebug

# Install on connected device/emulator
./gradlew :androidApp:installDebug

# Release build (requires signing)
./gradlew :androidApp:bundleRelease
```

### Build iOS App (macOS only)

```bash
# Open in Xcode
open iosApp/iosApp.xcodeproj
```

## Features

### Free Tier
- Track 3 of 7 habits
- Binary Yes/No check-in
- Current streak display
- Weekly calendar view
- Daily reminder

### Premium ($2.99/mo or $19.99 lifetime)
- All 7 habits
- Up to 3 custom habits
- Full historical data
- Insights & correlations
- Achievement badges
- Weekly reflections

## App Store Optimization

**Title:** DailyWell: 7 Health Habits
**Subtitle:** Simple Yes/No Tracking

**Keywords:**
- simple habit tracker
- minimalist health app
- basic health habits
- yes no habit tracker
- daily wellness tracker

## License

Proprietary - All rights reserved

---

Built with the philosophy: "Health doesn't have to be complicated."
