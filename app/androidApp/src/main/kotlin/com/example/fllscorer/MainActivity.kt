package com.example.fllscorer

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        androidAppContext = applicationContext

        setContent {
            var setupComplete by remember { mutableStateOf(loadSetting("setup_complete") == "true") }
            var updateMessage by remember {
                mutableStateOf(if (BuildConfig.SELF_UPDATER_ENABLED) "Checking for updates…" else null)
            }
            var updateUrl by remember { mutableStateOf<String?>(null) }
            val checkForUpdates = {
                updateMessage = "Checking for updates…"
                ReleaseUpdater.check { result ->
                    runOnUiThread {
                        updateMessage = result.message
                        updateUrl = result.apkUrl
                    }
                }
            }
            LaunchedEffect(BuildConfig.SELF_UPDATER_ENABLED, setupComplete) {
                if (BuildConfig.SELF_UPDATER_ENABLED && setupComplete) checkForUpdates()
            }
            App(
                setupRequired = !setupComplete,
                initialEdition = loadSetting("edition") ?: "FOUNDERS",
                initialTheme = loadSetting("theme") ?: "MINT",
                showUpdateControls = BuildConfig.SELF_UPDATER_ENABLED && setupComplete,
                updateMessage = updateMessage,
                updateAvailable = updateUrl != null,
                onSetupComplete = { edition, theme ->
                    saveSetting("setup_complete", "true")
                    saveSetting("edition", edition)
                    saveSetting("theme", theme)
                    setupComplete = true
                },
                onCheckForUpdates = checkForUpdates,
                onInstallUpdate = {
                    updateUrl?.let { url ->
                        ReleaseUpdater.downloadAndInstall(this@MainActivity, url) { message ->
                            runOnUiThread { updateMessage = message }
                        }
                    }
                },
            )
        }
    }
}

data class UpdateCheckResult(val message: String, val apkUrl: String? = null)
