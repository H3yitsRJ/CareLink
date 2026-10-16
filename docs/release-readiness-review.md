# Branch and Jira readiness review

Reviewed October 15, 2026 against the live SAD board filtered to Robert Tobiasz.

## Incoming main

Fetched origin and fast-forwarded roberts-branch from 35eb9a1 to ea6625c. Six incoming commits update bottom navigation, add/fix Android CI, and update the upstream Android README. Saved all existing work in a retained safety stash before integration and reapplied it successfully. Local README and IDE changes remain excluded from publishing.

## Jira acceptance checks

| Item | Evidence |
| --- | --- |
| SAD-89, global offline states | Network and Firebase reachability detection, shared announced banner, cache reads, connection guards with draft retention, refresh on reconnect, documented outage checks, offline workflow tests. |
| SAD-67, caregiver permissions | Current grant and identity checks, supported View/Edit levels, denied read-only writes/unauthorized/revoked access, patient access, permission-matrix and authorization-rule tests. |
| SAD-64, refill creation | Patient/caregiver medication selection, required fields and validation, transaction-backed REQUESTED status/server timestamp, confirmation after save, active duplicate prevention, offline guards, repository/form tests. |

SAD-72 remains in Week 7 Sprint. It requires an outside five-person usability study and is not satisfied by automated tests.

## Reported defect fixes

- A failed dose attempt clears the previous success state; details also prioritize errors over stale success feedback.
- Medication validation asks patients to select frequency and reminder times, matching the existing pickers.
- Reminder guidance explicitly names Android Settings. A persisted device preference prevents repeating the permission explanation after it has already been shown.
- Task completion uses one accessible checkbox row, so the label and checkbox toggle the same state instead of opening editing.
- Dashboard uses a time-neutral greeting.
- Sign-in keeps profile state unresolved until the profile check returns, avoiding the empty setup-form flash for existing patients.
- New refill requests retain a medication-name snapshot derived in the transaction from the current medication. Server rules validate that name. Legacy requests with removed medications show a readable fallback instead of a raw medication ID.

The one-time offline care-task disappearance from the prior manual walkthrough was not reproduced on repeat checks. No speculative cache change was added. Existing offline cache, draft and reconnection tests remain part of validation.

## Deployment boundary

Firestore rules are validated locally and checked in, not deployed to production by this change. Deploy the matching rules before releasing a build that writes medicationName snapshots. Existing requests without the optional field remain readable and valid. Automatic server notification generation and pharmacy integrations are outside these Jira tasks.

## Validation

Focused UI regression tests reproduced four failures before the fixes and then passed all five tests after fixes. The focused missing-schedule-copy unit regression also failed before and passed after the fix.

The full JVM suite passes 100 tests. Debug app and instrumentation APK builds succeed. Android lint reports zero errors, 37 warnings, and six informational hints. Existing compile SDK/AGP compatibility and Gradle deprecation warnings remain.

All four local Firestore suites pass: caregiver permissions/refill lifecycle, medication/dose authorization, care-task authorization, and owner notification read/mark-read restrictions. New rule cases reject forged or invalid medication-name snapshots and accept verified names.

The original emulator lacked internal storage for APK installation. Used an isolated emulator with a new workspace data image rather than elevating privileges or deleting patient records. Full Android suite and remote CI results are recorded below after completion.

Main integration check found one test selector mismatch caused by the incoming visible navigation labels. Updated the scheduler test to select the visible Home label; retained main navigation behavior and styling.

Final complete Android run passed all 74 tests, zero failures. The full validation total is 174 passing tests plus all four authorization-rule suites. Whitespace checks pass. APK installation and Android tests used emulator-5556; no production rules were deployed.
