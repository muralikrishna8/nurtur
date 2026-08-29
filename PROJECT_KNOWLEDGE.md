# Nurtur Project Knowledge Document

This file is the working knowledge base for future prompts in this repository. Update it on every feature request or behavior change.

## 1) Product Intent

- **App Name:** Nurtur
- **Primary Use Case:** Track baby feeding sessions with low cognitive load for sleep-deprived parents.
- **Units:** All feeding amounts must be in **ml**.

## 2) Current Technical Baseline

- Android app using Kotlin and Jetpack Compose.
- Single-activity structure with bottom navigation:
  - Home
  - Analytics
  - Settings
- Layer boundaries:
  - `presentation` (UI + ViewModel)
  - `domain` (models, repository contracts, domain services)
  - `infrastructure` (Room, DataStore, repository implementations)

## 3) Data Model Baseline

`FeedLog` / Room entity fields:

- `id` (Long, auto-generated primary key)
- `remoteId` (String?, nullable for future Firebase sync)
- `startTime` (Long, timestamp)
- `endTime` (Long, timestamp)
- `amountOffered` (Int, ml)
- `amountConsumed` (Int, ml)
- `milkType` (String, default Formula)
- `notes` (String?, optional)

## 4) Implemented Feature Requirements Ledger

### Initial MVP

- Room persistence scaffolded (entity, DAO, database).
- Repository abstraction added for future local/remote composition.
- Home hero section shows time since last feed.
- Today snapshot displays:
  - total consumed ml
  - total wasted ml (`offered - consumed`, clamped at 0)
  - feed count
- Recent activity shows 5 most recent entries with swipe-to-delete.
- Log Feed popup supports save to Room.
- Analytics screen shows last 7 days grouped summary.
- Settings uses DataStore for default bottle size and milk type.

### Refactor Pass (DDD + UX)

- Epoch-millis text entry replaced with user-friendly date/time pickers.
- Codebase reorganized to stricter DDD package/folder boundaries.

### Validation Fix

- Log popup now stays open on validation failures (including missing `amountConsumed`).

### Feed Log Editing and Deletion

- Users can tap a recent feed row to open the form in edit mode with prefilled values.
- Edit mode supports updating incorrect log values and saving them back to the same record ID.
- Edit mode includes an explicit delete action in the popup for record removal.
- Existing validation rules remain enforced before update save.

### Amount Offered Input UX Fix

- Removed reactive auto-defaulting of `amountOfferedInput` while typing.
- Default bottle size is now applied only when starting a fresh new-entry flow, not during manual field edits.
- This prevents cleared input from being re-appended (for example `90` turning into `90120`).

### Dynamic Color-Coded Feed Timer

- Requirement requested: Surface urgency at a glance by color-coding "Time since last feed" against a configurable target interval.
- What changed:
  - Added a persisted settings field: `targetFeedIntervalMinutes` (DataStore-backed, default 180 minutes / 3 hours).
  - Added Settings input: "Target feed interval (hours)".
  - Home hero timer now computes a timer status (`SAFE`, `APPROACHING`, `OVERDUE`) via new domain service `FeedTimerStatusCalculator`.
  - Timer text color now changes in real time on the Home screen without refresh.
- Constraints/validation rules:
  - Settings repository clamps stored interval to `30..720` minutes.
  - ViewModel accepts only whole-hour input `1..12` before persisting.
  - Color thresholds:
    - Safe: elapsed more than 30 minutes before target -> theme default dark text.
    - Approaching: elapsed within 30 minutes before target -> amber (`#9C6A0C`).
    - Overdue: elapsed 30+ minutes past target -> red (`#B3261E`).
- Testing impact:
  - Added unit tests for timer status threshold transitions in `FeedTimerStatusCalculatorTest`.

### Consumed vs. Wasted Milk Analytics Chart

- Requirement requested:
  - Show a stacked 7-day chart for daily consumed vs wasted milk and allow tap-to-view exact daily breakdown.
- What changed:
  - Replaced the Analytics list view with a stacked bar chart UI in `AnalyticsScreen`.
  - Added consumed (solid primary color) and wasted (lighter primary tint) stacked sections per day.
  - Added bar tap interaction to show selected-day breakdown text (`Consumed Xml, Wasted Yml`).
  - Changed 7-day aggregation behavior to always include a fixed timeline of the last 7 calendar days.
  - Days without feeds now remain in the dataset and render as empty bar slots to preserve visual continuity.
- Constraints/validation rules:
  - Waste remains derived as `max(amountOffered - amountConsumed, 0)` to avoid negative values.
  - Aggregation uses local device timezone date boundaries and includes today plus previous 6 days.
- Testing impact:
  - Updated and expanded `FeedMetricsCalculatorTest` to verify:
    - seven-day fixed-size output,
    - zero-feed day preservation,
    - empty input behavior returning seven empty days.

### Analytics Axis and Gridline Readability

- Requirement requested:
  - Add a visible Y-axis and dotted horizontal guide lines so ml levels are easier to read from the stacked chart.
- What changed:
  - Added Y-axis labels (`0ml` to dynamic max) on the left side of the analytics chart.
  - Added dashed horizontal grid lines across the chart area, parallel to the X-axis.
  - Y-axis max now rounds up to the nearest 50ml step so ticks stay stable and readable.
- Constraints/validation rules:
  - Axis max has a safe minimum of `50ml` to prevent degenerate scale rendering on no-data weeks.
  - Guide lines and ticks use a fixed 4-segment scale for consistent visual interpretation.
- Testing impact:
  - Existing unit tests pass; no domain behavior changed in this iteration.

### App Logo Branding Update

- Requirement requested:
  - Replace the app logo using the provided high-resolution `nurtur-logo.png` and include the logo in project documentation.
- What changed:
  - Generated Android launcher icon assets from the source logo for all standard densities:
    - `mipmap-mdpi` (`48x48`)
    - `mipmap-hdpi` (`72x72`)
    - `mipmap-xhdpi` (`96x96`)
    - `mipmap-xxhdpi` (`144x144`)
    - `mipmap-xxxhdpi` (`192x192`)
  - Added both `ic_launcher.png` and `ic_launcher_round.png` in each density folder.
  - Updated `AndroidManifest.xml` to set `android:icon` and `android:roundIcon` explicitly to the generated launcher assets.
  - Added a documentation-sized logo asset at `docs/assets/nurtur-logo-320.png` and embedded it in `README.md`.
- Constraints/validation rules:
  - Launcher icon naming follows Android resource conventions (`ic_launcher`, `ic_launcher_round`) to ensure deterministic packaging.
  - Documentation uses a resized asset to avoid loading the original multi-megabyte image in README render paths.
- Testing impact:
  - No domain or validation logic changed.
  - Recommended verification is visual: confirm launcher icon renders on emulator/device and README logo displays correctly.

### Adaptive Dark Theme with Manual Override

- Requirement requested:
  - Support dark theme automatically based on OS preference and allow users to switch between light and dark themes in Settings.
- What changed:
  - Added a new domain-level theme preference enum: `ThemeMode` with `SYSTEM`, `LIGHT`, and `DARK`.
  - Extended `SettingsState` with persisted `themeMode`.
  - Extended `SettingsRepository` and `DataStoreSettingsRepository` to store and update `theme_mode` via DataStore.
  - Updated app composition so `NurturTheme` is driven by `settings.themeMode`.
  - Added settings UI control to choose theme mode from `System`, `Light`, and `Dark`.
  - Added deterministic resolver logic so `SYSTEM` maps to current OS dark preference and explicit modes always override it.
- Constraints/validation rules:
  - Stored theme values are enum-name based; invalid or unknown stored values safely fall back to `SYSTEM`.
  - Settings UI only emits constrained enum options rather than free-form input.
- Testing impact:
  - Added `ThemeResolverTest` to verify all theme resolution branches (`SYSTEM`, `LIGHT`, `DARK`) and OS preference behavior.

## 5) Open Decisions / Next Features

- Phase 2 Firebase Firestore sync:
  - Add remote datasource implementation
  - Keep domain repository contracts stable
  - Introduce sync conflict strategy and offline-first policy
- Improve form UX with Material date/time picker components in Compose
- Add fuller test coverage:
  - ViewModel validation and state transitions
  - Repository mapping tests
  - UI tests for dialog save/validation behavior
  - UI tests for timer color transitions across safe/approaching/overdue states

## 6) Update Protocol (Mandatory for Every Prompt)

For each new prompt that modifies behavior, data, architecture, or UX:

1. Add a new subsection under **Implemented Feature Requirements Ledger** with:
   - requirement requested
   - what changed
   - any constraints/validation rules
2. Update **Open Decisions / Next Features** if priorities changed.
3. If setup or usage changed, also update `README.md`.

## 7) Prompt Handoff Template

Use this template when preparing context for the next prompt:

- **Goal:** <feature or fix requested>
- **Files likely impacted:** <paths>
- **Architecture impact:** <presentation/domain/infrastructure notes>
- **Validation/security impact:** <input constraints, data safety>
- **Testing impact:** <tests to add/update>
