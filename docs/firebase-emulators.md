# Firebase emulators

Debug builds connect Firebase Authentication to port 9099 and Cloud Firestore to port 8080 on the development computer. Release builds continue to use the Firebase project configured by `app/google-services.json`.

## Start the emulators

Install Node.js 20 or later and the Firebase CLI, then run these commands from the `android-app` directory:

```powershell
npm install --global firebase-tools
firebase emulators:start
```

Open `http://localhost:4000` to inspect local accounts, Firestore data, and generated password-reset links. The Authentication emulator does not deliver email. After submitting the reset form in the app, open the emulator UI or terminal output to retrieve the link.

The app uses `10.0.2.2` to reach the host computer from the standard Android emulator. A physical Android device needs an `adb reverse` mapping or a debug configuration that uses the computer's LAN address.
