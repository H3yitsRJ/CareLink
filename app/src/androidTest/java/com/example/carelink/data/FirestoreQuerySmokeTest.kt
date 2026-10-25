package com.example.carelink.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.carelink.BuildConfig
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/** Explicitly opted-in, read-only server checks using the existing signed-in test-device account. */
@RunWith(AndroidJUnit4::class)
class FirestoreQuerySmokeTest {
    @Test fun coreQueriesRunAgainstLiveFirestore() {
        assumeTrue(
            "Live Firestore check must be explicitly enabled",
            InstrumentationRegistry.getArguments().getString("runLiveFirestoreQueryCheck") == "true"
        )
        assertFalse("Use a normal debug build, not Firebase emulator routing", BuildConfig.USE_FIREBASE_EMULATORS)
        val user = FirebaseAuth.getInstance().currentUser
        assertNotNull("Sign into CareLink on the connected Android emulator, then rerun this check", user)
        val db = FirebaseFirestore.getInstance()
        val patient = db.collection("users").document(requireNotNull(user).uid)
        Tasks.await(patient.get(Source.SERVER), 15, TimeUnit.SECONDS)

        val collections = listOf("medications", "appointments", "healthConcerns", "careTasks",
            "doseRecords", "careHistory", "refillRequests", "medicationCaregivers", "notifications")
        val queries = collections.map { name -> name to patient.collection(name).limit(1) } + listOf(
            "recipient directory" to db.collection("caregiverAccess").document(user.uid)
                .collection("recipients").limit(1),
            "notification ordering" to patient.collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING).limit(1)
        ) + listOf(
            "doseRecords" to "completionTimeMillis",
            "careHistory" to "occurredAtMillis",
            "careHistory" to "occurredAt"
        ).map { (collection, field) ->
            "$collection ordered by $field" to patient.collection(collection)
                .orderBy(field, Query.Direction.DESCENDING)
                .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING).limit(1)
        }
        for ((label, query) in queries) {
            try {
                val first = Tasks.await(query.get(Source.SERVER), 15, TimeUnit.SECONDS)
                first.documents.lastOrNull()?.let { cursor ->
                    Tasks.await(query.startAfter(cursor).get(Source.SERVER), 15, TimeUnit.SECONDS)
                }
            } catch (failure: Exception) {
                throw AssertionError("Live query failed: $label", failure)
            }
        }
        // No documents, credentials, profile values, or returned records are logged or changed.
    }
}
