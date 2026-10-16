package com.example.carelink.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.firestore.FirebaseFirestoreException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirebaseReachabilityTest {
    @Test fun unavailableAndTimeoutDoNotConfirmAConnection() {
        assertFalse(serverResponded(failure(FirebaseFirestoreException.Code.UNAVAILABLE)))
        assertFalse(serverResponded(failure(FirebaseFirestoreException.Code.DEADLINE_EXCEEDED)))
        assertFalse(serverResponded(IllegalStateException("Unknown client failure")))
    }

    @Test fun permissionAndMissingRecordResponsesConfirmServerReachability() {
        assertTrue(serverResponded(failure(FirebaseFirestoreException.Code.PERMISSION_DENIED)))
        assertTrue(serverResponded(failure(FirebaseFirestoreException.Code.NOT_FOUND)))
    }

    private fun failure(code: FirebaseFirestoreException.Code) = FirebaseFirestoreException("Request failed", code)
}
