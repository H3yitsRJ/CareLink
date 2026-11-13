package com.example.carelink.data

import com.google.firebase.firestore.Source
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileReadSourceTest {
    @Test fun waitsForFirebaseWhileOnlineAndUsesCacheWhenOffline() {
        assertNull(profileReadSource(networkAvailable = true, firebaseAvailable = null))
        assertEquals(Source.DEFAULT, profileReadSource(networkAvailable = true, firebaseAvailable = true))
        assertEquals(Source.CACHE, profileReadSource(networkAvailable = true, firebaseAvailable = false))
        assertEquals(Source.CACHE, profileReadSource(networkAvailable = false, firebaseAvailable = null))
    }
}
