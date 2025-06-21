package com.android.launcher3

object Flags {

    @JvmStatic
    fun restoreArchivedAppIconsFromDb(): Boolean {
        return true
    }

    @JvmStatic
    fun forceMonochromeAppIcons(): Boolean {
        return false
    }

    @JvmStatic
    fun useNewIconForArchivedApps(): Boolean {
        return true
    }

    @JvmStatic
    fun enableLauncherIconShapes(): Boolean {
        return true
    }

    @JvmStatic
    fun enableSmartspaceRemovalToggle(): Boolean {
        return true
    }

    @JvmStatic
    fun enableWorkspaceInflation(): Boolean {
        return true
    }

    @JvmStatic
    fun oneGridSpecs(): Boolean {
        return true
    }

    @JvmStatic
    fun enableSupportForArchiving(): Boolean {
        return true
    }

    @JvmStatic
    fun restoreArchivedShortcuts(): Boolean {
        return true
    }

    @JvmStatic
    fun newCustomizationPickerUi(): Boolean {
        return true
    }

    @JvmStatic
    fun enableScalingRevealHomeAnimation(): Boolean {
        return true
    }
}