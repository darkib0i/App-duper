package com.darkib.appduper.core

/**
 * Shared constants for the cross-profile bridge between the main-profile
 * instance of App Duper and its twin running inside the Dupe Space
 * (managed work profile).
 */
object Bridge {
    const val ACTION_BRIDGE = "com.darkib.appduper.action.BRIDGE"
    const val PERMISSION = "com.darkib.appduper.BRIDGE"

    const val EXTRA_COMMAND = "com.darkib.appduper.extra.COMMAND"
    const val EXTRA_PACKAGE = "com.darkib.appduper.extra.PACKAGE"

    const val CMD_CLONE = "clone"
    const val CMD_REMOVE = "remove"
}
