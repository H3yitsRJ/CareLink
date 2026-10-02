# Week 4 task completion

Implementation covers SAD-83, SAD-44, and SAD-50 for a signed-in patient managing their own care. This change is based on main at c575e5f and preserves the older local roberts-branch checkout.

- SAD-83: patient-scoped task create/list/update/completion, transactional updates of existing records, validation, appointment links, and matching Firestore authorization at users/{patientId}/careTasks. Both completed and status are written together.
- SAD-44: appointment details opens the existing task form with an editable appointment-based title and notes, displays the source appointment, and saves its ID. Successful save shows the repository-backed list; cancel returns to appointment details. A later ordinary task does not inherit the appointment. Failed saves preserve the draft and allow retry. Long appointment details scroll at increased text size.
- SAD-50: invitation fields, documented pending/accepted/declined/revoked statuses, strict Firestore conversion, and expiration detection. Tests cover every status, the exact expiry boundary, and malformed records. Invitation delivery and caregiver access grants are outside this model task.

## Validation

Run from this checkout with Android Studio's JBR as JAVA_HOME:

- `gradlew.bat :app:testDebugUnitTest --tests com.example.carelink.data.CareTaskRepositoryTest --tests com.example.carelink.model.CaregiverInvitationTest --tests com.example.carelink.model.CareTaskTest --tests com.example.carelink.CareTaskValidationTest`
- `gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.carelink.AppointmentFollowUpTest,com.example.carelink.CareTaskWorkflowTest`
- `gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest`
- Start the installed Firestore emulator with `--host 127.0.0.1 --port 8180 --project_id demo-carelink-tasks --rules firestore.rules`, then run `node scripts/test-care-task-rules.mjs`. This tests persistence, listing, completion status, retained appointment linkage, patient isolation, unauthenticated denial, and ownership transfer denial using disposable local records.

The full unit suite runs 81 tests with one existing failure: HealthConcernTest.invalidRequiredFieldsAreRejectedWithoutCrashing expects missing descriptions to be rejected, while main's HealthConcern.fromFirestore accepts them as empty. That implementation and test are unchanged here. The existing HealthConcernDetailsTest fake needed its missing addConcern method implemented so Android tests could compile.

Production Firestore rules have not been deployed. The matching task rules must be deployed before the repository can access this collection in production. No live patient records were used in validation.
