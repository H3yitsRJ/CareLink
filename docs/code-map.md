# CareLink code map

This describes the Android checkout as it exists today. Start with `app/src/main/java/com/example/carelink/MainActivity.kt` to follow the running app. Source comments explain each Kotlin file's purpose. Existing code includes unfinished features, so a screen's presence does not mean it is available in the app.

## Where things are

Paths below are relative to the Android project root.

| Location | Responsibility |
| --- | --- |
| `app/src/main/java/com/example/carelink/MainActivity.kt` | App startup, authentication, profile loading, current screen, care lists, and screen callbacks |
| `app/src/main/java/screens/` | Compose screens, form validation, shared cards, and offline banner. Most declare the `com.example.carelink.screens` package despite this shorter folder path. |
| `app/src/main/java/navigation/BottomNavBar.kt` | Home, Medications, Appointments, Care tasks, and Profile navigation controls |
| `app/src/main/java/com/example/carelink/model/` | Care records, validation, and Firestore field conversion |
| `app/src/main/java/com/example/carelink/data/` | In-memory repositories, prepared Firestore repositories, and connectivity monitoring |
| `app/src/main/java/com/example/carelink/notifications/` | Android alarm scheduling and notification delivery |
| `app/src/main/java/com/example/carelink/ui/theme/` | Shared colors, typography, and Material theme |
| `app/src/debug/java/com/example/carelink/FirebaseEmulatorConfiguration.kt` | Optional local Firebase emulator connection |
| `app/src/release/java/com/example/carelink/FirebaseEmulatorConfiguration.kt` | Release configuration, which uses the configured Firebase project |
| `firestore.rules` | Server-side access rules for profiles and patient collections |
| `app/src/test/java/com/example/carelink/` | Local tests for forms, models, repositories, and reminder calculations |
| `app/src/androidTest/` | Android instrumented test source set |

## Follow the running flows

### Account and profile

`LoginScreen`, `CreateAccountScreen`, and `PasswordResetEmailScreen` collect and validate input. Their callbacks in `MainActivity` call Firebase Authentication and pass pending, failure, or success state back to the screen.

After authentication, `MainActivity` reads `users/{uid}` from Firestore. A null `hasProfile` value displays loading. False opens `CreateProfileScreen`; true opens the app. Profile setup writes its field map directly in `MainActivity`. `UserProfile.kt` also defines a profile model, but the activity does not use that serializer.

### Medications

Home or bottom navigation opens `MedicationsScreen`. Add opens `AddEditMedicationScreen`; selecting a record opens `MedicationDetailsScreen`; Edit reuses the form.

The form returns a validated `Medication`. The activity writes medications to `users/{uid}/medications`; the list loads that collection when opened. After a successful save, the activity schedules or replaces reminders. Successful deletion cancels reminders. The in-memory medication repository remains available for isolated tests but is no longer the running app's medication store.

`MedicationReminderScheduler` schedules the next occurrence of each supplied time. `MedicationReminderReceiver` posts the notification if permission allows and handles Taken/Skipped actions by writing `users/{uid}/doseRecords`. These alarms are one-shot. The receiver does not schedule the next day, and frequency text does not determine alarm recurrence.

### Appointments and follow-up

`AppointmentsScreen` opens `AddEditAppointmentScreen` or `AppointmentDetailsScreen`. The activity stores appointments in a Compose state list. Cancellation changes the selected record's status. Generate follow-up task passes the appointment into `AddEditCareTaskScreen`, which pre-fills an editable title and retains the appointment ID on the task.

`CareTasksScreen` displays records from `InMemoryCareTaskRepository` and sends completion changes back to the activity. `FirestoreCareTaskRepository` exists separately and returns asynchronous Firebase tasks, but is not used by the running activity.

### Health concerns and settings

Home opens `HealthConcernsScreen`, and Add opens `AddHealthConcernScreen`. Saved concerns go into an in-memory list. Selecting a concern currently has an empty callback.

Profile opens Settings, then the log-out confirmation. The activity signs out of Firebase and resets navigation and the medication display. It does not currently clear every care list or repository.

## Present but not connected

| Files | Current state |
| --- | --- |
| `CaregiverAccessScreen`, `CaregiversAccessSettingsScreen` | Permission editing and revocation UI with caller-supplied callbacks; no activity route |
| `CaregiverAccessRepository` | Firestore access writes and a batched revocation/history write; no activity caller |
| `CareHistoryScreen` | Filters supplied entries by timestamp and activity type; no activity route or data loader |
| `RefillRequestScreen`, `RefillRequestRepository` | Form and Firestore creation code; not connected to each other by the activity |
| `ThemePreviewScreen` | Static theme sample |
| `AboutCareLinkScreen`, `AccountSecurityScreen`, `AddCaregiverScreen`, `ContactUsScreen`, `CreatePasswordScreen`, `DisplaySettingsScreen`, `HealthConcernDetailsScreen`, `HelpCenterScreen`, `LanguageScreen`, `MedicationScheduleScreen`, `NotificationsScreen` | Static placeholders, some with copied labels or previews |

## Limits to keep in mind when reading the code

- Appointments, health concerns, and care tasks still use memory-only state in the running app. Profile, medication, and reminder-action workflows now use Firebase.
- Incoming medication and dose-action writes use `users/{uid}` subcollections. The checked-in Firestore rules do not grant those subcollections access. The deployed rules were not inspected during the sync; server-rule integration needs a separate check before claiming those workflows work end to end.
- `OfflineBanner` reports connectivity. It does not implement the caching or paused writes described by its current text.
- Appointment cancellation copy mentions stopping reminders, but the current callback only changes local status.
- Refill success text appears after invoking the callback, before any asynchronous persistence is confirmed. The repository's duplicate check and write are separate operations.
- Disabled caregiver controls are UI behavior. Server authorization depends on the deployed Firestore rules. The general collection rule overlaps the specific caregiver-access and history rules; matching allow rules combine, so the specific blocks alone do not guarantee owner-only access changes or immutable history.

These are existing implementation limits documented during the comment pass, not behavior changes or new release commitments.

## Checks and further reading

Run local tests from the Android project with `./gradlew.bat :app:testDebugUnitTest`. Use `./gradlew.bat :app:assembleDebug` to check the debug build and `./gradlew.bat :app:lintDebug` for Android lint. These need a configured JDK, Android SDK, and available Gradle dependencies.

- [Firebase emulator setup](firebase-emulators.md)
- [Offline testing](offline-testing.md)
- [Usability test plan](usability-test-plan.md)

For a screen change, begin with its file in `screens/`, then inspect the matching callback in `MainActivity`. For stored field changes, inspect the model conversion, repository, Firestore rules, and relevant tests together.
