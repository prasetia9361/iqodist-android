package com.iqodist.core.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences>
    by preferencesDataStore(name = "iqodist_session")

@Singleton
class SessionManager @Inject constructor( @ApplicationContext private val context: Context)
{
    suspend fun saveSession(
        accessToken: String,
        refreshToken: String,
        userId: String,
        userName: String,
        userRole: String,
        entityId: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[SessionKeys.ACCESS_TOKEN]  = accessToken
            prefs[SessionKeys.REFRESH_TOKEN] = refreshToken
            prefs[SessionKeys.USER_ID]       = userId
            prefs[SessionKeys.USER_NAME]     = userName
            prefs[SessionKeys.USER_ROLE]     = userRole
            prefs[SessionKeys.ENTITY_ID]     = entityId
            prefs[SessionKeys.IS_LOGGED_IN]  = true
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs -> prefs.clear() }
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[SessionKeys.IS_LOGGED_IN] ?: false }

    val userRole: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[SessionKeys.USER_ROLE] }

    val accessToken: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[SessionKeys.ACCESS_TOKEN] }

    val userName: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[SessionKeys.USER_NAME] }
}
