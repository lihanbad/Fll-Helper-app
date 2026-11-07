package com.example.fllscorer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val settings = remember {
                getSharedPreferences("fll_scorer_setup", MODE_PRIVATE)
            }
            val setupComplete = settings.getBoolean("complete", false)
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
                initialEdition = settings.getString("edition", "FOUNDERS") ?: "FOUNDERS",
                initialTheme = settings.getString("theme", "MINT") ?: "MINT",
                showUpdateControls = BuildConfig.SELF_UPDATER_ENABLED && setupComplete,
                updateMessage = updateMessage,
                updateAvailable = updateUrl != null,
                onSetupComplete = { edition, theme ->
                    settings.edit()
                        .putBoolean("complete", true)
                        .putString("edition", edition)
                        .putString("theme", theme)
                        .apply()
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

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
