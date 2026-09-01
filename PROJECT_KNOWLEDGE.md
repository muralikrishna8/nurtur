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

### Averages and Intake Trendline Analytics

- Requirement requested:
  - Show average feed volume and average time between feeds, overlay an intake trendline on the daily chart, and hide derived insights when data is insufficient.
- What changed:
  - Added a new domain model `AnalyticsInsights` to carry average metrics and smoothed consumed-volume trend data.
  - Added `FeedMetricsCalculator.buildAveragesAndTrend(...)` to compute:
    - `Average Volume per Feed` from qualifying feeds only (`amountConsumed >= 10ml`).
    - `Average Time Between Feeds` as the mean of `(next.startTime - previous.endTime)` across qualifying sequential feeds.
    - A smoothed moving-average trend series for consumed daily volume over the active 7-day analytics window.
  - Updated `FeedViewModel` to build analytics from the selected dashboard window (last 7 days) and expose `analyticsInsights`.
  - Updated `AnalyticsScreen` to:
    - render two summary text fields below the chart,
    - format duration values for readability,
    - overlay a smoothed trendline on top of the existing daily volume bars.
  - Added insufficient-data behavior: when the selected window has fewer than 3 logged feeds, trendline is hidden and both averages display `--`.
- Constraints/validation rules:
  - Outlier filtering excludes feeds with `amountConsumed < 10ml` from average calculations.
  - Interval duration is clamped to non-negative values to avoid invalid time gaps from malformed chronological inputs.
  - Trendline rendering requires at least 3 points and is suppressed in low-sample windows to reduce misleading interpretation risk.
- Testing impact:
  - Expanded `FeedMetricsCalculatorTest` with coverage for:
    - outlier exclusion impact on both averages,
    - insufficient-data fallback state,
    - all-feeds-under-threshold behavior for averages while preserving trend generation when feed count threshold is met.

### Custom Date-Range Filtering on Analytics

- Requirement requested:
  - Allow parents to choose an exact analytics date window (manual range and quick presets) so charts and metrics can be reviewed between specific milestones.
- What changed:
  - Added analytics date-range state to `FeedUiState` (`analyticsStartDate`, `analyticsEndDate`, `analyticsQuickFilterDays`).
  - Added `FeedViewModel.updateAnalyticsDateRange(...)` and `FeedViewModel.applyAnalyticsQuickFilter(...)` for manual and one-tap range updates.
  - Updated analytics data derivation in `FeedViewModel` to filter feeds by the selected inclusive date window and immediately recompute summary + insights.
  - Extended `FeedMetricsCalculator` with `buildDailySummary(...)` for variable-length windows while preserving `buildSevenDaySummary(...)` compatibility.
  - Updated `AnalyticsScreen` with a top date selector button, Material 3 `DateRangePicker` modal, and quick filter chips for `Last 7 Days`, `Last 14 Days`, and `Last 30 Days`.
- Constraints/validation rules:
  - Date range selection is bounded to `today` (future dates are disabled in picker and clamped in ViewModel).
  - Manual apply requires both start and end dates and rejects `end < start`.
  - Quick filters always map to inclusive windows ending at today:
    - 7 days -> `today-6` to `today`
    - 14 days -> `today-13` to `today`
    - 30 days -> `today-29` to `today`
- Testing impact:
  - Added tests for `buildDailySummary(...)` inclusive custom-range behavior.
  - Added tests for custom trendline point count in `buildAveragesAndTrend(...)` across a 14-day window.

### Analytics Date Picker Apply UX and Wide-Range Chart Legibility

- Requirement requested:
  - Make custom date-range application explicit when selecting dates, and prevent X-axis label squeeze for windows longer than one week.
- What changed:
  - Reworked Analytics date picker dialog actions to include visible in-modal `Cancel` and `Apply` controls within dialog content.
  - Kept one-tap quick filters (`Last 7/14/30 Days`) and made them apply immediately.
  - Added horizontal chart scrolling for selected windows larger than 7 days.
  - Extended X-axis labels to show full date text (instead of 3-letter day abbreviations) when range size exceeds one week.
- Constraints/validation rules:
  - `Apply` is enabled only when both start and end dates are selected.
  - Date apply still enforces no future dates and `end >= start`.
  - Horizontal scroll is activated only when visible range exceeds 7 days to preserve compact view for short windows.
- Testing impact:
  - No domain-calculation behavior changed; existing unit tests continue to validate range aggregation and trend sizing.

### Analytics Scroll Affordance and Axis Legibility Polish

- Requirement requested:
  - Add edge-fade scroll hints, restore dotted guide lines, and prevent multiline X-axis labels from shrinking the plotted graph area.
- What changed:
  - Added left/right fade overlays on the chart viewport that appear only when additional horizontal content is available to scroll.
  - Moved horizontal dotted guide-line rendering into a dedicated plot-area layer so guides remain visible and aligned while scrolling.
  - Split chart layout into two vertical zones: fixed-height plot area + separate fixed-height label area.
  - Kept extended-range labels at up to two lines while preventing them from consuming bar/trend plotting height.
- Constraints/validation rules:
  - Fade hints are enabled only when selected range exceeds 7 days and the viewport can scroll in that direction.
  - Plot area and trendline now share the same bounded height, preventing label overflow from distorting Y-axis scaling visuals.
- Testing impact:
  - UI-only rendering update; no domain logic changes.

### Analytics Gridline Layering Correction

- Requirement requested:
  - Ensure horizontal dotted guide lines remain visible after scroll and label-layout refactors.
- What changed:
  - Reverted the overlay-layer rendering approach after validating theme-specific contrast as the core issue.
  - Restored plot-area background guide-line rendering and tuned dark-mode contrast using adaptive color selection.
- Constraints/validation rules:
  - Guide lines still use the fixed 4-segment Y-axis scale and full plot width across the scrollable canvas.
  - Dark surfaces use `onSurface`-derived guide-line tint for stronger visibility; light surfaces keep `outlineVariant` styling.
- Testing impact:
  - UI rendering only; no domain or aggregation logic changes.

### Analytics Axis Baseline and Scroll-State Fix

- Requirement requested:
  - Keep X-axis labels from encroaching on graph semantics and ensure the 7-day view always shows all days after switching from longer, scrolled ranges.
- What changed:
  - Aligned Y-axis tick labels with the plot baseline by replacing fixed Y-axis bottom padding with dynamic padding equal to the dedicated X-axis label area height.
  - Added a reset for horizontal scroll position when the chart is in non-extended mode (7 days or fewer), preventing residual offset from previous long-range scrolling.
- Constraints/validation rules:
  - Scroll reset applies only when extended scrolling is disabled, preserving user position for long-range windows.
  - Baseline alignment is driven by shared layout constants, keeping `0ml` tick and plot floor synchronized.
- Testing impact:
  - UI layout/state behavior change only; no domain calculation changes.

### Seven-Day Chart Fit Guarantee

- Requirement requested:
  - Ensure the 7-day analytics chart always displays all seven days without horizontal scrolling.
- What changed:
  - Updated chart width behavior to use full available viewport width when selected range is 7 days or fewer.
  - Retained explicit widened scrollable width only for ranges greater than 7 days.
- Constraints/validation rules:
  - Non-extended mode (<= 7 days) never relies on horizontal scrolling and must render all day labels/bars within the card width.
  - Extended mode (> 7 days) continues to use horizontal scroll with fade-edge hints.
- Testing impact:
  - UI rendering/layout behavior update only; no domain or metrics computation changes.

### Seven-Day Data Visibility Regression Fix

- Requirement requested:
  - Restore visible bars/labels in 7-day mode after scroll-layout changes.
- What changed:
  - Applied horizontal scrolling modifier only when the selected range exceeds 7 days.
  - Removed scroll-container measurement behavior from non-extended mode to preserve normal width constraints.
- Constraints/validation rules:
  - `<= 7 days`: no horizontal scroll modifier, full-width chart measurement.
  - `> 7 days`: horizontal scroll remains enabled with widened chart canvas.
- Testing impact:
  - UI layout behavior fix only; analytics calculations unchanged.

### Recent Activity Capacity Expansion

- Requirement requested:
  - In the Home screen recent activity list, show the last 30 feed entries instead of only 5.
- What changed:
  - Updated `FeedViewModel` to request `observeRecentFeeds(limit = 30)` using a dedicated constant.
  - Kept repository and DAO contracts unchanged; only the bounded query limit at the presentation orchestration layer was updated.
  - Updated `README.md` MVP feature text to reflect the new 30-entry behavior.
- Constraints/validation rules:
  - Recent activity remains explicitly bounded (30) to avoid unbounded list rendering and query growth.
  - Ordering semantics remain unchanged (most recent first from existing repository/DAO behavior).
- Testing impact:
  - Added `FeedViewModelTest` that verifies `observeRecentFeeds` is called with a limit of 30 during ViewModel initialization.

### Remove Swipe-to-Delete from Recent Activity

- Requirement requested:
  - Remove swipe-to-delete from the Home screen recent activity list because it causes accidental deletions and is rarely used intentionally.
- What changed:
  - Removed `SwipeToDismissBox` behavior from Home recent activity rows.
  - Recent activity rows are now tap-to-open only, preserving edit/delete flows through the existing log editor dialog.
  - Removed now-unused `onDeleteFeed` callback parameter from `HomeScreen` and its call site in `NurturApp`.
- Constraints/validation rules:
  - Deletion remains an explicit action available from the editor dialog, not from list gestures.
  - Recent activity ordering and list size behavior remain unchanged.
- Testing impact:
  - UX interaction behavior changed; no domain calculation or persistence contracts were modified.
  - Existing unit tests continue to validate data/logic paths; recommend manual UI verification to confirm swipe no longer triggers deletion.

### Recent Activity Immediate Refresh Fix

- Requirement requested:
  - After saving a new activity, the Home recent activity list should refresh immediately without requiring scroll or tab switch.
- What changed:
  - Wrapped the Home `LazyColumn` in a Compose `key(...)` tied to recent feed identity (`firstOrNull()?.id` and list size).
  - This forces list subtree refresh when new feed items are added, ensuring immediate redraw when `recentFeeds` emits.
- Constraints/validation rules:
  - Feed ordering, item tap behavior, and edit/delete flows remain unchanged.
  - The fix is presentation-layer only and does not modify repository or persistence behavior.
- Testing impact:
  - UI behavior fix; domain/unit logic unchanged.
  - Recommended manual verification: add a feed and confirm it appears instantly at the top of Recent Activity.

### UI Revamp v2 (Modern Parent-Friendly Refresh)

- Requirement requested:
  - Modernize the app UI across Home, Log Feed, Analytics, and Settings to a friendlier Material 3 presentation while preserving existing feed tracking behavior and validation.
- What changed:
  - Added explicit app design tokens for light/dark color schemes and updated typography scale in the theme layer.
  - Locked v2 brand palette to soft plum anchored on Primary `#4A4458` (dark Primary `#CFC3E6`), with matching surface, warning, error, and success tokens.
  - Updated app shell bottom navigation styling to consistently use themed surface/inset behavior.
  - Rebuilt Home presentation with centered app bar, refined hero timer card, daily snapshot card with progress ring, and expanded/collapsing extended FAB behavior tied to list scroll.
  - Migrated feed entry UX from `AlertDialog` to `ModalBottomSheet` while preserving add/edit/delete flows and form validation.
  - Kept log semantics as `Offered` + `Consumed` with derived read-only `Wasted milk`.
  - Modernized Analytics layout with dedicated top app bar and inline quick-filter chips (7D/14D/30D) while preserving existing date-range and chart calculation behavior.
  - Reworked Settings into sectioned cards, switched theme choice to segmented controls, switched default milk type to segmented controls, and moved target interval editing to a bounded slider.
  - Added non-interactive v2 data-management placeholders (`Export Data`, `Delete All Data`) labeled as coming soon.
- Constraints/validation rules:
  - Existing ViewModel validation behavior remains authoritative for feed save/update:
    - numeric parse required,
    - `endTime >= startTime`,
    - offered range `1..1000`,
    - consumed range `0..offered`.
  - Recent feed list remains tap-to-edit only with no swipe-delete on Home.
  - Target interval remains bounded by existing hour constraints (`1..12`) via Settings -> ViewModel persistence.
  - Analytics quick filters remain constrained to supported windows (7/14/30); no mandatory always-on `All` range was introduced.
- Testing impact:
  - Ran `./gradlew test` successfully after UI refactor.
  - Existing domain and ViewModel unit tests remain passing, validating unchanged business logic and validation paths.
  - Recommended manual regression checks:
    - Home FAB expand/collapse behavior on scroll,
    - Log bottom sheet open/edit/save/delete behavior,
    - Theme segmented toggle persistence and immediate UI application,
    - Analytics quick filter updates and chart readability in both themes.

### Home Screen Mockup Alignment (Soft Plum)

- Requirement requested:
  - Align Home presentation with the provided light/dark mockups using the soft plum palette.
- What changed:
  - Rebuilt Home header as left-aligned `Nurtur` title with decorative profile icon action.
  - Replaced hero timer card with Primary Container surface, status badge (`On track` / `Approaching` / `Overdue`), last-feed subtext, and optional gentle-warning copy.
  - Replaced single snapshot card + ring with three equal Daily Snapshot metric cards (`Consumed`, `Wasted`, `Feeds`); wasted values use Warning color.
  - Redesigned Recent Feeds rows with milk-type icon tile, type + relative timestamp, consumed volume, and `Clean feed` / wasted status styling.
  - Switched FAB to a square Primary Container `+` button.
  - Tuned bottom navigation selected indicator/colors to Primary Container / Primary for visual parity with the mock.
- Constraints/validation rules:
  - Tap-to-edit only remains enforced for recent feeds; no swipe-delete on Home.
  - Timer status thresholds remain driven by existing `FeedTimerStatusCalculator`.
  - Profile icon is visual-only and does not introduce a new settings route.
- Testing impact:
  - Presentation-only change; domain/unit tests unchanged.
  - Recommended manual verification of light and dark Home states for badge colors, wasted highlighting, and FAB placement.

### Analytics Mockup Alignment (Consumed vs Wasted + Period Stats)

- Requirement requested:
  - Align the Analytics screen with the provided light/dark mockups while keeping chart axis labels, guide lines, and the average trend line.
- What changed:
  - Restyled Analytics with a left-aligned `Analytics` title, pill quick filters (`7D` / `14D` / `30D`), and a calendar action for the existing custom date-range picker.
  - Wrapped the chart in a titled card (`Consumed vs. Wasted`) with total-volume label and Consumed/Wasted legend.
  - Updated stacked bar colors only: Consumed uses Primary tokens; Wasted uses Warning tokens in both themes. Y-axis labels, dashed guide lines, X-axis labels, and the smoothed average trend line remain.
  - Replaced the previous text averages block with a 2x2 period stats grid (`Avg Consumed`, `Avg Wasted`, `Total Feeds`, `Avg Feeds`); Avg Wasted uses Warning color.
- Constraints/validation rules:
  - Quick filters remain limited to 7/14/30 day windows; custom inclusive date ranges remain available via the calendar action.
  - Chart scroll behavior is unchanged (`<= 7` days full-width, `> 7` days horizontally scrollable with edge fades).
  - Period stats are derived from the currently selected `DailyAnalytics` window (presentation aggregation only).
- Testing impact:
  - Added `AnalyticsPeriodStatsTest` for period aggregation and display formatting helpers.
  - Domain/ViewModel analytics calculation behavior unchanged.
  - Recommended manual verification of light and dark Analytics: filter selection, chart colors/axes/trend line, and stats grid values.

### Log Feed Mockup Alignment (Offered/Consumed Number Inputs)

- Requirement requested:
  - Align the Log Feed / Edit Feed bottom sheet with the updated mockup for both light and dark mode, and ensure Offered/Consumed use number input type.
- What changed:
  - Restyled `LogFeedDialog` as a themed `ModalBottomSheet` with uppercase field labels, clock-trailing date/time fields, icon milk-type segmented control (`Breast` display / `Breastmilk` stored), side-by-side Offered/Consumed fields, warning-colored auto-calculated wasted milk, multiline notes placeholder, and primary Save / red Delete Feed Entry actions.
  - Edit mode shows a relative "Logged …" subtitle; create and edit share the same field order with Time of Completion directly under Time of Feed, and both modes display full date+time in those fields.
  - Offered and Consumed inputs use `KeyboardType.Number` with digit-only sanitization (max 4 digits).
  - All text inputs and the inactive milk-type segment use `LightBackground` / `DarkBackground` container colors; wasted milk uses Warning tokens so primary actions remain accessible in both themes.
  - Milk-type segmented control, date/time fields, and volume fields share a 56dp control height for visual alignment.
- Constraints/validation rules:
  - Domain persistence values remain unchanged (`Breastmilk` / `Formula`).
  - Existing ViewModel validation remains authoritative (`endTime >= startTime`, offered `1..1000`, consumed `0..offered`).
  - Notes length cap remains 280 characters in the ViewModel.
- Testing impact:
  - Presentation-only change; domain/unit tests unchanged.
  - Recommended manual verification: create and edit flows in light and dark mode, number keyboard on volume fields, wasted auto-update, and delete entry affordance on edit.

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
