# Screen usability review — October 9

## Scope and evidence

Reviewed the connected application flows in MainActivity and the Week 6 screens, including the refill and notification changes integrated from main. This review combines route/callback inspection, the Android instrumentation suite, unit tests, and local Firestore rules tests. It is not a claim that every production integration or accessibility interaction has been exercised.

| Flow | Review and coverage |
| --- | --- |
| Sign in, registration, password reset, profile create/edit, logout | Inspected connected callbacks, validation, busy/error handling, and offline guards. No real account was created or changed and no reset email was sent. Live authentication/email delivery remains unverified. |
| Home and bottom navigation | Traced destinations to MainActivity routes. Scheduler bottom navigation now has actual callbacks. Home summaries are static navigation prompts, not live care counts. |
| Patient medication list | Fixed fixed-width actions that could overflow a narrow display and changed the header/list into one scrolling page. New test exercises Add, Scheduler, and medication selection at 320 dp and increased text size. Existing load/error/retry and empty states retained. |
| Add/edit medication and medication details | Existing tests cover validation, reminder preservation, cancellation, removal confirmation, and dose recording/reminders. Added Android back handling. Server-side authorization verified by rule tests. |
| Medication scheduler | Previously never loaded medications and wrote to an unrelated, unauthorized medicationSchedules collection. Now loads active saved medications with loading/error/retry states and opens the established medication editor. Saving uses the existing medication/reminder path, including supported frequency and time validation. Empty state offers Add medication. |
| Refill hub, request form, tracking/details | Back navigation, pharmacy chooser, medication selection, loading/error/retry, empty-state recovery, duplicate protection, persistence confirmation, and long forms reviewed. External pharmacy launch now reports a missing browser instead of crashing. Tracking detail notes scroll. CVS/Walgreens websites are external links; CareLink does not submit an order to a pharmacy. |
| Caregiver recipient selection and medication details/edit/refill | Permission and revocation tests retained. Removed dose-recording controls from the caregiver view because they had no persistence callback and caregiver dose-record authorization is not implemented. View/edit medication and refill actions remain available within the grant. |
| Caregiver access list/editor/revoke | Existing tests cover View/Edit selection, failed saves, conflict handling, revocation, offline restrictions, cancellation, and account isolation. Existing permission wording mismatch for health-concern/appointment access remains documented in workspace open questions. |
| Care tasks and appointment follow-up | Existing tests cover creation, edit, completion, deletion, validation, failed persistence, offline restrictions, and follow-up linking. |
| Appointments, add/edit, details/cancel | Fixed disabled date/time entry and retained explicit date/time picker buttons. Added Android back handling. Existing past-date correction test serves as the regression check. Save/cancel/follow-up and validation tests retained. |
| Health concerns, add/detail/status/appointment link | Existing workflow tests retained. Filter rows now scroll horizontally so all choices remain reachable at larger text sizes. Offline write guards remain in the flow. |
| Care history | Existing date/type filtering, invalid ranges, loading/error/retry, and long list tests retained. Added Android back handling. |
| Settings | Medication caregiver access and logout are wired. Added Android back handling. Other standalone settings/design files are not connected destinations and are not newly exposed by this review. |
| Notifications | Added Back, loading/error/retry, safe errors, explicit Read/Unread labels, pending-write protection, and recovery that keeps the list visible. New rule permits owner reads and only marking isRead=true. Clients cannot create/delete notifications, change content, or read another patient's notifications. |

## Remaining limits

Notification creation/delivery is not implemented by any producer in this repository. The list and mark-as-read flow can handle existing server-created notifications, but an empty list does not prove event generation works. Refill tracking displays recorded status; no pharmacy backend or automatic processing/completion integration is present. No new backend lifecycle or status-changing permissions were invented during the review.

Firestore rules are checked locally and have not been deployed. Live patient/caregiver accounts, real notification delivery, real pharmacy handoff, real network outages, and a full TalkBack pass remain unverified. Existing Gradle compile SDK compatibility warning remains. Unrelated README and IDE edits were preserved. All audit fixes remain uncommitted locally.

## Validation

The initial full Android run passed 65 of 66 tests and exposed the disabled appointment date field. That defect was fixed; final rerun results follow. All four local rule suites passed: caregiver/refill, medication/dose, care tasks, and notifications.

Commands: `gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:connectedDebugAndroidTest --offline`; bundled Node runtime with `scripts/test-caregiver-rules.mjs`, `scripts/test-medication-rules.mjs`, `scripts/test-care-task-rules.mjs`, and `scripts/test-notification-rules.mjs`; `git diff --check`.

Final result: 98 unit tests and all 69 Android instrumentation tests passed, with zero failures or skips. Debug build and whitespace checks passed. All four local Firestore rules suites passed. The final run includes the corrected appointment date test and seven new screen-usability tests. The pre-existing Android emulator was left running; only the temporary Firestore emulator started for this audit was stopped.
