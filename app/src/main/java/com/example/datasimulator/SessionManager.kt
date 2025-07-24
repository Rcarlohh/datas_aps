package com.example.datasimulator

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_ID = "current_user_id"
        private const val KEY_USERNAME = "current_username"
        private const val KEY_FULL_NAME = "current_full_name"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    fun saveUserSession(user: DatabaseHelper.User) {
        sharedPreferences.edit().apply {
            putLong(KEY_USER_ID, user.id)
            putString(KEY_USERNAME, user.username)
            putString(KEY_FULL_NAME, user.fullName)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    fun getCurrentUserId(): Long {
        return sharedPreferences.getLong(KEY_USER_ID, -1)
    }

    fun getCurrentUsername(): String? {
        return sharedPreferences.getString(KEY_USERNAME, null)
    }

    fun getCurrentFullName(): String? {
        return sharedPreferences.getString(KEY_FULL_NAME, null)
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false) &&
                getCurrentUserId() != -1L
    }

    fun clearSession() {
        sharedPreferences.edit().clear().apply()
    }
}