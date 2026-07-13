package com.darkib.appduper.core

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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

    private const val ICON_SIZE = 132

    /**
     * Every launchable app on the device, except App Duper itself.
     *
     * Loading is defensive on purpose: a single app whose icon or label can't
     * be resolved (a broken package, a stale instant app, a locked profile
     * entry) must never crash the whole list — we fall back to a plain icon
     * or, worst case, skip just that entry.
     */
    fun loadLaunchableApps(context: Context): List<AppEntry> {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = try {
            pm.queryIntentActivities(launcherIntent, 0)
        } catch (t: Throwable) {
            emptyList()
        }

        return resolved
            .asSequence()
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != context.packageName }
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                try {
                    val appInfo: ApplicationInfo? = info.activityInfo.applicationInfo
                    val label = runCatching { info.loadLabel(pm).toString() }
                        .getOrNull()
                        ?.takeIf { it.isNotBlank() }
                        ?: pkg
                    val icon = runCatching {
                        info.loadIcon(pm).toBitmap(ICON_SIZE, ICON_SIZE).asImageBitmap()
                    }.getOrElse { fallbackIcon() }
                    val isSystem = appInfo != null &&
                        appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
                    AppEntry(pkg, label, icon, isSystem)
                } catch (t: Throwable) {
                    null
                }
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    /** Shared neutral placeholder used when an app's real icon can't be loaded. */
    private val fallback: ImageBitmap by lazy {
        val bmp = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)
        Canvas(bmp).drawColor(Color.argb(255, 42, 42, 48))
        bmp.asImageBitmap()
    }

    private fun fallbackIcon(): ImageBitmap = fallback
}
