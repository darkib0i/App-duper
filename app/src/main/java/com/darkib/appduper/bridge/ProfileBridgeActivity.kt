package com.darkib.appduper.bridge

import android.app.Activity
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.darkib.appduper.admin.DuperAdmin
import com.darkib.appduper.core.Bridge

/**
 * Invisible activity that runs inside the Dupe Space. It receives commands
 * forwarded across the profile boundary and executes them as profile owner.
 */
class ProfileBridgeActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            handleCommand()
        } finally {
            finish()
        }
    }

    private fun handleCommand() {
        val dpm = getSystemService(DevicePolicyManager::class.java)
        if (!dpm.isProfileOwnerApp(packageName)) return

        val pkg = intent.getStringExtra(Bridge.EXTRA_PACKAGE) ?: return
        when (intent.getStringExtra(Bridge.EXTRA_COMMAND)) {
            Bridge.CMD_CLONE -> {
                val ok = try {
                    dpm.installExistingPackage(DuperAdmin.component(this), pkg)
                } catch (e: Exception) {
                    false
                }
                if (!ok) {
                    Toast.makeText(this, "Couldn't dupe $pkg", Toast.LENGTH_LONG).show()
                }
            }
            Bridge.CMD_REMOVE -> {
                try {
                    val callback = PendingIntent.getBroadcast(
                        this, 0,
                        Intent("com.darkib.appduper.UNINSTALL_RESULT").setPackage(packageName),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    packageManager.packageInstaller.uninstall(pkg, callback.intentSender)
                } catch (e: Exception) {
                    Toast.makeText(this, "Couldn't remove $pkg", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
