package com.example.carelink

import androidx.test.platform.app.InstrumentationRegistry
import com.example.carelink.data.FirestoreCareTaskRepository
import com.example.carelink.model.CareTask
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.UUID
import java.util.concurrent.TimeUnit

/** Run with -Pandroid.testInstrumentationRunnerArguments.week4Emulators=true.
 * Requires Auth :9199 and Firestore :8180 for demo-carelink-week4, using this branch's rules.
 */
class CareTaskFirestoreTest {
    @Test fun repositoryPersistsUpdatesCompletionAndAppointmentLinkAndRejectsOtherAccounts() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("week4Emulators") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = FirebaseApp.initializeApp(context, FirebaseOptions.Builder()
            .setApplicationId("1:123456789:android:week4").setApiKey("fake-api-key")
            .setProjectId("demo-carelink-week4").build(), "week4-${UUID.randomUUID()}")
        val auth = FirebaseAuth.getInstance(app)
        auth.useEmulator("10.0.2.2", 9199)
        val db = FirebaseFirestore.getInstance(app)
        db.useEmulator("10.0.2.2", 8180)
        db.firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
        fun <T> await(task: com.google.android.gms.tasks.Task<T>): T = Tasks.await(task, 30, TimeUnit.SECONDS)
        try {
            val owner = await(auth.signInAnonymously()).user!!.uid
            val repository = FirestoreCareTaskRepository(db)
            val original = CareTask(UUID.randomUUID().toString(), owner, "Call provider", "2026-10-01", appointmentId = "appointment-1")
            listOf(".", "..", "a/b").forEach { invalid ->
                assertTrue(runCatching { await(repository.create(original.copy(id = invalid))) }.isFailure)
                assertTrue(runCatching { await(repository.update(original.copy(patientId = invalid))) }.isFailure)
                assertTrue(runCatching { await(repository.list(invalid)) }.isFailure)
                assertTrue(runCatching { await(repository.setCompleted(owner, invalid, true)) }.isFailure)
            }
            assertEquals(original, await(repository.create(original)))
            assertTrue(runCatching { await(repository.create(original)) }.isFailure)
            assertTrue(runCatching { await(repository.update(original.copy(id = "missing"))) }.isFailure)
            assertTrue(runCatching { await(repository.create(original.copy(id = "invalid", title = ""))) }.isFailure)
            val updated = original.copy(title = "Book follow-up")
            await(repository.update(updated))
            await(repository.setCompleted(owner, original.id, true))
            // A new repository must retrieve server data, not state in the first instance.
            assertEquals(updated.copy(completed = true), await(FirestoreCareTaskRepository(db).list(owner)).single())
            await(repository.setCompleted(owner, original.id, false))
            assertEquals(updated, await(repository.list(owner)).single())
            auth.signOut()
            await(auth.signInAnonymously())
            assertTrue(await(repository.list(auth.currentUser!!.uid)).isEmpty())
            assertTrue(runCatching { await(repository.list(owner)) }.isFailure)
            assertTrue(runCatching { await(repository.create(original.copy(id = "other-account"))) }.isFailure)
            assertTrue(runCatching { await(repository.update(updated)) }.isFailure)
            assertTrue(runCatching { await(repository.setCompleted(owner, original.id, true)) }.isFailure)
        } finally {
            await(db.terminate())
            app.delete()
        }
    }
}
