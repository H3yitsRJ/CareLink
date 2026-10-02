# Care-recipient selector (SAD-54)

The caregiver medication entry point now lists recipients from the signed-in caregiver's existing medication grants. A grant must match its `users/{patientId}/medicationCaregivers/{caregiverId}` path, name the signed-in caregiver, explicitly be unrevoked, and include VIEW. The selector does not grant access to appointments, concerns, or tasks.

The current recipient is displayed above their medication screens. The selection is held in MainActivity, scoped to the signed-in account, and survives leaving and returning to the caregiver flow. Switching recipients recreates the medication screen and detaches previous listeners. Revoked recipients disappear; failed or cached-only access verification hides all recipient data until a successful retry.

## Firebase setup

Deploy the updated `firestore.rules` and `firestore.indexes.json` to the intended Firebase project before using live discovery. The collection-group query requires the caregiverId collection-group ascending index. The added list rule permits discovery only of documents whose caregiverId equals the authenticated user. Medication record rules still enforce VIEW/EDIT and revocation.

No production deployment is performed by the local build or tests.

## Verification

Unit tests cover recipient mapping, path/identity mismatches, missing VIEW, malformed grants, and revocation. Emulator UI tests use a controlled repository to exercise selection, switching, retained selection, revoked access, connection failure, and stale callbacks. Existing caregiver medication workflow tests remain part of regression verification.

For a live check after deployment, grant two recipients' medication access to one caregiver, sign in as that caregiver, select each recipient, navigate away and back, and revoke the selected grant from the owner account. Only authorized recipients and their medications should remain visible.
