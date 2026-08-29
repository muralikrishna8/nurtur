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
