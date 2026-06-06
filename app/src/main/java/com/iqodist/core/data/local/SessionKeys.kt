package com.iqodist.core.data.local

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object SessionKeys {
    val ACCESS_TOKEN  = stringPreferencesKey("access_token")
    val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    val USER_ROLE     = stringPreferencesKey("user_role")
    val USER_ID       = stringPreferencesKey("user_id")
    val USER_NAME     = stringPreferencesKey("user_name")
    val ENTITY_ID     = stringPreferencesKey("entity_id")
    val IS_LOGGED_IN  = booleanPreferencesKey("is_logged_in")
}
