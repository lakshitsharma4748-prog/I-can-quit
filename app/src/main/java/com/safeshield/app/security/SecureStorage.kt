package com.safeshield.app.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Creates the Keystore-backed encrypted preferences file used to persist
 * the administrator PIN's salt/hash/rate-limit state.
 *
 * [EncryptedSharedPreferences] is Jetpack Security's wrapper around the
 * Android Keystore: the master key is generated and kept inside Keystore
 * (never readable in plaintext, even by SafeShield itself), and every
 * value written to this preferences file is encrypted with a key derived
 * from it. This satisfies the PRD's "never store PIN as plaintext, use
 * Android Keystore" requirement without hand-rolling Keystore `Cipher`
 * calls, which are easy to get subtly wrong.
 */
internal object SecureStorage {
    private const val PREFS_FILE_NAME = "safeshield_secure_prefs"

    fun encryptedPreferences(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
