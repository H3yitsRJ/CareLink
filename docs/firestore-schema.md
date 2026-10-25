# Firestore collection map and indexes — SAD-65

Reviewed against the merged application on October 25, 2026. This describes the implemented schema, including compatibility fields, rather than proposing new collections. Firebase Authentication owns credentials; Firestore stores application records.

## Collection map

```text
users/{patientId}                         User profile / care recipient
  medications/{medicationId}              Medication and reminder schedule
  doseRecords/{doseId}                    Recorded medication-dose outcomes
  appointments/{appointmentId}            Appointments
  healthConcerns/{concernId}               Concerns; optional appointment link
  careTasks/{taskId}                      Tasks; optional appointment link
  medicationCaregivers/{caregiverId}       Authoritative access grant
  refillRequests/{medicationId}           Current refill lifecycle per medication
  careHistory/{eventId}                   Shared activity and legacy access events
  notifications/{notificationId}         Owner notification inbox
caregiverAccess/{caregiverId}
  recipients/{patientId}                  Minimal reverse lookup for recipient selection
```

A care recipient is a user identified by Firebase Auth UID; there is no separate patient-profile collection in the current workflows. Caregivers are authenticated users, linked through patient-owned grants. Parent documents do not automatically authorize subcollections. `firestore.rules` is the authority for allowed operations.

## Fields and relationships

Unless stated otherwise, text values are strings, IDs refer to the path IDs, and record IDs come from the document path rather than requiring a duplicate `id` field. Fields described as required are current workflow/model requirements; several owner-write rules are broader than the models and are not complete schema validators.

| Record | Required / current write fields | Optional, default, or compatibility fields |
| --- | --- | --- |
| User profile | Current profile screen writes `fullName`, `firstName`, `lastName`, `preferredName`, `dateOfBirth`, `phoneNumber`, `addressLine1`, `addressLine2`, `city`, `state`, `zipCode`, `email`; optional form values may be empty strings. Auth UID is the path ID. | `UserProfile` also supports `userId`, `primaryPhysician`, `preferredPharmacy`; do not require these for the current screen's profile documents. |
| Medication | `patientId`, `name`, `strength`, `dose`, `frequency`, `reminderTimes` (string array, HH:mm) | `instructions` defaults empty, `active` defaults true, `updatedById`, `updatedAt` (server timestamp). MainActivity also writes `id`; the model serializer does not require that duplicate. |
| Dose record | `medicationId`, `scheduledTimeMillis` (positive integer), `status` (`scheduled`, `taken`, `skipped`, `missed`, `delayed`) | `completionTimeMillis` is required for completed outcomes and null for scheduled records. `DoseHistoryStore` adds `medicationName`, `dosage`, `actorId` snapshots; notification receiver records can omit these. ID is normally medication ID plus scheduled milliseconds. |
| Appointment | `patientId`, `title`, `date` (YYYY-MM-DD), `time` (HH:mm) | `provider`, `location`, `notes` default empty; `status` is SCHEDULED / COMPLETED / CANCELLED, default SCHEDULED. |
| Health concern | `patientId`, `title`, `severity` (LOW / MEDIUM / HIGH), `recordedDate` (YYYY-MM-DD) | `description` defaults empty; `status` defaults ACTIVE and also supports DISCUSSED; nullable `appointmentId` refers to an appointment under the same patient. |
| Care task | `patientId`, `title` | `description`, `dueDate`, `time` default empty; `completed` boolean and `status` (PENDING / COMPLETED) are written together; nullable `appointmentId`. Readers accept legacy `dueTime` and `completed` without `status`. |
| Caregiver grant | `patientId`, `caregiverId`, `permissions` (string array), explicit `revoked` boolean; current save adds `patientName` and `historyEventId` | Current rules accept VIEW and EDIT only; EDIT requires VIEW. Other permission enum values do not grant additional server rights. The document ID is caregiver ID; `historyEventId` links the access-change event. |
| Recipient lookup | `patientId`, `patientName`, `grantedAt` (server timestamp) | Document ID is patient ID. Created/deleted in the same transaction as the grant and its history event. The authoritative permission source remains the patient grant. No contact, health, or medication fields belong here. |
| Refill request | `patientId`, `medicationId`, `requestedById`, `note` (at most 1,000 characters), `status`, `requestedAt` (server timestamp) | Optional `medicationName` snapshot must match the referenced medication when written. Request ID equals medication ID, preventing duplicate active requests. REQUESTED / PROCESSING are active; COMPLETED / CANCELLED permit a new request. The client submission flow does not mark requests terminal. Readers accept legacy numeric times. |
| Shared care-history event | `patientId`, `actorId`, `occurredAtMillis` (integer), `type`, `summary`; medication writes also require `relatedRecordId` | Model types: MEDICATION / APPOINTMENT / HEALTH_CONCERN / CARE_TASK / CAREGIVER_ACCESS. `relatedRecordId` is otherwise model-optional. Supported model types do not imply every type has a writer or server write authorization. See existing event-model documentation and actual rules. |
| Legacy caregiver history | `patientId`, `caregiverId`, `permissions`, `revoked`, `actorId`, `type` = CAREGIVER_ACCESS, `action`, `title`, `occurredAt` (server timestamp) | Adapted to shared history when read. Keep its separate timestamp query until old records are migrated. |
| Notification | Current UI reads `title`, `message`, `timestamp` (Firestore timestamp), `isRead` (boolean) | `relatedFeature`, `relatedId` are optional links. Owner clients can read and mark `isRead=true`; creation belongs to a trusted backend, not this client. |

Relationships stay within `users/{patientId}`: a dose/refill points to a medication; a concern or follow-up task can point to an appointment; a shared history event points to a feature record by type and `relatedRecordId`. Access-history events instead carry the caregiver ID. Deleted source records may leave historical events or snapshots; links must handle missing records. IDs are references, not cascading-delete guarantees.

The invitation model is currently a serializable model without an active Firestore repository/collection in these workflows. Root-level `careTasks` has legacy security rules, but active repositories use patient subcollections. Do not create duplicate top-level feature data based on those legacy rules.

## Query and index inventory

| Application query | Required index |
| --- | --- |
| Profile or individual feature record by document path | Document-key lookup; no composite index |
| Patient medication, appointment, concern, task, refill, dose, history, grant, and notification lists/listeners | Collection-scope automatic document-key index |
| `caregiverAccess/{caregiverId}/recipients` list | Collection-scope automatic document-key index; alphabetical sort is in memory |
| Notifications ordered by `timestamp DESC` | Automatic single-field descending index on `notifications.timestamp` |
| Dose history ordered by `completionTimeMillis DESC, __name__ DESC`, with cursor and limit | Automatic single-field descending index on `doseRecords.completionTimeMillis` |
| Shared history ordered by `occurredAtMillis DESC, __name__ DESC`, with cursor and limit | Automatic single-field descending index on `careHistory.occurredAtMillis` |
| Legacy access history ordered by `occurredAt DESC, __name__ DESC`, with cursor and limit | Automatic single-field descending index on `careHistory.occurredAt` |

There are currently no application `where...` filters or collection-group queries. History type/date filtering, refill sorting, and dashboard attention calculations occur in the client. The history pager merges three ordered streams.

For Firestore Standard/native query indexing, each single-field index already includes the document-name tie-breaker in the same direction. These exact history orders therefore do not need extra composite indexes. Keep automatic collection-scope indexes enabled for the four ordered fields above. An orderBy excludes documents without its ordered field; that is why old caregiver history needs a separate `occurredAt` stream.

`firestore.indexes.json` intentionally has an empty `indexes` array: no additional composite index is required by current queries. Its existing `medicationCaregivers.caregiverId` collection-group ascending override is retained for compatibility; current recipient discovery uses the reverse lookup instead. The file also preserves ascending/descending collection-scope indexes for that field. `firebase.json` already references this index file and the rules file. This task does not remove indexes or change deployed settings.

References: [Firebase index types](https://firebase.google.com/docs/firestore/query-data/index-overview), [index management](https://firebase.google.com/docs/firestore/query-data/indexing), [Firestore emulator limitations](https://firebase.google.com/docs/emulator-suite/connect_firestore#how_the_cloud_firestore_emulator_differs_from_production).

## Sensitive data and duplication review

Profiles hold contact/address/birth-date data once per user. Feature records carry their own care details and IDs rather than copies of the whole profile. The limited intentional snapshots are patient display name in grants/directory entries, medication name in refill requests, medication name/dosage in explicit dose-history entries, and event summaries. They support selection or historical context after a record changes. Never copy passwords, tokens, full profiles, or unrelated clinical notes into lookup entries, notifications, or history summaries. Keep summaries minimal; display names and medication snapshots are still sensitive and require access rules.

`patientId` on feature records is deliberate ownership metadata used by validation and authorization, even though it also appears in the path. Caregiver directory entries are not permission grants. Current permissions and revocation must still be checked against the authoritative grant. Review name changes and historical snapshot retention as a product decision; no database migration or deletion is performed by this documentation.

## Live validation and deployment

The emulator does not enforce compound-index requirements. A passing emulator query is not proof that deployed indexes support production queries.

On a connected Android emulator signed into the normal CareLink debug app, run:

```powershell
.\gradlew.bat connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.example.carelink.data.FirestoreQuerySmokeTest' '-Pandroid.testInstrumentationRunnerArguments.runLiveFirestoreQueryCheck=true'
```

This opt-in test uses the existing Firebase session, performs server-only reads, and never creates, updates, deletes, or prints documents or credentials. It checks the profile, feature collection queries, recipient directory, notification ordering, all three history orders, and a subsequent cursor page when the first page has a record. It fails if not signed in or a server query fails. In ordinary device-test runs it is skipped unless explicitly enabled. Empty collections verify query acceptance but do not validate populated-record behavior; a populated test account is needed to exercise every cursor path.

If a live server returns FAILED_PRECONDITION for an index, record the query and create only its required index, wait for READY, then rerun. If authorized to deploy checked-in index configuration, use `firebase deploy --only firestore:indexes --project carelink-30dde` from the repository. Do not claim deployment or completion based on local configuration alone. No production index changes are deployed by this task.

## Acceptance evidence

The collection map, field inventory, record relationships, index rationale, and sensitive-data review above address the design/documentation criteria. Query acceptance requires the separate live-test result. Preserve the test report and note the tested commit/account role without recording credentials or patient content. Any skipped or failed live check remains an open validation item.

### Validation result — October 25, 2026

- App source baseline: `e60efd4` on `Timothy-`; schema document and query smoke test are the SAD-65 follow-up.
- Debug build passed; all 112 unit tests passed with zero failures/errors.
- The opted-in live query smoke test passed on Pixel 9 Pro XL / Android 16 with an existing signed-in account. The initial attempt stopped at the sign-in prerequisite; after sign-in, the complete check passed.
- The live check covered a profile read and 14 collection/order query shapes; subsequent cursor reads ran for query results containing a first document. No missing-index errors occurred. Coverage is one account's accessible paths, not every possible caregiver grant or populated-data combination.
- Existing automatic indexes support the tested queries. No additional composite indexes, database writes, migrations, or production index deployments were needed or performed.
