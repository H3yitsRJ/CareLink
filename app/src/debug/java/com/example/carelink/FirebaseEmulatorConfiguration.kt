package com.example.carelink

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// Android reserves 10.0.2.2 as the emulator's route back to the development computer.
private const val ANDROID_EMULATOR_HOST = "10.0.2.2"

// Real Firebase is the debug default. Local emulators remain available as an explicit build option.
fun configureFirebaseEmulators() {
    if (BuildConfig.USE_FIREBASE_EMULATORS) {
        FirebaseAuth.getInstance().useEmulator(ANDROID_EMULATOR_HOST, 9099)
        FirebaseFirestore.getInstance().useEmulator(ANDROID_EMULATOR_HOST, 8080)
    }
}
