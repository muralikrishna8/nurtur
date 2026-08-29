# Nurtur (FeedTracker)

Nurtur is an Android baby feeding tracker that helps parents quickly log feeds, monitor milk intake and waste, and review trends over time.

## Context

- **Platform:** Android
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material Design 3
- **Local Storage:** Room
- **State Management:** ViewModel + Coroutines/Flow
- **Settings Storage:** DataStore (Preferences)
- **Architecture:** DDD-leaning layers (`presentation` -> `domain` -> `infrastructure`)
- **Future Plan:** Firebase Firestore sync (Phase 2) through repository abstraction

## Prerequisites

- Android Studio (latest stable recommended)
- Android SDK 34
- JDK 17
- Gradle 8.x

## Quick Start

1. Open this folder in Android Studio.
2. Let Gradle sync the project.
3. Run the `app` configuration on an emulator/device.

If you prefer CLI and have Gradle available:

```bash
gradle :app:assembleDebug
```

## Current MVP Features

- Home tab with:
  - Time since last feed hero section
  - Today snapshot (consumed ml, wasted ml, feed count)
  - Recent feed activity list (latest 5)
  - Swipe-to-delete feed entries
- Log Feed popup with:
  - Date/time pickers for start and end time
  - Offered/consumed inputs in ml
  - Dynamic wasted milk calculation
  - Milk type selection (Formula/Breastmilk)
  - Optional notes
  - Validation that keeps popup open on invalid input
- Analytics tab:
  - Last 7 days grouped summary (consumed, wasted, feed count)
- Settings tab:
  - Default bottle size (ml)
  - Default milk type

## Architecture Overview

```mermaid
flowchart LR
    A[Presentation\nCompose Screens + ViewModel]
    B[Domain\nModels + Repository Interfaces + Services]
    C[Infrastructure\nRoom + DataStore + Repository Implementations]

    A --> B
    C --> B
```

## Documentation Workflow

Use `PROJECT_KNOWLEDGE.md` as the persistent prompt/feature memory file.

When adding or changing a feature, update both:

1. `README.md` -> `Current MVP Features` and any setup changes.
2. `PROJECT_KNOWLEDGE.md` -> requirement log, design decisions, and prompt context.

## Common Issues

- **Gradle command not found:** use Android Studio bundled Gradle, or install Gradle locally.
- **SDK mismatch errors:** confirm Android SDK 34 and JDK 17 are selected in Android Studio.
