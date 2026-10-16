# SAD-64 caregiver refill requests

A caregiver with current medication View and Edit access opens a patient's active medication and selects Request refill. The form requires an eligible medication and retains the existing optional request note. Notes are limited to 1000 characters. Inactive medications and other patients' medications cannot be selected.

Submitting creates `users/{patientId}/refillRequests/{medicationId}` with the existing `REQUESTED` status, patient, medication and caregiver identities, note, and server timestamp. A Firestore transaction rereads the medication and request. Concurrent caregivers cannot create duplicate active requests. A terminal `COMPLETED` or `CANCELLED` document can be replaced by a new request. Status management and historical refill records remain outside this task.

The form preserves selection and note after validation or save errors. Confirmation appears only after persistence succeeds. Submission pauses while saving, offline, or without Edit access. Access revocation removes the patient's medication flow. The medication list and access listeners retain their existing loading, empty, permission, and connection states.

Validation coverage is in `RefillRequestRepositoryTest` and `RefillRequestFormTest`. The UI tests cover required selection, asynchronous submission, duplicate-error recovery with retained text, confirmation, and exclusion of inactive medications. Firestore authorization and race protection are covered by the SAD-67 rules tests. Production rules must be deployed before using this workflow against production Firebase.
