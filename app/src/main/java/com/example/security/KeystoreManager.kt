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
import javax.crypto.spec.SecretKeySpec

class KeystoreManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("ahi_keystore_vault", Context.MODE_PRIVATE)

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "ahi_gemini_master_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val PREF_KEY_CIPHERTEXT = "encrypted_gemini_key"
        private const val PREF_KEY_IV = "gemini_key_iv"
        private const val PREF_KEY_FLAG = "api_key_configured_flag"
        private const val PREF_KEY_BACKUP = "vault_token_backup"
    }

    private fun getOrCreateSecretKey(): SecretKey? {
        return try {
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
                keyGenerator.generateKey()
            }
            (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        } catch (e: Exception) {
            null
        }
    }

    // Static device-bound seed for fallback encryption if AndroidKeyStore daemon is unavailable
    private fun getFallbackKey(): SecretKey {
        val seed = (context.packageName + "ahi_neural_vault_seed_2026").toByteArray(Charsets.UTF_8)
        val keyBytes = ByteArray(32) { i -> if (i < seed.size) seed[i] else (i xor 0x5A).toByte() }
        return SecretKeySpec(keyBytes, "AES")
    }

    fun saveApiKey(rawKey: String): Boolean {
        val trimmed = rawKey.trim()
        if (trimmed.isBlank()) return false

        var saved = false

        // 1. Primary: Android Keystore Hardware AES-GCM
        try {
            val secretKey = getOrCreateSecretKey()
            if (secretKey != null) {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                val iv = cipher.iv
                val encryptedBytes = cipher.doFinal(trimmed.toByteArray(Charsets.UTF_8))

                prefs.edit()
                    .putString(PREF_KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                    .putString(PREF_KEY_CIPHERTEXT, Base64.encodeToString(encryptedBytes, Base64.NO_WRAP))
                    .putBoolean(PREF_KEY_FLAG, true)
                    .commit()
                saved = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Reliable local vault backup (so key is NEVER lost across restarts/reboots)
        try {
            val fallbackKey = getFallbackKey()
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, fallbackKey)
            val encryptedBackup = cipher.doFinal(trimmed.toByteArray(Charsets.UTF_8))

            prefs.edit()
                .putString(PREF_KEY_BACKUP, Base64.encodeToString(encryptedBackup, Base64.NO_WRAP))
                .putBoolean(PREF_KEY_FLAG, true)
                .commit()
            saved = true
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return saved
    }

    fun getApiKey(): String? {
        // Try Keystore first
        try {
            val ivBase64 = prefs.getString(PREF_KEY_IV, null)
            val cipherBase64 = prefs.getString(PREF_KEY_CIPHERTEXT, null)
            if (ivBase64 != null && cipherBase64 != null) {
                val secretKey = getOrCreateSecretKey()
                if (secretKey != null) {
                    val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
                    val encryptedBytes = Base64.decode(cipherBase64, Base64.NO_WRAP)
                    val cipher = Cipher.getInstance(TRANSFORMATION)
                    val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                    cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
                    val decrypted = String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
                    if (decrypted.isNotBlank()) return decrypted
                }
            }
        } catch (e: Exception) {
            // fallback to backup
        }

        // Fallback to local encrypted backup
        try {
            val backupBase64 = prefs.getString(PREF_KEY_BACKUP, null)
            if (backupBase64 != null) {
                val encryptedBackup = Base64.decode(backupBase64, Base64.NO_WRAP)
                val fallbackKey = getFallbackKey()
                val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, fallbackKey)
                val decrypted = String(cipher.doFinal(encryptedBackup), Charsets.UTF_8)
                if (decrypted.isNotBlank()) return decrypted
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    fun hasApiKey(): Boolean {
        val flag = prefs.getBoolean(PREF_KEY_FLAG, false)
        val key = getApiKey()
        return flag && !key.isNullOrBlank()
    }

    fun isConfigured(): Boolean {
        return prefs.getBoolean(PREF_KEY_FLAG, false) || !getApiKey().isNullOrBlank()
    }

    fun getMaskedApiKey(): String {
        val key = getApiKey() ?: return "Not configured"
        if (key.length <= 10) return "••••••••"
        return key.take(6) + "••••••••" + key.takeLast(4)
    }

    fun clearApiKey() {
        prefs.edit().clear().commit()
    }
}
