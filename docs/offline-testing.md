# Offline testing

1. Start the app while connected and open a screen that has loaded Firebase data.
2. In the emulator, turn off Wi-Fi and mobile data. With ADB, use `adb shell svc wifi disable` and `adb shell svc data disable`.
3. Confirm the offline banner appears and previously cached Firestore data remains readable.
4. Confirm actions that require Firebase stay disabled or report that they are paused. Entered form values must remain intact.
5. Restore connectivity with `adb shell svc wifi enable`.
6. Confirm the banner disappears and the current screen refreshes through the connectivity callback.

Firestore's Android client keeps disk persistence enabled by default. Screens must still distinguish cached data from actions that require a confirmed server write.
