package com.darkib.appduper.core

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

/** App metadata only — no bitmap. Icons are loaded lazily, per visible card. */
data class AppEntry(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
)

object AppRepository {

    private const val ICON_SIZE = 132

    /**
     * Every launchable app on the device, except App Duper itself. This is
     * intentionally cheap: it resolves names only. Icons are decoded later,
     * off the main thread, one visible card at a time (see [loadIcon]) — that
     * keeps memory bounded and stops a single bad icon from crashing the UI.
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
            .distinctBy { it.activityInfo?.packageName }
            .filter { it.activityInfo?.packageName != null && it.activityInfo.packageName != context.packageName }
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                val label = runCatching { info.loadLabel(pm).toString() }
                    .getOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?: pkg
                val isSystem = runCatching {
                    val flags = info.activityInfo.applicationInfo?.flags ?: 0
                    flags and ApplicationInfo.FLAG_SYSTEM != 0
                }.getOrDefault(false)
                AppEntry(pkg, label, isSystem)
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    // Small in-memory cache so scrolling doesn't re-decode icons.
    private val iconCache = LruCache<String, ImageBitmap>(256)

    /**
     * Safely decode a single app's icon. Forces an ARGB_8888 software bitmap
     * (never a hardware bitmap, which can crash when drawn in Compose) and
     * falls back to a neutral tile if anything goes wrong. Call off the main
     * thread.
     */
    fun loadIcon(context: Context, packageName: String): ImageBitmap {
        iconCache.get(packageName)?.let { return it }
        val bitmap = runCatching {
            context.packageManager
                .getApplicationIcon(packageName)
                .toBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)
                .asImageBitmap()
        }.getOrElse { fallback }
        iconCache.put(packageName, bitmap)
        return bitmap
    }

    /** Neutral placeholder used when a real icon can't be decoded. */
    private val fallback: ImageBitmap by lazy {
        val bmp = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)
        Canvas(bmp).drawColor(Color.argb(255, 42, 42, 48))
        bmp.asImageBitmap()
    }
}
