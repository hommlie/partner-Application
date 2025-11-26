package com.hommlie.partner.utils

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharePreference  @Inject constructor(
    @ApplicationContext context: Context
) {

    companion object {
        private const val PREF_NAME = "hm_partner"
    }


    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun setString(key: String, value: String) {
        sharedPrefs.edit().putString(key, value).apply()
    }

    fun getString(key: String, default: String = ""): String {
        return sharedPrefs.getString(key, default) ?: default
    }

    fun setInt(key: String, value: Int) {
        sharedPrefs.edit().putInt(key, value).apply()
    }

    fun getInt(key: String, default: Int = -1): Int {
        return sharedPrefs.getInt(key, default)
    }

    fun setBoolean(key: String, value: Boolean) {
        sharedPrefs.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean {
        return sharedPrefs.getBoolean(key, default)
    }

    fun clearKey(key: String) {
        sharedPrefs.edit().remove(key).apply()
    }

    fun clearAll() {
        sharedPrefs.edit().clear().apply()
    }

}