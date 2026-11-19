package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "talikhata_prefs")

enum class AppLanguage {
    BN, EN
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.BN,
    val onboardingCompleted: Boolean = false,
    val pinEnabled: Boolean = false,
    val pinHash: String = "",
    val fingerprintEnabled: Boolean = false,
    val autoBackupEnabled: Boolean = true,
    val lastAutoBackupAt: Long = 0L,
    val smsTemplate: String = "প্রিয় {name}, আপনার {shop}-তে মোট বকেয়া {due}। অনুগ্রহ করে দ্রুত পরিশোধ করুন। ধন্যবাদ।",
    val printShowAddress: Boolean = true,
    val printShowPhone: Boolean = true,
    val defaultCommonNote: String = "অনুগ্রহ করে নির্ধারিত সময়ের মধ্যে বকেয়া পরিশোধ করুন। ধন্যবাদ।",
    val currencySymbol: String = "৳",
    val overdueDaysThreshold: Int = 7
)

class PreferencesManager(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val FINGERPRINT_ENABLED = booleanPreferencesKey("fingerprint_enabled")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val LAST_AUTO_BACKUP_AT = longPreferencesKey("last_auto_backup_at")
        val SMS_TEMPLATE = stringPreferencesKey("sms_template")
        val PRINT_SHOW_ADDRESS = booleanPreferencesKey("print_show_address")
        val PRINT_SHOW_PHONE = booleanPreferencesKey("print_show_phone")
        val DEFAULT_COMMON_NOTE = stringPreferencesKey("default_common_note")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val OVERDUE_DAYS_THRESHOLD = intPreferencesKey("overdue_days_threshold")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            language = runCatching {
                AppLanguage.valueOf(prefs[Keys.LANGUAGE] ?: AppLanguage.BN.name)
            }.getOrDefault(AppLanguage.BN),
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            pinEnabled = prefs[Keys.PIN_ENABLED] ?: false,
            pinHash = prefs[Keys.PIN_HASH] ?: "",
            fingerprintEnabled = prefs[Keys.FINGERPRINT_ENABLED] ?: false,
            autoBackupEnabled = prefs[Keys.AUTO_BACKUP_ENABLED] ?: true,
            lastAutoBackupAt = prefs[Keys.LAST_AUTO_BACKUP_AT] ?: 0L,
            smsTemplate = prefs[Keys.SMS_TEMPLATE]
                ?: "প্রিয় {name}, আপনার {shop}-তে মোট বকেয়া {due}। অনুগ্রহ করে দ্রুত পরিশোধ করুন। ধন্যবাদ।",
            printShowAddress = prefs[Keys.PRINT_SHOW_ADDRESS] ?: true,
            printShowPhone = prefs[Keys.PRINT_SHOW_PHONE] ?: true,
            defaultCommonNote = prefs[Keys.DEFAULT_COMMON_NOTE]
                ?: "অনুগ্রহ করে নির্ধারিত সময়ের মধ্যে বকেয়া পরিশোধ করুন। ধন্যবাদ।",
            currencySymbol = prefs[Keys.CURRENCY_SYMBOL] ?: "৳",
            overdueDaysThreshold = prefs[Keys.OVERDUE_DAYS_THRESHOLD] ?: 7
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun completeOnboarding() {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = true }
    }

    suspend fun resetOnboarding() {
        context.dataStore.edit {
            it[Keys.ONBOARDING_COMPLETED] = false
            it[Keys.PIN_ENABLED] = false
            it[Keys.PIN_HASH] = ""
            it[Keys.FINGERPRINT_ENABLED] = false
        }
    }

    suspend fun setPin(pin: String) {
        val hash = hashPin(pin)
        context.dataStore.edit {
            it[Keys.PIN_ENABLED] = true
            it[Keys.PIN_HASH] = hash
        }
    }

    suspend fun disablePin() {
        context.dataStore.edit {
            it[Keys.PIN_ENABLED] = false
            it[Keys.PIN_HASH] = ""
            it[Keys.FINGERPRINT_ENABLED] = false
        }
    }

    suspend fun setFingerprintEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FINGERPRINT_ENABLED] = enabled }
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_BACKUP_ENABLED] = enabled }
    }

    suspend fun setLastAutoBackupAt(timestamp: Long) {
        context.dataStore.edit { it[Keys.LAST_AUTO_BACKUP_AT] = timestamp }
    }

    suspend fun setSmsTemplate(template: String) {
        context.dataStore.edit { it[Keys.SMS_TEMPLATE] = template }
    }

    suspend fun setPrintSettings(showAddress: Boolean, showPhone: Boolean, defaultNote: String) {
        context.dataStore.edit {
            it[Keys.PRINT_SHOW_ADDRESS] = showAddress
            it[Keys.PRINT_SHOW_PHONE] = showPhone
            it[Keys.DEFAULT_COMMON_NOTE] = defaultNote
        }
    }

    suspend fun setCurrencySymbol(symbol: String) {
        context.dataStore.edit { it[Keys.CURRENCY_SYMBOL] = symbol }
    }

    suspend fun setOverdueDaysThreshold(days: Int) {
        context.dataStore.edit { it[Keys.OVERDUE_DAYS_THRESHOLD] = days.coerceIn(1, 90) }
    }

    companion object {
        fun hashPin(pin: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest("talikhata_amol_salt_$pin".toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
