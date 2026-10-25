
package com.example.carelink.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecureStorageManagerTest {

    private lateinit var storage: SecureStorageManager

    @Before
    fun setUp() {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        storage = SecureStorageManager(context)
        assertTrue(storage.clearAll())
    }

    @After
    fun tearDown() {
        assertTrue(storage.clearAll())
    }

    @Test
    fun saveAndRetrieveValue() {
        assertTrue(storage.save("test_key", "dummy_value"))
        assertEquals("dummy_value", storage.get("test_key"))
    }

    @Test
    fun deleteStoredValue() {
        assertTrue(storage.save("test_key", "dummy_value"))
        assertTrue(storage.delete("test_key"))
        assertNull(storage.get("test_key"))
    }

    @Test
    fun clearAllStoredValues() {
        assertTrue(storage.save("first_key", "value_one"))
        assertTrue(storage.save("second_key", "value_two"))

        assertTrue(storage.clearAll())

        assertNull(storage.get("first_key"))
        assertNull(storage.get("second_key"))
    }

    @Test
    fun missingKeyReturnsNull() {
        assertNull(storage.get("missing_key"))
    }


    @Test
    fun blankStorageKeyIsRejected() {
        assertFalse(storage.save("", "dummy_value"))
        assertNull(storage.get(""))
        assertFalse(storage.delete(""))
    }


    @Test
    fun storedValueIsEncrypted() {
        val testKey = "encryption_test"
        val testValue = "dummy_sensitive_value"

        assertTrue(storage.save(testKey, testValue))

        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        val preferences = context.getSharedPreferences(
            "carelink_secure_storage",
            android.content.Context.MODE_PRIVATE
        )

        val storedValue = preferences.getString(testKey, null)

        assertNotNull(storedValue)
        assertNotEquals(testValue, storedValue)
        assertEquals(testValue, storage.get(testKey))
    }

}
