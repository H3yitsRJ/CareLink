# Unified care-history event model — SAD-57

CareHistoryEntry represents activity associated with a patient.

## Fields

| Field | Meaning |
| --- | --- |
| id | Event ID; supplied from the Firestore document ID |
| patientId | Care recipient's user ID |
| actorId | User who performed the action |
| occurredAtMillis | Event time as Unix epoch milliseconds |
| type | CareActivityType enum name |
| summary | Human-readable event description |
| relatedRecordId | Optional ID of the affected record |

## Supported types

- MEDICATION: medication or dose activity.
- APPOINTMENT: appointment activity.
- HEALTH_CONCERN: health-concern activity.
- CARE_TASK: care-task activity.
- CAREGIVER_ACCESS: caregiver permission changes.

## Firestore conversion

toFirestore() serializes fields while leaving the event ID in the
document path. New events require a nonblank actor ID.

fromFirestore() reconstructs an event using its document ID.
Missing required fields, unknown types, and invalid field types
are rejected.

The model defines the shared format. Writing this format to Firestore
requires corresponding server rules; existing caregiver events retain
their current validated format.

## Compatibility and ordering

CareHistoryStore reads the shared format and adapts existing caregiver
events containing occurredAt, action, and actorId.

Existing dose records remain supported. Their actor is unknown unless
recorded explicitly; the loader does not invent an actor.

newestFirst() orders events by descending occurredAtMillis, then by ID
for consistent ordering when timestamps match.

## Verification

CareHistoryEntryTest covers all supported types, Firestore round trips,
optional related IDs, malformed data, unknown actors, and sorting.

CareHistoryFilterTest covers existing date and type filtering.