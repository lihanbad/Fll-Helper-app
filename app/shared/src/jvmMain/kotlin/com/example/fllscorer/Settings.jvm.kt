package com.example.fllscorer

import java.io.File
import java.util.Properties

private val settingsFile by lazy {
    val home = System.getProperty("user.home") ?: "."
    File(home, ".fllscorer").apply { createNewFile() }
}

private val props by lazy {
    Properties().apply {
        settingsFile.inputStream().use { load(it) }
    }
}

private fun flush() {
    settingsFile.outputStream().use { props.store(it, "FLL Scorer settings") }
}

actual fun loadSetting(key: String): String? = props.getProperty(key)

actual fun saveSetting(key: String, value: String) {
    props.setProperty(key, value)
    flush()
}

actual fun clearSetting(key: String) {
    props.remove(key)
    flush()
}
