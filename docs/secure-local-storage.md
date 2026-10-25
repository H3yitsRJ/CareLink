# Secure local storage — SAD-68

`SecureStorageManager` provides reusable encrypted local storage using native Android Keystore and AES-256/GCM. No additional security package is required. Values are encrypted before they reach the `carelink_secure_storage` preferences file; keys are held by Android Keystore. Secure preferences and legacy medication reminder preferences are excluded from cloud backup and device transfer.

## Approved values and boundaries

The current callers store medication-reminder and care-task-reminder payloads needed for local notifications. Store only the fields needed for those reminders. Do not store passwords, private authentication credentials, or unrelated profile data through this wrapper. Passwords continue to go to Firebase Authentication and are not added to this storage by the wrapper's callers.

Legacy medication reminders may still be read from the previous preferences file for compatibility. The existing migration path encrypts a reminder before removing its legacy entry; new reminders use the encrypted wrapper. This change does not add any plaintext persistence.

## Operations and errors

| Operation | Success | Failure |
| --- | --- | --- |
| `save(key, value)` | `true` | `false` with `lastError` |
| `get(key)` | Decrypted value; `null` for a missing key | `null` with `lastError` |
| `delete(key)` | `true` | `false` with `lastError` |
| `getAllKeys()` | Stored key names | Empty set with `lastError` |
| `clearAll()` | `true` | `false` with `lastError` |

Read `lastError` immediately after an operation on that instance. It contains an understandable, operation-specific message suitable for display. Every operation clears the preceding error before starting, so a successful retry or an ordinary missing-key read does not retain a stale failure. Existing return types remain compatible with reminder callers.

False disk-commit results are treated as failures as well as exceptions. Keystore initialization is lazy so initialization failures during save/read are handled by the same error path. Logs do not include storage keys, values, ciphertext, or exception details.

## Logout

The logout workflow clears secure storage before signing out. If cleanup fails, the app remains signed in and shows an announced error explaining that the user can retry Log out. Successful cleanup continues the existing reminder cleanup and Firebase sign-out flow. The logout error is scoped to the screen and cleared before a retry.

## Verification

Device tests cover encrypted storage, round-trip retrieval, deletion, clear-all, missing and invalid keys, corrupted ciphertext, unavailable preferences, failed disk commits, successful retry clearing stale errors, and visible logout-error/retry behavior. Existing medication-reminder device tests and the complete unit suite are run alongside the focused storage tests.
