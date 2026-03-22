package com.eclipse.browser.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Small utility for encrypting sensitive strings before persistence.
 * Uses Android Keystore-backed AES/GCM keys.
 */
object StorageCrypto {
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "eclipse_secure_storage"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PREFIX = "enc:v1:"

    fun encrypt(context: Context, plainText: String): String {
        if (plainText.isBlank() || plainText.startsWith(PREFIX)) return plainText
        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            val packed = ByteBuffer.allocate(4 + iv.size + encrypted.size)
                .putInt(iv.size)
                .put(iv)
                .put(encrypted)
                .array()
            PREFIX + Base64.encodeToString(packed, Base64.NO_WRAP)
        } catch (_: Exception) {
            plainText
        }
    }

    fun decrypt(context: Context, cipherText: String): String {
        if (cipherText.isBlank() || !cipherText.startsWith(PREFIX)) return cipherText
        return try {
            val bytes = Base64.decode(cipherText.removePrefix(PREFIX), Base64.NO_WRAP)
            val buffer = ByteBuffer.wrap(bytes)
            val ivSize = buffer.int
            if (ivSize <= 0 || ivSize > 32) return ""

            val iv = ByteArray(ivSize)
            buffer.get(iv)
            val encrypted = ByteArray(buffer.remaining())
            buffer.get(encrypted)

            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}
