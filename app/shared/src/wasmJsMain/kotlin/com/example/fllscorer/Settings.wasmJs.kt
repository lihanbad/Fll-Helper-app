package com.example.fllscorer

actual fun loadSetting(key: String): String? =
    kotlinx.browser.window.localStorage.getItem("fllscorer.$key")

actual fun saveSetting(key: String, value: String) {
    kotlinx.browser.window.localStorage.setItem("fllscorer.$key", value)
}

actual fun clearSetting(key: String) {
    kotlinx.browser.window.localStorage.removeItem("fllscorer.$key")
}
