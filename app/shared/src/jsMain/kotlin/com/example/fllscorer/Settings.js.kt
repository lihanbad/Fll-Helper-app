package com.example.fllscorer

import kotlinx.browser.window

actual fun loadSetting(key: String): String? = window.localStorage.getItem("fllscorer.$key")

actual fun saveSetting(key: String, value: String) {
    window.localStorage.setItem("fllscorer.$key", value)
}

actual fun clearSetting(key: String) {
    window.localStorage.removeItem("fllscorer.$key")
}
