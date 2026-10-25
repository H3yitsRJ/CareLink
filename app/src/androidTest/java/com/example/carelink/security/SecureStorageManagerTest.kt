
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

    @Test
    fun corruptValueReportsReadFailureAndMissingValueDoesNot() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("carelink_secure_storage", android.content.Context.MODE_PRIVATE)
        assertTrue(preferences.edit().putString("corrupt", "invalid encrypted data").commit())

        assertNull(storage.get("corrupt"))
        assertEquals("We couldn't read your saved secure data on this device. Please try again.", storage.lastError)
        assertNull(storage.get("missing"))
        assertNull(storage.lastError)
    }

    @Test
    fun unavailablePreferencesReportCleanupFailureWithoutThrowing() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val failingContext = object : android.content.ContextWrapper(context) {
            override fun getSharedPreferences(name: String, mode: Int): android.content.SharedPreferences {
                throw IllegalStateException("Unavailable storage")
            }
        }
        val failing = SecureStorageManager(failingContext)
        assertFalse(failing.clearAll())
        assertEquals("We couldn't remove your saved secure data from this device. Please try again.", failing.lastError)
        assertFalse(failing.delete("saved"))
        assertNotNull(failing.lastError)
        assertTrue(failing.getAllKeys().isEmpty())
        assertNotNull(failing.lastError)
    }

    @Test
    fun failedDiskCommitReportsFailureAndSuccessfulRetryClearsError() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("carelink_secure_storage", android.content.Context.MODE_PRIVATE)
        var failCommit = true
        val intercepted = java.lang.reflect.Proxy.newProxyInstance(
            android.content.SharedPreferences::class.java.classLoader,
            arrayOf(android.content.SharedPreferences::class.java)
        ) { _, method, args ->
            if (method.name == "edit") {
                val editor = preferences.edit()
                java.lang.reflect.Proxy.newProxyInstance(
                    android.content.SharedPreferences.Editor::class.java.classLoader,
                    arrayOf(android.content.SharedPreferences.Editor::class.java)
                ) { proxy, editMethod, editArgs ->
                    when {
                        editMethod.name == "commit" && failCommit -> false
                        editMethod.returnType == android.content.SharedPreferences.Editor::class.java -> {
                            editMethod.invoke(editor, *(editArgs ?: emptyArray()))
                            proxy
                        }
                        else -> editMethod.invoke(editor, *(editArgs ?: emptyArray()))
                    }
                }
            } else method.invoke(preferences, *(args ?: emptyArray()))
        } as android.content.SharedPreferences
        val failingContext = object : android.content.ContextWrapper(context) {
            override fun getSharedPreferences(name: String, mode: Int) = intercepted
        }
        val retriable = SecureStorageManager(failingContext)
        assertFalse(retriable.save("retry", "dummy value"))
        assertNotNull(retriable.lastError)
        assertFalse(retriable.delete("retry"))
        assertNotNull(retriable.lastError)
        assertFalse(retriable.clearAll())
        assertNotNull(retriable.lastError)
        failCommit = false
        assertTrue(retriable.clearAll())
        assertNull(retriable.lastError)
    }

}
