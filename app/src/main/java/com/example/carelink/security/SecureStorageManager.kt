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

    private val keyStore: KeyStore =
        KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
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

    fun save(key: String, value: String): Boolean {
        return try {
            require(key.isNotBlank()) {
                "Storage key cannot be blank"
            }

            val encryptedValue = encrypt(value)

            securePreferences.edit()
                .putString(key, encryptedValue)
                .commit()
        } catch (e: Exception) {
            android.util.Log.e(
                "SecureStorageManager",
                "Failed to save encrypted value",
                e
            )
            false
        }
    }

    fun get(key: String): String? {
        return try {
            require(key.isNotBlank()) {
                "Storage key cannot be blank"
            }

            val encryptedValue =
                securePreferences.getString(key, null)
                    ?: return null

            decrypt(encryptedValue)
        } catch (e: Exception) {
            android.util.Log.e(
                "SecureStorageManager",
                "Failed to retrieve encrypted value",
                e
            )
            null
        }
    }


    fun delete(key: String): Boolean {
        return try {
            require(key.isNotBlank()) {
                "Storage key cannot be blank"
            }

            securePreferences.edit()
                .remove(key)
                .commit()
        } catch (e: Exception) {
            android.util.Log.e(
                "SecureStorageManager",
                "Failed to delete stored value",
                e
            )
            false
        }
    }

    fun getAllKeys(): Set<String> {
        return try {
            securePreferences.all.keys.toSet()
        } catch (e: Exception) {
            android.util.Log.e(
                "SecureStorageManager",
                "Failed to retrieve secure storage keys",
                e
            )
            emptySet()
        }
    }


    fun clearAll(): Boolean {
        return try {
            securePreferences.edit()
                .clear()
                .commit()
        } catch (e: Exception) {
            android.util.Log.e(
                "SecureStorageManager",
                "Failed to clear secure storage",
                e
            )
            false
        }
    }



}