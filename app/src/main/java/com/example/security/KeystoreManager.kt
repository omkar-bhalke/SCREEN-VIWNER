package com.example.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeystoreManager(context: Context) {

    private val prefs = context.getSharedPreferences("ahi_keystore_vault", Context.MODE_PRIVATE)

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "ahi_gemini_master_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
        private const val PREF_KEY_CIPHERTEXT = "encrypted_gemini_key"
        private const val PREF_KEY_IV = "gemini_key_iv"
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        }
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    fun saveApiKey(rawKey: String): Boolean {
        if (rawKey.isBlank()) return false
        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(rawKey.trim().toByteArray(Charsets.UTF_8))

            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val cipherBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)

            prefs.edit()
                .putString(PREF_KEY_IV, ivBase64)
                .putString(PREF_KEY_CIPHERTEXT, cipherBase64)
                .apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getApiKey(): String? {
        val ivBase64 = prefs.getString(PREF_KEY_IV, null) ?: return null
        val cipherBase64 = prefs.getString(PREF_KEY_CIPHERTEXT, null) ?: return null
        return try {
            val secretKey = getOrCreateSecretKey()
            val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
            val encryptedBytes = Base64.decode(cipherBase64, Base64.NO_WRAP)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun hasApiKey(): Boolean {
        val key = getApiKey()
        return !key.isNullOrBlank()
    }

    fun clearApiKey() {
        prefs.edit().clear().apply()
    }
}
