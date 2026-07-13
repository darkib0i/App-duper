package com.darkib.appduper.install

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.widget.Toast

/**
 * Receives PackageInstaller session status. For a non-privileged installer the
 * key case is STATUS_PENDING_USER_ACTION: the system hands back an intent we
 * must launch to show the user the install/uninstall confirmation dialog.
 */
class InstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    if (confirm != null) context.startActivity(confirm)
                } catch (t: Throwable) {
                    // ignore
                }
            }
            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                // user cancelled; stay quiet
            }
            PackageInstaller.STATUS_SUCCESS -> {
                // handled by the app polling for the new package
            }
            else -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                if (!message.isNullOrBlank()) {
                    Toast.makeText(context, "Dupe install: $message", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    companion object {
        const val ACTION = "com.darkib.appduper.INSTALL_STATUS"
    }
}
