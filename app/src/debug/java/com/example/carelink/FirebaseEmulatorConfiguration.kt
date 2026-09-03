package com.example.carelink

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

private const val ANDROID_EMULATOR_HOST = "10.0.2.2"

fun configureFirebaseEmulators() {
    FirebaseAuth.getInstance().useEmulator(ANDROID_EMULATOR_HOST, 9099)
    FirebaseFirestore.getInstance().useEmulator(ANDROID_EMULATOR_HOST, 8080)
}
