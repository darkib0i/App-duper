package com.darkib.appduper.core

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap,
    val isSystem: Boolean,
)

object AppRepository {

    /** Every launchable app on the device, except App Duper itself. */
    fun loadLaunchableApps(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)

        return resolved
            .asSequence()
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != context.packageName }
            .map { info ->
                val appInfo = info.activityInfo.applicationInfo
                AppEntry(
                    packageName = info.activityInfo.packageName,
                    label = info.loadLabel(pm).toString(),
                    icon = info.loadIcon(pm).toBitmap(ICON_SIZE, ICON_SIZE).asImageBitmap(),
                    isSystem = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    private const val ICON_SIZE = 144
}
