# Offline testing

1. Start the app while connected and open a screen that has loaded Firebase data.
2. In the emulator, turn off Wi-Fi and mobile data. With ADB, use `adb shell svc wifi disable` and `adb shell svc data disable`.
3. Confirm the shared "CareLink can't connect" banner appears on every screen. Previously cached profile, medications, appointments, care tasks, health concerns, and care history remain readable. Lists with no cached information may be empty. A patient without a cached profile sees a connection message rather than a new-profile form.
4. Open medication add/edit, medication removal, dose recording, appointment add/edit/cancel, profile edit, care task add/edit/delete/complete, and health concern add/status/link. Confirm each action reports that a connection is required before sending a write, and entered values stay intact. Sign-in, account creation, and password reset also need a connection. Caregiver access and medication edits retain their existing server-confirmed access requirement. Cached permissions do not authorize caregiver changes.
5. Restore connectivity with `adb shell svc wifi enable`.
6. Confirm the banner disappears after Firebase answers, and profile, medications, appointments, care tasks, and care history refresh without navigating away. Health concern and caregiver listeners refresh automatically. Keep an open form during reconnection and check that its values survive.

## Firebase outage with a working network

Use the debug Firebase emulator setup in `firebase-emulators.md`. Load data, then stop only the Firestore emulator while leaving device Wi-Fi enabled. Confirm the banner appears after the next server check and timeout, at most about 23 seconds. The check runs every 15 seconds and allows 8 seconds for a server-only profile read. Restart Firestore and confirm automatic recovery. Permission-denied and missing-record responses prove server reachability and must remain ordinary operation errors rather than being treated as offline.

Also test a cold start in airplane mode with a cached account/profile, and a cold start without a cached profile. Rotate the device with an unsaved form open and test larger text at the 412 by 892 phone baseline. Check that TalkBack announces the shared banner and that content and navigation remain reachable beneath it.

An action already submitted before connection loss can remain pending in Firestore and finish after reconnection. The banner does not claim that an in-flight action was cancelled or saved. Confirm that its screen stays in the existing busy state until the write result arrives, and that it never shows success before Firebase acknowledges it.

Firestore's Android client keeps disk persistence enabled by default. Screens must still distinguish cached data from actions that require a confirmed server write.
