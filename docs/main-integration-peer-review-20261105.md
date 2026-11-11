# Main integration and peer review, November 5, 2026

## Integration

Compared GitHub main `f525aac` with Robert's `bab3707`. The common base was `e60efd4`; only two new implementation commits needed integration. Earlier work from all group members was already in Robert's history and remains present. Merge commit `1946210` includes both parents.

| Group member | Work retained | Integration adjustment |
| --- | --- | --- |
| RJ Vogel | Repository integration for medication reads, deletion, medication saving, refill loading/submission, and health concerns | Medication and care-history saving use one repository batch. Refill callbacks adapt the existing validated transaction and cache-aware reads. The health-concern repository implements the existing observable contract, preserving live changes, listener disposal, appointment validation, and existing screen/test consumers. |
| Katie | Owner isolation tests for profile, medication, appointment, concern, task, dose, and immutable medication-history data; Firebase emulator configuration | Retained all authorization cases. Aligned the Android debug Firestore port with the new 8180 configuration. |
| Timothy Jackson and earlier group changes | Existing history, dashboard, reminder, secure-storage error handling, notifications, permissions, navigation, and CI work | Already ancestors of Robert's branch; preserved rather than applied twice. |

Robert's loading feedback, return navigation, theme, layout, and readable concern severity changes remain intact. Removed an incoming duplicate medication save that would run independently of the medication/history batch and navigate before the batch completed. Kept notification permission recovery and the medication editor's return destination.

## Three peer reviews

Reviews ran after the merge commit. Reviewers inspected code without editing it or running competing device tests.

| Reviewer | Finding | Fix |
| --- | --- | --- |
| Data correctness | Remembered active refill data could indefinitely block a new request after completion elsewhere | The production form lets the transaction check current request status. Existing transaction validation still rejects actual duplicates and returns a readable error. |
| Data correctness and UI | An open refill dialog kept a stale request object after refresh or removal | Store only its ID; derive current details from the latest list and dismiss on removal, loading, or error. |
| Data correctness | The callback appointment-linking API bypassed target validation | Adapt it to the existing transaction that verifies the concern and appointment. |
| UI | Appointment title, provider, location, and notes stayed editable during saving | Disable all four while saving, and enable them again afterward. |
| UI | Missing refill timestamps appeared as dates in 1969/1970 | Show Date unavailable in both the card and dialog. |
| Security and tests | Android debug routing still used Firestore port 8080 | Use 8180, document it, and add an executable configuration consistency check. |
| Security and tests | Logout erased encrypted reminder metadata before cancelling alarms and visible notifications | A tested local-session cleanup function cancels reminders before clearing secure storage. Logout still stops if secure storage cleanup fails. |
| Security and tests | Refill results and delayed reads could cross account changes | Key refill state by authenticated UID, clear it before loading, and reject callbacks for a different UID. Guard delayed appointment, dose-history, and dashboard task reads too. |

## Validation

The four added UI regression cases failed against the old UI, confirming stale details, retained dialogs after errors, false dates, and editable fields during saving. The emulator configuration check failed with 8080 versus 8180 before the routing fix and passed afterward.

Commands used:

```powershell
node scripts/test-emulator-config.mjs
node scripts/test-owner-rules.mjs
node scripts/test-caregiver-rules.mjs
node scripts/test-medication-rules.mjs
node scripts/test-care-task-rules.mjs
node scripts/test-notification-rules.mjs
.\gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug connectedDebugAndroidTest --offline
```

All five authorization suites passed against disposable local Firestore databases. No production rules or data were changed. The JVM suite passes all 113 tests. App and instrumentation APK builds pass. Lint reports zero errors, 33 warnings, and five hints. All five new device regression tests pass after the fixes, including the logout cleanup test. These local totals include restored, pre-existing tests that remain outside the new commits. The complete device run on the resizable Android 14 emulator finished with 101 passing tests and nine native Back-key tests failing with Espresso window-focus timeouts. Android window diagnostics confirmed that system and System UI ANR dialogs held focus above the test activities. After the user stopped that emulator, resumed testing on the existing Medium Phone API 36 device launched inside Android Studio. All nine affected methods passed on their isolated rerun, zero failures, in 29 seconds. Every one of the 110 device test methods now has a passing result; this was not a single clean 110-test run. Original and rerun result XML files are preserved locally under `../.qa/main-integration-20261105/`.

The Medium Phone initially refused APK installation because only 226 MB of storage remained. Android cache trimming freed no space. Temporarily set its storage reserve percentage to 1 for the 26 MB test APK installation, then removed that override after testing, restoring the original unset/default value. No emulator application data, accounts, or records were removed.

The MainActivity account-change callbacks and production refill route have no injected route-level test seam. They were checked through code review; screen tests alone would not verify that wiring. The logout regression exercises real encrypted reminder metadata, PendingIntents, and posted notifications through the cleanup function used by logout.

The first device run was invalidated by overlapping instrumentation commands. A later baseline run was deliberately stopped to reproduce the focused bugs. The first full final run later stalled when Android's activity service timed out. Rebooted the existing Android Studio emulator, waited for Android boot completion, and successfully reran all five new regressions. An attempt before boot completion failed to install because the package service was not ready. These environmental failures and aborted runs are not counted as product failures or passing validation.

All three reviewers also inspected the fixes and confirmed their original findings were addressed. The UI reviewer requested dialog-scoped selectors for duplicated status/date text; those refinements are included in the passing regression tests.

## Local work preservation

Saved pre-existing tracked and untracked changes in stash `pre-main-integration-20261105`, restored them after the merge, and retained the stash as a backup. Existing README, IDE, test, and documentation edits remain uncommitted. No AGENTS.md, CONTEXT.md, or README.md is included in the new commits. No implementation stack or dependencies were added.
