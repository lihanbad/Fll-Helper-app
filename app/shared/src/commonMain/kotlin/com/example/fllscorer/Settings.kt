package com.example.fllscorer

/** Cross-platform simple key-value settings (Android prefs / desktop file / browser localStorage). */
expect fun loadSetting(key: String): String?

expect fun saveSetting(key: String, value: String)

expect fun clearSetting(key: String)
