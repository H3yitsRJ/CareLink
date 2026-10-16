package com.example.carelink.data

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

// A validated network can still fail to reach Firebase. A server-only read distinguishes
// that state from the cached reads used to keep the patient's information available.
internal suspend fun canReachFirebase(reference: DocumentReference): Boolean =
    withTimeoutOrNull(8_000) {
        suspendCancellableCoroutine { continuation ->
            reference.get(Source.SERVER)
                .addOnSuccessListener {
                    if (continuation.isActive) continuation.resume(true)
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) continuation.resume(serverResponded(error))
                }
        }
    } ?: false

internal fun serverResponded(error: Exception): Boolean {
    val code = (error as? FirebaseFirestoreException)?.code ?: return false
    // A rejected request still proves the server is reachable. Authorization remains
    // the responsibility of the operation and Firebase rules.
    return code != FirebaseFirestoreException.Code.UNAVAILABLE &&
        code != FirebaseFirestoreException.Code.DEADLINE_EXCEEDED
}
