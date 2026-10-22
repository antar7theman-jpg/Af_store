package com.example.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.firebase.StaffAccount
import com.example.data.firebase.StaffAccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_session_datastore")

data class SavedUserCredentials(
    val rememberMe: Boolean = true,
    val identifier: String = "",
    val secretPin: String = "",
    val userId: String = "",
    val fullName: String = "",
    val role: String = "",
    val isDirectOwner: Boolean = false,
    val isSessionActive: Boolean = false
)

class UserSessionDataStore(private val context: Context) {

    companion object {
        val KEY_REMEMBER_ME = booleanPreferencesKey("key_remember_me")
        val KEY_SAVED_IDENTIFIER = stringPreferencesKey("key_saved_identifier")
        val KEY_SAVED_SECRET = stringPreferencesKey("key_saved_secret")
        val KEY_USER_ID = stringPreferencesKey("key_user_id")
        val KEY_FULL_NAME = stringPreferencesKey("key_full_name")
        val KEY_ROLE = stringPreferencesKey("key_role")
        val KEY_IS_DIRECT_OWNER = booleanPreferencesKey("key_is_direct_owner")
        val KEY_SESSION_ACTIVE = booleanPreferencesKey("key_session_active")
        val KEY_SESSION_TOKEN = stringPreferencesKey("key_session_token")
    }

    val savedCredentialsFlow: Flow<SavedUserCredentials> = context.userDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            SavedUserCredentials(
                rememberMe = prefs[KEY_REMEMBER_ME] ?: true,
                identifier = prefs[KEY_SAVED_IDENTIFIER] ?: "",
                secretPin = prefs[KEY_SAVED_SECRET] ?: "",
                userId = prefs[KEY_USER_ID] ?: "",
                fullName = prefs[KEY_FULL_NAME] ?: "",
                role = prefs[KEY_ROLE] ?: "",
                isDirectOwner = prefs[KEY_IS_DIRECT_OWNER] ?: false,
                isSessionActive = prefs[KEY_SESSION_ACTIVE] ?: false
            )
        }

    suspend fun saveUserSession(
        identifier: String,
        secretPin: String,
        account: StaffAccount,
        rememberMe: Boolean = true,
        isDirectOwner: Boolean = false,
        sessionToken: String = ""
    ) {
        context.userDataStore.edit { prefs ->
            prefs[KEY_REMEMBER_ME] = rememberMe
            if (rememberMe) {
                prefs[KEY_SAVED_IDENTIFIER] = identifier
                prefs[KEY_SAVED_SECRET] = secretPin
                prefs[KEY_USER_ID] = account.userId
                prefs[KEY_FULL_NAME] = account.fullName
                prefs[KEY_ROLE] = account.role
                prefs[KEY_IS_DIRECT_OWNER] = isDirectOwner || account.isOwner
                prefs[KEY_SESSION_ACTIVE] = true
                if (sessionToken.isNotBlank()) {
                    prefs[KEY_SESSION_TOKEN] = sessionToken
                }
            } else {
                prefs[KEY_SAVED_IDENTIFIER] = ""
                prefs[KEY_SAVED_SECRET] = ""
                prefs[KEY_USER_ID] = ""
                prefs[KEY_FULL_NAME] = ""
                prefs[KEY_ROLE] = ""
                prefs[KEY_IS_DIRECT_OWNER] = false
                prefs[KEY_SESSION_ACTIVE] = false
                prefs[KEY_SESSION_TOKEN] = ""
            }
        }
    }

    suspend fun setRememberMePreference(rememberMe: Boolean) {
        context.userDataStore.edit { prefs ->
            prefs[KEY_REMEMBER_ME] = rememberMe
        }
    }

    suspend fun clearSession() {
        context.userDataStore.edit { prefs ->
            val rememberPreference = prefs[KEY_REMEMBER_ME] ?: true
            prefs.clear()
            prefs[KEY_REMEMBER_ME] = rememberPreference
            prefs[KEY_SESSION_ACTIVE] = false
        }
    }

    suspend fun getSavedCredentials(): SavedUserCredentials? {
        val creds = savedCredentialsFlow.firstOrNull()
        return if (creds != null && creds.isSessionActive && creds.rememberMe) {
            creds
        } else {
            null
        }
    }
}
