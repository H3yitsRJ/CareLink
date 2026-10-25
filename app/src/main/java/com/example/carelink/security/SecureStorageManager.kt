package com.example.carelink.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureStorageManager(
    private val context: Context
) {
    companion object {
        private const val KEY_ALIAS = "CareLinkSecureKey"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION =
            "AES/GCM/NoPadding"
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val existingKey = keyStore.getKey(
            KEY_ALIAS,
            null
        ) as? SecretKey

        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(
                KeyProperties.ENCRYPTION_PADDING_NONE
            )
            .setKeySize(256)
            .build()

        keyGenerator.init(keySpec)

        return keyGenerator.generateKey()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)

        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateSecretKey()
        )

        val encryptedBytes = cipher.doFinal(
            value.toByteArray(Charsets.UTF_8)
        )

        val combined = cipher.iv + encryptedBytes

        return android.util.Base64.encodeToString(
            combined,
            android.util.Base64.NO_WRAP
        )
    }
    private fun decrypt(encryptedValue: String): String {
        val combined = android.util.Base64.decode(
            encryptedValue,
            android.util.Base64.NO_WRAP
        )

        require(combined.size > 12) {
            "Invalid encrypted data"
        }

        val iv = combined.copyOfRange(0, 12)
        val encryptedBytes = combined.copyOfRange(
            12,
            combined.size
        )

        val cipher = Cipher.getInstance(TRANSFORMATION)

        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(128, iv)
        )

        val decryptedBytes = cipher.doFinal(encryptedBytes)

        return String(decryptedBytes, Charsets.UTF_8)
    }

    private val securePreferences by lazy {
        context.getSharedPreferences(
            "carelink_secure_storage",
            Context.MODE_PRIVATE
        )
    }

    /** Null after success or an ordinary missing-key lookup; otherwise safe to display to users. */
    var lastError: String? = null
        private set

    private fun <T> attempt(message: String, fallback: T, action: () -> T): T {
        lastError = null
        return try {
            action()
        } catch (_: Exception) {
            lastError = message
            // Do not log keys, values, ciphertext, or exception details containing user data.
            android.util.Log.e("SecureStorageManager", message)
            fallback
        }
    }

    private fun validKey(key: String) {
        require(key.isNotBlank()) { "Storage key cannot be blank" }
    }

    fun save(key: String, value: String): Boolean = attempt(
        "We couldn't securely save your data on this device. Please try again.", false
    ) {
        validKey(key)
        val encryptedValue = encrypt(value)
        check(securePreferences.edit().putString(key, encryptedValue).commit())
        true
    }

    fun get(key: String): String? = attempt(
        "We couldn't read your saved secure data on this device. Please try again.", null
    ) {
        validKey(key)
        securePreferences.getString(key, null)?.let(::decrypt)
    }

    fun delete(key: String): Boolean = attempt(
        "We couldn't remove the saved secure value from this device. Please try again.", false
    ) {
        validKey(key)
        check(securePreferences.edit().remove(key).commit())
        true
    }

    fun getAllKeys(): Set<String> = attempt(
        "We couldn't access your saved secure data on this device. Please try again.", emptySet()
    ) {
        securePreferences.all.keys.toSet()
    }

    fun clearAll(): Boolean = attempt(
        "We couldn't remove your saved secure data from this device. Please try again.", false
    ) {
        check(securePreferences.edit().clear().commit())
        true
    }
}
