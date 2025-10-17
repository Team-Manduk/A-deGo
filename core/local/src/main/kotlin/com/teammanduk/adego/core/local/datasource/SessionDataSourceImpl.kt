package com.teammanduk.adego.core.local.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.teammanduk.adego.core.data_api.datasource.SessionDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionDataSourceImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SessionDataSource {

    private object PreferencesKeys {
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val ROOM_ID = stringPreferencesKey("room_id")
        val ROOM_NAME = stringPreferencesKey("room_name")
    }

    override suspend fun saveUserId(userId: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_ID] = userId
        }
    }

    override suspend fun getUserId(): String? {
        return dataStore.data.map { preferences ->
            preferences[PreferencesKeys.USER_ID]
        }.first()
    }

    override suspend fun saveUserName(userName: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = userName
        }
    }

    override suspend fun getUserName(): String? {
        return dataStore.data.map { preferences ->
            preferences[PreferencesKeys.USER_NAME]
        }.first()
    }

    override suspend fun saveRoomId(roomId: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ROOM_ID] = roomId
        }
    }

    override suspend fun getRoomId(): String? {
        return dataStore.data.map { preferences ->
            preferences[PreferencesKeys.ROOM_ID]
        }.first()
    }

    override suspend fun saveRoomName(roomName: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ROOM_NAME] = roomName
        }
    }

    override suspend fun getRoomName(): String? {
        return dataStore.data.map { preferences ->
            preferences[PreferencesKeys.ROOM_NAME]
        }.first()
    }

    override suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
