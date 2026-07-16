# V1.2.0 UI Refresh Regression and Archive Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Verify that the V1.2.0 UI replacement preserves every existing bookkeeping entry point and core workflow, fix only UI regressions, and produce an installable archived debug APK without changing protected business logic or stored data.

**Architecture:** Treat the current dirty worktree as the release candidate because it contains the UI replacement being validated. Audit Compose navigation callbacks against the existing ViewModel/repository APIs, use existing unit tests for parsers, backup/restore, deduplication and statistics, then perform non-destructive ADB smoke tests on the connected device. Version-only edits are limited to display/build metadata and release documentation.

**Tech Stack:** Android, Kotlin, Jetpack Compose, Room, Gradle, JUnit, ADB, Android UI Automator.

---

### Task 1: Establish the regression baseline

**Files:**
- Inspect: `app/src/main/java/com/localbookkeeping/app/MainActivity.kt`
- Inspect: `app/src/main/java/com/localbookkeeping/app/BookkeepingViewModel.kt`
- Inspect: `app/src/main/java/com/localbookkeeping/app/data/AppDatabase.kt`
- Inspect: `app/src/main/java/com/localbookkeeping/app/backup/BackupManager.kt`
- Inspect: `app/src/test/java/**/*.kt`

**Step 1:** Record the dirty worktree and current version without modifying or resetting existing files.

**Step 2:** Map bottom tabs and all requested subpage entry points to their Compose callbacks and screen enum destinations.

**Step 3:** Map protected operations—notification listening, amount parsing, deduplication, backup restore and Room schema—to existing implementation and tests.

**Step 4:** Verify the Room schema version and migration files remain unchanged by any V1.2.0 edits.

### Task 2: Run automated regression checks

**Files:**
- Test: `app/src/test/java/**/*.kt`
- Output: `app/build/test-results/testDebugUnitTest/`
- Output: `app/build/outputs/apk/debug/app-debug.apk`

**Step 1:** Run `gradlew.bat compileDebugKotlin --no-daemon` and require a successful Kotlin compile.

**Step 2:** Run `gradlew.bat testDebugUnitTest --no-daemon` and require zero failed or errored tests.

**Step 3:** Run `gradlew.bat assembleDebug --no-daemon` and require a fresh APK.

**Step 4:** Parse the JUnit XML directly and record exact totals.

### Task 3: Perform non-destructive device regression

**Files:**
- Output: `test-artifacts/2026-07-16-v1.2.0-regression/`

**Step 1:** Confirm the connected device identity and install with `adb install -r app-debug.apk` so application data is retained.

**Step 2:** Verify the four bottom tabs and capture their UI hierarchy/screenshots.

**Step 3:** Open recent bill detail, pending bills, manual entry, tools, statistics ranges, daily limit and listener diagnostics without saving, deleting, confirming, ignoring, restoring or changing settings.

**Step 4:** Verify system back and top back return to the parent screen instead of exiting from subpages.

**Step 5:** Confirm the foreground listener service/notification listener state and scan Logcat for crashes or ANRs.

**Step 6:** Confirm the existing Room database remains present after installation and testing.

### Task 4: Fix only confirmed UI regressions

**Files:**
- Modify if required: `app/src/main/java/com/localbookkeeping/app/MainActivity.kt`
- Test if required: existing focused test file under `app/src/test/java/`

**Step 1:** Reproduce each confirmed issue before editing.

**Step 2:** Make the smallest UI/navigation correction without changing repository, parser, listener, backup or database code.

**Step 3:** Re-run the focused test, full unit test suite and affected device flow.

### Task 5: Archive V1.2.0 UI Refresh

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/localbookkeeping/app/MainActivity.kt`
- Modify: `CHANGELOG.md`
- Modify: `PROJECT_LOG.md`
- Create: `test-artifacts/2026-07-16-v1.2.0-regression/REPORT.md`

**Step 1:** Set `versionName = "1.2.0"`, `versionCode = 120`, and the visible version label to `V1.2.0` only after regression checks pass.

**Step 2:** Document retained entry points, protected components, exact test results, device results, APK hash and any remaining non-blocking warnings.

**Step 3:** Re-run compile, unit tests and assemble against the final version metadata.

**Step 4:** Reinstall the final APK with `adb install -r`, verify the displayed/build version and leave the app in a safe main-screen state.

**Step 5:** Run `git diff --check` and provide the final archive recommendation without creating a commit or tag unless explicitly requested.
