# Robert's Week 3 work

## Scope

This work covers independently testable parts of SAD-33, SAD-36, and SAD-39. It does not complete teammates' repositories, editors, permission prompts, or detail screens. Kotlin and Jetpack Compose are the implementation stack; Flutter was officially scrapped by the project owner on September 15, 2026.

## SAD-33: appointment details

The screen renders caller-provided appointment fields and loading/missing states, emits edit/back callbacks, and scrolls long content at enlarged text sizes. Cancellation confirmation is scoped to the selected appointment so it cannot carry over to a different record.

Integration still requires RJ's appointment repository and editor, SAD-78 and SAD-80. Verify selection loads the correct persisted record, save changes survive reload, and back returns to the appointment list. Cancellation persistence and feedback belong to Katie's SAD-35.

### Update after syncing origin/main at 35a7dcf

The incoming appointment model now includes Firestore conversion and tests. The incoming appointment form's layout was combined with the existing local edit-prefill, identity, validation, and Cancel behavior. Appointment persistence is still absent from the running flow, so SAD-33 remains open pending repository integration.

The sync also incorporated Firestore medication storage, medication removal and reminder actions, and profile editing. Local Week 3 components, tests, comments, and unfinished care flows were retained. The pre-sync recovery snapshot is the Git stash named `pre-origin-main-sync-2026-09-16`; keep it until the combined work has been reviewed. Local changes remain uncommitted.

Post-sync `testDebugUnitTest assembleDebug assembleDebugAndroidTest --no-configuration-cache` passed with 56 unit tests and no failures. Device tests were not rerun during this sync. The incoming Android Gradle plugin 9.1.0 warns that compile SDK 37.1 exceeds its tested range. No plugin or SDK versions were changed locally to hide that warning.

Post-sync `lintDebug --no-configuration-cache --max-workers=1` completed analysis but failed with one error and 21 warnings. `InvalidFragmentVersionForActivityResult` flags the incoming `registerForActivityResult` permission launcher in MainActivity. Dependency changes or suppression were not included in this synchronization task.

### Branch sync on September 17, 2026

Saved local progress in checkpoint `8cedce5`, then merged `origin/main` at `dfccc5b`, bringing in seven commits. Resolved conflicts in MainActivity and the medication list by retaining local care routes and reminder replacement/cancellation while incorporating the incoming medication details, removal, edit/back navigation, and save feedback. The incoming schedule editor and button remain unfinished upstream: the button has no activity callback, and the editor has no medication loader.

`testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug --no-configuration-cache --max-workers=1` passed the unit tests and both APK builds. Lint still reports the previously documented `InvalidFragmentVersionForActivityResult` error and 21 warnings. Device tests were not run. The Firestore authorization and integration limitations in `docs/code-map.md` remain unresolved. This merge saves and synchronizes progress; it does not certify release readiness.

## SAD-36: appointment reminders

Jira's title requests reminders, but its description duplicates SAD-35 cancellation. The scheduling component follows the title. The issue's final acceptance criteria still need clarification before closure.

`AppointmentReminderScheduler` receives an appointment and an explicit future epoch timestamp in milliseconds. The caller must apply the agreed reminder lead time and time-zone policy. No default lead time is assumed.

`AndroidAppointmentReminderAlarms` creates one private alarm per patient/appointment. Scheduling replaces the previous alarm. Cancel is safe to repeat and clears delivered notifications too. Past times, completed/cancelled appointments, and denied notification permission do not produce new alarms. Delivery rechecks permission and uses generic notification text.

After teammates submit their code:

1. Construct the scheduler with `AndroidAppointmentReminderAlarms(context)`.
2. After a successful appointment save/edit, call `schedule(appointment, triggerAtMillis)` and handle the result. Platform exceptions must be handled by the calling flow.
3. After successful cancellation/removal, call `cancel(appointment)`.
4. Connect Katie's notification permission flow, SAD-26. Retry scheduling only after permission is granted.
5. Resolve reminder lead time, time-zone changes, notification tap destination, and restoration after reboot or logout/account changes. These are not implemented by this standalone component.
6. Demonstrate delivery, replacement, cancellation, and denied permission on a device. Alarms are inexact, matching the current medication implementation; they do not promise delivery at an exact second.

The component is not invoked by MainActivity yet. This prevents scheduling against temporary memory-only appointments.

## SAD-39: health-concern list

The screen displays supplied concerns, sorts higher severity first, combines status/severity filters, and renders loading/error/empty states. The whole card, including its title, emits the selected record. Filters stack vertically when scaled labels cannot fit a row, and the whole screen scrolls for narrow displays and large text.

Integration still requires RJ's health-concern repository, SAD-81, and Timothy's detail screen, SAD-82. MainActivity's selection callback remains empty until the detail screen is available. Supply patient-scoped records and actual loading/error state from the repository; then connect selection to the detail route. Do not treat isolated callback tests as proof of navigation or persistence.

## Verification

`AppointmentReminderSchedulerTest` uses a fake alarm store and a fixed clock to test scheduling, replacement, cancellation, permission denial, identity isolation, and invalid/past appointments.

`Week3ScreensTest` uses isolated Compose content to test fields, callbacks, filters, states, and 320 dp layouts at 200% text size. It does not start Firebase or require teammates' implementations.

Run `gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug`, then `gradlew.bat connectedDebugAndroidTest` with a device or emulator attached. Successful compilation alone is not a device test.

### Results on September 15, 2026

- `gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest --offline`: passed, 38 unit tests with no failures, including four appointment-reminder tests.
- `gradlew.bat lintDebug --offline --no-configuration-cache --max-workers=1`: passed, zero errors and 16 warnings in existing configuration/resources. An earlier lint run crashed internally; the serial retry succeeded.
- Seven `Week3ScreensTest` tests passed on `Medium_Phone_API_36.0`, including the 320 dp, 200% text cases. Tests verify behavior and visibility; no separate visual screenshot review was performed.
- Gradle's connected-test task could not resolve an uncached UTP dependency offline. The compiled APKs were installed on the emulator and the same tests ran successfully using `adb shell am instrument -w -e class com.example.carelink.Week3ScreensTest com.example.carelink.test/androidx.test.runner.AndroidJUnitRunner`.
- End-to-end appointment/concern storage, actual reminder delivery, and teammate-owned navigation remain unverified and blocked on integration. These Jira tasks must remain open.
