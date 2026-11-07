package com.example.fllscorer

import android.app.Activity

/** The F-Droid build delegates updates to the F-Droid client. */
object ReleaseUpdater {
    fun check(onResult: (UpdateCheckResult) -> Unit) {
        onResult(UpdateCheckResult("Updates are managed by F-Droid."))
    }

    fun downloadAndInstall(activity: Activity, url: String, onStatus: (String) -> Unit) {
        onStatus("Install updates through F-Droid.")
    }
}
