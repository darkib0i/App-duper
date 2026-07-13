package com.darkib.appduper

import android.app.Application
import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Captures any uncaught crash to a file so the next launch can show the exact
 * stack trace on-screen (with a copy button) instead of the app just vanishing.
 */
class AppDuperApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                File(filesDir, CRASH_FILE).writeText(sw.toString())
            } catch (_: Throwable) {
                // best effort
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        private const val CRASH_FILE = "last_crash.txt"

        /** Returns the last crash trace (if any) and clears it. */
        fun consumeLastCrash(context: Context): String? {
            val file = File(context.filesDir, CRASH_FILE)
            if (!file.exists()) return null
            return try {
                val text = file.readText()
                file.delete()
                text.takeIf { it.isNotBlank() }
            } catch (_: Throwable) {
                null
            }
        }
    }
}
