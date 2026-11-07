package com.example.fllscorer

import android.content.Context

@Volatile
var androidAppContext: Context? = null

actual fun loadSetting(key: String): String? =
    androidAppContext
        ?.getSharedPreferences("fllscorer_settings", Context.MODE_PRIVATE)
        ?.getString(key, null)

actual fun saveSetting(key: String, value: String) {
    androidAppContext
        ?.getSharedPreferences("fllscorer_settings", Context.MODE_PRIVATE)
        ?.edit()
        ?.putString(key, value)
        ?.apply()
}

actual fun clearSetting(key: String) {
    androidAppContext
        ?.getSharedPreferences("fllscorer_settings", Context.MODE_PRIVATE)
        ?.edit()
        ?.remove(key)
        ?.apply()
}
