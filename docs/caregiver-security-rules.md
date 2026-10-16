# Caregiver authorization checks (SAD-67)

Firestore checks the patient's current `medicationCaregivers/{caregiverId}` grant on every delegated operation. Both identities must match the path and authenticated caregiver, `revoked` must explicitly be false, permissions must contain only supported VIEW/EDIT values, and EDIT requires VIEW. Missing, empty, malformed, mismatched, or revoked grants deny patient data access. Caregivers can read their own grant metadata to detect revocation; they cannot grant themselves access.

## Existing authorization scope

| Data | VIEW | VIEW + EDIT | Patient |
| --- | --- | --- | --- |
| Medications | Read/list | Read/list, edit existing details with authenticated editor and server timestamp | Create/read/update/delete |
| Health concerns | Read/list | Create/read/update/delete | Create/read/update/delete |
| Appointments | Read/list | Read/list | Create/read/update/delete |
| Refill requests | Read/list | Submit for an active medication, request again after COMPLETED/CANCELLED | Same submission lifecycle plus read/list |
| Profile, dose records, care tasks, care history | No access | No access | Existing owner operations |

Medication creation, removal, deactivation, and ownership changes remain unavailable to caregivers. Appointment writes remain patient-only. Recipient lookup entries require an active VIEW grant. Immutable caregiver-access history and refill lifecycle restrictions apply to patients too.

The incoming main branch intentionally grants concern CRUD and appointment reads through the same grant currently labeled medication access. SAD-67 preserves this behavior. The grant UI/store still describe medication-only access, so the product should resolve whether to explain the broader grant or introduce category-specific grants. This check does not invent new permission categories or change the incoming concern workflow.

Refill documents live at `users/{patientId}/refillRequests/{medicationId}`. Rules require patientId, medicationId, requestedById, note, status, and requestedAt. An optional medicationName snapshot must match the referenced medication's current name; this keeps legacy requests valid and prevents forged names. The initial status is REQUESTED, note is a string of at most 1,000 characters, actor matches authentication, and requestedAt uses the server timestamp. The referenced medication must belong to the patient and be active. Existing nonterminal requests cannot be overwritten. Clients cannot delete requests or mark them terminal in this flow.

## Local verification

Start the installed Firestore emulator with this checkout's rules on localhost:8180, then run:

```powershell
node scripts/test-caregiver-rules.mjs
node scripts/test-medication-rules.mjs
node scripts/test-care-task-rules.mjs
```

The new matrix checks absent, empty, VIEW, VIEW/EDIT, revoked, EDIT without VIEW, unknown permissions, identity mismatches, missing revocation, strangers, and unauthenticated requests. It also exercises collection lists, allowed concern operations, denied medication/appointment operations, actor forgery, stale revoked edits, owner operations, recipient lookup permission checks, refill schema failures, active duplicates, inactive/missing/foreign medications, and terminal re-request authorization.

All three scripts passed against the installed Firestore emulator with these rules. The matrix clears only its disposable local demo-carelink-permissions database before each run. No production deployment was performed.
