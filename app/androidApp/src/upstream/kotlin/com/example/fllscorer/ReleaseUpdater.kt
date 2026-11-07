package com.example.fllscorer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Checks public GitHub releases and launches Android's normal package installer. */
object ReleaseUpdater {
    private const val RELEASE_API = "https://api.github.com/repos/lihanbad/Fll-Helper-app/releases/latest"
    private const val APK_ASSET = "fll-helper-app.apk"

    fun check(onResult: (UpdateCheckResult) -> Unit) {
        Thread {
            val result: UpdateCheckResult = runCatching<UpdateCheckResult> {
                val connection = (URL(RELEASE_API).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 15_000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "FLL-Helper-Android")
                }
                try {
                    if (connection.responseCode == 404)
                        return@runCatching UpdateCheckResult("No public release has been published yet.")
                    if (connection.responseCode !in 200..299) error("GitHub update check failed")
                    val release = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                    val latest = release.optString("tag_name").removePrefix("v")
                    val current = BuildConfig.VERSION_NAME
                    if (!isNewer(latest, current)) {
                        UpdateCheckResult("You're up to date ($current).")
                    } else {
                        val assets = release.optJSONArray("assets")
                        val apk = (0 until (assets?.length() ?: 0))
                            .map { assets!!.getJSONObject(it) }
                            .firstOrNull { it.optString("name") == APK_ASSET }
                        if (apk == null) UpdateCheckResult("Version $latest is available; its APK is not attached yet.")
                        else UpdateCheckResult("Version $latest is ready to install.", apk.optString("browser_download_url"))
                    }
                } finally {
                    connection.disconnect()
                }
            }.getOrElse { UpdateCheckResult("Could not check GitHub for updates. Try again later.") }
            onResult(result)
        }.start()
    }

    fun downloadAndInstall(activity: Activity, url: String, onStatus: (String) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !activity.packageManager.canRequestPackageInstalls()) {
            val settingsIntent = Intent(
                android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${activity.packageName}"),
            )
            activity.startActivity(settingsIntent)
            onStatus("Allow installs from FLL Helper, then tap INSTALL UPDATE again.")
            return
        }

        onStatus("Downloading update…")
        Thread {
            val outcome = runCatching {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20_000
                    readTimeout = 30_000
                    setRequestProperty("User-Agent", "FLL-Helper-Android")
                }
                val updateDir = File(activity.cacheDir, "updates").apply { mkdirs() }
                val apkFile = File(updateDir, APK_ASSET)
                try {
                    if (connection.responseCode !in 200..299) error("APK download failed")
                    connection.inputStream.use { input ->
                        apkFile.outputStream().use { output -> input.copyTo(output) }
                    }
                } finally {
                    connection.disconnect()
                }
                val apkUri = FileProvider.getUriForFile(
                    activity,
                    "${BuildConfig.APPLICATION_ID}.fileprovider",
                    apkFile,
                )
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                activity.startActivity(installIntent)
                "Downloaded. Confirm the update in Android's installer."
            }.getOrElse { "Update download failed. Check your connection and try again." }
            activity.runOnUiThread { onStatus(outcome) }
        }.start()
    }

    private fun isNewer(latest: String, current: String): Boolean {
        val latestParts = Regex("\\d+").findAll(latest).map { it.value.toInt() }.toList()
        val currentParts = Regex("\\d+").findAll(current).map { it.value.toInt() }.toList()
        if (latestParts.isEmpty() || currentParts.isEmpty()) return false
        for (index in 0 until maxOf(latestParts.size, currentParts.size)) {
            val latestPart = latestParts.getOrElse(index) { 0 }
            val currentPart = currentParts.getOrElse(index) { 0 }
            if (latestPart != currentPart) return latestPart > currentPart
        }
        return false
    }
}
