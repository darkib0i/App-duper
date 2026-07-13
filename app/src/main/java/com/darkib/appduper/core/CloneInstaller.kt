package com.darkib.appduper.core

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import com.darkib.appduper.install.InstallReceiver
import java.io.File

/** Installs (and uninstalls) cloned APKs via [PackageInstaller]. */
object CloneInstaller {

    /**
     * Streams the signed clone APK(s) into an install session and commits it.
     * The system then shows its install confirmation dialog to the user (App
     * Duper must be allowed to install unknown apps).
     */
    fun install(context: Context, apks: List<File>) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL
        )
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            apks.forEachIndexed { index, apk ->
                session.openWrite("part$index.apk", 0, apk.length()).use { out ->
                    apk.inputStream().use { it.copyTo(out) }
                    session.fsync(out)
                }
            }
            session.commit(statusSender(context, sessionId))
        }
    }

    fun uninstall(context: Context, packageName: String) {
        context.packageManager.packageInstaller.uninstall(packageName, statusSender(context, packageName.hashCode()))
    }

    private fun statusSender(context: Context, requestCode: Int): android.content.IntentSender {
        val intent = Intent(context, InstallReceiver::class.java)
            .setAction(InstallReceiver.ACTION)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        return PendingIntent.getBroadcast(context, requestCode, intent, flags).intentSender
    }
}
