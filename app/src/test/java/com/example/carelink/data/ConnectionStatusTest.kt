package com.example.carelink.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionStatusTest {
    @Test fun initialFirebaseCheckDoesNotShowAnOfflineWarning() {
        assertFalse(showOfflineWarning(true, true, null))
    }

    @Test fun confirmedFailuresStillShowTheWarning() {
        assertTrue(showOfflineWarning(false, true, null))
        assertTrue(showOfflineWarning(true, true, false))
        assertTrue(showOfflineWarning(false, false, null))
    }

    @Test fun successfulChecksAndSignedOutInternetDoNotShowTheWarning() {
        assertFalse(showOfflineWarning(true, true, true))
        assertFalse(showOfflineWarning(true, false, null))
        assertFalse(showOfflineWarning(true, false, false))
    }
}
