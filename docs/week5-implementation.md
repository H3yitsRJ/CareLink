# Week 5 implementation and Jira comparison

## Branch comparison

GitHub roberts-branch at c575e5f matches main. SAD-83 lacks general task updates; SAD-44 is not connected from appointment details to the task repository; SAD-50 lacks invitation-model tests. The older local roberts-branch at d3bf4b6 contains all three implementations. Its seven repository/invitation unit tests and four Week4CareTaskScreenTest UI tests pass. These local changes are not published in GitHub roberts-branch. The current-main-compatible corrections are on fix/complete-week4-tasks at 19c9db2, with prior validation documented in week4-task-completion.md.

## Week 5 scope

SAD-53, SAD-56, and SAD-60 were delegated to three sub-agents and moved from WEEK 5 SPRINT to In Progress in Jira. Their human assignee remains Robert Tobiasz. This branch starts at fix/complete-week4-tasks and implements:

- SAD-53: existing medication View/Edit permission editor, current grant state, owner-only updates, local editable draft, cancel without save, disabled/busy/revoked/missing states, accessible toggle rows, and save-error recovery. Other permission types are not exposed until corresponding server-authorized features exist.
- SAD-56: authorized caregiver selection, update and revoke confirmation, cancellation, revoked-data denial, and immutable history events written atomically with grant changes. Server rules validate patient ownership, caregiver identity, supported permissions, authenticated actor, server timestamp, and matching grant/event state. Collection-group discovery only returns the requesting caregiver's grant metadata.
- SAD-60: inclusive calendar start/end dates in the device timezone, multiple activity types, Apply and Clear, invalid ranges, active filter indication, and helpful empty results. Dose history is preserved and combined with caregiver access events. Other activity categories remain selectable in the existing model, without inventing new producers for unrelated workflows.

## Validation

- `gradlew.bat :app:testDebugUnitTest :app:assembleDebug`: app builds; 86 of 87 unit tests pass. The unchanged HealthConcernTest.invalidRequiredFieldsAreRejectedWithoutCrashing failure remains because main permits an absent description while its test requires rejection. All six newly added Week 5 unit tests pass.
- `gradlew.bat :app:assembleDebugAndroidTest`: passes.
- `gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.carelink.CaregiverPermissionEditorTest,com.example.carelink.CaregiverAccessManagementTest,com.example.carelink.CareHistoryScreenTest`: 11 tests pass on Medium_Phone_API_36.0.
- `node scripts/test-medication-rules.mjs` with the installed Firestore emulator on localhost:8180, demo-carelink-review and this branch's firestore.rules: passes medication/dose regression, caregiver query, patient isolation, permission update, atomic history, forged actor/event rejection, and revoked-access denial checks.
- `git diff --check`: passes.

Production rules are not deployed. Existing local edits in android-app/README.md and docs/week-3-integration.md are preserved. No PR was created, no merge occurred, and original In Progress cards were not marked Done.
